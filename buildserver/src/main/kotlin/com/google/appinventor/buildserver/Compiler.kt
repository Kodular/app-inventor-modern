// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2021 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0
package com.google.appinventor.buildserver

import com.android.ide.common.internal.AaptCruncher
import com.google.appinventor.buildserver.util.AARLibraries
import com.google.appinventor.buildserver.util.AARLibrary
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener
import java.awt.Graphics2D
import java.awt.Image
import java.awt.image.BufferedImage
import java.io.*
import java.util.*
import java.util.concurrent.*
import java.util.logging.Level
import java.util.logging.Logger
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import javax.imageio.ImageIO
import kotlin.math.roundToLong
import kotlin.system.exitProcess

/**
 * Main entry point for the YAIL compiler.
 *
 *
 * Supplies entry points for building Young Android projects.
 *
 * @author markf@google.com (Mark Friedman)
 * @author lizlooney@google.com (Liz Looney)
 *
 * [Will 2016/9/20, Refactored [.writeAndroidManifest] to
 * accomodate the new annotations for adding <activity> and <receiver>
 * elements.]
</receiver></activity> */
class Compiler internal constructor(
    project: Project?,
    compTypes: Set<String?>?,
    compBlocks: Map<String?, Set<String?>?>,
    out: PrintStream,
    err: PrintStream,
    userErrors: PrintStream,
    isForCompanion: Boolean,
    isForEmulator: Boolean,
    includeDangerousPermissions: Boolean,
    childProcessMaxRam: Int,
    dexCacheDir: String?,
    reporter: BuildServer.ProgressReporter?
) {
    private val assetsNeeded: ConcurrentMap<String, Set<String>> = ConcurrentHashMap()
    private val activitiesNeeded: ConcurrentMap<String, Set<String>> = ConcurrentHashMap()
    private val metadataNeeded: ConcurrentMap<String, Set<String>> = ConcurrentHashMap()
    private val activityMetadataNeeded: ConcurrentMap<String, Set<String>> = ConcurrentHashMap()
    private val broadcastReceiversNeeded: ConcurrentMap<String, Set<String>> = ConcurrentHashMap()
    private val servicesNeeded: ConcurrentMap<String, Set<String>> = ConcurrentHashMap()
    private val contentProvidersNeeded: ConcurrentMap<String, Set<String>> = ConcurrentHashMap()
    private val libsNeeded: ConcurrentMap<String, Set<String>> = ConcurrentHashMap()
    private val nativeLibsNeeded: ConcurrentMap<String, Set<String>> = ConcurrentHashMap()
    private val permissionsNeeded: ConcurrentMap<String, Set<String>> = ConcurrentHashMap()
    private val minSdksNeeded: ConcurrentMap<String, Set<String>> = ConcurrentHashMap()
    private val uniqueLibsNeeded: Set<String> = Sets.newHashSet()
    private val conditionals: ConcurrentMap<String, Map<String, Map<String, Set<String>>>> = ConcurrentHashMap()

    /**
     * Maps component type names to a set of blocks used in the project from the
     * named component. For example, Hello Purr might produce:
     *
     * `
     * {
     * "Button": {"Click", "Image", "Text"},
     * "Screen": {"Title"},
     * "Sound": {"Play", "Source", "Vibrate"}
     * }
    ` *
     */
    private val compBlocks: Map<String?, Set<String?>?>

    /**
     * Set of exploded AAR libraries.
     */
    private var explodedAarLibs: AARLibraries? = null

    /**
     * File where the compiled R resources are written.
     */
    private var appRJava: File? = null

    /**
     * The file containing the text version of the R resources map.
     */
    private var appRTxt: File? = null

    /**
     * Directory where the merged resource XML files are placed.
     */
    private var mergedResDir: File? = null

    /**
     * Zip file containing all compiled resources with AAPT2
     */
    private var resourcesZip: File? = null

    // TODO(Will): Remove the following Set once the deprecated
    //             @SimpleBroadcastReceiver annotation is removed. It should
    //             should remain for the time being because otherwise we'll break
    //             extensions currently using @SimpleBroadcastReceiver.
    private val componentBroadcastReceiver: ConcurrentMap<String, Set<String>> =
        ConcurrentHashMap()

    companion object {
        /**
         * reading guide:
         * Comp == Component, comp == component, COMP == COMPONENT
         * Ext == External, ext == external, EXT == EXTERNAL
         */
        var currentProgress = 10

        // Kawa and DX processes can use a lot of memory. We only launch one Kawa or DX process at a time.
        private val SYNC_KAWA_OR_DX: Any = Object()
        private val SLASH: String = File.separator
        private val SLASHREGEX = if (File.separatorChar == '\\') "\\\\" else "/"
        private val COLON: String = File.pathSeparator
        private const val ZIPSLASH = "/"
        const val RUNTIME_FILES_DIR = "/" + "files" + "/"

        // Native library directory names
        private const val LIBS_DIR_NAME = "libs"
        private const val ARMEABI_DIR_NAME = "armeabi"
        private const val ARMEABI_V7A_DIR_NAME = "armeabi-v7a"
        private const val ARM64_V8A_DIR_NAME = "arm64-v8a"
        private const val X86_64_DIR_NAME = "x86_64"
        private const val ASSET_DIR_NAME = "assets"
        private const val EXT_COMPS_DIR_NAME = "external_comps"
        private const val DEFAULT_APP_NAME = ""
        private const val DEFAULT_ICON = RUNTIME_FILES_DIR + "ya.png"
        private const val DEFAULT_VERSION_CODE = "1"
        private const val DEFAULT_VERSION_NAME = "1.0"
        private const val DEFAULT_MIN_SDK = "7"
        private const val DEFAULT_THEME = "AppTheme.Light.DarkActionBar"

        /*
   * Resource paths to yail runtime, runtime library files and sdk tools.
   * To get the real file paths, call getResource() with one of these constants.
   */
        private const val ACRA_RUNTIME = RUNTIME_FILES_DIR + "acra-4.4.0.jar"
        private const val ANDROID_RUNTIME = RUNTIME_FILES_DIR + "android.jar"
        private val SUPPORT_JARS: Array<String>
        private val SUPPORT_AARS: Array<String>
        private const val COMP_BUILD_INFO = RUNTIME_FILES_DIR + "simple_components_build_info.json"
        private const val DX_JAR = RUNTIME_FILES_DIR + "dx.jar"
        private const val KAWA_RUNTIME = RUNTIME_FILES_DIR + "kawa.jar"
        private const val SIMPLE_ANDROID_RUNTIME_JAR = RUNTIME_FILES_DIR + "AndroidRuntime.jar"
        private const val APKSIGNER_JAR = RUNTIME_FILES_DIR + "apksigner.jar"

        /*
   * Note for future updates: This list can be obtained from an Android Studio project running the
   * following command:
   *
   * ./gradlew :app:dependencies --configuration releaseRuntimeClasspath --console=plain | \
   *     awk 'BEGIN {FS="--- "} {print $2}' | cut -d : -f2 | sort -u
   */
        private val CRITICAL_JARS: Set<String> = setOf(
                RUNTIME_FILES_DIR + "appcompat.jar",
                RUNTIME_FILES_DIR + "collection.jar",
                RUNTIME_FILES_DIR + "core.jar",
                RUNTIME_FILES_DIR + "core-common.jar",
                RUNTIME_FILES_DIR + "lifecycle-common.jar",
                RUNTIME_FILES_DIR + "vectordrawable.jar",
                RUNTIME_FILES_DIR + "vectordrawable-animated.jar",
                RUNTIME_FILES_DIR + "annotation.jar",
                RUNTIME_FILES_DIR + "asynclayoutinflater.jar",
                RUNTIME_FILES_DIR + "coordinatorlayout.jar",
                RUNTIME_FILES_DIR + "core-runtime.jar",
                RUNTIME_FILES_DIR + "cursoradapter.jar",
                RUNTIME_FILES_DIR + "customview.jar",
                RUNTIME_FILES_DIR + "documentfile.jar",
                RUNTIME_FILES_DIR + "drawerlayout.jar",
                RUNTIME_FILES_DIR + "fragment.jar",
                RUNTIME_FILES_DIR + "interpolator.jar",
                RUNTIME_FILES_DIR + "legacy-support-core-ui.jar",
                RUNTIME_FILES_DIR + "legacy-support-core-utils.jar",
                RUNTIME_FILES_DIR + "lifecycle-livedata.jar",
                RUNTIME_FILES_DIR + "lifecycle-livedata-core.jar",
                RUNTIME_FILES_DIR + "lifecycle-runtime.jar",
                RUNTIME_FILES_DIR + "lifecycle-viewmodel.jar",
                RUNTIME_FILES_DIR + "loader.jar",
                RUNTIME_FILES_DIR + "localbroadcastmanager.jar",
                RUNTIME_FILES_DIR + "print.jar",
                RUNTIME_FILES_DIR + "slidingpanelayout.jar",
                RUNTIME_FILES_DIR + "swiperefreshlayout.jar",
                RUNTIME_FILES_DIR + "versionedparcelable.jar",
                RUNTIME_FILES_DIR + "viewpager.jar"
        )
        private const val LINUX_AAPT_TOOL = "/tools/linux/aapt"
        private const val LINUX_ZIPALIGN_TOOL = "/tools/linux/zipalign"
        private const val MAC_AAPT_TOOL = "/tools/mac/aapt"
        private const val MAC_ZIPALIGN_TOOL = "/tools/mac/zipalign"
        private const val WINDOWS_AAPT_TOOL = "/tools/windows/aapt"
        private const val WINDOWS_PTHEAD_DLL = "/tools/windows/libwinpthread-1.dll"
        private const val WINDOWS_ZIPALIGN_TOOL = "/tools/windows/zipalign"
        private const val LINUX_AAPT2_TOOL = "/tools/linux/aapt2"
        private const val MAC_AAPT2_TOOL = "/tools/mac/aapt2"
        private const val WINDOWS_AAPT2_TOOL = "/tools/windows/aapt2"
        private const val BUNDLETOOL_JAR = RUNTIME_FILES_DIR + "bundletool.jar"

        const val YAIL_RUNTIME = RUNTIME_FILES_DIR + "runtime.scm"

        /**
         * Map used to hold the names and paths of resources that we've written out
         * as temp files.
         * Don't use this map directly. Please call getResource() with one of the
         * constants above to get the (temp file) path to a resource.
         */
        private val resources: ConcurrentMap<String, File> = ConcurrentHashMap()

        // TODO(user,lizlooney): i18n here and in lines below that call String.format(...)
        private const val COMPILATION_ERROR = "Error: Your build failed due to an error when compiling %s.\n"
        private const val ERROR_IN_STAGE =
            "Error: Your build failed due to an error in the %s stage, not because of an error in your program.\n"
        private const val ICON_ERROR = "Error: Your build failed because %s cannot be used as the application icon.\n"
        private const val NO_USER_CODE_ERROR = "Error: No user code exists.\n"
        private val LOG: Logger = Logger.getLogger(Compiler::class.java.name)


        /**
         * Creates a new YAIL compiler.
         *
         * @param project  project to build
         * @param compTypes component types used in the project
         * @param compBlocks component types mapped to blocks used in project
         * @param out  stdout stream for compiler messages
         * @param err  stderr stream for compiler messages
         * @param userErrors stream to write user-visible error messages
         * @param childProcessMaxRam  maximum RAM for child processes, in MBs.
         */
        init {
            this.project = project
            this.compBlocks = compBlocks
            prepareCompTypes(compTypes)
            readBuildInfo()
            this.out = out
            this.err = err
            this.userErrors = userErrors
            this.isForCompanion = isForCompanion
            this.isForEmulator = isForEmulator
            this.includeDangerousPermissions = includeDangerousPermissions
            childProcessRamMb = childProcessMaxRam
            this.dexCacheDir = dexCacheDir
            this.reporter = reporter
        }

        /**
         * Write out a style definition customized with the given colors.
         *
         * @param out The writer the style will be written to.
         * @param name The name of the new style.
         * @param parent The parent style to inherit from.
         * @param sdk The SDK version that the theme overlays
         * @throws IOException if the writer cannot be written to.
         */
        @Throws(IOException::class)
        private fun writeTheme(out: Writer, name: String, parent: String, sdk: Int) {
            out.write("<style name=\"")
            out.write(name)
            out.write("\" parent=\"")
            out.write(parent)
            out.write("\">\n")
            out.write("<item name=\"colorPrimary\">@color/colorPrimary</item>\n")
            out.write("<item name=\"colorPrimaryDark\">@color/colorPrimaryDark</item>\n")
            out.write("<item name=\"colorAccent\">@color/colorAccent</item>\n")
            val holo = sdk in 11..20
            var needsClassicSwitch = false
            if (parent != "android:Theme") {
                out.write("<item name=\"windowActionBar\">true</item>\n")
                out.write("<item name=\"android:windowActionBar\">true</item>\n") // Honeycomb ActionBar
                if ("Holo" in parent || holo) {
                    out.write("<item name=\"android:actionBarStyle\">@style/AIActionBar</item>\n")
                    out.write("<item name=\"actionBarStyle\">@style/AIActionBar</item>\n")
                }
                // Handles theme for Notifier
                out.write("<item name=\"android:dialogTheme\">@style/AIDialog</item>\n")
                out.write("<item name=\"dialogTheme\">@style/AIDialog</item>\n")
                out.write("<item name=\"android:cacheColorHint\">#000</item>\n") // Fixes crash in ListPickerActivity
            } else {
                out.write("<item name=\"switchStyle\">@style/ClassicSwitch</item>\n")
                needsClassicSwitch = true
            }
            out.write("</style>\n")
            if (needsClassicSwitch) {
                out.write("<style name=\"ClassicSwitch\" parent=\"Widget.AppCompat.CompoundButton.Switch\">\n")
                if (sdk == 23) {
                    out.write("<item name=\"android:background\">@drawable/abc_control_background_material</item>\n")
                } else {
                    out.write("<item name=\"android:background\">@drawable/abc_item_background_holo_light</item>\n")
                }
                out.write("</style>\n")
            }
        }

        @Throws(IOException::class)
        private fun writeActionBarStyle(
            out: Writer, name: String, parent: String,
            blackText: Boolean
        ) {
            out.write("<style name=\"")
            out.write(name)
            out.write("\" parent=\"")
            out.write(parent)
            out.write("\">\n")
            out.write("<item name=\"android:background\">@color/colorPrimary</item>\n")
            out.write("<item name=\"android:titleTextStyle\">@style/AIActionBarTitle</item>\n")
            out.write("</style>\n")
            out.write("<style name=\"AIActionBarTitle\" parent=\"android:TextAppearance.Holo.Widget.ActionBar.Title\">\n")
            out.write("""<item name="android:textColor">${if (blackText) "#000" else "#fff"}</item>""")
            out.write("</style>\n")
        }

        @Throws(IOException::class)
        private fun writeDialogTheme(out: Writer, name: String, parent: String) {
            out.write("<style name=\"")
            out.write(name)
            out.write("\" parent=\"")
            out.write(parent)
            out.write("\">\n")
            out.write("<item name=\"colorPrimary\">@color/colorPrimary</item>\n")
            out.write("<item name=\"colorPrimaryDark\">@color/colorPrimaryDark</item>\n")
            out.write("<item name=\"colorAccent\">@color/colorAccent</item>\n")
            if ("Holo" in parent) {
                // workaround for weird window border effect
                out.write("<item name=\"android:windowBackground\">@android:color/transparent</item>\n")
                out.write("<item name=\"android:gravity\">center</item>\n")
                out.write("<item name=\"android:layout_gravity\">center</item>\n")
                out.write("<item name=\"android:textColor\">@color/colorPrimary</item>\n")
            }
            out.write("</style>\n")
        }

        /**
         * Builds a YAIL project.
         *
         * @param project  project to build
         * @param compTypes component types used in the project
         * @param compBlocks component type mapped to blocks used in project
         * @param out  stdout stream for compiler messages
         * @param err  stderr stream for compiler messages
         * @param userErrors stream to write user-visible error messages
         * @param keystoreFilePath
         * @param childProcessRam   maximum RAM for child processes, in MBs.
         * @return  `true` if the compilation succeeds, `false` otherwise
         * @throws JSONException
         * @throws IOException
         */
        @Throws(IOException::class, JSONException::class)
        fun compile(
            project: Project, compTypes: Set<String?>?, compBlocks: Map<String?, Set<String?>?>,
            out: PrintStream, err: PrintStream, userErrors: PrintStream,
            isForCompanion: Boolean, isForEmulator: Boolean,
            includeDangerousPermissions: Boolean, keystoreFilePath: String?,
            childProcessRam: Int, dexCacheDir: String?, outputFileName: String?,
            reporter: BuildServer.ProgressReporter?, isAab: Boolean
        ): Boolean {
            val start: Long = System.currentTimeMillis()

            // Create a new compiler instance for the compilation
            val compiler = Compiler(
                project, compTypes, compBlocks, out, err, userErrors,
                isForCompanion, isForEmulator, includeDangerousPermissions, childProcessRam, dexCacheDir,
                reporter
            )

            // Set initial progress to 0%
            reporter?.report(0)
            compiler.generateAssets()
            compiler.generateActivities()
            compiler.generateMetadata()
            compiler.generateActivityMetadata()
            compiler.generateBroadcastReceivers()
            compiler.generateServices()
            compiler.generateContentProviders()
            compiler.generateLibNames()
            compiler.generateNativeLibNames()
            compiler.generatePermissions()
            compiler.generateMinSdks()

            // TODO(Will): Remove the following call once the deprecated
            //             @SimpleBroadcastReceiver annotation is removed. It should
            //             should remain for the time being because otherwise we'll break
            //             extensions currently using @SimpleBroadcastReceiver.
            compiler.generateBroadcastReceiver()

            // Create build directory.
            val buildDir: File = createDir(project.buildDirectory)

            // Prepare application icon.
            out.println("________Preparing application icon")
            val resDir: File = createDir(buildDir, "res")
            val drawableDir: File = createDir(resDir, "drawable")

            // Create mipmap directories
            val mipmapV26: File = createDir(resDir, "mipmap-anydpi-v26")
            val mipmapHdpi: File = createDir(resDir, "mipmap-hdpi")
            val mipmapMdpi: File = createDir(resDir, "mipmap-mdpi")
            val mipmapXhdpi: File = createDir(resDir, "mipmap-xhdpi")
            val mipmapXxhdpi: File = createDir(resDir, "mipmap-xxhdpi")
            val mipmapXxxhdpi: File = createDir(resDir, "mipmap-xxxhdpi")

            // Create list of mipmaps for all icon types with respective sizes
            val mipmapDirectoriesForIcons: List<File> =
                listOf(mipmapMdpi, mipmapHdpi, mipmapXhdpi, mipmapXxhdpi, mipmapXxxhdpi)
            val standardICSizesForMipmaps: MutableList<Int> = mutableListOf(48, 72, 96, 144, 192)
            val foregroundICSizesForMipmaps: MutableList<Int> = mutableListOf(108, 162, 216, 324, 432)
            if (!compiler.prepareApplicationIcon(
                    File(drawableDir, "ya.png"),
                    mipmapDirectoriesForIcons,
                    standardICSizesForMipmaps,
                    foregroundICSizesForMipmaps
                )
            ) {
                return false
            }
            reporter?.report(15) // Static context

            // Create anim directory and animation xml files
            out.println("________Creating animation xml")
            val animDir: File = createDir(resDir, "anim")
            if (!compiler.createAnimationXml(animDir)) {
                return false
            }

            // Create values directory and style xml files
            out.println("________Creating style xml")
            val styleDir: File = createDir(resDir, "values")
            val style11Dir: File = createDir(resDir, "values-v11")
            val style14Dir: File = createDir(resDir, "values-v14")
            val style21Dir: File = createDir(resDir, "values-v21")
            val style23Dir: File = createDir(resDir, "values-v23")
            if (!compiler.createValuesXml(styleDir, "") ||
                !compiler.createValuesXml(style11Dir, "-v11") ||
                !compiler.createValuesXml(style14Dir, "-v14") ||
                !compiler.createValuesXml(style21Dir, "-v21") ||
                !compiler.createValuesXml(style23Dir, "-v23")
            ) {
                return false
            }
            out.println("________Creating provider_path xml")
            val providerDir: File = createDir(resDir, "xml")
            if (!compiler.createProviderXml(providerDir)) {
                return false
            }
            out.println("________Creating network_security_config xml")
            if (!compiler.createNetworkConfigXml(providerDir)) {
                return false
            }

            // Generate ic_launcher.xml
            out.println("________Generating adaptive icon file")
            val icLauncher = File(mipmapV26, "ic_launcher.xml")
            if (!compiler.writeICLauncher(icLauncher, false)) {
                return false
            }

            // Generate ic_launcher_round.xml
            out.println("________Generating round adaptive icon file")
            val icLauncherRound = File(mipmapV26, "ic_launcher_round.xml")
            if (!compiler.writeICLauncher(icLauncherRound, true)) {
                return false
            }

            // Generate ic_launcher_background.xml
            out.println("________Generating adaptive icon background file")
            val icBackgroundColor = File(styleDir, "ic_launcher_background.xml")
            if (!compiler.writeICLauncherBackground(icBackgroundColor)) {
                return false
            }

            // Generate AndroidManifest.xml
            out.println("________Generating manifest file")
            val manifestFile = File(buildDir, "AndroidManifest.xml")
            if (!compiler.writeAndroidManifest(manifestFile)) {
                return false
            }
            reporter?.report(20)

            // Insert native libraries
            out.println("________Attaching native libraries")
            if (!compiler.insertNativeLibs(buildDir)) {
                return false
            }

            // Attach Android AAR Library dependencies
            out.println("________Attaching Android Archive (AAR) libraries")
            if (!compiler.attachAarLibraries(buildDir)) {
                return false
            }

            // Add raw assets to sub-directory of project assets.
            out.println("________Attaching component assets")
            if (!compiler.attachCompAssets()) {
                return false
            }

            // Invoke aapt to package everything up
            out.println("________Invoking AAPT")
            val deployDir: File = createDir(buildDir, "deploy")
            val tmpPackageName: String =
                "${deployDir.absolutePath}$SLASH${project.projectName.toString()}.${if (isAab) "apk" else "ap_"}"
            val srcJavaDir: File = createDir(buildDir, "generated/src")
            val rJavaDir: File = createDir(buildDir, "generated/symbols")
            if (isAab) {
                if (!compiler.runAapt2Compile(resDir)) {
                    return false
                }
                if (!compiler.runAapt2Link(manifestFile, tmpPackageName, rJavaDir)) {
                    return false
                }
            } else {
                if (!compiler.runAaptPackage(manifestFile, resDir, tmpPackageName, srcJavaDir, rJavaDir)) {
                    return false
                }
            }
            reporter?.report(30)

            // Create class files.
            out.println("________Compiling source files")
            val classesDir: File = createDir(buildDir, "classes")
            val tmpDir: File = createDir(buildDir, "tmp")
            val dexedClassesDir: String = tmpDir.absolutePath
            if (!compiler.generateRClasses(classesDir)) {
                return false
            }
            if (!compiler.generateClasses(classesDir)) {
                return false
            }
            reporter?.report(35)

            // Invoke dx on class files
            out.println("________Invoking DX")
            // TODO(markf): Running DX is now pretty slow (~25 sec overhead the first time and ~15 sec
            // overhead for subsequent runs).  I think it's because of the need to dx the entire
            // kawa runtime every time.  We should probably only do that once and then copy all the
            // kawa runtime dx files into the generated classes.dex (which would only contain the
            // files compiled for this project).
            // Aargh.  It turns out that there's no way to manipulate .dex files to do the above.  An
            // Android guy suggested an alternate approach of shipping the kawa runtime .dex file as
            // data with the application and then creating a new DexClassLoader using that .dex file
            // and with the original app class loader as the parent of the new one.
            // TODONE(zhuowei): Now using the new Android DX tool to merge dex files
            // Needs to specify a writable cache dir on the command line that persists after shutdown
            // Each pre-dexed file is identified via its MD5 hash (since the standard Android SDK's
            // method of identifying via a hash of the path won't work when files
            // are copied into temporary storage) and processed via a hacked up version of
            // Android SDK's Dex Ant task
            if (!compiler.runMultidex(classesDir, dexedClassesDir)) {
                return false
            }
            reporter?.report(85)
            if (isAab) {
                if (!compiler.bundleTool(
                        buildDir,
                        childProcessRam,
                        tmpPackageName,
                        outputFileName,
                        deployDir,
                        keystoreFilePath,
                        dexedClassesDir
                    )
                ) {
                    return false
                }
            } else {
                // Seal the apk with ApkBuilder
                out.println("________Invoking ApkBuilder")
                var fileName = outputFileName ?: "${project.projectName}.apk"
                val apkAbsolutePath: String = deployDir.absolutePath + SLASH + fileName
                if (!compiler.runApkBuilder(apkAbsolutePath, tmpPackageName, dexedClassesDir)) {
                    return false
                }
                reporter?.report(95)

                // ZipAlign the apk file
                out.println("________ZipAligning the apk file")
                if (!compiler.runZipAlign(apkAbsolutePath, tmpDir)) {
                    return false
                }

                // Sign the apk file
                out.println("________Signing the apk file")
                if (!compiler.runApkSigner(apkAbsolutePath, keystoreFilePath)) {
                    return false
                }
            }
            reporter?.report(100)
            out.println("Build finished in ${((System.currentTimeMillis() - start) / 1000.0)} seconds")
            return true
        }

        /**
         * Writes out the given resource as a temp file and returns the absolute path.
         * Caches the location of the files, so we can reuse them.
         *
         * @param resourcePath the name of the resource
         */
        @Synchronized
        fun getResource(resourcePath: String): String {
            return try {
                var file: File? = resources[resourcePath]
                if (file == null) {
                    val basename: String = PathUtil.basename(resourcePath)
                    var prefix: String
                    val suffix: String
                    val lastDot: Int = basename.lastIndexOf(".")
                    if (lastDot != -1) {
                        prefix = basename.substring(0, lastDot)
                        suffix = basename.substring(lastDot)
                    } else {
                        prefix = basename
                        suffix = ""
                    }
                    while (prefix.length < 3) {
                        prefix += "_"
                    }
                    file = File.createTempFile(prefix, suffix)
                    file.setExecutable(true)
                    file.deleteOnExit()
                    file.parentFile.mkdirs()
                    Files.copy(
                        Resources.newInputStreamSupplier(Compiler::class.java.getResource(resourcePath)),
                        file
                    )
                    resources[resourcePath] = file
                }
                file.absolutePath
            } catch (e: NullPointerException) {
                throw IllegalStateException("Unable to find required library: $resourcePath", e)
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
        }

        /**
         * Copy one file to another. If destination file does not exist, it is created.
         *
         * @param srcPath absolute path to source file
         * @param dstPath absolute path to destination file
         * @return  `true` if the copy succeeds, `false` otherwise
         */
        private fun copyFile(srcPath: String, dstPath: String): Boolean {
            try {
                val `in` = FileInputStream(srcPath)
                val out = FileOutputStream(dstPath)
                val buf = ByteArray(1024)
                var len: Int
                while (`in`.read(buf).also { len = it } > 0) {
                    out.write(buf, 0, len)
                }
                `in`.close()
                out.close()
            } catch (e: IOException) {
                e.printStackTrace()
                return false
            }
            return true
        }

        /**
         * Creates a new directory (if it doesn't exist already).
         *
         * @param dir  new directory
         * @return  new directory
         */
        private fun createDir(dir: File): File {
            if (!dir.exists()) {
                dir.mkdir()
            }
            return dir
        }

        /**
         * Creates a new directory (if it doesn't exist already).
         *
         * @param parentDir  parent directory of new directory
         * @param name  name of new directory
         * @return  new directory
         */
        private fun createDir(parentDir: File?, name: String): File {
            val dir = File(parentDir, name)
            if (!dir.exists()) {
                dir.mkdir()
            }
            return dir
        }

        private fun basename(path: String): String = File(path).name

        private fun getExtAssetPath(extCompDir: String?, assetName: String): String {
            return extCompDir + File.separator + ASSET_DIR_NAME + File.separator + assetName
        }

        init {
            val aars = mutableListOf<String>()
            val jars = mutableListOf<String>()
            try {
                val res = Compiler::class.java.getResourceAsStream("${RUNTIME_FILES_DIR}aars.txt")
                BufferedReader(InputStreamReader(res))
                    .forEachLine { line ->
                        aars.add(line)
                    }
            } catch (e: IOException) {
                System.err.println("Fatal error on startup reading aars.txt")
                e.printStackTrace()
                exitProcess(1)
            }
            SUPPORT_AARS = aars.toTypedArray()
            try {
                val res = Compiler::class.java.getResourceAsStream("${RUNTIME_FILES_DIR}jars.txt")
                BufferedReader(InputStreamReader(res))
                    .forEachLine { line ->
                        jars.add(line)
                    }
            } catch (e: IOException) {
                System.err.println("Fatal error on startup reading jars.txt")
                e.printStackTrace()
                exitProcess(1)
            }
            SUPPORT_JARS = jars.toTypedArray()
        }
    }

    private val childProcessRamMb: Int // Maximum ram that can be used by a child processes, in MB.
    private val isForCompanion: Boolean
    private val isForEmulator: Boolean
    private val includeDangerousPermissions: Boolean
    private val project: Project?
    private val out: PrintStream
    private val err: PrintStream
    private val userErrors: PrintStream
    private var libsDir: File? = null // The directory that will contain any native libraries for packaging
    private val dexCacheDir: String?
    private var simpleCompsBuildInfo: JSONArray? = null
    private var extCompsBuildInfo: JSONArray? = null
    private var simpleCompTypes: Set<String>? = null // types needed by the project
    private var extCompTypes: Set<String>? = null // types needed by the project

    /**
     * A list of the dex files created by [.runMultidex].
     */
    private val dexFiles: List<File> = ArrayList()

    /**
     * Mapping from type name to path in project to minimize tests against the file system.
     */
    private val extTypePathCache: Map<String, String> = HashMap()
    private val reporter: BuildServer.ProgressReporter? // Used to report progress of the build

    /*
   * Generate the set of Android permissions needed by this project.
   */
    fun generatePermissions() {
        try {
            loadJsonInfo(permissionsNeeded, ComponentDescriptorConstants.PERMISSIONS_TARGET)
            if (project != null) {    // Only do this if we have a project (testing doesn't provide one :-( ).
                LOG.log(Level.INFO, "usesLocation = " + project.usesLocation)
                if (project.usesLocation == "True") { // Add location permissions if any WebViewer requests it
                    val locationPermissions: Set<String> = Sets.newHashSet() // via a Property.
                    // See ProjectEditor.recordLocationSettings()
                    locationPermissions.add("android.permission.ACCESS_FINE_LOCATION")
                    locationPermissions.add("android.permission.ACCESS_COARSE_LOCATION")
                    locationPermissions.add("android.permission.ACCESS_MOCK_LOCATION")
                    permissionsNeeded["com.google.appinventor.components.runtime.WebViewer"] = locationPermissions
                }
            }
        } catch (e: IOException) {
            // This is fatal.
            e.printStackTrace()
            userErrors.print(String.format(ERROR_IN_STAGE, "Permissions"))
        } catch (e: JSONException) {
            // This is fatal, but shouldn't actually ever happen.
            e.printStackTrace()
            userErrors.print(String.format(ERROR_IN_STAGE, "Permissions"))
        }
        mergeConditionals(conditionals[ComponentDescriptorConstants.PERMISSIONS_TARGET], permissionsNeeded)
        var n = 0
        for (type in permissionsNeeded.keySet()) {
            n += permissionsNeeded[type].size
        }
        println("Permissions needed, n = $n")
    }

    /**
     * Merge the given `values` into the set at `key` in `map`.
     * If `key` is not set, then its value is treated as the empty set and
     * the key is set to a copy of `values`. `values` can be unmodifiable.
     * @param map A mapping of strings to sets of strings, representing component
     * types to, e.g., permissions
     * @param key The key to evaluate, e.g., "Texting"
     * @param values The values associated with the key that need to be merged, e.g.,
     * {"android.permission.SEND_SMS"}
     */
    private fun setOrMerge(map: Map<String, Set<String>>, key: String, values: Set<String>) {
        if (map.containsKey(key)) {
            map[key].addAll(values)
        } else {
            map.put(key, HashSet(values))
        }
    }

    /**
     * Merge the conditionals from the given conditional map into the existing
     * map of required infos.
     * @param conditionalMap A map of component type names to maps of blocks to
     * sets of values (e.g., permission names)
     * @param infoMap A map of component type names to sets of values (e.g.,
     * permission names)
     */
    private fun mergeConditionals(
        conditionalMap: Map<String, Map<String, Set<String>>>?,
        infoMap: Map<String, Set<String>>
    ) {
        if (conditionalMap != null) {
            if (isForCompanion) {
                // For the companion, we take all of the conditionals
                for (entry in conditionalMap.entrySet()) {
                    for (items in entry.getValue().values()) {
                        setOrMerge(infoMap, entry.getKey(), items)
                    }
                }
                // If necessary, we can remove permissions at this point (e.g., Texting, PhoneCall)
            } else {
                // We walk the set of components and the blocks used in the project. If
                // any <component, block> combination is in the set of conditionals,
                // then we merge the associated set of values into the existing set. If
                // no existing set exists, we create one.
                for (entry in compBlocks.entrySet()) {
                    if (conditionalMap.containsKey(entry.getKey())) {
                        val blockPermsMap = conditionalMap[entry.getKey()]!!
                        for (blockName in entry.getValue()) {
                            val blockPerms = blockPermsMap[blockName]
                            if (blockPerms != null) {
                                val typePerms = infoMap[entry.getKey()]
                                if (typePerms != null) {
                                    typePerms.addAll(blockPerms)
                                } else {
                                    infoMap.put(entry.getKey(), HashSet(blockPerms))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Just used for testing
    val permissions: Map<String, Set<String>>
        get() = permissionsNeeded

    // Just used for testing
    val broadcastReceivers: Map<String, Set<String>>
        get() = broadcastReceiversNeeded

    // Just used for testing
    val services: Map<String, Set<String>>
        get() = servicesNeeded

    // Just used for testing
    val contentProviders: Map<String, Set<String>>
        get() = contentProvidersNeeded

    // Just used for testing
    val activities: Map<String, Set<String>>
        get() = activitiesNeeded

    /*
   * Generate the set of Android libraries needed by this project.
   */
    fun generateLibNames() {
        try {
            loadJsonInfo(libsNeeded, ComponentDescriptorConstants.LIBRARIES_TARGET)
        } catch (e: IOException) {
            // This is fatal.
            e.printStackTrace()
            userErrors.print(String.format(ERROR_IN_STAGE, "Libraries"))
        } catch (e: JSONException) {
            // This is fatal, but shouldn't actually ever happen.
            e.printStackTrace()
            userErrors.print(String.format(ERROR_IN_STAGE, "Libraries"))
        }
        var n = 0
        for (type in libsNeeded.keySet()) {
            n += libsNeeded[type].size()
        }
        println("Libraries needed, n = $n")
    }

    /*
   * Generate the set of conditionally included libraries needed by this project.
   */
    fun generateNativeLibNames() {
        if (isForEmulator) {  // no libraries for emulator
            return
        }
        try {
            loadJsonInfo(nativeLibsNeeded, ComponentDescriptorConstants.NATIVE_TARGET)
        } catch (e: IOException) {
            // This is fatal.
            e.printStackTrace()
            userErrors.print(String.format(ERROR_IN_STAGE, "Native Libraries"))
        } catch (e: JSONException) {
            // This is fatal, but shouldn't actually ever happen.
            e.printStackTrace()
            userErrors.print(String.format(ERROR_IN_STAGE, "Native Libraries"))
        }
        var n = 0
        for (type in nativeLibsNeeded.keySet()) {
            n += nativeLibsNeeded[type].size()
        }
        println("Native Libraries needed, n = $n")
    }

    /*
   * Generate the set of conditionally included assets needed by this project.
   */
    fun generateAssets() {
        try {
            loadJsonInfo(assetsNeeded, ComponentDescriptorConstants.ASSETS_TARGET)
        } catch (e: IOException) {
            // This is fatal.
            e.printStackTrace()
            userErrors.print(String.format(ERROR_IN_STAGE, "Assets"))
        } catch (e: JSONException) {
            // This is fatal, but shouldn't actually ever happen.
            e.printStackTrace()
            userErrors.print(String.format(ERROR_IN_STAGE, "Assets"))
        }
        var n = 0
        for (type in assetsNeeded.keySet()) {
            n += assetsNeeded[type].size()
        }
        println("Component assets needed, n = $n")
    }

    /*
   * Generate the set of conditionally included activities needed by this project.
   */
    fun generateActivities() {
        try {
            loadJsonInfo(activitiesNeeded, ComponentDescriptorConstants.ACTIVITIES_TARGET)
        } catch (e: IOException) {
            // This is fatal.
            e.printStackTrace()
            userErrors.print(String.format(ERROR_IN_STAGE, "Activities"))
        } catch (e: JSONException) {
            // This is fatal, but shouldn't actually ever happen.
            e.printStackTrace()
            userErrors.print(String.format(ERROR_IN_STAGE, "Activities"))
        }
        var n = 0
        for (type in activitiesNeeded.keySet()) {
            n += activitiesNeeded[type].size()
        }
        println("Component activities needed, n = $n")
    }

    /**
     * Generate a set of conditionally included metadata needed by this project.
     */
    fun generateMetadata() {
        try {
            loadJsonInfo(metadataNeeded, ComponentDescriptorConstants.METADATA_TARGET)
        } catch (e: IOException) {
            // This is fatal.
            e.printStackTrace()
            userErrors.print(String.format(ERROR_IN_STAGE, "Metadata"))
        } catch (e: JSONException) {
            // This is fatal, but shouldn't actually ever happen.
            e.printStackTrace()
            userErrors.print(String.format(ERROR_IN_STAGE, "Metadata"))
        }
        var n = 0
        for (type in metadataNeeded.keySet()) {
            n += metadataNeeded[type].size()
        }
        println("Component metadata needed, n = $n")
    }

    /**
     * Generate a set of conditionally included activity metadata needed by this project.
     */
    fun generateActivityMetadata() {
        try {
            loadJsonInfo(activityMetadataNeeded, ComponentDescriptorConstants.ACTIVITY_METADATA_TARGET)
        } catch (e: IOException) {
            // This is fatal.
            e.printStackTrace()
            userErrors.print(String.format(ERROR_IN_STAGE, "Activity Metadata"))
        } catch (e: JSONException) {
            // This is fatal, but shouldn't actually ever happen.
            e.printStackTrace()
            userErrors.print(String.format(ERROR_IN_STAGE, "Activity Metadata"))
        }
        var n = 0
        for (type in activityMetadataNeeded.keySet()) {
            n += activityMetadataNeeded[type].size()
        }
        println("Component metadata needed, n = $n")
    }

    /*
   * Generate a set of conditionally included broadcast receivers needed by this project.
   */
    fun generateBroadcastReceivers() {
        try {
            loadJsonInfo(broadcastReceiversNeeded, ComponentDescriptorConstants.BROADCAST_RECEIVERS_TARGET)
        } catch (e: IOException) {
            // This is fatal.
            e.printStackTrace()
            userErrors.print(String.format(ERROR_IN_STAGE, "BroadcastReceivers"))
        } catch (e: JSONException) {
            // This is fatal, but shouldn't actually ever happen.
            e.printStackTrace()
            userErrors.print(String.format(ERROR_IN_STAGE, "BroadcastReceivers"))
        }
        mergeConditionals(
            conditionals[ComponentDescriptorConstants.BROADCAST_RECEIVERS_TARGET],
            broadcastReceiversNeeded
        )
    }

    /*
   * Generate a set of conditionally included services needed by this project.
   */
    fun generateServices() {
        try {
            loadJsonInfo(servicesNeeded, ComponentDescriptorConstants.SERVICES_TARGET)
        } catch (e: IOException) {
            // This is fatal.
            e.printStackTrace()
            userErrors.print(String.format(ERROR_IN_STAGE, "Services"))
        } catch (e: JSONException) {
            // This is fatal, but shouldn't actually ever happen.
            e.printStackTrace()
            userErrors.print(String.format(ERROR_IN_STAGE, "Services"))
        }
        mergeConditionals(conditionals[ComponentDescriptorConstants.SERVICES_TARGET], servicesNeeded)
    }

    /*
   * Generate a set of conditionally included content providers needed by this project.
   */
    fun generateContentProviders() {
        try {
            loadJsonInfo(contentProvidersNeeded, ComponentDescriptorConstants.CONTENT_PROVIDERS_TARGET)
        } catch (e: IOException) {
            // This is fatal.
            e.printStackTrace()
            userErrors.print(String.format(ERROR_IN_STAGE, "Content Providers"))
        } catch (e: JSONException) {
            // This is fatal, but shouldn't actually ever happen.
            e.printStackTrace()
            userErrors.print(String.format(ERROR_IN_STAGE, "Content Providers"))
        }
        mergeConditionals(
            conditionals[ComponentDescriptorConstants.CONTENT_PROVIDERS_TARGET],
            contentProvidersNeeded
        )
    }

    /*
   * TODO(Will): Remove this method once the deprecated @SimpleBroadcastReceiver
   *             annotation is removed. This should remain for the time being so
   *             that we don't break extensions currently using the
   *             @SimpleBroadcastReceiver annotation.
   */
    fun generateBroadcastReceiver() {
        try {
            loadJsonInfo(componentBroadcastReceiver, ComponentDescriptorConstants.BROADCAST_RECEIVER_TARGET)
        } catch (e: IOException) {
            // This is fatal.
            e.printStackTrace()
            userErrors.print(String.format(ERROR_IN_STAGE, "BroadcastReceiver"))
        } catch (e: JSONException) {
            // This is fatal, but shouldn't actually ever happen.
            e.printStackTrace()
            userErrors.print(String.format(ERROR_IN_STAGE, "BroadcastReceiver"))
        }
    }

    private fun generateMinSdks() {
        try {
            loadJsonInfo(minSdksNeeded, ComponentDescriptorConstants.ANDROIDMINSDK_TARGET)
        } catch (e: IOException) {
            // This is fatal.
            e.printStackTrace()
            userErrors.print(String.format(ERROR_IN_STAGE, "AndroidMinSDK"))
        } catch (e: JSONException) {
            e.printStackTrace()
            userErrors.print(String.format(ERROR_IN_STAGE, "AndroidMinSDK"))
        }
    }

    // This patches around a bug in AAPT (and other placed in Android)
    // where an ampersand in the name string breaks AAPT.
    private fun cleanName(name: String?): String {
        return name.replace("&", "and")
    }

    private fun cleanColor(color: String, makeOpaque: Boolean): String {
        var result = color
        if (color.startsWith("&H") || color.startsWith("&h")) {
            result = "#" + color.substring(2)
        }
        if (makeOpaque && result.length() === 9) {  // true for #AARRGGBB strings
            result = "#" + result.substring(3) // remove any alpha value
        }
        return result
    }

    /**
     * Create the default color and styling for the app.
     */
    private fun createValuesXml(valuesDir: File, suffix: String): Boolean {
        var colorPrimary = if (project.getPrimaryColor() == null) "#A5CF47" else project.getPrimaryColor()
        var colorPrimaryDark = if (project.getPrimaryColorDark() == null) "#41521C" else project.getPrimaryColorDark()
        var colorAccent = if (project.getAccentColor() == null) "#00728A" else project.getAccentColor()
        val theme = if (project.getTheme() == null) "Classic" else project.getTheme()
        val actionbar: String = project.getActionBar()
        var parentTheme: String
        val isClassicTheme = "Classic" == theme || suffix.isEmpty() // Default to classic theme prior to SDK 11
        var needsBlackTitleText = false
        val holo = "-v11" == suffix || "-v14" == suffix
        if (isClassicTheme) {
            parentTheme = "android:Theme"
        } else {
            if (suffix == "-v11") {  // AppCompat needs SDK 14, so we explicitly name Holo for SDK 11 through 13
                parentTheme = theme.replace("AppTheme", "android:Theme.Holo")
                needsBlackTitleText = theme.contains("Light") && !theme.contains("DarkActionBar")
                if (theme.contains("Light")) {
                    parentTheme = "android:Theme.Holo.Light"
                }
            } else {
                parentTheme = theme.replace("AppTheme", "Theme.AppCompat")
            }
            if (!"true".equalsIgnoreCase(actionbar)) {
                if (parentTheme.endsWith("DarkActionBar")) {
                    parentTheme = parentTheme.replace("DarkActionBar", "NoActionBar")
                } else {
                    parentTheme += ".NoActionBar"
                }
            }
        }
        colorPrimary = cleanColor(colorPrimary, true)
        colorPrimaryDark = cleanColor(colorPrimaryDark, true)
        colorAccent = cleanColor(colorAccent, true)
        val colorsXml = File(valuesDir, "colors$suffix.xml")
        val stylesXml = File(valuesDir, "styles$suffix.xml")
        try {
            var out = BufferedWriter(OutputStreamWriter(FileOutputStream(colorsXml), "UTF-8"))
            out.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")
            out.write("<resources>\n")
            out.write("<color name=\"colorPrimary\">")
            out.write(colorPrimary)
            out.write("</color>\n")
            out.write("<color name=\"colorPrimaryDark\">")
            out.write(colorPrimaryDark)
            out.write("</color>\n")
            out.write("<color name=\"colorAccent\">")
            out.write(colorAccent)
            out.write("</color>\n")
            out.write("</resources>\n")
            out.close()
            out = BufferedWriter(OutputStreamWriter(FileOutputStream(stylesXml), "UTF-8"))
            out.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")
            out.write("<resources>\n")
            writeTheme(
                out, "AppTheme", parentTheme,
                if (suffix.isEmpty()) 7 else Integer.parseInt(suffix.substring(2))
            )
            if (!isClassicTheme) {
                if (holo) {  // Handle Holo
                    if (parentTheme.contains("Light")) {
                        writeActionBarStyle(
                            out,
                            "AIActionBar",
                            "android:Widget.Holo.Light.ActionBar",
                            needsBlackTitleText
                        )
                    } else {
                        writeActionBarStyle(out, "AIActionBar", "android:Widget.Holo.ActionBar", needsBlackTitleText)
                    }
                }
                if (parentTheme.contains("Light")) {
                    writeDialogTheme(out, "AIDialog", "Theme.AppCompat.Light.Dialog")
                    writeDialogTheme(out, "AIAlertDialog", "Theme.AppCompat.Light.Dialog.Alert")
                } else {
                    writeDialogTheme(out, "AIDialog", "Theme.AppCompat.Dialog")
                    writeDialogTheme(out, "AIAlertDialog", "Theme.AppCompat.Dialog.Alert")
                }
            }
            out.write("<style name=\"TextAppearance.AppCompat.Button\">\n")
            out.write("<item name=\"textAllCaps\">false</item>\n")
            out.write("</style>\n")
            out.write("</resources>\n")
            out.close()
        } catch (e: IOException) {
            return false
        }
        return true
    }

    private fun createNetworkConfigXml(configDir: File): Boolean {
        val networkConfig = File(configDir, "network_security_config.xml")
        try {
            PrintWriter(OutputStreamWriter(FileOutputStream(networkConfig))).use { out ->
                out.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>")
                out.println("<network-security-config>")
                out.println("<base-config cleartextTrafficPermitted=\"true\">")
                out.println("<trust-anchors>")
                out.println("<certificates src=\"system\"/>")
                out.println("</trust-anchors>")
                out.println("</base-config>")
                out.println("</network-security-config>")
            }
        } catch (e: IOException) {
            return false
        }
        return true
    }

    /*
   * Creates the provider_paths file which is used to setup a "Files" content
   * provider.
   */
    private fun createProviderXml(providerDir: File): Boolean {
        val paths = File(providerDir, "provider_paths.xml")
        try {
            val out = BufferedWriter(OutputStreamWriter(FileOutputStream(paths), "UTF-8"))
            out.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")
            out.write("<paths xmlns:android=\"http://schemas.android.com/apk/res/android\">\n")
            out.write("   <external-path name=\"external_files\" path=\".\"/>\n")
            out.write("</paths>\n")
            out.close()
        } catch (e: IOException) {
            return false
        }
        return true
    }

    // Writes ic_launcher.xml to initialize adaptive icon
    private fun writeICLauncher(adaptiveIconFile: File, isRound: Boolean): Boolean {
        val mainClass: String = project.getMainClass()
        val packageName: String = Signatures.getPackageName(mainClass)
        try {
            val out = BufferedWriter(OutputStreamWriter(FileOutputStream(adaptiveIconFile), "UTF-8"))
            out.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")
            out.write("""<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android" >""")
            out.write("<background android:drawable=\"@color/ic_launcher_background\" />\n")
            out.write("<foreground android:drawable=\"@mipmap/ic_launcher_foreground\" />\n")
            out.write("</adaptive-icon>\n")
            out.close()
        } catch (e: IOException) {
            e.printStackTrace()
            userErrors.print(String.format(ERROR_IN_STAGE, "ic launcher"))
            return false
        }
        return true
    }

    // Writes ic_launcher_background.xml to indicate background color of adaptive icon
    private fun writeICLauncherBackground(icBackgroundFile: File): Boolean {
        try {
            val out = BufferedWriter(OutputStreamWriter(FileOutputStream(icBackgroundFile), "UTF-8"))
            out.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")
            out.write("<resources>\n")
            out.write("<color name=\"ic_launcher_background\">#ffffff</color>\n")
            out.write("</resources>\n")
            out.close()
        } catch (e: IOException) {
            e.printStackTrace()
            userErrors.print(String.format(ERROR_IN_STAGE, "ic launcher background"))
            return false
        }
        return true
    }

    /*
   * Creates an AndroidManifest.xml file needed for the Android application.
   */
    private fun writeAndroidManifest(manifestFile: File): Boolean {
        // Create AndroidManifest.xml
        val mainClass: String = project.getMainClass()
        val packageName: String = Signatures.getPackageName(mainClass)
        val className: String = Signatures.getClassName(mainClass)
        val projectName: String = project.getProjectName()
        val vCode = if (project.getVCode() == null) DEFAULT_VERSION_CODE else project.getVCode()
        var vName = if (project.getVName() == null) DEFAULT_VERSION_NAME else cleanName(project.getVName())
        if (includeDangerousPermissions) {
            vName += "u"
        }
        val aName = if (project.getAName() == null) DEFAULT_APP_NAME else cleanName(project.getAName())
        LOG.log(Level.INFO, "VCode: " + project.getVCode())
        LOG.log(Level.INFO, "VName: " + project.getVName())

        // TODO(user): Use com.google.common.xml.XmlWriter
        try {
            val out = BufferedWriter(OutputStreamWriter(FileOutputStream(manifestFile), "UTF-8"))
            out.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")
            // TODO(markf) Allow users to set versionCode and versionName attributes.
            // See http://developer.android.com/guide/publishing/publishing.html for
            // more info.
            out.write("""<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="$packageName" android:versionCode="$vCode" android:versionName="$vName" >""")

            // If we are building the Wireless Debugger (AppInventorDebugger) add the uses-feature tag which
            // is used by the Google Play store to determine which devices the app is available for. By adding
            // these lines we indicate that we use these features BUT THAT THEY ARE NOT REQUIRED so it is ok
            // to make the app available on devices that lack the feature. Without these lines the Play Store
            // makes a guess based on permissions and assumes that they are required features.
            if (isForCompanion) {
                out.write("  <uses-feature android:name=\"android.hardware.bluetooth\" android:required=\"false\" />\n")
                out.write("  <uses-feature android:name=\"android.hardware.location\" android:required=\"false\" />\n")
                out.write("  <uses-feature android:name=\"android.hardware.telephony\" android:required=\"false\" />\n")
                out.write("  <uses-feature android:name=\"android.hardware.location.network\" android:required=\"false\" />\n")
                out.write("  <uses-feature android:name=\"android.hardware.location.gps\" android:required=\"false\" />\n")
                out.write("  <uses-feature android:name=\"android.hardware.microphone\" android:required=\"false\" />\n")
                out.write("  <uses-feature android:name=\"android.hardware.touchscreen\" android:required=\"false\" />\n")
                out.write("  <uses-feature android:name=\"android.hardware.camera\" android:required=\"false\" />\n")
                out.write("  <uses-feature android:name=\"android.hardware.camera.autofocus\" android:required=\"false\" />\n")
                if (isForEmulator) {
                    out.write("  <uses-feature android:name=\"android.hardware.wifi\" android:required=\"false\" />\n") // We actually require wifi
                } else {
                    out.write("  <uses-feature android:name=\"android.hardware.wifi\" />\n") // We actually require wifi
                }
            }
            var minSdk: Int =
                Integer.parseInt(if (project.getMinSdk() == null) DEFAULT_MIN_SDK else project.getMinSdk())
            if (!isForCompanion) {
                for (minSdks in minSdksNeeded.values()) {
                    for (sdk in minSdks) {
                        val sdkInt: Int = Integer.parseInt(sdk)
                        if (sdkInt > minSdk) {
                            minSdk = sdkInt
                        }
                    }
                }
            }

            // make permissions unique by putting them in one set
            val permissions: Set<String> = Sets.newHashSet()
            for (compPermissions in permissionsNeeded.values()) {
                permissions.addAll(compPermissions)
            }

            // Remove Google's Forbidden Permissions
            // This code is crude because we had to do this on short notice
            // List of permissions taken from
            // https://support.google.com/googleplay/android-developer/answer/9047303#intended
            if (isForCompanion && !includeDangerousPermissions) {
                // Default SMS handler
                permissions.remove("android.permission.READ_SMS")
                permissions.remove("android.permission.RECEIVE_MMS")
                permissions.remove("android.permission.RECEIVE_SMS")
                permissions.remove("android.permission.RECEIVE_WAP_PUSH")
                permissions.remove("android.permission.SEND_SMS")
                permissions.remove("android.permission.WRITE_SMS")
                // Default Phone handler
                permissions.remove("android.permission.PROCESS_OUTGOING_CALLS")
                permissions.remove("android.permission.CALL_PHONE")
                permissions.remove("android.permission.READ_CALL_LOG")
                permissions.remove("android.permission.WRITE_CALL_LOG")
            }
            for (permission in permissions) {
                out.write(
                    "  <uses-permission android:name=\"" +
                            permission
                                .replace(
                                    "%packageName%",
                                    packageName
                                ) // replace %packageName% with the actual packageName
                                .toString() + "\" />\n"
                )
            }
            if (isForCompanion) {      // This is so ACRA can do a logcat on phones older then Jelly Bean
                out.write("  <uses-permission android:name=\"android.permission.READ_LOGS\" />\n")
            }

            // TODO(markf): Change the minSdkVersion below if we ever require an SDK beyond 1.5.
            // The market will use the following to filter apps shown to devices that don't support
            // the specified SDK version.  We right now support building for minSDK 4.
            // We might also want to allow users to specify minSdk version or targetSDK version.
            out.write(
                """  <uses-sdk android:minSdkVersion="$minSdk" android:targetSdkVersion="${YaVersion.TARGET_SDK_VERSION}" />
"""
            )
            out.write("  <application ")

            // TODO(markf): The preparing to publish doc at
            // http://developer.android.com/guide/publishing/preparing.html suggests removing the
            // 'debuggable=true' but I'm not sure that our users would want that while they're still
            // testing their packaged apps.  Maybe we should make that an option, somehow.
            // TODONE(jis): Turned off debuggable. No one really uses it and it represents a security
            // risk for App Inventor App end-users.
            out.write("android:debuggable=\"false\" ")
            // out.write("android:debuggable=\"true\" "); // DEBUGGING
            if (aName == "") {
                out.write("android:label=\"$projectName\" ")
            } else {
                out.write("android:label=\"$aName\" ")
            }
            out.write("android:networkSecurityConfig=\"@xml/network_security_config\" ")
            out.write("android:requestLegacyExternalStorage=\"true\" ") // For SDK 29 (Android Q)
            if (YaVersion.TARGET_SDK_VERSION >= 30) {
                out.write("android:preserveLegacyExternalStorage=\"true\" ") // For SDK 30 (Android R)
            }
            out.write("android:icon=\"@mipmap/ic_launcher\" ")
            out.write("android:roundIcon=\"@mipmap/ic_launcher\" ")
            if (isForCompanion) {              // This is to hook into ACRA
                out.write("android:name=\"com.google.appinventor.components.runtime.ReplApplication\" ")
            } else {
                out.write("android:name=\"com.google.appinventor.components.runtime.multidex.MultiDexApplication\" ")
            }
            // Write theme info if we are not using the "Classic" theme (i.e., no theme)
            if (true) {
//      if (!"Classic".equalsIgnoreCase(project.getTheme())) {
                out.write("android:theme=\"@style/AppTheme\" ")
            }
            out.write(">\n")
            out.write("<uses-library android:name=\"org.apache.http.legacy\" android:required=\"false\" />")
            for (source in project!!.getSources()) {
                val formClassName: String = source.getQualifiedName()
                // String screenName = formClassName.substring(formClassName.lastIndexOf('.') + 1);
                val isMain = formClassName == mainClass
                if (isMain) {
                    // The main activity of the application.
                    out.write("    <activity android:name=\".$className\" ")
                } else {
                    // A secondary activity of the application.
                    out.write("    <activity android:name=\"$formClassName\" ")
                }

                // This line is here for NearField and NFC.   It keeps the activity from
                // restarting every time NDEF_DISCOVERED is signaled.
                // TODO:  Check that this doesn't screw up other components.  Also, it might be
                // better to do this programmatically when the NearField component is created, rather
                // than here in the manifest.
                if (simpleCompTypes!!.contains("com.google.appinventor.components.runtime.NearField") &&
                    !isForCompanion && isMain
                ) {
                    out.write("android:launchMode=\"singleTask\" ")
                } else if (isMain && isForCompanion) {
                    out.write("android:launchMode=\"singleTop\" ")
                }
                out.write("android:windowSoftInputMode=\"stateHidden\" ")

                // The keyboard option prevents the app from stopping when a external (bluetooth)
                // keyboard is attached.
                out.write(
                    """
    android:configChanges="orientation|screenSize|keyboardHidden|keyboard|screenLayout|smallestScreenSize">
    
    """.trimIndent()
                )
                out.write("      <intent-filter>\n")
                out.write("        <action android:name=\"android.intent.action.MAIN\" />\n")
                if (isMain) {
                    out.write("        <category android:name=\"android.intent.category.LAUNCHER\" />\n")
                }
                out.write("      </intent-filter>\n")
                if (isForCompanion) {
                    out.write("<intent-filter>\n")
                    out.write("<action android:name=\"android.intent.action.VIEW\" />\n")
                    out.write("<category android:name=\"android.intent.category.DEFAULT\" />\n")
                    out.write("<category android:name=\"android.intent.category.BROWSABLE\" />\n")
                    out.write("<data android:scheme=\"aicompanion\" android:host=\"comp\" />\n")
                    out.write("</intent-filter>\n")
                }
                if (simpleCompTypes!!.contains("com.google.appinventor.components.runtime.NearField") &&
                    !isForCompanion && isMain
                ) {
                    //  make the form respond to NDEF_DISCOVERED
                    //  this will trigger the form's onResume method
                    //  For now, we're handling text/plain only,but we can add more and make the Nearfield
                    // component check the type.
                    out.write("      <intent-filter>\n")
                    out.write("        <action android:name=\"android.nfc.action.NDEF_DISCOVERED\" />\n")
                    out.write("        <category android:name=\"android.intent.category.DEFAULT\" />\n")
                    out.write("        <data android:mimeType=\"text/plain\" />\n")
                    out.write("      </intent-filter>\n")
                }
                val metadataElements: Set<Map.Entry<String, Set<String>>> = activityMetadataNeeded.entrySet()

                // If any component needs to register additional activity metadata,
                // insert them into the manifest here.
                if (!metadataElements.isEmpty()) {
                    for (metadataElementSetPair in metadataElements) {
                        val metadataElementSet: Set<String> = metadataElementSetPair.getValue()
                        for (metadataElement in metadataElementSet) {
                            out.write(
                                metadataElement
                                    .replace(
                                        "%packageName%",
                                        packageName
                                    ) // replace %packageName% with the actual packageName
                            )
                        }
                    }
                }
                out.write("    </activity>\n")

                // Companion display a splash screen... define it's activity here
                if (isMain && isForCompanion) {
                    out.write("    <activity android:name=\"com.google.appinventor.components.runtime.SplashActivity\" android:screenOrientation=\"behind\" android:configChanges=\"keyboardHidden|orientation\">\n")
                    out.write("      <intent-filter>\n")
                    out.write("        <action android:name=\"android.intent.action.MAIN\" />\n")
                    out.write("      </intent-filter>\n")
                    out.write("    </activity>\n")
                }
            }

            // Collect any additional <application> subelements into a single set.
            val subelements: Set<Map.Entry<String, Set<String>>> = Sets.newHashSet()
            subelements.addAll(activitiesNeeded.entrySet())
            subelements.addAll(metadataNeeded.entrySet())
            subelements.addAll(broadcastReceiversNeeded.entrySet())
            subelements.addAll(servicesNeeded.entrySet())
            subelements.addAll(contentProvidersNeeded.entrySet())


            // If any component needs to register additional activities,
            // broadcast receivers, services or content providers, insert
            // them into the manifest here.
            if (!subelements.isEmpty()) {
                for (componentSubElSetPair in subelements) {
                    val subelementSet: Set<String> = componentSubElSetPair.getValue()
                    for (subelement in subelementSet) {
                        if (isForCompanion && !includeDangerousPermissions &&
                            subelement.contains("android.provider.Telephony.SMS_RECEIVED")
                        ) {
                            continue
                        }
                        out.write(
                            subelement
                                .replace(
                                    "%packageName%",
                                    packageName
                                ) // replace %packageName% with the actual packageName
                        )
                    }
                }
            }

            // TODO(Will): Remove the following legacy code once the deprecated
            //             @SimpleBroadcastReceiver annotation is removed. It should
            //             should remain for the time being because otherwise we'll break
            //             extensions currently using @SimpleBroadcastReceiver.

            // Collect any legacy simple broadcast receivers
            val simpleBroadcastReceivers: Set<String> = Sets.newHashSet()
            for (componentType in componentBroadcastReceiver.keySet()) {
                simpleBroadcastReceivers.addAll(componentBroadcastReceiver[componentType])
            }

            // The format for each legacy Broadcast Receiver in simpleBroadcastReceivers is
            // "className,Action1,Action2,..." where the class name is mandatory, and
            // actions are optional (and as many as needed).
            for (broadcastReceiver in simpleBroadcastReceivers) {
                val brNameAndActions: Array<String> = broadcastReceiver.split(",")
                if (brNameAndActions.size == 0) {
                    continue
                }
                // Remove the SMS_RECEIVED broadcast receiver if we aren't including dangerous permissions
                if (isForCompanion && !includeDangerousPermissions) {
                    var skip = false
                    for (action in brNameAndActions) {
                        if (action.equalsIgnoreCase("android.provider.Telephony.SMS_RECEIVED")) {
                            skip = true
                            break
                        }
                    }
                    if (skip) {
                        continue
                    }
                }
                out.write(
                    """<receiver android:name="${brNameAndActions[0]}" >
"""
                )
                if (brNameAndActions.size > 1) {
                    out.write("  <intent-filter>\n")
                    for (i in 1 until brNameAndActions.size) {
                        out.write(
                            """    <action android:name="${brNameAndActions[i]}" />
"""
                        )
                    }
                    out.write("  </intent-filter>\n")
                }
                out.write("</receiver> \n")
            }

            // Add the FileProvider because in Sdk >=24 we cannot pass file:
            // URLs in intents (and in other contexts)
            out.write("      <provider\n")
            out.write("         android:name=\"androidx.core.content.FileProvider\"\n")
            out.write("         android:authorities=\"$packageName.provider\"\n")
            out.write("         android:exported=\"false\"\n")
            out.write("         android:grantUriPermissions=\"true\">\n")
            out.write("         <meta-data\n")
            out.write("            android:name=\"android.support.FILE_PROVIDER_PATHS\"\n")
            out.write("            android:resource=\"@xml/provider_paths\"/>\n")
            out.write("      </provider>\n")
            out.write("  </application>\n")
            out.write("</manifest>\n")
            out.close()
        } catch (e: IOException) {
            e.printStackTrace()
            userErrors.print(String.format(ERROR_IN_STAGE, "manifest"))
            return false
        }
        return true
    }

    /*
   * Creates all the animation xml files.
   */
    private fun createAnimationXml(animDir: File): Boolean {
        // Store the filenames, and their contents into a HashMap
        // so that we can easily add more, and also to iterate
        // through creating the files.
        val files: Map<String, String> = HashMap()
        files.put("fadein.xml", AnimationXmlConstants.FADE_IN_XML)
        files.put("fadeout.xml", AnimationXmlConstants.FADE_OUT_XML)
        files.put("hold.xml", AnimationXmlConstants.HOLD_XML)
        files.put("zoom_enter.xml", AnimationXmlConstants.ZOOM_ENTER)
        files.put("zoom_exit.xml", AnimationXmlConstants.ZOOM_EXIT)
        files.put("zoom_enter_reverse.xml", AnimationXmlConstants.ZOOM_ENTER_REVERSE)
        files.put("zoom_exit_reverse.xml", AnimationXmlConstants.ZOOM_EXIT_REVERSE)
        files.put("slide_exit.xml", AnimationXmlConstants.SLIDE_EXIT)
        files.put("slide_enter.xml", AnimationXmlConstants.SLIDE_ENTER)
        files.put("slide_exit_reverse.xml", AnimationXmlConstants.SLIDE_EXIT_REVERSE)
        files.put("slide_enter_reverse.xml", AnimationXmlConstants.SLIDE_ENTER_REVERSE)
        files.put("slide_v_exit.xml", AnimationXmlConstants.SLIDE_V_EXIT)
        files.put("slide_v_enter.xml", AnimationXmlConstants.SLIDE_V_ENTER)
        files.put("slide_v_exit_reverse.xml", AnimationXmlConstants.SLIDE_V_EXIT_REVERSE)
        files.put("slide_v_enter_reverse.xml", AnimationXmlConstants.SLIDE_V_ENTER_REVERSE)
        for (filename in files.keySet()) {
            val file = File(animDir, filename)
            if (!writeXmlFile(file, files[filename])) {
                return false
            }
        }
        return true
    }

    /*
   * Writes the given string input to the provided file.
   */
    private fun writeXmlFile(file: File, input: String?): Boolean {
        try {
            val writer = BufferedWriter(FileWriter(file))
            writer.write(input)
            writer.close()
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }
        return true
    }

    /*
   * Runs ApkBuilder by using the API instead of calling its main method because the main method
   * can call System.exit(1), which will bring down our server.
   */
    private fun runApkBuilder(apkAbsolutePath: String, zipArchive: String, dexedClassesDir: String): Boolean {
        return try {
            val apkBuilder = ApkBuilder(
                apkAbsolutePath,
                zipArchive,
                dexedClassesDir + File.separator.toString() + "classes.dex",
                null,
                System.out
            )
            if (dexFiles.size() > 1) {
                for (f in dexFiles) {
                    if (!f.name.equals("classes.dex")) {
                        apkBuilder.addFile(f, f.name)
                    }
                }
            }
            if (nativeLibsNeeded.size() !== 0) { // Need to add native libraries...
                apkBuilder.addNativeLibraries(libsDir)
            }
            apkBuilder.sealApk()
            true
        } catch (e: Exception) {
            // This is fatal.
            e.printStackTrace()
            LOG.warning("YAIL compiler - ApkBuilder failed.")
            err.println("YAIL compiler - ApkBuilder failed.")
            userErrors.print(String.format(ERROR_IN_STAGE, "ApkBuilder"))
            false
        }
    }

    /*
   * Runs the Kawa compiler in a separate process to generate classes. Returns false if not able to
   * create a class file for every source file in the project.
   *
   * As a side effect, we generate uniqueLibsNeeded which contains a set of libraries used by
   * runDx. Each library appears in the set only once (which is why it is a set!). This is
   * important because when we Dex the libraries, a given library can only appear once.
   *
   */
    private fun generateClasses(classesDir: File): Boolean {
        try {
            val sources: List<Project.SourceDescriptor> = project!!.getSources()
            val sourceFileNames: List<String> = Lists.newArrayListWithCapacity(sources.size)
            val classFileNames: List<String> = Lists.newArrayListWithCapacity(sources.size)
            var userCodeExists = false
            for (source in sources) {
                val sourceFileName: String = source.getFile().absolutePath
                LOG.log(Level.INFO, "source file: $sourceFileName")
                val srcIndex: Int =
                    sourceFileName.indexOf("${File.separator}..${File.separator}src${File.separator}")
                val sourceFileRelativePath: String = sourceFileName.substring(srcIndex + 8)
                val classFileName: String = "${classesDir.absolutePath}/$sourceFileRelativePath"
                    .replace(YoungAndroidConstants.YAIL_EXTENSION, ".class")

                // Check whether user code exists by seeing if a left parenthesis exists at the beginning of
                // a line in the file
                // TODO(user): Replace with more robust test of empty source file.
                if (!userCodeExists) {
                    val fileReader: Reader = FileReader(sourceFileName)
                    try {
                        while (fileReader.ready()) {
                            val c: Int = fileReader.read()
                            if (c == '('.code) {
                                userCodeExists = true
                                break
                            }
                        }
                    } finally {
                        fileReader.close()
                    }
                }
                sourceFileNames.add(sourceFileName)
                classFileNames.add(classFileName)
            }
            if (!userCodeExists) {
                userErrors.print(NO_USER_CODE_ERROR)
                return false
            }

            // Construct the class path including component libraries (jars)
            val classpath = StringBuilder(getResource(KAWA_RUNTIME))
            classpath.append(COLON)
            classpath.append(getResource(ACRA_RUNTIME))
            classpath.append(COLON)
            classpath.append(getResource(SIMPLE_ANDROID_RUNTIME_JAR))
            classpath.append(COLON)
            for (jar in SUPPORT_JARS) {
                classpath.append(getResource(jar))
                classpath.append(COLON)
            }

            // attach the jars of external comps
            val addedExtJars: Set<String> = HashSet()
            for (type in extCompTypes!!) {
                val sourcePath = getExtCompDirPath(type) + SIMPLE_ANDROID_RUNTIME_JAR
                if (!addedExtJars.contains(sourcePath)) {  // don't add multiple copies for bundled extensions
                    classpath.append(sourcePath)
                    classpath.append(COLON)
                    addedExtJars.add(sourcePath)
                }
            }

            // Add component library names to classpath
            for (type in libsNeeded.keySet()) {
                for (lib in libsNeeded[type]) {
                    var sourcePath = ""
                    val pathSuffix = RUNTIME_FILES_DIR + lib
                    sourcePath = if (simpleCompTypes!!.contains(type)) {
                        getResource(pathSuffix)
                    } else if (extCompTypes!!.contains(type)) {
                        getExtCompDirPath(type) + pathSuffix
                    } else {
                        userErrors.print(String.format(ERROR_IN_STAGE, "Compile"))
                        return false
                    }
                    uniqueLibsNeeded.add(sourcePath)
                    classpath.append(sourcePath)
                    classpath.append(COLON)
                }
            }

            // Add dependencies for classes.jar in any AAR libraries
            for (classesJar in explodedAarLibs.getClasses()) {
                // true for optimized AARs in App Inventor libs
                val abspath: String = classesJar.absolutePath
                uniqueLibsNeeded.add(abspath)
                classpath.append(abspath)
                classpath.append(COLON)
            }
            if (explodedAarLibs.size > 0) {
                classpath.append(explodedAarLibs.getOutputDirectory().getAbsolutePath())
                classpath.append(COLON)
            }
            classpath.append(getResource(ANDROID_RUNTIME))
            println("Libraries Classpath = $classpath")
            val yailRuntime = getResource(YAIL_RUNTIME)
            val kawaCommandArgs: List<String> = Lists.newArrayList()
            val mx = childProcessRamMb - 200
            Collections.addAll(
                kawaCommandArgs, "${System.getProperty("java.home")}/bin/java",
                "-Dfile.encoding=UTF-8",
                "-mx" + mx + "M",
                "-cp", classpath.toString(),
                "kawa.repl",
                "-f", yailRuntime,
                "-d", classesDir.absolutePath,
                "-P", Signatures.getPackageName(project.mainClass) + ".",
                "-C"
            )
            // TODO(lizlooney) - we are currently using (and have always used) absolute paths for the
            // source file names. The resulting .class files contain references to the source file names,
            // including the name of the tmp directory that contains them. We may be able to avoid that
            // by using source file names that are relative to the project root and using the project
            // root as the working directory for the Kawa compiler process.
            kawaCommandArgs.addAll(sourceFileNames)
            kawaCommandArgs.add(yailRuntime)
            val kawaCommandLine: Array<String> = kawaCommandArgs.toTypedArray()
            val start: Long = System.currentTimeMillis()
            // Capture Kawa compiler stderr. The ODE server parses out the warnings and errors and adds
            // them to the protocol buffer for logging purposes. (See
            // buildserver/ProjectBuilder.processCompilerOutout.
            val kawaOutputStream = ByteArrayOutputStream()
            var kawaSuccess: Boolean
            synchronized(SYNC_KAWA_OR_DX) {
                kawaSuccess = Execution.execute(
                    null, kawaCommandLine,
                    System.out, PrintStream(kawaOutputStream)
                )
            }
            if (!kawaSuccess) {
                LOG.log(Level.SEVERE, "Kawa compile has failed.")
            }
            val kawaOutput: String = kawaOutputStream.toString()
            out.print(kawaOutput)
            val kawaCompileTimeMessage = "Kawa compile time: ${(System.currentTimeMillis() - start) / 1000.0} seconds"
            out.println(kawaCompileTimeMessage)
            LOG.info(kawaCompileTimeMessage)

            // Check that all of the class files were created.
            // If they weren't, return with an error.
            for (classFileName in classFileNames) {
                val classFile = File(classFileName)
                if (!classFile.exists()) {
                    LOG.log(Level.INFO, "Can't find class file: $classFileName")
                    val screenName: String = classFileName.substring(
                        classFileName.lastIndexOf('/') + 1,
                        classFileName.lastIndexOf('.')
                    )
                    userErrors.print(String.format(COMPILATION_ERROR, screenName))
                    return false
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            userErrors.print(String.format(ERROR_IN_STAGE, "Compile"))
            return false
        }
        return true
    }

    private fun runZipAlign(apkAbsolutePath: String, tmpDir: File): Boolean {
        // TODO(user): add zipalign tool appinventor->lib->android->tools->linux and windows
        // Need to make sure assets directory exists otherwise zipalign will fail.
        createDir(project.assetsDirectory)
        val zipAlignTool: String
        val osName: String = System.getProperty("os.name")
        zipAlignTool = when {
            osName == "Mac OS X" -> MAC_ZIPALIGN_TOOL
            osName == "Linux" -> LINUX_ZIPALIGN_TOOL
            osName.startsWith("Windows") -> WINDOWS_ZIPALIGN_TOOL
            else -> {
                LOG.warning("YAIL compiler - cannot run ZIPALIGN on OS $osName")
                err.println("YAIL compiler - cannot run ZIPALIGN on OS $osName")
                userErrors.print(String.format(ERROR_IN_STAGE, "ZIPALIGN"))
                return false
            }
        }
        // TODO: create tmp file for zipaling result
        val zipAlignedPath: String = tmpDir.absolutePath + SLASH + "zipaligned.apk"
        // zipalign -f 4 infile.zip outfile.zip
        val zipAlignCommandLine = arrayOf(
            getResource(zipAlignTool),
            "-f",
            "4",
            apkAbsolutePath,
            zipAlignedPath
        )
        val startZipAlign: Long = System.currentTimeMillis()
        // Using System.err and System.out on purpose. Don't want to pollute build messages with
        // tools output
        if (!Execution.execute(null, zipAlignCommandLine, System.out, System.err)) {
            LOG.warning("YAIL compiler - ZIPALIGN execution failed.")
            err.println("YAIL compiler - ZIPALIGN execution failed.")
            userErrors.print(String.format(ERROR_IN_STAGE, "ZIPALIGN"))
            return false
        }
        if (!copyFile(zipAlignedPath, apkAbsolutePath)) {
            LOG.warning("YAIL compiler - ZIPALIGN file copy failed.")
            err.println("YAIL compiler - ZIPALIGN file copy failed.")
            userErrors.print(String.format(ERROR_IN_STAGE, "ZIPALIGN"))
            return false
        }
        val zipALignTimeMessage = "ZIPALIGN time: ${(System.currentTimeMillis() - startZipAlign) / 1000.0} seconds"
        out.println(zipALignTimeMessage)
        LOG.info(zipALignTimeMessage)
        return true
    }

    private fun runApkSigner(apkAbsolutePath: String, keystoreAbsolutePath: String?): Boolean {
        val mx = childProcessRamMb - 200
        /*
      apksigner sign\
      --ks <keystore file>\
      --ks-key-alias AndroidKey\
      --ks-pass pass:android\
      <APK>
    */
        val apksignerCommandLine = listOf(
            "${System.getProperty("java.home")}/bin/java", "-jar",
            "-mx" + mx + "M",
            getResource(APKSIGNER_JAR), "sign",
            "-ks", keystoreAbsolutePath,
            "-ks-key-alias", "AndroidKey",
            "-ks-pass", "pass:android",
            apkAbsolutePath
        )
        val startApkSigner: Long = System.currentTimeMillis()
        if (!Execution.execute(null, apksignerCommandLine, System.out, System.err)) {
            LOG.warning("YAIL compiler - apksigner execution failed.")
            err.println("YAIL compiler - apksigner execution failed.")
            userErrors.print(String.format(ERROR_IN_STAGE, "APKSIGNER"))
            return false
        }
        val apkSignerTimeMessage = "APKSIGNER time: ${(System.currentTimeMillis() - startApkSigner) / 1000.0} seconds"
        out.println(apkSignerTimeMessage)
        LOG.info(apkSignerTimeMessage)
        return true
    }

    /*
   * Returns a resized image given a new width and height
   */
    private fun resizeImage(icon: BufferedImage, height: Int, width: Int): BufferedImage {
        val tmp: Image = icon.getScaledInstance(width, height, Image.SCALE_SMOOTH)
        val finalResized = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val g2: Graphics2D = finalResized.createGraphics()
        g2.drawImage(tmp, 0, 0, null)
        return finalResized
    }

    /*
   * Creates the circle image of an icon
   */
    private fun produceRoundIcon(icon: BufferedImage?): BufferedImage {
        val imageWidth: Int = icon.getWidth()
        // Ratio of icon size to png image size for round icon is 0.80
        val iconWidth = imageWidth * 0.80
        // Round iconWidth value to even int for a centered png
        val intIconWidth = (iconWidth / 2).roundToLong() as Int * 2
        val tmp: Image = icon.getScaledInstance(intIconWidth, intIconWidth, Image.SCALE_SMOOTH)
        val marginWidth = (imageWidth - intIconWidth) / 2
        val roundIcon = BufferedImage(imageWidth, imageWidth, BufferedImage.TYPE_INT_ARGB)
        val g2: Graphics2D = roundIcon.createGraphics()
        g2.setClip(Float(marginWidth, marginWidth, intIconWidth, intIconWidth))
        g2.drawImage(tmp, marginWidth, marginWidth, null)
        return roundIcon
    }

    /*
   * Creates the image of an icon with rounded corners
   */
    private fun produceRoundedCornerIcon(icon: BufferedImage?): BufferedImage {
        val imageWidth: Int = icon.getWidth()
        // Ratio of icon size to png image size for roundRect icon is 0.93
        val iconWidth = imageWidth * 0.93
        // Round iconWidth value to even int for a centered png
        val intIconWidth = (iconWidth / 2).roundToLong() as Int * 2
        val tmp: Image = icon.getScaledInstance(intIconWidth, intIconWidth, Image.SCALE_SMOOTH)
        val marginWidth = (imageWidth - intIconWidth) / 2
        // Corner radius of roundedCornerIcon needs to be 1/12 of width according to Android material guidelines
        val cornerRadius = (intIconWidth / 12).toFloat()
        val roundedCornerIcon = BufferedImage(imageWidth, imageWidth, BufferedImage.TYPE_INT_ARGB)
        val g2: Graphics2D = roundedCornerIcon.createGraphics()
        g2.setClip(Float(marginWidth, marginWidth, intIconWidth, intIconWidth, cornerRadius, cornerRadius))
        g2.drawImage(tmp, marginWidth, marginWidth, null)
        return roundedCornerIcon
    }

    /*
   * Creates the foreground image of an icon
   */
    private fun produceForegroundImageIcon(icon: BufferedImage?): BufferedImage {
        val imageWidth: Int = icon.getWidth()
        // According to the adaptive icon documentation, both layers are 108x108dp but only the inner
        // 72x72dp appears in the masked viewport, so we shrink down the size of the image accordingly.
        val iconWidth = imageWidth * 72.0 / 108.0
        // Round iconWidth value to even int for a centered png
        val intIconWidth = (iconWidth / 2).roundToLong() as Int * 2
        val tmp: Image = icon.getScaledInstance(intIconWidth, intIconWidth, Image.SCALE_SMOOTH)
        val marginWidth = (imageWidth - intIconWidth) / 2
        val foregroundImageIcon = BufferedImage(imageWidth, imageWidth, BufferedImage.TYPE_INT_ARGB)
        val g2: Graphics2D = foregroundImageIcon.createGraphics()
        g2.drawImage(tmp, marginWidth, marginWidth, null)
        return foregroundImageIcon
    }

    /*
   * Loads the icon for the application, either a user provided one or the default one.
   */
    private fun prepareApplicationIcon(
        outputPngFile: File,
        mipmapDirectories: List<File>,
        standardICSizes: List<Int>,
        foregroundICSizes: List<Int>
    ): Boolean {
        val userSpecifiedIcon: String = project.icon ?: ""
        try {
            val icon: BufferedImage
            if (userSpecifiedIcon.isNotEmpty()) {
                val iconFile = File(project.assetsDirectory, userSpecifiedIcon)
                icon = ImageIO.read(iconFile)
                if (icon == null) {
                    // This can happen if the iconFile isn't an image file.
                    // For example, icon is null if the file is a .wav file.
                    // TODO(lizlooney) - This happens if the user specifies a .ico file. We should
                    // fix that.
                    userErrors.print(String.format(ICON_ERROR, userSpecifiedIcon))
                    return false
                }
            } else {
                // Load the default image.
                icon = ImageIO.read(Compiler::class.java.getResource(DEFAULT_ICON))
            }
            val roundIcon: BufferedImage = produceRoundIcon(icon)
            val roundRectIcon: BufferedImage = produceRoundedCornerIcon(icon)
            val foregroundIcon: BufferedImage = produceForegroundImageIcon(icon)

            // For each mipmap directory, create all types of ic_launcher photos with respective mipmap sizes
            for (i in mipmapDirectories.indices) {
                val mipmapDirectory: File = mipmapDirectories[i]
                val standardSize: Int = standardICSizes[i]
                val foregroundSize: Int = foregroundICSizes[i]
                val round: BufferedImage = resizeImage(roundIcon, standardSize, standardSize)
                val roundRect: BufferedImage = resizeImage(roundRectIcon, standardSize, standardSize)
                val foreground: BufferedImage = resizeImage(foregroundIcon, foregroundSize, foregroundSize)
                val roundIconPng = File(mipmapDirectory, "ic_launcher_round.png")
                val roundRectIconPng = File(mipmapDirectory, "ic_launcher.png")
                val foregroundPng = File(mipmapDirectory, "ic_launcher_foreground.png")
                ImageIO.write(round, "png", roundIconPng)
                ImageIO.write(roundRect, "png", roundRectIconPng)
                ImageIO.write(foreground, "png", foregroundPng)
            }
            ImageIO.write(icon, "png", outputPngFile)
        } catch (e: Exception) {
            e.printStackTrace()
            // If the user specified the icon, this is fatal.
            if (userSpecifiedIcon.isNotEmpty()) {
                userErrors.print(String.format(ICON_ERROR, userSpecifiedIcon))
                return false
            }
        }
        return true
    }

    /**
     * Processes recursively the directory pointed at by `dir` and adds any class files
     * encountered to the `classes` set.
     *
     * @param dir the directory to examine for class files
     * @param classes the Set used to record the classes
     * @param root the root path where the recursion started, which gets stripped from the file name
     * to determine the class name
     */
    private fun recordDirectoryForMainDex(dir: File, classes: Set<String>, root: String) {
        val files: Array<File> = dir.listFiles() ?: return
        for (f in files) {
            when {
                f.isDirectory -> recordDirectoryForMainDex(f, classes, root)
                f.name.endsWith(".class") -> {
                    var className = f.absolutePath.replace(root, "")
                    className = className.substring(0, className.length - 6)
                    classes.add(className.replaceAll("/", "."))
                }
            }
        }
    }

    /**
     * Processes the JAR file pointed at by `file` and adds the contained class names to
     * `classes`.
     *
     * @param file a File object pointing to a JAR file
     * @param classes the Set used to record the classes
     * @throws IOException if the input file cannot be read
     */
    @Throws(IOException::class)
    private fun recordJarForMainDex(file: File, classes: Set<String>) {
        ZipInputStream(FileInputStream(file)).use { `is` ->
            var entry: ZipEntry
            while (`is`.getNextEntry().also { entry = it } != null) {
                var className: String = entry.getName()
                if (className.endsWith(".class")) {
                    className = className.substring(0, className.length - 6)
                    classes.add(className.replaceAll("/", "."))
                }
            }
        }
    }

    /**
     * Examines the given file and records its classes for the main dex class list.
     *
     * @param file a File object pointing to a JAR file or a directory containing class files
     * @param classes the Set used to record the classes
     * @return the input file
     * @throws IOException if the input file cannot be read
     */
    @Throws(IOException::class)
    private fun recordForMainDex(file: File, classes: Set<String>): File {
        when {
            file.isDirectory -> recordDirectoryForMainDex(file, classes, file.absolutePath + File.separator)
            file.name.endsWith(".jar") -> recordJarForMainDex(file, classes)
        }
        return file
    }

    /**
     * Writes out the class list for the main dex file. The format of this file is the pathname of
     * the class, including the .class extension, one per line.
     *
     * @param classesDir directory to place the main classes list
     * @param classes the set of classes to include in the main dex file
     * @return the path to the file containing the main classes list
     */
    private fun writeClassList(classesDir: File, classes: Set<String>): String? {
        val target = File(classesDir, "main-classes.txt")
        try {
            PrintStream(FileOutputStream(target)).use { out ->
                for (name in TreeSet(classes)) {
                    out.println(name.replaceAll("\\.", "/").toString() + ".class")
                }
                return target.absolutePath
            }
        } catch (e: IOException) {
            return null
        }
    }

    /**
     * Compiles Java class files and JAR files into the Dex file format using dx.
     *
     * @param classesDir directory containing compiled App Inventor screens
     * @param dexedClassesDir output directory for classes.dex
     * @return true if successful or false if an error occurred
     */
    private fun runMultidex(classesDir: File, dexedClassesDir: String): Boolean {
        val mainDexClasses: Set<String> = HashSet()
        val inputList: List<File> = ArrayList()
        var success: Boolean
        try {
            // Set up classes for main dex file
            inputList.add(recordForMainDex(classesDir, mainDexClasses))
            inputList.add(
                recordForMainDex(
                    File(getResource(SIMPLE_ANDROID_RUNTIME_JAR)),
                    mainDexClasses
                )
            )
            inputList.add(recordForMainDex(File(getResource(KAWA_RUNTIME)), mainDexClasses))
            for (jar in CRITICAL_JARS) {
                inputList.add(recordForMainDex(File(getResource(jar)), mainDexClasses))
            }

            // Only include ACRA for the companion app
            if (isForCompanion) {
                inputList.add(recordForMainDex(File(getResource(ACRA_RUNTIME)), mainDexClasses))
            }
            for (jar in SUPPORT_JARS) {
                if (CRITICAL_JARS.contains(jar)) {  // already covered above
                    continue
                }
                inputList.add(File(getResource(jar)))
            }

            // Add the rest of the libraries in any order
            for (lib in uniqueLibsNeeded) {
                inputList.add(File(lib))
            }

            // Add extension libraries
            val addedExtJars: Set<String> = HashSet()
            for (type in extCompTypes!!) {
                val sourcePath = getExtCompDirPath(type) + SIMPLE_ANDROID_RUNTIME_JAR
                if (!addedExtJars.contains(sourcePath)) {
                    inputList.add(File(sourcePath))
                    addedExtJars.add(sourcePath)
                }
            }

            // Run the dx utility
            val dexTask = DexExecTask()
            dexTask.setExecutable(getResource(DX_JAR))
            dexTask.setMainDexClassesFile(writeClassList(classesDir, mainDexClasses))
            dexTask.setOutput(dexedClassesDir)
            dexTask.setChildProcessRamMb(childProcessRamMb)
            if (dexCacheDir == null) {
                dexTask.setDisableDexMerger(true)
            } else {
                createDir(File(dexCacheDir))
                dexTask.setDexedLibs(dexCacheDir)
            }
            var dxTimeMessage: String?
            synchronized(SYNC_KAWA_OR_DX) {
                setProgress(50)
                val startDx: Long = System.currentTimeMillis()
                success = dexTask.execute(inputList)
                dxTimeMessage = String.format(
                    Locale.getDefault(), "DX time: %f seconds",
                    (System.currentTimeMillis() - startDx) / 1000.0
                )
                setProgress(75)
            }

            // Aggregate all of the classes.dex files output by dx
            val files: Array<File> = File(dexedClassesDir).listFiles { dir, name -> name.endsWith(".dex") } ?: throw FileNotFoundException("Could not find classes.dex")
            Collections.addAll(dexFiles, files)

            // Log status
            out.println(dxTimeMessage)
            LOG.info(dxTimeMessage)
        } catch (e: IOException) {
            // Error will be reported below
            success = false
        }
        if (!success) {
            LOG.warning("YAIL compiler - DX execution failed.")
            err.println("YAIL compiler - DX execution failed.")
            userErrors.print(String.format(ERROR_IN_STAGE, "DX"))
        }
        return success
    }

    private fun runAaptPackage(
        manifestFile: File,
        resDir: File,
        tmpPackageName: String,
        sourceOutputDir: File,
        symbolOutputDir: File
    ): Boolean {
        // Need to make sure assets directory exists otherwise aapt will fail.
        val mergedAssetsDir: File = createDir(project.buildDirectory, ASSET_DIR_NAME)
        val aaptTool: String
        val osName: String = System.getProperty("os.name")
        aaptTool = if (osName == "Mac OS X") {
            MAC_AAPT_TOOL
        } else if (osName == "Linux") {
            LINUX_AAPT_TOOL
        } else if (osName.startsWith("Windows")) {
            WINDOWS_AAPT_TOOL
        } else {
            LOG.warning("YAIL compiler - cannot run AAPT on OS $osName")
            err.println("YAIL compiler - cannot run AAPT on OS $osName")
            userErrors.print(String.format(ERROR_IN_STAGE, "AAPT"))
            return false
        }
        if (!mergeResources(resDir, project.buildDirectory, aaptTool)) {
            LOG.warning("Unable to merge resources")
            err.println("Unable to merge resources")
            userErrors.print(String.format(ERROR_IN_STAGE, "AAPT"))
            return false
        }
        val aaptPackageCommandLineArgs: List<String> = ArrayList()
        aaptPackageCommandLineArgs.add(getResource(aaptTool))
        aaptPackageCommandLineArgs.add("package")
        aaptPackageCommandLineArgs.add("-v")
        aaptPackageCommandLineArgs.add("-f")
        aaptPackageCommandLineArgs.add("-M")
        aaptPackageCommandLineArgs.add(manifestFile.absolutePath)
        aaptPackageCommandLineArgs.add("-S")
        aaptPackageCommandLineArgs.add(mergedResDir.getAbsolutePath())
        aaptPackageCommandLineArgs.add("-A")
        aaptPackageCommandLineArgs.add(mergedAssetsDir.absolutePath)
        aaptPackageCommandLineArgs.add("-I")
        aaptPackageCommandLineArgs.add(getResource(ANDROID_RUNTIME))
        aaptPackageCommandLineArgs.add("-F")
        aaptPackageCommandLineArgs.add(tmpPackageName)
        if (explodedAarLibs.size() > 0) {
            // If AARs are used, generate R.txt for later processing
            val packageName: String = Signatures.getPackageName(project.getMainClass())
            aaptPackageCommandLineArgs.add("-m")
            aaptPackageCommandLineArgs.add("-J")
            aaptPackageCommandLineArgs.add(sourceOutputDir.absolutePath)
            aaptPackageCommandLineArgs.add("--custom-package")
            aaptPackageCommandLineArgs.add(packageName)
            aaptPackageCommandLineArgs.add("--output-text-symbols")
            aaptPackageCommandLineArgs.add(symbolOutputDir.absolutePath)
            aaptPackageCommandLineArgs.add("--no-version-vectors")
            appRJava = File(sourceOutputDir, packageName.replaceAll("\\.", SLASHREGEX) + SLASH + "R.java")
            appRTxt = File(symbolOutputDir, "R.txt")
        }
        val aaptPackageCommandLine: Array<String> =
            aaptPackageCommandLineArgs.toArray(arrayOfNulls<String>(aaptPackageCommandLineArgs.size()))
        libSetup() // Setup /tmp/lib64 on Linux
        val startAapt: Long = System.currentTimeMillis()
        // Using System.err and System.out on purpose. Don't want to pollute build messages with
        // tools output
        if (!Execution.execute(null, aaptPackageCommandLine, System.out, System.err)) {
            LOG.warning("YAIL compiler - AAPT execution failed.")
            err.println("YAIL compiler - AAPT execution failed.")
            userErrors.print(String.format(ERROR_IN_STAGE, "AAPT"))
            return false
        }
        val aaptTimeMessage = "AAPT time: " +
                ((System.currentTimeMillis() - startAapt) / 1000.0).toString() + " seconds"
        out.println(aaptTimeMessage)
        LOG.info(aaptTimeMessage)
        return true
    }

    private fun runAapt2Compile(resDir: File): Boolean {
        resourcesZip = File(resDir, "resources.zip")
        val aaptTool: String
        val aapt2Tool: String
        val osName: String = System.getProperty("os.name")
        if (osName == "Mac OS X") {
            aaptTool = MAC_AAPT_TOOL
            aapt2Tool = MAC_AAPT2_TOOL
        } else if (osName == "Linux") {
            aaptTool = LINUX_AAPT_TOOL
            aapt2Tool = LINUX_AAPT2_TOOL
        } else if (osName.startsWith("Windows")) {
            aaptTool = WINDOWS_AAPT_TOOL
            aapt2Tool = WINDOWS_AAPT2_TOOL
        } else {
            LOG.warning("YAIL compiler - cannot run AAPT2 on OS $osName")
            err.println("YAIL compiler - cannot run AAPT2 on OS $osName")
            userErrors.print(String.format(ERROR_IN_STAGE, "AAPT2"))
            return false
        }
        if (!mergeResources(resDir, project.buildDirectory, aaptTool)) {
            LOG.warning("Unable to merge resources")
            err.println("Unable to merge resources")
            userErrors.print(String.format(ERROR_IN_STAGE, "AAPT"))
            return false
        }
        libSetup() // Setup /tmp/lib64 on Linux
        val aapt2CommandLine: List<String> = ArrayList()
        aapt2CommandLine.add(getResource(aapt2Tool))
        aapt2CommandLine.add("compile")
        aapt2CommandLine.add("--dir")
        aapt2CommandLine.add(mergedResDir.getAbsolutePath())
        aapt2CommandLine.add("-o")
        aapt2CommandLine.add(resourcesZip.getAbsolutePath())
        aapt2CommandLine.add("--no-crunch")
        aapt2CommandLine.add("-v")
        val aapt2CompileCommandLine: Array<String> = aapt2CommandLine.toArray(arrayOfNulls<String>(0))
        val startAapt2: Long = System.currentTimeMillis()
        if (!Execution.execute(null, aapt2CompileCommandLine, System.out, System.err)) {
            LOG.warning("YAIL compiler - AAPT2 compile execution failed.")
            err.println("YAIL compiler - AAPT2 compile execution failed.")
            userErrors.print(String.format(ERROR_IN_STAGE, "AAPT2 compile"))
            return false
        }
        val aaptTimeMessage =
            "AAPT2 compile time: " + ((System.currentTimeMillis() - startAapt2) / 1000.0).toString() + " seconds"
        out.println(aaptTimeMessage)
        LOG.info(aaptTimeMessage)
        return true
    }

    private fun runAapt2Link(manifestFile: File, tmpPackageName: String, symbolOutputDir: File): Boolean {
        val aapt2Tool: String
        val osName: String = System.getProperty("os.name")
        aapt2Tool = if (osName == "Mac OS X") {
            MAC_AAPT2_TOOL
        } else if (osName == "Linux") {
            LINUX_AAPT2_TOOL
        } else if (osName.startsWith("Windows")) {
            WINDOWS_AAPT2_TOOL
        } else {
            LOG.warning("YAIL compiler - cannot run AAPT2 on OS $osName")
            err.println("YAIL compiler - cannot run AAPT2 on OS $osName")
            userErrors.print(String.format(ERROR_IN_STAGE, "AAPT2"))
            return false
        }
        appRTxt = File(symbolOutputDir, "R.txt")
        val aapt2CommandLine: List<String> = ArrayList()
        aapt2CommandLine.add(getResource(aapt2Tool))
        aapt2CommandLine.add("link")
        aapt2CommandLine.add("--proto-format")
        aapt2CommandLine.add("-o")
        aapt2CommandLine.add(tmpPackageName)
        aapt2CommandLine.add("-I")
        aapt2CommandLine.add(getResource(ANDROID_RUNTIME))
        aapt2CommandLine.add("-R")
        aapt2CommandLine.add(resourcesZip.getAbsolutePath())
        aapt2CommandLine.add("-A")
        aapt2CommandLine.add(createDir(project.buildDirectory, ASSET_DIR_NAME).absolutePath)
        aapt2CommandLine.add("--manifest")
        aapt2CommandLine.add(manifestFile.absolutePath)
        aapt2CommandLine.add("--output-text-symbols")
        aapt2CommandLine.add(appRTxt.getAbsolutePath())
        aapt2CommandLine.add("--auto-add-overlay")
        aapt2CommandLine.add("--no-version-vectors")
        aapt2CommandLine.add("--no-auto-version")
        aapt2CommandLine.add("--no-version-transitions")
        aapt2CommandLine.add("--no-resource-deduping")
        aapt2CommandLine.add("-v")
        val aapt2LinkCommandLine: Array<String> = aapt2CommandLine.toArray(arrayOfNulls<String>(0))
        val startAapt2: Long = System.currentTimeMillis()
        if (!Execution.execute(null, aapt2LinkCommandLine, System.out, System.err)) {
            LOG.warning("YAIL compiler - AAPT2 link execution failed.")
            err.println("YAIL compiler - AAPT2 link execution failed.")
            userErrors.print(String.format(ERROR_IN_STAGE, "AAPT2 link"))
            return false
        }
        val aaptTimeMessage =
            "AAPT2 link time: " + ((System.currentTimeMillis() - startAapt2) / 1000.0).toString() + " seconds"
        out.println(aaptTimeMessage)
        LOG.info(aaptTimeMessage)
        return true
    }

    private fun bundleTool(
        buildDir: File, childProcessRam: Int, tmpPackageName: String,
        outputFileName: String?, deployDir: File, keystoreFilePath: String?, dexedClassesDir: String
    ): Boolean {
        try {
            val jarsignerTool = "jarsigner"
            val fileName = outputFileName ?: "${project.projectName}.aab"
            val aabCompiler: AabCompiler = AabCompiler(out, buildDir, childProcessRam - 200)
                .setLibsDir(libsDir)
                .setProtoApk(File(tmpPackageName))
                .setJarsigner(jarsignerTool)
                .setBundletool(getResource(BUNDLETOOL_JAR))
                .setDeploy(deployDir.absolutePath + SLASH + fileName)
                .setKeystore(keystoreFilePath)
                .setDexDir(dexedClassesDir)
            val aab: Future<Boolean> = Executors.newSingleThreadExecutor().submit(aabCompiler)
            return aab.get()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        } catch (e: ExecutionException) {
            e.printStackTrace()
        }
        return false
    }

    private fun insertNativeLibs(buildDir: File): Boolean {
        /**
         * Native libraries are targeted for particular processor architectures.
         * Here, non-default architectures (ARMv5TE is default) are identified with suffixes
         * before being placed in the appropriate directory with their suffix removed.
         */
        libsDir = createDir(buildDir, LIBS_DIR_NAME)
        val armeabiDir: File = createDir(libsDir, ARMEABI_DIR_NAME)
        val armeabiV7aDir: File = createDir(libsDir, ARMEABI_V7A_DIR_NAME)
        val arm64V8aDir: File = createDir(libsDir, ARM64_V8A_DIR_NAME)
        val x8664Dir: File = createDir(libsDir, X86_64_DIR_NAME)
        return try {
            for (type in nativeLibsNeeded.keySet()) {
                for (lib in nativeLibsNeeded[type]) {
                    val isV7a: Boolean = lib.endsWith(ComponentDescriptorConstants.ARMEABI_V7A_SUFFIX)
                    val isV8a: Boolean = lib.endsWith(ComponentDescriptorConstants.ARM64_V8A_SUFFIX)
                    val isx8664: Boolean = lib.endsWith(ComponentDescriptorConstants.X86_64_SUFFIX)
                    var sourceDirName: String
                    var targetDir: File
                    if (isV7a) {
                        sourceDirName = ARMEABI_V7A_DIR_NAME
                        targetDir = armeabiV7aDir
                        lib = lib.substring(0, lib.length() - ComponentDescriptorConstants.ARMEABI_V7A_SUFFIX.length())
                    } else if (isV8a) {
                        sourceDirName = ARM64_V8A_DIR_NAME
                        targetDir = arm64V8aDir
                        lib = lib.substring(0, lib.length() - ComponentDescriptorConstants.ARM64_V8A_SUFFIX.length())
                    } else if (isx8664) {
                        sourceDirName = X86_64_DIR_NAME
                        targetDir = x8664Dir
                        lib = lib.substring(0, lib.length() - ComponentDescriptorConstants.X86_64_SUFFIX.length())
                    } else {
                        sourceDirName = ARMEABI_DIR_NAME
                        targetDir = armeabiDir
                    }
                    var sourcePath = ""
                    val pathSuffix = RUNTIME_FILES_DIR + sourceDirName + ZIPSLASH + lib
                    if (simpleCompTypes!!.contains(type)) {
                        sourcePath = getResource(pathSuffix)
                    } else if (extCompTypes!!.contains(type)) {
                        sourcePath = getExtCompDirPath(type) + pathSuffix
                        targetDir = createDir(targetDir, EXT_COMPS_DIR_NAME)
                        targetDir = createDir(targetDir, type)
                    } else {
                        userErrors.print(String.format(ERROR_IN_STAGE, "Native Code"))
                        return false
                    }
                    Files.copy(File(sourcePath), File(targetDir, lib))
                }
            }
            true
        } catch (e: IOException) {
            e.printStackTrace()
            userErrors.print(String.format(ERROR_IN_STAGE, "Native Code"))
            false
        }
    }

    /**
     * Attach any AAR libraries to the build.
     *
     * @param buildDir Base directory of the build
     * @return true on success, otherwise false
     */
    private fun attachAarLibraries(buildDir: File): Boolean {
        val explodedBaseDir: File = createDir(buildDir, "exploded-aars")
        val generatedDir: File = createDir(buildDir, "generated")
        val genSrcDir: File = createDir(generatedDir, "src")
        explodedAarLibs = AARLibraries(genSrcDir)
        val processedLibs: Set<String> = HashSet()

        // Attach the Android support libraries (needed by every app)
        libsNeeded.put("ANDROID", HashSet(listOf(SUPPORT_AARS)))

        // walk components list for libraries ending in ".aar"
        return try {
            for (libs in libsNeeded.values()) {
                val i = libs.iterator()
                while (i.hasNext()) {
                    val libname = i.next()
                    if (libname.endsWith(".aar")) {
                        i.remove()
                        if (!processedLibs.contains(libname)) {
                            // explode libraries into ${buildDir}/exploded-aars/<package>/
                            val aarLib = AARLibrary(File(getResource(RUNTIME_FILES_DIR + libname)))
                            aarLib.unpackToDirectory(explodedBaseDir)
                            explodedAarLibs.add(aarLib)
                            processedLibs.add(libname)
                        }
                    }
                }
            }
            true
        } catch (e: IOException) {
            e.printStackTrace()
            userErrors.print(String.format(ERROR_IN_STAGE, "Attach AAR Libraries"))
            false
        }
    }

    private fun attachCompAssets(): Boolean {
        createDir(project.buildDirectory) // Needed to insert resources.
        return try {
            // Gather non-library assets to be added to apk's Asset directory.
            // The assets directory have been created before this.
            val mergedAssetDir: File = createDir(project.buildDirectory, ASSET_DIR_NAME)

            // Copy component/extension assets to build/assets
            for (type in assetsNeeded.keySet()) {
                for (assetName in assetsNeeded[type]) {
                    var targetDir: File = mergedAssetDir
                    var sourcePath: String
                    if (simpleCompTypes!!.contains(type)) {
                        val pathSuffix = RUNTIME_FILES_DIR + assetName
                        sourcePath = getResource(pathSuffix)
                    } else if (extCompTypes!!.contains(type)) {
                        val extCompDir = getExtCompDirPath(type)
                        sourcePath = getExtAssetPath(extCompDir, assetName)
                        // If targetDir's location is changed here, you must update Form.java in components to
                        // reference the new location. The path for assets in compiled apps is assumed to be
                        // assets/EXTERNAL-COMP-PACKAGE/ASSET-NAME
                        targetDir = createDir(targetDir, basename(extCompDir))
                    } else {
                        userErrors.print(String.format(ERROR_IN_STAGE, "Assets"))
                        return false
                    }
                    Files.copy(File(sourcePath), File(targetDir, assetName))
                }
            }

            // Copy project assets to build/assets
            val assets: Array<File> = project.getAssetsDirectory().listFiles()
            if (assets != null) {
                for (asset in assets) {
                    if (asset.isFile) {
                        Files.copy(asset, File(mergedAssetDir, asset.name))
                    }
                }
            }
            true
        } catch (e: IOException) {
            e.printStackTrace()
            userErrors.print(String.format(ERROR_IN_STAGE, "Assets"))
            false
        }
    }

    /**
     * Merge XML resources from different dependencies into a single file that can be passed to AAPT.
     *
     * @param mainResDir Directory for resources from the application (i.e., not libraries)
     * @param buildDir Build directory path. Merged resources will be placed at $buildDir/intermediates/res/merged
     * @param aaptTool Path to the AAPT tool
     * @return true if the resources were merged successfully, otherwise false
     */
    private fun mergeResources(mainResDir: File, buildDir: File, aaptTool: String): Boolean {
        // these should exist from earlier build steps
        val intermediates: File = createDir(buildDir, "intermediates")
        val resDir: File = createDir(intermediates, "res")
        mergedResDir = createDir(resDir, "merged")
        val cruncher: PngCruncher = AaptCruncher(getResource(aaptTool), null, null)
        return explodedAarLibs.mergeResources(mergedResDir, mainResDir, cruncher)
    }

    private fun generateRClasses(outputDir: File): Boolean {
        if (explodedAarLibs.size() === 0) {
            return true // nothing to see here
        }
        val error: Int
        error = try {
            explodedAarLibs.writeRClasses(outputDir, Signatures.getPackageName(project.getMainClass()), appRTxt)
        } catch (e: IOException) {
            e.printStackTrace()
            userErrors.print(String.format(ERROR_IN_STAGE, "Generate R Classes"))
            return false
        } catch (e: InterruptedException) {
            e.printStackTrace()
            userErrors.print(String.format(ERROR_IN_STAGE, "Generate R Classes"))
            return false
        }
        if (error != 0) {
            System.err.println("javac returned error code $error")
            userErrors.print(String.format(ERROR_IN_STAGE, "Attach AAR Libraries"))
            return false
        }
        return true
    }

    private fun ensureLib(tempdir: String, name: String, resource: String) {
        try {
            val outFile = File(tempdir, name)
            if (outFile.exists()) {
                return
            }
            val tmpLibDir = File(tempdir)
            tmpLibDir.mkdirs()
            Files.copy(Resources.newInputStreamSupplier(Compiler::class.java.getResource(resource)), outFile)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    /*
   * This code extracts platform specific dynamic libraries needed by the build tools. These
   * libraries cannot be extracted using the usual mechanism as that assigns a random suffix,
   * causing dynamic linking to fail.
   */
    private fun libSetup() {
        val osName: String = System.getProperty("os.name")
        if (osName == "Linux") {
            ensureLib("/tmp/lib64", "libc++.so", "/tools/linux/lib64/libc++.so")
        } else if (osName.startsWith("Windows")) {
            ensureLib(System.getProperty("java.io.tmpdir"), "libwinpthread-1.dll", WINDOWS_PTHEAD_DLL)
        }
    }

    /*
   *  Loads permissions and information on component libraries and assets.
   */
    @Throws(IOException::class, JSONException::class)
    private fun loadJsonInfo(infoMap: ConcurrentMap<String, Set<String>>, targetInfo: String) {
        synchronized(infoMap) {
            if (!infoMap.isEmpty()) return
            val buildInfo = JSONArray("[${simpleCompsBuildInfo.join(",")},${extCompsBuildInfo.join(",")}]"
            )
            for (i in 0 until buildInfo.length()) {
                val compJson: JSONObject = buildInfo.getJSONObject(i)
                val type: String = compJson.getString("type")
                val infoArray: JSONArray = try {
                    compJson.getJSONArray(targetInfo)
                } catch (e: JSONException) {
                    // Older compiled extensions will not have a broadcastReceiver
                    // defined. Rather then require them all to be recompiled, we
                    // treat the missing attribute as empty.
                    if (e.message.contains("broadcastReceiver")) {
                        LOG.log(Level.INFO, "Component \"$type\" does not have a broadcast receiver.")
                        continue
                    } else if (e.message.contains(ComponentDescriptorConstants.ANDROIDMINSDK_TARGET)) {
                        LOG.log(Level.INFO, "Component \"$type\" does not specify a minimum SDK.")
                        continue
                    } else {
                        throw e
                    }
                }
                if (!simpleCompTypes!!.contains(type) && !extCompTypes!!.contains(type)) {
                    continue
                }
                val infoSet: Set<String> = Sets.newHashSet()
                for (j in 0 until infoArray.length()) {
                    val info: String = infoArray.getString(j)
                    if (!info.isEmpty()) {
                        infoSet.add(info)
                    }
                }
                if (!infoSet.isEmpty()) {
                    infoMap.put(type, infoSet)
                }
                processConditionalInfo(compJson, type, targetInfo)
            }
        }
    }

    /**
     * Processes the conditional info from simple_components_build_info.json into
     * a structure mapping annotation types to component names to block names to
     * values.
     *
     * @param compJson Parsed component data from JSON
     * @param type The name of the type being processed
     * @param targetInfo Name of the annotation target being processed (e.g.,
     * permissions). Any of: PERMISSIONS_TARGET,
     * BROADCAST_RECEIVERS_TARGET, SERVICES_TARGET,
     * CONTENT_PROVIDERS_TARGET
     */
    private fun processConditionalInfo(compJson: JSONObject, type: String, targetInfo: String) {
        // Strip off the package name since SCM and BKY use unqualified names
        var type = type.substring(type.lastIndexOf('.') + 1)
        val conditionals: JSONObject? = compJson.optJSONObject(ComponentDescriptorConstants.CONDITIONALS_TARGET)
        if (conditionals != null) {
            val jsonBlockMap: JSONObject? = conditionals.optJSONObject(targetInfo)
            if (jsonBlockMap != null) {
                if (!this.conditionals.containsKey(targetInfo)) {
                    this.conditionals[targetInfo] = HashMap()
                }
                val blockMap: Map<String, Set<String>> = HashMap()
                this.conditionals[targetInfo].put(type, blockMap)
                for (key in Lists.newArrayList(jsonBlockMap.keys())) {
                    val data: JSONArray = jsonBlockMap.optJSONArray(key)
                    val result: HashSet<String> = HashSet()
                    for (i in 0 until data.length()) {
                        result.add(data.optString(i))
                    }
                    blockMap.put(key, result)
                }
            }
        }
    }

    private fun setProgress(increments: Int) {
        LOG.info("The current progress is $increments%")
        reporter?.report(increments)
    }

    private fun readBuildInfo() {
        try {
            simpleCompsBuildInfo = JSONArray(
                Resources.toString(
                    Compiler::class.java.getResource(COMP_BUILD_INFO), Charsets.UTF_8
                )
            )
            extCompsBuildInfo = JSONArray()
            val readComponentInfos: Set<String> = HashSet()
            for (type in extCompTypes!!) {
                // .../assets/external_comps/com.package.MyExtComp/files/component_build_info.json
                var extCompRuntimeFileDir = File(getExtCompDirPath(type) + RUNTIME_FILES_DIR)
                if (!extCompRuntimeFileDir.exists()) {
                    // try extension package name for multi-extension files
                    var path = getExtCompDirPath(type)
                    path = path.substring(0, path.lastIndexOf('.'))
                    extCompRuntimeFileDir = File(path + RUNTIME_FILES_DIR)
                }
                var jsonFile = File(extCompRuntimeFileDir, "component_build_infos.json")
                if (!jsonFile.exists()) {
                    // old extension with a single component?
                    jsonFile = File(extCompRuntimeFileDir, "component_build_info.json")
                    if (!jsonFile.exists()) {
                        throw IllegalStateException("No component_build_info.json in extension for $type")
                    }
                }
                if (readComponentInfos.contains(jsonFile.absolutePath)) {
                    continue  // already read the build infos for this type (bundle extension)
                }
                val buildInfo: String = Resources.toString(jsonFile.toURI().toURL(), Charsets.UTF_8)
                val tokener = JSONTokener(buildInfo)
                val value: Object = tokener.nextValue()
                if (value is JSONObject) {
                    extCompsBuildInfo.put(value as JSONObject)
                    readComponentInfos.add(jsonFile.absolutePath)
                } else if (value is JSONArray) {
                    val infos: JSONArray = value as JSONArray
                    for (i in 0 until infos.length()) {
                        extCompsBuildInfo.put(infos.getJSONObject(i))
                    }
                    readComponentInfos.add(jsonFile.absolutePath)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun prepareCompTypes(neededTypes: Set<String?>?) {
        try {
            val buildInfo = JSONArray(
                Resources.toString(
                    Compiler::class.java.getResource(COMP_BUILD_INFO), Charsets.UTF_8
                )
            )
            val allSimpleTypes: Set<String> = Sets.newHashSet()
            for (i in 0 until buildInfo.length()) {
                val comp: JSONObject = buildInfo.getJSONObject(i)
                allSimpleTypes.add(comp.getString("type"))
            }
            simpleCompTypes = Sets.newHashSet(neededTypes)
            simpleCompTypes.retainAll(allSimpleTypes)
            extCompTypes = Sets.newHashSet(neededTypes)
            extCompTypes.removeAll(allSimpleTypes)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getExtCompDirPath(type: String): String {
        createDir(project.assetsDirectory)
        var candidate = extTypePathCache[type]
        if (candidate != null) {  // already computed the path
            return candidate
        }
        candidate = project.assetsDirectory.getAbsolutePath() + SLASH + EXT_COMPS_DIR_NAME + SLASH + type
        if (File(candidate).exists()) {  // extension has FCQN as path element
            extTypePathCache.put(type, candidate)
            return candidate
        }
        candidate = project.assetsDirectory.getAbsolutePath() + SLASH +
                EXT_COMPS_DIR_NAME + SLASH + type.substring(0, type.lastIndexOf('.'))
        if (File(candidate).exists()) {  // extension has package name as path element
            extTypePathCache.put(type, candidate)
            return candidate
        }
        throw IllegalStateException("Project lacks extension directory for $type")
    }
}