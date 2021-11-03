// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2021 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0
package com.google.appinventor.buildserver

import com.google.common.base.Joiner

/**
 * Utility class for command execution and I/O redirection.
 *
 */
object Execution {
    // Logging support
    private val LOG: Logger = Logger.getLogger(Execution::class.java.getName())
    private val joiner: Joiner = Joiner.on(" ")

    /**
     * Executes a command in a command shell.
     *
     * @param workingDir  working directory for the command
     * @param command  command to execute and its arguments
     * @param out  standard output stream to redirect to
     * @param err  standard error stream to redirect to
     * @return  `true` if the command succeeds, `false` otherwise
     */
    fun execute(
        workingDir: File?, command: Array<String>, out: PrintStream?,
        err: PrintStream?
    ): Boolean {
        LOG.log(Level.INFO, "____Executing " + joiner.join(command))
        if (System.getProperty("os.name").startsWith("Windows")) {
            for (i in command.indices) {
                command[i] = command[i].replace("\"", "\\\"")
            }
        }
        return try {
            val process: Process = Runtime.getRuntime().exec(command, null, workingDir)
            RedirectStreamHandler(PrintWriter(out, true), process.getInputStream())
            RedirectStreamHandler(PrintWriter(err, true), process.getErrorStream())
            process.waitFor() === 0
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
    fun execute(
        workingDir: File?, command: Array<String?>?, out: StringBuffer?,
        err: StringBuffer?
    ): Int {
        LOG.log(Level.INFO, "____Executing ${joiner.join(command)}")
        val process: Process = Runtime.getRuntime().exec(command, null, workingDir)
        val outThread: Thread = RedirectStreamToStringBuffer(out, process.getInputStream())
        val errThread: Thread = RedirectStreamToStringBuffer(err, process.getErrorStream())
        try {
            process.waitFor()
            outThread.join()
            errThread.join()
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
        return process.exitValue()
    }

    /*
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
            try {
                val reader = BufferedReader(InputStreamReader(input))
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    output.println(line)
                }
            } catch (ioe: IOException) {
                // OK to ignore...
                LOG.log(Level.WARNING, "____I/O Redirection failure: ", ioe)
            }
        }
    }

    /*
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
            try {
                val reader = BufferedReader(InputStreamReader(input))
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    output.append(line).append("\n")
                }
            } catch (ioe: IOException) {
                // OK to ignore...
                LOG.log(Level.WARNING, "____I/O Redirection failure: ", ioe)
            }
        }
    }
}