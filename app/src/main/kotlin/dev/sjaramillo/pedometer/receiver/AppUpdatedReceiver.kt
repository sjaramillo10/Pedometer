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
package dev.sjaramillo.pedometer.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import dev.sjaramillo.pedometer.SensorListener
import dev.sjaramillo.pedometer.util.API26Wrapper.startForegroundService
import dev.sjaramillo.pedometer.util.Logger

class AppUpdatedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Logger.log("app updated")
        if (Build.VERSION.SDK_INT >= 26) {
            startForegroundService(context, Intent(context, SensorListener::class.java))
        } else {
            context.startService(Intent(context, SensorListener::class.java))
        }
    }
}