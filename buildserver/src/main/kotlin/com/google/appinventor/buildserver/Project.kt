// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2021 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0
package com.google.appinventor.buildserver

import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.*
import java.util.logging.Logger

/**
 * This class gives access to Young Android project files.
 *
 *
 * A Young Android project file is essentially a Java properties file.
 *
 * @author markf@google.com (Mark Friedman)
 */
class Project {
    /**
     * Representation of a source file containing its name and file location.
     */
    class SourceDescriptor(
        /**
         * Returns the qualified name of the class defined by the source file.
         *
         * @return  class name of source file
         */
        val qualifiedName: String, // Qualified name of the class defined by the source file
        private val file: File // File descriptor for the source
    ) {

        /**
         * Returns a file descriptor for the source file
         *
         * @return  file descriptor
         */
        fun getFile(): File = file
    }

    // Table containing project properties
    private val properties = Properties()

    /**
     * Returns the project directory. This directory contains the project.properties file.
     *
     * @return  project directory
     */
    // Project directory. This directory contains the project.properties file.
    private var projectDir: String? = null

    // Build output directory override, or null.
    private var buildDirOverride: String? = null

    // List of source files
    private var sources: MutableList<SourceDescriptor>? = null


    /**
     * Creates a new Young Android project descriptor.
     *
     * @param file  project file
     */
    constructor(file: File) {
        val parentFile: File = requireNotNull(file.parentFile)
        projectDir = parentFile.absolutePath

        // Load project file
        FileInputStream(file).use {
            properties.load(it)
        }
    }

    /**
     * Creates a new Young Android project descriptor.
     *
     * @param projectFile  path to project file
     * @param buildDirOverride  build output directory override, or null
     */
    constructor(projectFile: String, buildDirOverride: String? = null) : this(File(projectFile)) {
        this.buildDirOverride = buildDirOverride
    }
    /**
     * Returns the name of the main form class
     *
     * @return  main form class name
     */
    /**
     * Sets the name of the main form class.
     *
     * @param main  main form class name
     */
    var mainClass: String?
        get() = properties.getProperty(MAINTAG)
        set(main) {
            properties.setProperty(MAINTAG, main)
        }
    /**
     * Returns the name of the project (application).
     *
     * @return  project name
     */
    /**
     * Sets the name of the project (application)
     *
     * @param name  project name
     */
    var projectName: String?
        get() = properties.getProperty(NAMETAG)
        set(name) {
            properties.setProperty(NAMETAG, name)
        }
    /**
     * Returns the name of the icon
     *
     * @return  icon name
     */
    /**
     * Sets the name of the icon
     *
     * @param icon  icon name
     */
    var icon: String?
        get() = properties.getProperty(ICONTAG)
        set(icon) {
            properties.setProperty(ICONTAG, icon)
        }
    /**
     * Returns the version code.
     *
     * @return  version code
     */
    /**
     * Sets the version code.
     *
     * @param vcode  version code
     */
    var vCode: String?
        get() = properties.getProperty(VCODETAG)
        set(vcode) {
            properties.setProperty(VCODETAG, vcode)
        }
    /**
     * Returns the version name.
     *
     * @return  version name
     */
    /**
     * Sets the version name.
     *
     * @param vname  version name
     */
    var vName: String?
        get() = properties.getProperty(VNAMETAG)
        set(vname) {
            properties.setProperty(VNAMETAG, vname)
        }// Older Projects won't have this

    /**
     * gets the useslocation property
     *
     * @return useslocation property
     */
    val usesLocation: String
        get() = properties.getProperty(USESLOCATIONTAG, "False")
    /**
     * Sets the app name.
     *
     * @param aname  app name
     */
    /**
     * Returns the app name.
     *
     * @return  app name
     */
    var aName: String?
        get() = properties.getProperty(ANAMETAG)
        set(aname) {
            properties.setProperty(ANAMETAG, aname)
        }

    /**
     * Returns the minimum SDK desired for the app.
     *
     * @return  the minimum Android sdk
     */
    val minSdk: String?
        get() = properties.getProperty(ANDROID_MIN_SDK_TAG, "7")

    /**
     * Returns whether the ActionBar should be enabled in the project.
     *
     * @return  "true" if the ActionBar should be included in the project.
     */
    val actionBar: String
        get() = properties.getProperty(ACTIONBAR_TAG, "false")

    /**
     * Returns the primary color provided by the user.
     *
     * @return  primary color, or null if the default is requested
     */
    val primaryColor: String?
        get() = properties.getProperty(COLOR_PRIMARYTAG)

    /**
     * Returns the dark primary color provided by the user.
     *
     * @return  dark primary color, or null if the default is requested
     */
    val primaryColorDark: String?
        get() = properties.getProperty(COLOR_PRIMARY_DARKTAG)

    /**
     * Returns the accent color provided by the user.
     *
     * @return  accent color, or null if the default is requested
     */
    val accentColor: String?
        get() = properties.getProperty(COLOR_ACCENTTAG)

    /**
     * Returns the theme for the project set by the user.
     *
     * @return  theme, or null if the default is requested
     */
    val theme: String?
        get() = properties.getProperty(COLOR_THEMETAG)

    /**
     * Returns the location of the assets directory.
     *
     * @return  assets directory
     */
    val assetsDirectory: File
        get() = File(projectDir, properties.getProperty(ASSETSTAG))

    /**
     * Returns the location of the build output directory.
     *
     * @return  build output directory
     */
    val buildDirectory: File
        get() = if (buildDirOverride != null) File(buildDirOverride) else File(projectDir, properties.getProperty(BUILDTAG))

    /*
   * Recursively visits source directories and adds found Young Android source files to the list of
   * source files.
   */
    private fun visitSourceDirectories(root: String, file: File) {
        if (file.isDirectory) {
            // Recursively visit nested directories.
            file.walk().filter { it.isFile }.forEach { child ->
                visitSourceDirectories(root, child)
            }
        } else {
            // Add Young Android source files to the source file list
            if (file.name.endsWith(YoungAndroidConstants.YAIL_EXTENSION)) {
                val absName: String = file.absolutePath
                val name: String = absName.substring(root.length + 1, absName.length - YoungAndroidConstants.YAIL_EXTENSION.length)
                sources?.add(SourceDescriptor(name.replace(File.separatorChar, '.'), file))
            }
        }
    }

    /**
     * Returns a list of Yail files in the project.
     *
     * @return  list of source files
     */
    fun getSources(): List<SourceDescriptor> {
        // Lazily discover source files
        if (sources == null) {
            sources = mutableListOf()
            val sourceTag: String = properties.getProperty(SOURCETAG)
            for (sourceDir in sourceTag.split(",")) {
                val dir = File(projectDir + File.separatorChar + sourceDir)
                visitSourceDirectories(dir.absolutePath, dir)
            }
        }
        return sources!!
    }

    companion object {
        /*
   * Property tags defined in the project file:
   *
   *    main - qualified name of main form class
   *    name - application name
   *    icon - application icon
   *    versioncode - version code
   *    versionname - version name
   *    source - comma separated list of source root directories
   *    assets - assets directory (for image and data files bundled with the application)
   *    build - output directory for the compiler
   *    useslocation - flag indicating whether or not the project uses locations
   *    aname - the human-readable application name
   *    androidminsdk - the minimum Android sdk required for the app
   *    theme - the base theme for the app
   *    color.primary - the primary color for the theme
   *    color.primary.dark - the dark color for the theme (not yet applicable)
   *    color.accent - the accent color used in the app theme
   */
        private const val MAINTAG = "main"
        private const val NAMETAG = "name"
        private const val ICONTAG = "icon"
        private const val SOURCETAG = "source"
        private const val VCODETAG = "versioncode"
        private const val VNAMETAG = "versionname"
        private const val ASSETSTAG = "assets"
        private const val BUILDTAG = "build"
        private const val USESLOCATIONTAG = "useslocation"
        private const val ANAMETAG = "aname"
        private const val ANDROID_MIN_SDK_TAG = "androidminsdk"
        private const val ACTIONBAR_TAG = "actionbar"
        private const val COLOR_THEMETAG = "theme"
        private const val COLOR_PRIMARYTAG = "color.primary"
        private const val COLOR_PRIMARY_DARKTAG = "color.primary.dark"
        private const val COLOR_ACCENTTAG = "color.accent"

        // Logging support
        private val LOG: Logger = Logger.getLogger(Project::class.java.name)
    }
}