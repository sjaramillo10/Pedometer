package dev.sjaramillo.pedometer.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyStepsDao {
    @Query("SELECT * FROM daily_steps WHERE day>0 ORDER BY day")
    suspend fun getAll(): List<DailySteps>

    @Query("SELECT * FROM daily_steps WHERE day>0 ORDER BY day")
    fun observeAll(): Flow<List<DailySteps>>

    @Query("SELECT * FROM daily_steps WHERE day >= :start AND day <= :end")
    suspend fun getEntriesInRange(
        start: Long,
        end: Long,
    ): List<DailySteps>

    @Query("DELETE FROM daily_steps WHERE day >= :start AND day <= :end")
    suspend fun deleteFrom(
        start: Long,
        end: Long,
    )

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(dailySteps: List<DailySteps>)
}
