/*
 * Copyright 2014 Thomas Hoffmann
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
package dev.sjaramillo.pedometer.ui

import android.app.Dialog
import android.content.Context
import android.view.View
import android.view.Window
import android.widget.TextView
import dev.sjaramillo.pedometer.R
import dev.sjaramillo.pedometer.data.PedometerDatabase
import dev.sjaramillo.pedometer.util.FormatUtil
import dev.sjaramillo.pedometer.util.DateUtil
import java.time.format.DateTimeFormatter

// TODO Extend from Dialog class
object StatisticsDialog {
    fun getDialog(context: Context, sinceBoot: Int): Dialog {
        // TODO Inject Database
        val db = PedometerDatabase.getInstance(context)
        val record = db.dailyStepsDao().getRecord()
        val recordDate = DateUtil.dayToLocalDate(record.day)
        val today = DateUtil.getToday()
        val dayOfMonth = DateUtil.getDayOfMonth()
        val thisWeek = db.dailyStepsDao().getStepsFromDayRange(today - 6, today) + sinceBoot
        val thisMonth = db.dailyStepsDao()
            .getStepsFromDayRange(today - dayOfMonth + 1, today) + sinceBoot
        db.close()
        val numberFormat = FormatUtil.numberFormat

        return Dialog(context).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.statistics)
            findViewById<View>(R.id.close).setOnClickListener { dismiss() }
            findViewById<TextView>(R.id.record).text =
                (numberFormat.format(record.steps) + " @ "
                        + DateTimeFormatter.ofPattern("d MMM uuuu").format(recordDate))
            findViewById<TextView>(R.id.totalthisweek).text = numberFormat.format(thisWeek)
            findViewById<TextView>(R.id.totalthismonth).text = numberFormat.format(thisMonth)
            findViewById<TextView>(R.id.averagethisweek).text = numberFormat.format(thisWeek / 7)
            findViewById<TextView>(R.id.averagethismonth).text =
                numberFormat.format((thisMonth / dayOfMonth))
        }
    }
}
