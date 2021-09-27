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
import dev.sjaramillo.pedometer.Database
import dev.sjaramillo.pedometer.SensorListener
import dev.sjaramillo.pedometer.util.Logger.log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        log("booted")
        val prefs = context.getSharedPreferences("pedometer", Context.MODE_PRIVATE)
        val db = Database.getInstance(context)
        if (!prefs.getBoolean("correctShutdown", false)) {
            log("Incorrect shutdown")
            // can we at least recover some steps?
            val steps = db.currentSteps.coerceAtLeast(0)
            log("Trying to recover $steps steps")
            db.addToLastEntry(steps)
        }
        // last entry might still have a negative step value, so remove that
        // row if that's the case
        db.removeNegativeEntries()
        db.saveCurrentSteps(0)
        db.close()
        prefs.edit().remove("correctShutdown").apply()

        val serviceIntent = Intent(context, SensorListener::class.java)
        if (Build.VERSION.SDK_INT >= 26) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}
