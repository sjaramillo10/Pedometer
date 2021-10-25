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


object Util {

    /**
     * @return  unix day calculated as the number of days elapsed since January 1st, 1970 up
     *          until today.
     */
    fun getToday(): Long {
        val todayLocalDate = LocalDate.now()
        return getUnixDay(todayLocalDate)
    }

    /**
     * @return  unix day calculated as the number of days elapsed since January 1st, 1970 up
     *          until localDate's day.
     */
    internal fun getUnixDay(localDate: LocalDate): Long {
        val firstUnixDay = LocalDate.of(1970, 1, 1)

        return ChronoUnit.DAYS.between(firstUnixDay, localDate)
    }

    // TODO Move to a different place.
    val numberFormat: NumberFormat
        get() = NumberFormat.getInstance(Locale.getDefault())
}
