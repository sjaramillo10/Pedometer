/*
 * Copyright 2016 Thomas Hoffmann
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

import android.annotation.TargetApi
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import dev.sjaramillo.pedometer.util.API26Wrapper

@TargetApi(Build.VERSION_CODES.O)
object API26Wrapper {

    private const val NOTIFICATION_CHANNEL_ID = "Notification"

    @JvmStatic // TODO Remove once it is used from Kotlin only
    fun getNotificationBuilder(context: Context): Notification.Builder {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_ID,
            NotificationManager.IMPORTANCE_NONE
        )
        channel.importance = NotificationManager.IMPORTANCE_MIN // ignored by Android O ...
        channel.enableLights(false)
        channel.enableVibration(false)
        channel.setBypassDnd(false)
        channel.setSound(null, null)
        manager.createNotificationChannel(channel)
        return Notification.Builder(context, NOTIFICATION_CHANNEL_ID)
    }

    @JvmStatic
    fun launchNotificationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
        intent.putExtra(Settings.EXTRA_CHANNEL_ID, NOTIFICATION_CHANNEL_ID)
        intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // TODO Extract string to resources
            val msg = "Settings not found - please search for the notification settings in the Android settings manually"
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }
    }
}
