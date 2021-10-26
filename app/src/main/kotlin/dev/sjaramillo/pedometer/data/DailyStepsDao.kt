package dev.sjaramillo.pedometer.data

import androidx.room.Dao
import androidx.room.Query

@Dao
interface DailyStepsDao {
    @Query("SELECT * FROM daily_steps")
    fun getAll(): List<DailySteps> // TODO migrate to suspend fun

    @Query("SELECT * FROM daily_steps WHERE day > 0 ORDER BY steps DESC LIMIT 1")
    fun getRecord(): DailySteps

    // TODO Account for the fact that today's value can be negative
    @Query("SELECT SUM(steps) FROM daily_steps WHERE day>=:start AND day<=:end")
    fun getStepsFromDayRange(start: Long, end: Long): Long
}
