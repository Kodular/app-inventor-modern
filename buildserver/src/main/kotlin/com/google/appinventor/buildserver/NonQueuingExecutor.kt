// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2021 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0
package com.google.appinventor.buildserver

import java.util.concurrent.Executor

/**
 * An [Executor] used for executing tasks using a thread pool.
 *
 *
 * This ExecutorService allows only a certain number of simultaneous tasks.
 * Additional tasks are rejected, not queued.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
internal class NonQueuingExecutor
/**
 * Creates a NonQueuingExecutor.
 *
 * @param maxActiveTasks the maximum number of active tasks
 */(  // The maximum number of active tasks. O means unlimited.
    val maxActiveTasks: Int
) : Executor {
    private val activeTaskCount: AtomicInteger = AtomicInteger(0)
    private val completedTaskCount: AtomicInteger = AtomicInteger(0)

    // lockExecute is used so that the execute method can be executed only one thread at a time.
    private val lockExecute: Object = Object()
    @Override
    fun execute(runnable: Runnable) {
        synchronized(lockExecute) {
            // Check whether the executor is below maximum capacity.
            if (maxActiveTasks == 0 || activeTaskCount.get() < maxActiveTasks) {
                // Create a new thread for the task.
                val thread = Thread(object : Runnable() {
                    @Override
                    fun run() {
                        runnable.run()
                        activeTaskCount.decrementAndGet()
                        completedTaskCount.incrementAndGet()
                    }
                })
                activeTaskCount.incrementAndGet()
                thread.start()
            } else {
                // If the executor is at maximum capacity, reject the task.
                throw RejectedExecutionException()
            }
        }
    }

    fun getActiveTaskCount(): Int {
        return activeTaskCount.get()
    }

    fun getCompletedTaskCount(): Int {
        return completedTaskCount.get()
    }

    companion object {
        // Logging support
        private val LOG: Logger = Logger.getLogger(NonQueuingExecutor::class.java.getName())
    }
}