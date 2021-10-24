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

import java.text.NumberFormat
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.*


object TimeUtil {
    /**
     * @return milliseconds since 1.1.1970 for today 0:00:00 local timezone
     */
    val today: Long
        get() {
            val c = Calendar.getInstance()
            c.timeInMillis = System.currentTimeMillis()
            c[Calendar.HOUR_OF_DAY] = 0
            c[Calendar.MINUTE] = 0
            c[Calendar.SECOND] = 0
            c[Calendar.MILLISECOND] = 0
            return c.timeInMillis
        }

    /**
     * @param today    used for testing to specify today's date.
     *
     * @return unixDay calculated as the number of days elapsed since January 1st, 1970.
     */
    fun getUnixDay(today: LocalDate = LocalDate.now()): Long {
        val firstUnixDay = LocalDate.of(1970, 1, 1)

        return ChronoUnit.DAYS.between(firstUnixDay, today)
    }

    /**
     * @return milliseconds since 1.1.1970 for tomorrow 0:00:01 local timezone
     */
    val tomorrow: Long
        get() {
            val c = Calendar.getInstance()
            c.timeInMillis = System.currentTimeMillis()
            c[Calendar.HOUR_OF_DAY] = 0
            c[Calendar.MINUTE] = 0
            c[Calendar.SECOND] = 1
            c[Calendar.MILLISECOND] = 0
            c.add(Calendar.DATE, 1)
            return c.timeInMillis
        }

    val numberFormat: NumberFormat
        get() = NumberFormat.getInstance(Locale.getDefault())
}
