// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2021 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0
package com.google.appinventor.buildserver

import java.util.logging.Logger

/**
 * Constants and utility methods related to storage.
 *
 * Based largely on com.google.appinventor.shared.storage.StorageUtil
 *
 * @author markf@google.com (Herbert Czymontek)
 */
object PathUtil {
    // Logging support
    private val LOG: Logger = Logger.getLogger(PathUtil::class.java.getName())

    // Default character encoding
    const val DEFAULT_CHARSET = "Cp1252"

    /**
     * Gets the final component from a path.  This assumes that path components
     * are separated by forward slashes.
     *
     * @param path The path to apply the basename operation to.
     * @return path, with any leading directory elements removed
     */
    fun basename(path: String): String {
        if (path.length() === 0) {
            return path
        }
        val pos: Int = path.lastIndexOf("/")
        return if (pos == -1) {
            path
        } else {
            path.substring(pos + 1)
        }
    }

    /**
     *
     * Determines the parent directory of the given path.  This is similar to
     * dirname(1).  This assumes that path components are separated by forward
     * slashes.
     *
     *
     * The returned path omits the last slash and trailing component;
     * for example, "/foo/bar.txt" becomes "/foo".  There are a special cases:
     *
     *  *  If the last slash is the first character in the input,
     * the return value is "/".
     *  *  If there are no slashes in the input, "." is returned.
     *
     *
     *
     * @param path the path to strip
     * @return the parent path, as described above
     */
    fun dirname(path: String): String {
        val lastSlash: Int = path.lastIndexOf("/")
        return if ("/".equals(path) || lastSlash == 0) {
            "/"
        } else if (lastSlash == -1) {
            "."
        } else {
            path.substring(0, lastSlash)
        }
    }

    /**
     * Returns a copy of the given path with the extension omitted.
     *
     * @param path the path
     * @return path, with the extension elements omitted.
     */
    fun trimOffExtension(path: String): String {
        val lastSlash: Int = path.lastIndexOf('/')
        val lastDot: Int = path.lastIndexOf('.')
        return if (lastDot > lastSlash) path.substring(0, lastDot) else path
    }

    /**
     * Returns the package name part of an dot-qualified class name.
     *
     * @param qualifiedName  qualified class name
     * @return  package name
     */
    fun getPackageName(qualifiedName: String): String {
        val index: Int = qualifiedName.lastIndexOf('.')
        return if (index < 0) "" else qualifiedName.substring(0, index)
    }

    /**
     * Returns the content type for the given filePath.
     */
    fun getContentTypeForFilePath(filePath: String): String {
        var filePath = filePath
        filePath = filePath.toLowerCase()
        if (filePath.endsWith(".gif")) {
            return "image/gif"
        }
        if (filePath.endsWith(".jpg") || filePath.endsWith(".jpeg")) {
            return "image/jpeg"
        }
        if (filePath.endsWith(".png")) {
            return "image/png"
        }
        if (filePath.endsWith(".apk")) {
            return "application/vnd.android.package-archive; charset=utf-8"
        }
        if (filePath.endsWith(".zip")) {
            return "application/zip; charset=utf-8"
        }
        return if (filePath.endsWith(".keystore")) {
            "application/octet-stream"
        } else "text/plain; charset=utf-8"

        // default
    }

    /**
     * Returns true if the given filePath refers an image file.
     */
    fun isImageFile(filePath: String): Boolean {
        val contentType = getContentTypeForFilePath(filePath)
        return contentType.startsWith("image/")
    }

    /**
     * Returns true if the given filePath refers a text file.
     */
    fun isTextFile(filePath: String): Boolean {
        val contentType = getContentTypeForFilePath(filePath)
        return contentType.startsWith("text/")
    }
}