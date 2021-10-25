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

import java.time.LocalDate


object DateUtil {

    /**
     * @return Today's Epoch Day, where day 0 is 1970-01-01
     */
    fun getToday(): Long {
        return LocalDate.now().toEpochDay()
    }

    /**
     * @return The LocalDate corresponding to the given day
     */
    fun dayToLocalDate(day: Long): LocalDate {
        return LocalDate.ofEpochDay(day)
    }

    /**
     * @return Day of the month, from 1 to 31
     */
    fun getDayOfMonth(): Int {
        return LocalDate.now().dayOfMonth
    }
}
