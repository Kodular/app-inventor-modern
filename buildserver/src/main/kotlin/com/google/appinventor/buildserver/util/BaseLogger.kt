// -*- mode: java; c-basic-offset: 2; -*-
// Copyright © 2017 Massachusetts Institute of Technology, All rights reserved.
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0
package com.google.appinventor.buildserver.util

import com.android.utils.ILogger

/**
 * BaseLogger provides a rudimentary implementation of Android's ILogger interface.
 *
 * @author ewpatton@mit.edu (Evan W. Patton)
 */
class BaseLogger : ILogger {
    override fun error(t: Throwable?, msgFormat: String, vararg args: Object?) {
        System.err.println("[ERROR] $msgFormat")
    }

    override fun warning(msgFormat: String, vararg args: Object?) {
        System.err.println("[WARN] $msgFormat")
    }

    override fun info(msgFormat: String, vararg args: Object?) {
        System.err.println("[INFO] $msgFormat")
    }

    override fun verbose(msgFormat: String, vararg args: Object?) {
        System.err.println("[DEBUG] $msgFormat")
    }
}