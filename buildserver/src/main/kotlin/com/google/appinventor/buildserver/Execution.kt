// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2021 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0
package com.google.appinventor.buildserver

import java.io.*
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Utility class for command execution and I/O redirection.
 *
 */
object Execution {
    // Logging support
    private val LOG: Logger = Logger.getLogger(Execution::class.java.name)

    /**
     * Executes a command in a command shell.
     *
     * @param workingDir  working directory for the command
     * @param command  command to execute and its arguments
     * @param out  standard output stream to redirect to
     * @param err  standard error stream to redirect to
     * @return  `true` if the command succeeds, `false` otherwise
     */
    fun execute(workingDir: File?, command: Array<String>, out: PrintStream, err: PrintStream): Boolean {
        LOG.log(Level.INFO, "____Executing " + command.joinToString(" "))
        if (System.getProperty("os.name").startsWith("Windows")) {
            for (i in command.indices) {
                command[i] = command[i].replace("\"", "\\\"")
            }
        }
        return try {
            val process: Process = Runtime.getRuntime().exec(command, null, workingDir)
            RedirectStreamHandler(PrintWriter(out, true), process.inputStream)
            RedirectStreamHandler(PrintWriter(err, true), process.errorStream)
            process.waitFor() == 0
        } catch (e: Exception) {
            LOG.log(Level.WARNING, "____Execution failure: ", e)
            false
        }
    }

    /**
     * Executes a command, redirects standard output and standard error to
     * string buffers, and returns the process's exit code.
     *
     * @param workingDir  working directory for the command
     * @param command  command to execute and its arguments
     * @param out  standard output stream to redirect to
     * @param err  standard error stream to redirect to
     * @return  the exit code of the process
     */
    @Throws(IOException::class)
    fun execute(workingDir: File?, command: Array<String?>, out: StringBuffer, err: StringBuffer): Int {
        LOG.log(Level.INFO, "____Executing ${command.joinToString(" ")}")
        val process: Process = Runtime.getRuntime().exec(command, null, workingDir)
        val outThread: Thread = RedirectStreamToStringBuffer(out, process.inputStream)
        val errThread: Thread = RedirectStreamToStringBuffer(err, process.errorStream)
        try {
            process.waitFor()
            outThread.join()
            errThread.join()
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
        return process.exitValue()
    }

    /**
     * Input stream handler used for stdout and stderr redirection.
     */
    private class RedirectStreamHandler(
        private val output: PrintWriter,
        private val input: InputStream
    ) : Thread() {

        init {
            start()
        }

        override fun run() {
            input.bufferedReader().forEachLine { line ->
                output.println(line)
            }
        }
    }

    /**
     * Input stream handler used for stdout and stderr redirection to StringBuffer
     */
    private class RedirectStreamToStringBuffer(
        private val output: StringBuffer,
        private val input: InputStream
    ) : Thread() {

        init {
            start()
        }

        override fun run() {
            input.bufferedReader().forEachLine { line ->
                output.appendLine(line)
            }
        }
    }
}