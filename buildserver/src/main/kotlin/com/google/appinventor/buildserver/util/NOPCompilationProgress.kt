// -*- mode: java; c-basic-offset: 2; -*-
// Copyright © 2017 Massachusetts Institute of Technology, All rights reserved.
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0
package com.google.appinventor.buildserver.util

import org.eclipse.jdt.core.compiler.CompilationProgress

/**
 * NOPCompilationProgress is an implementation of JDT Core's CompilationProgress that does nothing.
 * It is passed to the BatchCompiler during the generation of R.class files from the application
 * and any AAR library dependencies.
 *
 * @author ewpatton@mit.edu
 */
class NOPCompilationProgress : CompilationProgress() {
    override fun begin(remainingWork: Int) {
        // do nothing
    }

    override fun done() {
        // do nothing
    }

    // never cancelable
    @get:Override
    val isCanceled: Boolean
        get() =// never cancelable
            false

    override fun setTaskName(name: String?) {
        // do nothing
    }

    override fun worked(workIncrement: Int, remainingWork: Int) {
        // do nothing
    }
}