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
package dev.sjaramillo.pedometer.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Pair
import dev.sjaramillo.pedometer.util.Logger.log
import dev.sjaramillo.pedometer.util.DateUtil
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

class Database private constructor(context: Context) :
    SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
    override fun close() {
        if (openCounter.decrementAndGet() == 0) {
            super.close()
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        // TODO Rename table to 'daily_steps' and 'date' -> 'day' when migrating to Room
        db.execSQL("CREATE TABLE $DB_NAME (date INTEGER, steps INTEGER)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Current version is 1, no upgrades just yet
    }

    /**
     * Inserts a new entry in the database, if there is no entry for the given
     * date yet. Steps should be the current number of steps and it's negative
     * value will be used as offset for the new date. Also adds 'steps' steps to
     * the previous day, if there is an entry for that date.
     *
     *
     * This method does nothing if there is already an entry for 'date' - use
     * [.saveCurrentSteps] in this case.
     *
     *
     * To restore data from a backup, use [.insertDayFromBackup]
     *
     * @param date  the date in ms since 1970
     * @param steps the current step value to be used as negative offset for the
     * new day; must be >= 0
     */
    fun insertNewDay(date: Long, steps: Int) {
        writableDatabase.beginTransaction()
        try {
            val c: Cursor = readableDatabase.query(
                DB_NAME,
                arrayOf("date"),
                "date = ?",
                arrayOf(date.toString()),
                null,
                null,
                null
            )
            if (c.count == 0 && steps >= 0) {

                // add 'steps' to yesterdays count
                addToLastEntry(steps)

                // add today
                val values = ContentValues()
                values.put("date", date)
                // use the negative steps as offset
                values.put("steps", -steps)
                writableDatabase.insert(DB_NAME, null, values)
            }
            c.close()
            log("insertDay $date / $steps")
            writableDatabase.setTransactionSuccessful()
        } finally {
            writableDatabase.endTransaction()
        }
    }

    /**
     * Adds the given number of steps to the last entry in the database
     *
     * @param steps the number of steps to add
     */
    fun addToLastEntry(steps: Int) {
        writableDatabase.execSQL(
            "UPDATE $DB_NAME SET steps = steps + $steps WHERE date = (SELECT MAX(date) FROM $DB_NAME)"
        )
    }

    /**
     * Inserts a new entry in the database, overwriting any existing entry for the given date.
     * Use this method for restoring data from a backup.
     *
     * @param date  the date in ms since 1970
     * @param steps the step value for 'date'; must be >= 0
     * @return true if a new entry was created, false if there was already an
     * entry for 'date' (and it was overwritten)
     */
    fun insertDayFromBackup(date: Long, steps: Int): Boolean {
        writableDatabase.beginTransaction()
        var newEntryCreated = false
        try {
            val values = ContentValues()
            values.put("steps", steps)
            val updatedRows = writableDatabase
                .update(DB_NAME, values, "date = ?", arrayOf(date.toString()))
            if (updatedRows == 0) {
                values.put("date", date)
                writableDatabase.insert(DB_NAME, null, values)
                newEntryCreated = true
            }
            writableDatabase.setTransactionSuccessful()
        } finally {
            writableDatabase.endTransaction()
        }
        return newEntryCreated
    }

    /**
     * Get the total of steps taken without today's value
     *
     * @return number of steps taken, ignoring today
     */
    val totalWithoutToday: Int
        get() {
            val c: Cursor = readableDatabase
                .query(
                    DB_NAME,
                    arrayOf("SUM(steps)"),
                    "steps > 0 AND date > 0 AND date < ?",
                    arrayOf(DateUtil.getToday().toString()),
                    null,
                    null,
                    null
                )
            c.moveToFirst()
            val re = c.getInt(0)
            c.close()
            return re
        }

    /**
     * Get the number of steps taken for a specific date.
     *
     *
     * If date is Util.getToday(), this method returns the offset which needs to
     * be added to the value returned by getCurrentSteps() to get today's steps.
     *
     * @param date the date in millis since 1970
     * @return the steps taken on this date or Integer.MIN_VALUE if date doesn't
     * exist in the database
     */
    fun getSteps(date: Long): Int {
        val c: Cursor = readableDatabase.query(
            DB_NAME,
            arrayOf("steps"),
            "date = ?",
            arrayOf(date.toString()),
            null,
            null,
            null
        )
        c.moveToFirst()
        val re: Int = if (c.count == 0) Int.MIN_VALUE else c.getInt(0)
        c.close()
        return re
    }

    /**
     * Gets the last num entries in descending order of date (newest first)
     *
     * @param num the number of entries to get
     * @return a list of long,integer pair - the first being the date, the second the number of steps
     */
    fun getLastEntries(num: Int): List<Pair<Long, Int>> {
        val c: Cursor = readableDatabase
            .query(
                DB_NAME, arrayOf("date", "steps"), "date > 0", null, null, null,
                "date DESC", num.toString()
            )
        val max = c.count
        val result: MutableList<Pair<Long, Int>> = ArrayList(max)
        if (c.moveToFirst()) {
            do {
                result.add(Pair(c.getLong(0), c.getInt(1)))
            } while (c.moveToNext())
        }
        c.close()
        return result
    }

    /**
     * Removes all entries with negative values.
     *
     *
     * Only call this directly after boot, otherwise it might remove the current
     * day as the current offset is likely to be negative
     */
    fun removeNegativeEntries() {
        writableDatabase.delete(DB_NAME, "steps < ?", arrayOf("0"))
    }

    /**
     * Get the number of 'valid' days (= days with a step value > 0).
     *
     *
     * The current day is not added to this number.
     *
     * @return the number of days with a step value > 0, return will be >= 0
     */
    private val daysWithoutToday: Int
        get() {
            val c: Cursor = readableDatabase
                .query(
                    DB_NAME,
                    arrayOf("COUNT(*)"),
                    "steps > ? AND date < ? AND date > 0",
                    arrayOf(0.toString(), DateUtil.getToday().toString()),
                    null,
                    null,
                    null
                )
            c.moveToFirst()
            val re = c.getInt(0)
            c.close()
            return re.coerceAtLeast(0)
        }

    /**
     * Get the number of 'valid' days (= days with a step value > 0).
     *
     *
     * The current day is also added to this number, even if the value in the
     * database might still be < 0.
     *
     *
     * It is safe to divide by the return value as this will be at least 1 (and
     * not 0).
     *
     * @return the number of days with a step value > 0, return will be >= 1
     */
    val days: Int
        get() = daysWithoutToday + 1 // today is not counted yet

    /**
     * Saves the current 'steps since boot' sensor value in the database.
     *
     * @param steps since boot
     */
    fun saveCurrentSteps(steps: Int) {
        val values = ContentValues()
        values.put("steps", steps)
        if (writableDatabase.update(DB_NAME, values, "date = -1", null) == 0) {
            values.put("date", -1)
            writableDatabase.insert(DB_NAME, null, values)
        }
        log("saving steps in db: $steps")
    }

    /**
     * Reads the latest saved value for the 'steps since boot' sensor value.
     *
     * @return the current number of steps saved in the database or 0 if there
     * is no entry
     */
    val currentSteps: Int
        get() {
            val re = getSteps(-1)
            return if (re == Int.MIN_VALUE) 0 else re
        }

    companion object {
        private const val DB_NAME = "steps"
        private const val DB_VERSION = 1
        private var instance: Database? = null
        private val openCounter = AtomicInteger()

        @Synchronized
        fun getInstance(context: Context): Database {
            if (instance == null) {
                instance = Database(context.applicationContext)
            }
            openCounter.incrementAndGet()
            return instance!!
        }
    }
}