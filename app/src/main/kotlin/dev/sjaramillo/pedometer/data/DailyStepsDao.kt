package dev.sjaramillo.pedometer.data

import androidx.room.Dao
import androidx.room.Query

@Dao
interface DailyStepsDao {
    @Query("SELECT * from daily_steps")
    fun getAll(): List<DailySteps> // TODO migrate to suspend fun
}