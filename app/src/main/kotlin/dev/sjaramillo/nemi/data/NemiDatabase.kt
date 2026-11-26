package dev.sjaramillo.nemi.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

internal const val CREATE_DAILY_STEPS =
    "CREATE TABLE IF NOT EXISTS daily_steps (day INTEGER NOT NULL, steps INTEGER NOT NULL, " +
        "source TEXT NOT NULL, PRIMARY KEY (day, source))"

class NemiDatabase(context: Context) : SQLiteOpenHelper(context, "nemi-db", null, 1) {
    init {
        setWriteAheadLoggingEnabled(true)
    }

    override fun onCreate(database: SQLiteDatabase) {
        database.execSQL(CREATE_DAILY_STEPS)
    }

    override fun onUpgrade(
        database: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int,
    ) {
        error("Unsupported Nemi database upgrade from $oldVersion to $newVersion")
    }
}
