/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.appinventor.buildserver

import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/**
 * Dex task, modified from the Android SDK to run in BuildServer.
 * Custom task to execute dx while handling dependencies.
 */
class DexExecTask(val mExecutable: String) {
//    private var mExecutable: String
    private var mOutput: String = null
    private var mDexedLibs: String? = null
    private var mVerbose = false
    private var mNoLocals = false
    private var mChildProcessRamMb = 1024
    private var mDisableDexMerger = false
    private var mainDexFile: String? = null
    private var mPredex = true

    /**
     * Sets the value of the "verbose" attribute.
     *
     * @param verbose the value.
     */
    fun setVerbose(verbose: Boolean) {
        mVerbose = verbose
    }

    fun setMainDexClassesFile(classList: String?) {
        mainDexFile = classList
        if (classList != null) {
            mPredex = false
        }
    }

    /**
     * Sets the value of the "output" attribute.
     *
     * @param output the value.
     */
    fun setOutput(output: String?) {
        mOutput = output
    }

    fun setDexedLibs(dexedLibs: String?) {
        mDexedLibs = dexedLibs
    }

    fun setPredex(predex: Boolean) {
        mPredex = predex
    }

    /**
     * Sets the value of the "nolocals" attribute.
     *
     * @param verbose the value.
     */
    fun setNoLocals(nolocals: Boolean) {
        mNoLocals = nolocals
    }

    fun setChildProcessRamMb(mb: Int) {
        mChildProcessRamMb = mb
    }

    fun setDisableDexMerger(disable: Boolean) {
        mDisableDexMerger = disable
    }

    private fun preDexLibraries(inputs: MutableList<File>): Boolean {
        if (mDisableDexMerger || inputs.size == 1) {
            // only one input, no need to put a pre-dexed version, even if this path is
            // just a jar file (case for proguard'ed builds)
            return true
        }
        synchronized(semaphore) {
            val count: Int = inputs.size
            for (i in 0 until count) {
                val input: File = inputs[i]
                if (input.isFile) {
                    // check if this libs needs to be pre-dexed
                    val fileName = getDexFileName(input)
                    val dexedLib = File(mDexedLibs, fileName)
                    val dexedLibPath: String = dexedLib.absolutePath
                    if (!dexedLib.isFile /*||dexedLib.lastModified() < input.lastModified()*/) {
                        println("Pre-Dexing ${input.absolutePath} -> $fileName")
                        if (dexedLib.isFile) {
                            dexedLib.delete()
                        }
                        val dexSuccess: Boolean = runDx(input, dexedLibPath,false)
                        if (!dexSuccess) return false
                    } else {
                        println("Using Pre-Dexed $fileName <- ${input.absolutePath}")
                    }

                    // replace the input with the pre-dex libs.
                    inputs[i] = dexedLib
                }
            }
            return true
        }
    }

    private fun getDexFileName(inputFile: File): String {
        val hashed = getHashFor(inputFile)
        return "dex-cached-$hashed.jar"
    }

    private fun getHashFor(inputFile: File): String? {
        var retval = alreadyChecked[inputFile.absolutePath]
        return retval
            ?: try {
                val hashFunction: HashFunction = Hashing.md5()
                val hashCode: HashCode = hashFunction.hashBytes(Files.readAllBytes(inputFile.toPath()))
                retval = hashCode.toString()
                alreadyChecked.put(inputFile.absolutePath, retval)
                retval
            } catch (e: IOException) {
                e.printStackTrace()
                "ERROR"
            }
        // add a hash of the original file path
    }

    fun execute(paths: List<File>): Boolean {
        // pre dex libraries if needed
        if (mPredex) {
            val successPredex = preDexLibraries(paths.toMutableList())
            if (!successPredex) return false
        }
        println("Converting compiled files and external libraries into $mOutput...")
        return runDx(paths, mOutput, mVerbose)
    }

    private fun runDx(input: File, output: String, showInputs: Boolean): Boolean {
        return runDx(Collections.singleton(input), output, showInputs)
    }

    private fun runDx(inputs: Collection<File>, output: String, showInputs: Boolean): Boolean {
        val mx = mChildProcessRamMb - 200
        val commandLineList = ArrayList<String>()
        commandLineList.add("${System.getProperty("java.home")}/bin/java")
        commandLineList.add("-mx${mx}M")
        commandLineList.add("-jar")
        commandLineList.add(mExecutable)
        commandLineList.add("--dex")
        commandLineList.add("--positions=lines")
        if (mainDexFile != null) {
            commandLineList.add("--multi-dex")
            commandLineList.add("--main-dex-list=$mainDexFile")
            commandLineList.add("--minimal-main-dex")
        }
        if (mNoLocals) {
            commandLineList.add("--no-locals")
        }
        if (mVerbose) {
            commandLineList.add("--verbose")
        }
        commandLineList.add("--output=$output")
        for (input in inputs) {
            val absPath: String = input.absolutePath
            if (showInputs) {
                println("Input: $absPath")
            }
            commandLineList.add(absPath)
        }

        // Convert command line to an array
        return Execution.execute(null, commandLineList.toTypedArray(), System.out, System.err)
    }

    protected val execTaskName: String
        get() = "dx"

    companion object {
        private val alreadyChecked: Map<String, String> = HashMap()
        private val semaphore = Any() // Used to protect dex cache creation
    }
}