/*
 * Copyright 2013 Thomas Hoffmann
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.sjaramillo.pedometer.util

import android.util.Log
import dev.sjaramillo.pedometer.BuildConfig

// TODO Migrate to Timber to log data
object Logger {

    private const val APP = "Pedometer"

    fun log(ex: Throwable) {
        if (!BuildConfig.DEBUG) return
        log(ex.message ?: "generic exception error message")
        for (ste in ex.stackTrace) {
            log(ste.toString())
        }
    }

    fun log(msg: String) {
        if (!BuildConfig.DEBUG) return
        Log.d(APP, msg)
    }
}
