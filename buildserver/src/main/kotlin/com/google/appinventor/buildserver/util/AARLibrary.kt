// -*- mode: java; c-basic-offset: 2; -*-
// Copyright © 2017 Massachusetts Institute of Technology, All rights reserved.
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0
package com.google.appinventor.buildserver.util

import sun.misc.IOUtils
import java.io.*
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import javax.xml.xpath.XPathExpressionException

/**
 * AARLibrary encapsulates important information about Android Archive (AAR) files so that they
 * can be used as part of the App Inventor build process.
 *
 * @author ewpatton@mit.edu (Evan W. Patton)
 */
class AARLibrary(aar: File) {
    /**
     * Path to the AAR file modeled by the AARLibrary.
     */
    private val aarPath: File

    /**
     * Name of the simple name for the library based on its file name.
     */
    val simpleName: String

    /**
     * The package name for the library based on its AndroidManifest.
     */
    var packageName: String? = null
        private set

    /**
     * Base directory where the archive is unpacked.
     */
    private var basedir: File? = null

    /**
     * Resource directory location after unpacking.
     */
    private var resdir: File? = null
    // derived from https://sites.google.com/a/android.com/tools/tech-docs/new-build-system/aar-format
    /**
     * Manifest file location after unpacking.
     */
    private var manifest: File? = null

    /**
     * Classes.dex file location after unpacking.
     */
    private var classes: File? = null

    /**
     * R.txt file location after unpacking.
     */
    private var rtxt: File? = null

    /**
     * Set of all descendants of the resources hierarchy.
     */
    private val resources = mutableSetOf<File>()

    /**
     * Set of all assets found under the assets/ directory.
     */
    private val assets = mutableSetOf<File>()

    /**
     * Set of all JAR files found under the libs/ directory.
     */
    private val libs = mutableSetOf<File>()

    /**
     * Set of all dynamically linked libraries found under the jni/ directory.
     */
    private val jni = mutableSetOf<File>()

    /**
     * File wrapper around a zip stream to allow extracting the package name from the AndroidManifest.
     */
    private class ZipEntryWrapper(private val stream: InputStream) : BaseFileWrapper() {
        var contents: InputStream?
            get() = stream
            set(contents) {
                super.contents = contents
            }
    }

    val file: File
        get() = aarPath
    val directory: File?
        get() = basedir
    val resDirectory: File?
        get() = resdir

    fun getManifest(): File? {
        return manifest
    }

    val classesJar: File?
        get() = classes
    val rTxt: File?
        get() = rtxt

    fun getResources(): Set<File> {
        return resources
    }

    fun getAssets(): Set<File> {
        return assets
    }

    val libraries: Set<Any>
        get() = libs
    val natives: Set<Any>
        get() = jni

    /**
     * Extracts the package name from the Android Archive without needing to unzip it to a location
     * in the file system
     *
     * @param zip the input stream reading from the Android Archive.
     * @return the package name declared in the archive's AndroidManifest.xml.
     * @throws IOException if reading the input stream fails.
     */
    @Throws(IOException::class)
    private fun extractPackageName(zip: ZipFile): String {
        val entry: ZipEntry = zip.getEntry("AndroidManifest.xml")
            ?: throw IllegalArgumentException("${zip.name} does not contain AndroidManifest.xml")
        return try {
            val wrapper = ZipEntryWrapper(zip.getInputStream(entry))
            // the following call will automatically close the input stream opened above
            AndroidManifest.getPackage(wrapper)
        } catch (e: StreamException) {
            throw IOException("Exception processing AndroidManifest.xml", e)
        } catch (e: XPathExpressionException) {
            throw IOException("Exception processing AndroidManifest.xml", e)
        }
    }

    /**
     * Catalogs the file extracted from the Android Archive based on its file name.
     *
     * @param file the file name of an extracted file.
     */
    private fun catalog(file: File) {
        when {
            MANIFEST == file.name -> {
                manifest = file
            }
            CLASSES == file.name -> {
                classes = file
            }
            R_TEXT == file.name -> {
                rtxt = file
            }
            file.path.startsWith(RES_DIR) -> {
                resources.add(file)
            }
            file.path.startsWith(ASSET_DIR) -> {
                assets.add(file)
            }
            file.path.startsWith(LIBS_DIR) -> {
                libs.add(file)
            }
            file.path.startsWith(JNI_DIR) -> {
                jni.add(file)
            }
        }
    }

    /**
     * Unpacks the Android Archive to a directory in the file system. The unpacking operation will
     * create a new directory named with the archive's package name to prevent collisions with
     * other Android Archives.
     * @param path the path to where the archive will be unpacked.
     * @throws IOException if any error occurs attempting to read the archive or write new files to
     * the file system.
     */
    @Throws(IOException::class)
    fun unpackToDirectory(path: File) {
        var zip: ZipFile? = null
        try {
            zip = ZipFile(aarPath)
            packageName = extractPackageName(zip)
            basedir = File(path, packageName)
            if (!basedir.exists() && !basedir.mkdirs()) {
                throw IOException("Unable to create directory for AAR package: $basedir")
            }
            var input: InputStream? = null
            var output: OutputStream? = null
            val i: Enumeration<out ZipEntry> = zip.entries()
            while (i.hasMoreElements()) {
                val entry: ZipEntry = i.nextElement()
                val target = File(basedir, entry.name)
                if (entry.isDirectory && !target.exists() && !target.mkdirs()) {
                    throw IOException("Unable to create directory ${path.absolutePath}")
                } else if (!entry.isDirectory) {
                    try {
                        // Need to make sure the parent directory is present. Files can appear
                        // in a ZIP (AAR) file without an explicit directory object
                        val parentDir: File = target.parentFile
                        if (!parentDir.exists()) {
                            parentDir.mkdirs()
                        }
                        output = FileOutputStream(target)
                        input = zip.getInputStream(entry)
                        IOUtils.copy(input, output)
                    } finally {
                        IOUtils.closeQuietly(input)
                        IOUtils.closeQuietly(output)
                    }
                    catalog(target)
                }
            }
            resdir = File(basedir, "res")
            if (!resdir!!.exists()) {
                resdir = null
            }
        } finally {
            IOUtils.closeQuietly(zip)
        }
    }

    override fun equals(other: Any?): Boolean {
        return when {
            this === other -> true
            other == null -> false
            this.javaClass !== other.javaClass -> false
            else -> file == (other as AARLibrary).file
        }
    }

    override fun hashCode(): Int = aarPath.hashCode()

    companion object {
        private const val MANIFEST = "AndroidManifest.xml"
        private const val CLASSES = "classes.jar"
        private const val R_TEXT = "R.txt"
        private const val RES_DIR = "res/"
        private const val ASSET_DIR = "assets/"
        private const val LIBS_DIR = "libs/"
        private const val JNI_DIR = "jni/"
    }

    /**
     * Constructs a new AARLibrary.
     *
     * @param aar the file representation of the archive (a .aar file).
     */
    init {
        aarPath = aar
        val temp: String = aar.name
        simpleName = temp.substring(0, temp.length - 4)
    }
}