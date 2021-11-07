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
import dev.sjaramillo.pedometer.data.PedometerDatabase
import dev.sjaramillo.pedometer.data.StepsRepository
import dev.sjaramillo.pedometer.worker.StepsCounterWorker
import dev.sjaramillo.pedometer.util.Logger.log

// TODO Figure out if this Receiver is necessary. WorkManager is persisted.
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        log("booted")

        // Make sure to reset the steps since boot in the db
        val stepsRepository = StepsRepository(PedometerDatabase.getInstance(context))
        stepsRepository.updateStepsSinceBoot(0)

        StepsCounterWorker.enqueuePeriodicWork(context)
    }
}
