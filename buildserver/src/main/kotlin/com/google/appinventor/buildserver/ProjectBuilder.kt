// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2021 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0
package com.google.appinventor.buildserver

import com.google.appinventor.common.utils.StringUtils

/**
 * Provides support for building Young Android projects.
 *
 * @author markf@google.com (Mark Friedman)
 */
class ProjectBuilder {
    private var outputApk: File? = null
    private var outputKeystore: File? = null
    private var saveKeystore = false
    fun getOutputApk(): File? {
        return outputApk
    }

    fun getOutputKeystore(): File? {
        return outputKeystore
    }

    fun build(
        userName: String?,
        inputZip: ZipFile?,
        outputDir: File?,
        outputFileName: String?,
        isForCompanion: Boolean,
        isForEmulator: Boolean,
        includeDangerousPermissions: Boolean,
        extraExtensions: List<String>,
        childProcessRam: Int,
        dexCachePath: String?,
        reporter: BuildServer.ProgressReporter?,
        isAab: Boolean
    ): Result {
        return try {
            // Download project files into a temporary directory
            val projectRoot: File = createNewTempDir()
            LOG.info("temporary project root: " + projectRoot.getAbsolutePath())
            try {
                val sourceFiles: List<String>
                sourceFiles = try {
                    extractProjectFiles(inputZip, projectRoot)
                } catch (e: IOException) {
                    LOG.severe("unexpected problem extracting project file from zip")
                    return Result.createFailingResult("", "Problems processing zip file.")
                }
                val keyStoreFile = File(projectRoot, KEYSTORE_FILE_NAME)
                var keyStorePath: String? = keyStoreFile.getPath()
                if (!keyStoreFile.exists()) {
                    keyStorePath = createKeyStore(userName, projectRoot, KEYSTORE_FILE_NAME)
                    saveKeystore = true
                }

                // Create project object from project properties file.
                val project: Project = getProjectProperties(projectRoot)
                val buildTmpDir = File(projectRoot, "build/tmp")
                buildTmpDir.mkdirs()

                // Prepare for redirection of compiler message output
                val output = ByteArrayOutputStream()
                val console = PrintStream(output)
                val errors = ByteArrayOutputStream()
                val userErrors = PrintStream(errors)
                val componentTypes = getComponentTypes(sourceFiles, project.getAssetsDirectory())
                if (isForCompanion) {
                    componentTypes.addAll(allComponentTypes)
                }
                if (extraExtensions != null) {
                    System.err.println("Including extension: " + Arrays.toString(extraExtensions))
                    Collections.addAll(componentTypes, extraExtensions)
                }
                val componentBlocks = getComponentBlocks(sourceFiles)

                // Invoke YoungAndroid compiler
                val success: Boolean = Compiler.compile(
                    project, componentTypes, componentBlocks, console, console, userErrors,
                    isForCompanion, isForEmulator, includeDangerousPermissions, keyStorePath,
                    childProcessRam, dexCachePath, outputFileName, reporter, isAab
                )
                console.close()
                userErrors.close()

                // Retrieve compiler messages and convert to HTML and log
                val srcPath: String = projectRoot.getAbsolutePath().toString() + "/" + PROJECT_DIRECTORY + "/../src/"
                val messages = processCompilerOutput(
                    output.toString(PathUtil.DEFAULT_CHARSET),
                    srcPath
                )
                if (success) {
                    // Locate output file
                    var fileName = outputFileName
                    if (fileName == null) {
                        fileName = project.getProjectName().toString() + if (isAab) ".aab" else ".apk"
                    }
                    val outputFile = File(
                        projectRoot,
                        "build/deploy/$fileName"
                    )
                    if (!outputFile.exists()) {
                        LOG.warning("Young Android build - $outputFile does not exist")
                    } else {
                        outputApk = File(outputDir, outputFile.getName())
                        Files.copy(outputFile, outputApk)
                        if (saveKeystore) {
                            outputKeystore = File(outputDir, KEYSTORE_FILE_NAME)
                            Files.copy(keyStoreFile, outputKeystore)
                        }
                    }
                }
                Result(success, messages, errors.toString(PathUtil.DEFAULT_CHARSET))
            } finally {
                // On some platforms (OS/X), the java.io.tmpdir contains a symlink. We need to use the
                // canonical path here so that Files.deleteRecursively will work.

                // Note (ralph):  deleteRecursively has been removed from the guava-11.0.1 lib
                // Replacing with deleteDirectory, which is supposed to delete the entire directory.
                FileUtils.deleteQuietly(File(projectRoot.getCanonicalPath()))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.createFailingResult("", "Server error performing build")
        }
    }

    @Throws(IOException::class)
    private fun extractProjectFiles(inputZip: ZipFile?, projectRoot: File): ArrayList<String> {
        val projectFileNames: ArrayList<String> = Lists.newArrayList()
        val inputZipEnumeration: Enumeration<out ZipEntry?> = inputZip.entries()
        while (inputZipEnumeration.hasMoreElements()) {
            val zipEntry: ZipEntry = inputZipEnumeration.nextElement()
            val extractedInputStream: InputStream = inputZip.getInputStream(zipEntry)
            val extractedFile = File(projectRoot, zipEntry.getName())
            LOG.info("extracting " + extractedFile.getAbsolutePath().toString() + " from input zip")
            Files.createParentDirs(extractedFile) // Do I need this?
            Files.copy(
                object : InputSupplier<InputStream?>() {
                    @get:Throws(IOException::class)
                    val input: InputStream
                        get() = extractedInputStream
                },
                extractedFile
            )
            projectFileNames.add(extractedFile.getPath())
        }
        return projectFileNames
    }

    /*
   * Loads the project properties file of a Young Android project.
   */
    private fun getProjectProperties(projectRoot: File): Project {
        return Project(projectRoot.getAbsolutePath().toString() + "/" + PROJECT_PROPERTIES_FILE_NAME)
    }

    companion object {
        // Logging support
        private val LOG: Logger = Logger.getLogger(ProjectBuilder::class.java.getName())
        private const val MAX_COMPILER_MESSAGE_LENGTH = 160

        // Project folder prefixes
        // TODO(user): These constants are (or should be) also defined in
        // appengine/src/com/google/appinventor/server/project/youngandroid/YoungAndroidProjectService
        // They should probably be in some place shared with the server
        private const val PROJECT_DIRECTORY = "youngandroidproject"
        private const val PROJECT_PROPERTIES_FILE_NAME = PROJECT_DIRECTORY + "/" +
                "project.properties"
        private val KEYSTORE_FILE_NAME: String = YoungAndroidConstants.PROJECT_KEYSTORE_LOCATION
        private val FORM_PROPERTIES_EXTENSION: String = YoungAndroidConstants.FORM_PROPERTIES_EXTENSION
        private val YAIL_EXTENSION: String = YoungAndroidConstants.YAIL_EXTENSION
        private val CODEBLOCKS_SOURCE_EXTENSION: String = YoungAndroidConstants.CODEBLOCKS_SOURCE_EXTENSION
        private val ALL_COMPONENT_TYPES: String = Compiler.RUNTIME_FILES_DIR.toString() + "simple_components.txt"

        /**
         * Creates a new directory beneath the system's temporary directory (as
         * defined by the `java.io.tmpdir` system property), and returns its
         * name. The name of the directory will contain the current time (in millis),
         * and a random number.
         *
         *
         * This method assumes that the temporary volume is writable, has free
         * inodes and free blocks, and that it will not be called thousands of times
         * per second.
         *
         * @return the newly-created directory
         * @throws IllegalStateException if the directory could not be created
         */
        private fun createNewTempDir(): File {
            val baseDir = File(System.getProperty("java.io.tmpdir"))
            val baseNamePrefix: String = System.currentTimeMillis().toString() + "_" + Math.random() + "-"
            val TEMP_DIR_ATTEMPTS = 10000
            for (counter in 0 until TEMP_DIR_ATTEMPTS) {
                val tempDir = File(baseDir, baseNamePrefix + counter)
                if (tempDir.exists()) {
                    continue
                }
                if (tempDir.mkdir()) {
                    return tempDir
                }
            }
            throw IllegalStateException(
                "Failed to create directory within "
                        + TEMP_DIR_ATTEMPTS + " attempts (tried "
                        + baseNamePrefix + "0 to " + baseNamePrefix + (TEMP_DIR_ATTEMPTS - 1) + ')'
            )
        }

        @get:Throws(IOException::class)
        private val allComponentTypes: Set<String>
            private get() {
                val compSet: Set<String> = Sets.newHashSet()
                val components: Array<String> = Resources.toString(
                    ProjectBuilder::class.java.getResource(ALL_COMPONENT_TYPES), Charsets.UTF_8
                ).split("\n")
                for (component in components) {
                    compSet.add(component)
                }
                return compSet
            }

        @Throws(IOException::class, JSONException::class)
        private fun getComponentTypes(files: List<String>, assetsDir: File): Set<String> {
            val nameTypeMap = createNameTypeMap(assetsDir)
            val componentTypes: Set<String> = Sets.newHashSet()
            for (f in files) {
                if (f.endsWith(".scm")) {
                    val scmFile = File(f)
                    val scmContent = String(
                        Files.toByteArray(scmFile),
                        PathUtil.DEFAULT_CHARSET
                    )
                    for (compName in getTypesFromScm(scmContent)) {
                        componentTypes.add(nameTypeMap[compName])
                    }
                }
            }
            return componentTypes
        }

        /**
         * Constructs a mapping of component types to the blocks of each type used in
         * the project files. Properties specified in the designer are considered
         * blocks for the purposes of this operation.
         *
         * @param files A list of files contained in the project.
         * @return A mapping of component type names to sets of block names used in
         * the project
         * @throws IOException if any of the files named in `files` cannot be
         * read
         */
        @Throws(IOException::class)
        private fun getComponentBlocks(files: List<String>): Map<String?, Set<String>> {
            val result: Map<String?, Set<String>> = HashMap()
            for (f in files) {
                if (f.endsWith(".bky")) {
                    val bkyFile = File(f)
                    val bkyContent: String = Files.toString(bkyFile, StandardCharsets.UTF_8)
                    for (entry in FormPropertiesAnalyzer.getComponentBlocksFromBlocksFile(bkyContent).entrySet()) {
                        if (result.containsKey(entry.getKey())) {
                            result[entry.getKey()].addAll(entry.getValue())
                        } else {
                            result.put(entry.getKey(), entry.getValue())
                        }
                    }
                } else if (f.endsWith(".scm")) {
                    val scmFile = File(f)
                    val scmContent: String = Files.toString(scmFile, StandardCharsets.UTF_8)
                    for (entry in FormPropertiesAnalyzer.getComponentBlocksFromSchemeFile(scmContent).entrySet()) {
                        if (result.containsKey(entry.getKey())) {
                            result[entry.getKey()].addAll(entry.getValue())
                        } else {
                            result.put(entry.getKey(), entry.getValue())
                        }
                    }
                }
            }
            return result
        }

        /**
         * In ode code, component names are used to identify a component though the
         * variables storing component names appear to be "type". While there's no
         * harm in ode, here in build server, they need to be separated.
         * This method returns a name-type map, mapping the component names used in
         * ode to the corresponding type, aka fully qualified name. The type will be
         * used to build apk.
         */
        @Throws(IOException::class, JSONException::class)
        private fun createNameTypeMap(assetsDir: File): Map<String, String> {
            val nameTypeMap: Map<String, String> = Maps.newHashMap()
            val simpleCompsJson = JSONArray(
                Resources.toString(
                    ProjectBuilder::class.java.getResource("/files/simple_components.json"),
                    Charsets.UTF_8
                )
            )
            for (i in 0 until simpleCompsJson.length()) {
                val simpleCompJson: JSONObject = simpleCompsJson.getJSONObject(i)
                nameTypeMap.put(
                    simpleCompJson.getString("name"),
                    simpleCompJson.getString("type")
                )
            }
            val extCompsDir = File(assetsDir, "external_comps")
            if (!extCompsDir.exists()) {
                return nameTypeMap
            }
            for (extCompDir in extCompsDir.listFiles()) {
                if (!extCompDir.isDirectory()) {
                    continue
                }
                var extCompJsonFile = File(extCompDir, "component.json")
                if (extCompJsonFile.exists()) {
                    val extCompJson = JSONObject(
                        Resources.toString(
                            extCompJsonFile.toURI().toURL(), Charsets.UTF_8
                        )
                    )
                    nameTypeMap.put(
                        extCompJson.getString("name"),
                        extCompJson.getString("type")
                    )
                } else {  // multi-extension package
                    extCompJsonFile = File(extCompDir, "components.json")
                    if (extCompJsonFile.exists()) {
                        val extCompJson = JSONArray(
                            Resources.toString(
                                extCompJsonFile.toURI().toURL(), Charsets.UTF_8
                            )
                        )
                        for (i in 0 until extCompJson.length()) {
                            val extCompDescriptor: JSONObject = extCompJson.getJSONObject(i)
                            nameTypeMap.put(
                                extCompDescriptor.getString("name"),
                                extCompDescriptor.getString("type")
                            )
                        }
                    }
                }
            }
            return nameTypeMap
        }

        @Throws(IOException::class)
        fun createKeyStore(userName: String?, projectRoot: File, keystoreFileName: String?): String? {
            val keyStoreFile = File(projectRoot.getPath(), keystoreFileName)

            /* Note: must expire after October 22, 2033, to be in the Android
    * marketplace.  Android docs recommend "10000" as the expiration # of
    * days.
    *
    * For DNAME, US may not the right country to assign it to.
    */
            val keytoolCommandline = arrayOf(
                System.getProperty("java.home").toString() + "/bin/keytool",
                "-genkey",
                "-keystore", keyStoreFile.getAbsolutePath(),
                "-alias", "AndroidKey",
                "-keyalg", "RSA",
                "-dname", "CN=" + quotifyUserName(userName) + ", O=AppInventor for Android, C=US",
                "-validity", "10000",
                "-storepass", "android",
                "-keypass", "android"
            )
            if (Execution.execute(null, keytoolCommandline, System.out, System.err)) {
                if (keyStoreFile.length() > 0) {
                    return keyStoreFile.getAbsolutePath()
                }
            }
            return null
        }

        @VisibleForTesting
        fun getTypesFromScm(scm: String): Set<String> {
            return FormPropertiesAnalyzer.getComponentTypesFromFormFile(scm)
        }

        @VisibleForTesting
        fun processCompilerOutput(output: String, srcPath: String?): String {
            // First, remove references to the temp source directory from the messages.
            var messages: String = output.replace(srcPath, "")

            // Then, format warnings and errors nicely.
            try {
                // Split the messages by \n and process each line separately.
                val lines: Array<String> = messages.split("\n")
                val pattern: Pattern = Pattern.compile("(.*?):(\\d+):\\d+: (error|warning)?:? ?(.*?)")
                val sb = StringBuilder()
                var skippedErrorOrWarning = false
                for (line in lines) {
                    val matcher: Matcher = pattern.matcher(line)
                    if (matcher.matches()) {
                        // Determine whether it is an error or warning.
                        var kind: String
                        var spanClass: String
                        // Scanner messages do not contain either 'error' or 'warning'.
                        // I treat them as errors because they prevent compilation.
                        if ("warning".equals(matcher.group(3))) {
                            kind = "WARNING"
                            spanClass = "compiler-WarningMarker"
                        } else {
                            kind = "ERROR"
                            spanClass = "compiler-ErrorMarker"
                        }

                        // Extract the filename, lineNumber, and message.
                        val filename: String = matcher.group(1)
                        val lineNumber: String = matcher.group(2)
                        var text: String = matcher.group(4)

                        // If the error/warning is in a yail file, generate a div and append it to the
                        // StringBuilder.
                        if (filename.endsWith(YoungAndroidConstants.YAIL_EXTENSION)) {
                            skippedErrorOrWarning = false
                            sb.append(
                                "<div><span class='" + spanClass + "'>" + kind + "</span>: " +
                                        StringUtils.escape(filename) + " line " + lineNumber + ": " +
                                        StringUtils.escape(text) + "</div>"
                            )
                        } else {
                            // The error/warning is in runtime.scm. Don't append it to the StringBuilder.
                            skippedErrorOrWarning = true
                        }

                        // Log the message, first truncating it if it is too long.
                        if (text.length() > MAX_COMPILER_MESSAGE_LENGTH) {
                            text = text.substring(0, MAX_COMPILER_MESSAGE_LENGTH)
                        }
                    } else {
                        // The line isn't an error or a warning. This is expected.
                        // If the line begins with two spaces, it is a continuation of the previous
                        // error/warning.
                        if (line.startsWith("  ")) {
                            // If we didn't skip the most recent error/warning, append the line to our
                            // StringBuilder.
                            if (!skippedErrorOrWarning) {
                                sb.append(StringUtils.escape(line)).append("<br>")
                            }
                        } else {
                            skippedErrorOrWarning = false
                            // We just append the line to our StringBuilder.
                            sb.append(StringUtils.escape(line)).append("<br>")
                        }
                    }
                }
                messages = sb.toString()
            } catch (e: Exception) {
                // Report exceptions that happen during the processing of output, but don't make the
                // whole build fail.
                e.printStackTrace()

                // We were not able to process the output, so we just escape for HTML.
                messages = StringUtils.escape(messages)
            }
            return messages
        }

        /*
   * Adds quotes around the given userName and encodes embedded quotes as \".
   */
        private fun quotifyUserName(userName: String?): String {
            Preconditions.checkNotNull(userName)
            val length: Int = userName!!.length()
            val sb = StringBuilder(length + 2)
            sb.append('"')
            for (i in 0 until length) {
                val ch: Char = userName.charAt(i)
                if (ch == '"') {
                    sb.append('\\').append(ch)
                } else {
                    sb.append(ch)
                }
            }
            sb.append('"')
            return sb.toString()
        }
    }
}