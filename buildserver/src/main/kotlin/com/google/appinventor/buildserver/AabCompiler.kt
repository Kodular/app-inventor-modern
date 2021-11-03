// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2021 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0
package com.google.appinventor.buildserver

import com.google.appinventor.buildserver.util.AabZipper

/**
 * This Callable class will convert the compiled files into an Android App Bundle.
 * An AAB file structure looks like this:
 * - assets.pb
 * - resources.pb
 * - native.pb
 * - manifest/AndroidManifest.xml
 * - dex/
 * - res/
 * - assets/
 * - lib/
 */
class AabCompiler(out: PrintStream?, buildDir: File?, mx: Int) : Callable<Boolean?> {
    private val out: PrintStream?
    private val buildDir: File?
    private val mx: Int
    private val aab: AabPaths
    private var originalDexDir: String? = null
    private var originalLibsDir: File? = null
    private var bundletool: String? = null
    private var jarsigner: String? = null
    private var deploy: String? = null
    private var keystore: String? = null

    private inner class AabPaths {
        var root: File? = null
        var base: File? = null
        var protoApk: File? = null
        var assetsDir: File? = null
        var dexDir: File? = null
        var libDir: File? = null
        var manifestDir: File? = null
        var resDir: File? = null
    }

    fun setDexDir(dexDir: String?): AabCompiler {
        originalDexDir = dexDir
        return this
    }

    fun setLibsDir(libsDir: File?): AabCompiler {
        originalLibsDir = libsDir
        return this
    }

    fun setBundletool(bundletool: String?): AabCompiler {
        this.bundletool = bundletool
        return this
    }

    fun setDeploy(deploy: String?): AabCompiler {
        this.deploy = deploy
        return this
    }

    fun setKeystore(keystore: String?): AabCompiler {
        this.keystore = keystore
        return this
    }

    fun setJarsigner(jarsigner: String?): AabCompiler {
        this.jarsigner = jarsigner
        return this
    }

    fun setProtoApk(apk: File?): AabCompiler {
        aab.protoApk = apk
        return this
    }

    @Override
    fun call(): Boolean {
        out.println("___________Creating structure")
        aab.root = createDir(buildDir, "aab")
        if (!createStructure()) {
            return false
        }
        out.println("___________Extracting protobuf resources")
        if (!extractProtobuf()) {
            return false
        }
        out.println("________Running bundletool")
        if (!bundletool()) {
            return false
        }
        out.println("________Signing bundle")
        return if (!jarsigner()) {
            false
        } else true
    }

    private fun createStructure(): Boolean {
        // Manifest is extracted from the protobuffed APK
        aab.manifestDir = createDir(aab.root, "manifest")

        // Resources are extracted from the protobuffed APK
        aab.resDir = createDir(aab.root, "res")

        // Assets are extracted from the protobuffed APK
        aab.assetsDir = createDir(aab.root, "assets")
        aab.dexDir = createDir(aab.root, "dex")
        val dexFiles: Array<File> = File(originalDexDir).listFiles()
        if (dexFiles != null) {
            for (dex in dexFiles) {
                if (dex.isFile()) {
                    try {
                        Files.move(dex, File(aab.dexDir, dex.getName()))
                    } catch (e: IOException) {
                        e.printStackTrace()
                        return false
                    }
                }
            }
        }
        aab.libDir = createDir(aab.root, "lib")
        val libFiles: Array<File> = originalLibsDir.listFiles()
        if (libFiles != null) {
            for (lib in libFiles) {
                try {
                    Files.move(lib, File(createDir(aab.root, "lib"), lib.getName()))
                } catch (e: IOException) {
                    e.printStackTrace()
                    return false
                }
            }
        }
        return true
    }

    private fun extractProtobuf(): Boolean {
        try {
            ZipInputStream(FileInputStream(aab.protoApk)).use { `is` ->
                var entry: ZipEntry
                val buffer = ByteArray(1024)
                while (`is`.getNextEntry().also { entry = it } != null) {
                    val n: String = entry.getName()
                    var f: File? = null
                    if (n.equals("AndroidManifest.xml")) {
                        f = File(aab.manifestDir, n)
                    } else if (n.equals("resources.pb")) {
                        f = File(aab.root, n)
                    } else if (n.startsWith("assets")) {
                        f = File(aab.assetsDir, n.substring("assets".length()))
                    } else if (n.startsWith("res")) {
                        f = File(aab.resDir, n.substring("res".length()))
                    }
                    if (f != null) {
                        f.getParentFile().mkdirs()
                        try {
                            FileOutputStream(f).use { fos ->
                                var len: Int
                                while (`is`.read(buffer).also { len = it } > 0) {
                                    fos.write(buffer, 0, len)
                                }
                            }
                        } catch (e: IOException) {
                            e.printStackTrace()
                            return false
                        }
                    }
                }
                return true
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return false
    }

    private fun bundletool(): Boolean {
        aab.base = File(buildDir, "base.zip")
        if (!AabZipper.zipBundle(aab.root, aab.base, aab.root.getName() + File.separator)) {
            return false
        }
        val bundletoolCommandLine: List<String> = ArrayList<String>()
        bundletoolCommandLine.add(System.getProperty("java.home").toString() + "/bin/java")
        bundletoolCommandLine.add("-jar")
        bundletoolCommandLine.add("-mx" + mx + "M")
        bundletoolCommandLine.add(bundletool)
        bundletoolCommandLine.add("build-bundle")
        bundletoolCommandLine.add("--modules=" + aab.base)
        bundletoolCommandLine.add("--output=$deploy")
        val bundletoolBuildCommandLine: Array<String> = bundletoolCommandLine.toArray(arrayOfNulls<String>(0))
        return Execution.execute(null, bundletoolBuildCommandLine, System.out, System.err)
    }

    private fun jarsigner(): Boolean {
        val jarsignerCommandLine: List<String> = ArrayList<String>()
        jarsignerCommandLine.add(jarsigner)
        jarsignerCommandLine.add("-sigalg")
        jarsignerCommandLine.add("SHA256withRSA")
        jarsignerCommandLine.add("-digestalg")
        jarsignerCommandLine.add("SHA-256")
        jarsignerCommandLine.add("-keystore")
        jarsignerCommandLine.add(keystore)
        jarsignerCommandLine.add("-storepass")
        jarsignerCommandLine.add("android")
        jarsignerCommandLine.add(deploy)
        jarsignerCommandLine.add("AndroidKey")
        val jarsignerSignCommandLine: Array<String> = jarsignerCommandLine.toArray(arrayOfNulls<String>(0))
        return Execution.execute(null, jarsignerSignCommandLine, System.out, System.err)
    }

    companion object {
        private fun createDir(parentDir: File?, name: String): File {
            val dir = File(parentDir, name)
            if (!dir.exists()) {
                dir.mkdir()
            }
            return dir
        }
    }

    init {
        assert(out != null)
        assert(buildDir != null)
        assert(mx > 0)
        this.out = out
        this.buildDir = buildDir
        this.mx = mx
        aab = AabPaths()
    }
}