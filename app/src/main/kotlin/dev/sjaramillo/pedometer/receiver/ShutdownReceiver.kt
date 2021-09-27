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
import dev.sjaramillo.pedometer.Database
import dev.sjaramillo.pedometer.SensorListener
import dev.sjaramillo.pedometer.util.Logger.log
import dev.sjaramillo.pedometer.util.Util.today

class ShutdownReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        log("shutting down")
        context.startService(Intent(context, SensorListener::class.java))

        // if the user used a root script for shutdown, the DEVICE_SHUTDOWN
        // broadcast might not be send. Therefore, the app will check this
        // setting on the next boot and displays an error message if it's not
        // set to true
        context.getSharedPreferences("pedometer", Context.MODE_PRIVATE).edit()
            .putBoolean("correctShutdown", true).apply()

        val db = Database.getInstance(context)
        // if it's already a new day, add the temp. steps to the last one
        if (db.getSteps(today) == Int.MIN_VALUE) {
            val steps = db.currentSteps
            db.insertNewDay(today, steps)
        } else {
            db.addToLastEntry(db.currentSteps)
        }
        // current steps will be reset on boot @see BootReceiver
        db.close()
    }
}
