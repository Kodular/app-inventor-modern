// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2021 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0
package com.google.appinventor.buildserver

import java.util.logging.Logger

/**
 * Helper methods to deal with type names and signatures.
 *
 */
object Signatures {
    // Logging support
    private val LOG: Logger = Logger.getLogger(Signatures::class.java.name)

    /*
   * Extracts the package name from a compound name (assuming the package name to be all but the
   * last component of the compound name).
   */
    private fun getPackageName(name: String, separator: Char): String {
        val index: Int = name.lastIndexOf(separator)
        return if (index < 0) "" else name.substring(0, index)
    }

    /*
   * Extracts the class name from a compound name (assuming the class name to be the last component
   * of the compound name).
   */
    private fun getClassName(name: String, separator: Char): String {
        val index: Int = name.lastIndexOf(separator)
        return if (index < 0) name else name.substring(index + 1)
    }

    /**
     * Returns the package name part of an dot-qualified class name.
     *
     * @param qualifiedName  qualified class name
     * @return  package name
     */
    fun getPackageName(qualifiedName: String): String {
        return getPackageName(qualifiedName, '.')
    }

    /**
     * Returns the class name part of an dot-qualified class name.
     *
     * @param qualifiedName  qualified class name
     * @return  class name
     */
    fun getClassName(qualifiedName: String): String {
        return getClassName(qualifiedName, '.')
    }

    /**
     * Returns the package name part of an internal name (according to the Java
     * VM specification).
     *
     * @param internalName  Java VM style internal name
     * @return  package name
     */
    fun getInternalPackageName(internalName: String): String {
        return getPackageName(internalName, '/')
    }

    /**
     * Returns the class name part of an internal name (according to the Java
     * VM specification).
     *
     * @param internalName  Java VM style internal name
     * @return  class name
     */
    fun getInternalClassName(internalName: String): String {
        return getClassName(internalName, '/')
    }
}