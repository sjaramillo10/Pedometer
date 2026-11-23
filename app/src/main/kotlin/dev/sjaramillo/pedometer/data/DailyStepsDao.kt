package dev.sjaramillo.pedometer.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyStepsDao {
    @Query("SELECT * FROM daily_steps WHERE day > 0 ORDER BY day")
    suspend fun getAll(): List<StoredDailySteps>

    @Query("SELECT * FROM daily_steps WHERE day BETWEEN :start AND :end ORDER BY day")
    fun observeEntriesInRange(
        start: Long,
        end: Long,
    ): Flow<List<StoredDailySteps>>

    @Query("DELETE FROM daily_steps WHERE source = :source AND day BETWEEN :start AND :end")
    suspend fun deleteFrom(
        source: StepSource,
        start: Long,
        end: Long,
    )

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(dailySteps: List<StoredDailySteps>)
}
