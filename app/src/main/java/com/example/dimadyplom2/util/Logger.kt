package com.example.dimadyplom2.util

import android.util.Log

/**
 * Tiny per-class logger that wraps [android.util.Log] — no external libraries.
 *
 * Usage: give a class its own logger with the [logger] extension:
 * ```
 * private val log = logger()          // tag = the class name
 * log.i("signed in as id $id")
 * log.e("request failed", throwable)
 * ```
 */
class Logger(private val tag: String) {
    fun d(message: String) = Log.d(tag, message)
    fun i(message: String) = Log.i(tag, message)

    fun w(message: String, throwable: Throwable? = null) {
        if (throwable != null) Log.w(tag, message, throwable) else Log.w(tag, message)
    }

    fun e(message: String, throwable: Throwable? = null) {
        if (throwable != null) Log.e(tag, message, throwable) else Log.e(tag, message)
    }
}

/** Create a [Logger] tagged with the receiver's class name. */
fun Any.logger(): Logger = Logger(this::class.java.simpleName)

