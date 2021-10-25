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
import dev.sjaramillo.pedometer.db.Database
import dev.sjaramillo.pedometer.R
import dev.sjaramillo.pedometer.util.FormatUtil
import dev.sjaramillo.pedometer.util.DateUtil
import java.text.DateFormat

// TODO Extend from Dialog class
object StatisticsDialog {
    fun getDialog(context: Context, since_boot: Int): Dialog {
        // TODO Inject Database
        val db = Database.getInstance(context)
        val record = db.recordData
        val today = DateUtil.getToday()
        val dayOfMonth = DateUtil.getDayOfMonth()
        val thisWeek = db.getSteps(today - 6, System.currentTimeMillis()) + since_boot
        val thisMonth = db.getSteps(today - dayOfMonth + 1, System.currentTimeMillis()) + since_boot
        db.close()
        val numberFormat = FormatUtil.numberFormat

        return Dialog(context).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.statistics)
            findViewById<View>(R.id.close).setOnClickListener { dismiss() }
            findViewById<TextView>(R.id.record).text =
                (numberFormat.format(record.second) + " @ "
                    + DateFormat.getDateInstance().format(record.first))
            findViewById<TextView>(R.id.totalthisweek).text =
                numberFormat.format(thisWeek.toLong())
            findViewById<TextView>(R.id.totalthismonth).text =
                numberFormat.format(thisMonth.toLong())
            findViewById<TextView>(R.id.averagethisweek).text =
                numberFormat.format((thisWeek / 7).toLong())
            findViewById<TextView>(R.id.averagethismonth).text =
                numberFormat.format((thisMonth / dayOfMonth))
        }
    }
}
