package dev.sjaramillo.pedometer.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_steps")
data class DailySteps(
    @PrimaryKey val day: Long,
    @ColumnInfo(name = "steps") val steps: Long = Long.MIN_VALUE
)
