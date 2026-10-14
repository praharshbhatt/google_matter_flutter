package com.google.google_matter_flutter.google_matter_flutter.chip

import android.util.Log
import com.google.google_matter_flutter.google_matter_flutter.BuildConfig

/**
 * Minimal logging helper used throughout this plugin instead of raw `print()`/`println()`.
 *
 * All diagnostic output goes through here so it is tagged for logcat and, crucially, compiled
 * out (and never printed) unless this module was built with `BuildConfig.DEBUG == true`. This
 * keeps production logcat output clean for apps that depend on this library, instead of
 * unconditionally spamming stdout like plain `print()` does.
 */
internal object MatterLog {
    private const val TAG = "GoogleMatterFlutter"

    fun d(message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, message)
        }
    }

    fun e(message: String, throwable: Throwable? = null) {
        if (BuildConfig.DEBUG) {
            Log.e(TAG, message, throwable)
        }
    }
}
