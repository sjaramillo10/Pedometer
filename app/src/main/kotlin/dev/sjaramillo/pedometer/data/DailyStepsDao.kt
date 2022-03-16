package dev.sjaramillo.pedometer.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyStepsDao {
    @Query("SELECT * FROM daily_steps WHERE day>0")
    fun getAll(): List<DailySteps> // TODO migrate to suspend fun

    @Query("SELECT steps FROM daily_steps WHERE day=:day")
    fun getSteps(day: Long): Long?

    @Query("SELECT steps FROM daily_steps WHERE day=:day")
    fun getStepsFlow(day: Long): Flow<Long?>

    @Query("SELECT * FROM daily_steps WHERE day>0 ORDER BY day DESC LIMIT :num")
    fun getLastEntries(num: Int): List<DailySteps>

    @Query("SELECT * FROM daily_steps WHERE day > 0 ORDER BY steps DESC LIMIT 1")
    suspend fun getRecord(): DailySteps

    @Query("SELECT SUM(steps) FROM daily_steps WHERE day>=:start AND day<=:end")
    suspend fun getStepsFromDayRange(start: Long, end: Long): Long

    @Query("SELECT SUM(steps) FROM daily_steps WHERE day>=:start AND day<=:end")
    fun getStepsFromDayRangeFlow(start: Long, end: Long): Flow<Long>

    @Query("SELECT COUNT(*) FROM daily_steps WHERE day>0")
    suspend fun getTotalDays(): Long

    @Query("UPDATE daily_steps SET steps=steps+:steps WHERE day=(SELECT MAX(day) FROM daily_steps)")
    fun addToLastEntry(steps: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg dailySteps: DailySteps)

    @Update
    fun update(vararg dailySteps: DailySteps): Int
}
