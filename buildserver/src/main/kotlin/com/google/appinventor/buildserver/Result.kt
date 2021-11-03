// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2021 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0
package com.google.appinventor.buildserver

import java.util.logging.Logger

/**
 * Class used to report results of an external call or remote process.
 *
 * This class is designed to mimic the same information that would be
 * typically available when executing a separate process: an integer result
 * code and Strings for output and error.  The result value
 * (@link #SUCCESS} (0) should be used to indicate success; any other value
 * indicates failure.
 *
 *
 * While the interpretation of the text returned by [.getOutput] and
 * [.getError] is up to the clients of this class, some recommended
 * interpretations are:
 *
 *  *  The result of [.getOutput] should be displayed to the user on
 * success, and the result of [.getError] on failure, or
 *  *  The result of [.getOutput] should always be displayed to the
 * user, and the result of [.getError] on failure.
 *
 *
 * @author lizlooney@google.com (Liz Looney)
 */
class Result {
    /**
     * Returns the result code.
     */
    var result = 0
        private set

    /**
     * Returns the output String.
     */
    var output: String? = null
        private set

    /**
     * Returns the error String.  This is typically, but not necessarily,
     * displayed to the user.
     */
    var error: String? = null
        private set

    /**
     * Return the name of the form where an error occurred
     */
    // The name of the form being built when an error occurred
    var formName: String? = null
        private set

    /**
     * Creates a new Result object
     *
     * @param result the exit code
     * @param output an output string
     * @param error an error string
     * @param formName the name of the form being built when an error occurred
     */
    constructor(result: Int, output: String?, error: String?, formName: String?) {
        this.result = result
        this.output = output
        this.error = error
        this.formName = formName
    }

    /**
     * Creates a new Result object
     *
     * @param result the exit code
     * @param output an output string
     * @param error an error string
     */
    constructor(result: Int, output: String?, error: String?) {
        this.result = result
        this.output = output
        this.error = error
    }

    /**
     * Creates a new Result object
     *
     * @param successful a flag indicating whether the call succeeded
     * @param output an output string
     * @param error an error string
     */
    constructor(successful: Boolean, output: String?, error: String?) {
        result = if (successful) SUCCESS else GENERAL_FAILURE
        this.output = output
        this.error = error
    }

    /**
     * Default constructor. This constructor is required by GWT.
     */
    @SuppressWarnings("unused")
    private constructor() {
    }

    /**
     * Indicates whether this succeeded
     *
     * @return `true` if the RPC call succeeded, `false` otherwise
     */
    fun succeeded(): Boolean {
        return result == SUCCESS
    }

    /**
     * Indicates whether this failed
     *
     * @return `true` if it failed, `false` if successful
     */
    fun failed(): Boolean {
        return result != SUCCESS
    }

    companion object {
        /**
         * The value of the `result` if the RPC was successful.  Any
         * other value indicates failure.
         */
        const val SUCCESS = 0
        const val GENERAL_FAILURE = 1
        const val YAIL_GENERATION_ERROR = 2

        // Logging support
        private val LOG: Logger = Logger.getLogger(Result::class.java.getName())

        /**
         * Static constructor for a successful call.
         *
         * @param output the output string (possibly empty)
         * @param error the error string (possibly empty)
         *
         * @return information about a successful call
         */
        fun createSuccessfulResult(output: String?, error: String?): Result {
            return Result(SUCCESS, output, error)
        }

        /**
         * Static constructor for an unsuccessful call.  (The name
         * `createFailingRpcResult` was chosen over the more standard
         * `createUnsuccessfulRpcResult` because the former is more visually
         * distinguishable from
         * [.createSuccessfulResult].
         *
         * @param output the output string (possibly empty)
         * @param error the error string
         *
         * @return information about a failing call
         */
        fun createFailingResult(output: String?, error: String?): Result {
            return Result(GENERAL_FAILURE, output, error)
        }
    }
}