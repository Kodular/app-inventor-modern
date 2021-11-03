// -*- mode: java; c-basic-offset: 2; -*-
// Copyright © 2017 Massachusetts Institute of Technology, All rights reserved.
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0
package com.google.appinventor.buildserver.util

import java.io.InputStream

/**
 * BaseFileWrapper provides a stub implementation of Android's IAbstractFile interface.
 *
 * @author ewpatton@mit.edu (Evan W. Patton)
 */
abstract class BaseFileWrapper : IAbstractFile {
    @get:Override
    val name: String?
        get() = null

    @get:Override
    val osLocation: String?
        get() = null

    @Override
    fun exists(): Boolean {
        return false
    }

    @get:Override
    val parentFolder: IAbstractFolder?
        get() = null

    @Override
    fun delete(): Boolean {
        return false
    }

    @get:Throws(StreamException::class)
    @get:Override
    @set:Throws(StreamException::class)
    @set:Override
    var contents: InputStream?
        get() = null
        set(source) {}

    @get:Throws(StreamException::class)
    @get:Override
    val outputStream: OutputStream?
        get() = null

    @get:Override
    val preferredWriteMode: PreferredWriteMode?
        get() = null

    @get:Override
    val modificationStamp: Long
        get() = 0
}