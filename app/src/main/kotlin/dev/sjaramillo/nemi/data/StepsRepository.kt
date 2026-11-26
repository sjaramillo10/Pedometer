package dev.sjaramillo.nemi.data

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.time.LocalDate

class StepsRepository(
    private val database: NemiDatabase,
) {
    private val changes = MutableStateFlow(0L)

    suspend fun getAll(): List<DailySteps> = withContext(Dispatchers.IO) { readDays(1, lastSupportedEpochDay) }

    fun observeDay(day: LocalDate): Flow<DailySteps?> = observeDays(day, day).map { it.singleOrNull() }

    fun observeDays(
        start: LocalDate,
        end: LocalDate,
    ): Flow<List<DailySteps>> =
        changes
            .map { readDays(start.toEpochDay().coerceAtLeast(1), end.toEpochDay()) }
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)

    fun observeHistoryStats(): Flow<StepHistoryStats> =
        changes
            .map {
                query(RESOLVED_STEP_STATS, 1, lastSupportedEpochDay).use { rows ->
                    check(rows.moveToFirst())
                    StepHistoryStats(
                        rows.getLong(0),
                        if (rows.isNull(1)) null else DailySteps(rows.getLong(1), rows.getLong(2), rows.getInt(3) != 0),
                    )
                }
            }.distinctUntilChanged()
            .flowOn(Dispatchers.IO)

    suspend fun importDailySteps(dailySteps: List<DailySteps>) {
        write {
            insertAll(dailySteps.map { StoredDailySteps(it.day, it.steps, StepSource.BACKUP) })
        }
    }

    internal suspend fun replaceHealthConnectWindow(
        start: LocalDate,
        end: LocalDate,
        totals: Map<LocalDate, Long>,
    ) {
        write {
            delete(
                "daily_steps",
                "source = ? AND day BETWEEN ? AND ?",
                arrayOf(StepSource.HEALTH_CONNECT.name, start.toEpochDay().toString(), end.toEpochDay().toString()),
            )
            insertAll(totals.map { (day, steps) -> StoredDailySteps(day.toEpochDay(), steps, StepSource.HEALTH_CONNECT) })
        }
    }

    private fun readDays(
        firstDay: Long,
        lastDay: Long,
    ): List<DailySteps> =
        query(RESOLVED_DAYS_IN_ORDER, firstDay, lastDay).use { rows ->
            buildList {
                while (rows.moveToNext()) {
                    add(DailySteps(rows.getLong(0), rows.getLong(1), rows.getInt(2) != 0))
                }
            }
        }

    private fun query(
        sql: String,
        firstDay: Long,
        lastDay: Long,
    ): Cursor =
        database.readableDatabase.rawQuery(
            sql.replace(":firstDay", "?").replace(":lastDay", "?"),
            arrayOf(firstDay.toString(), lastDay.toString()),
        )

    private suspend fun write(operation: SQLiteDatabase.() -> Unit) {
        withContext(Dispatchers.IO) {
            val connection = database.writableDatabase
            connection.beginTransactionNonExclusive()
            try {
                connection.operation()
                connection.setTransactionSuccessful()
            } finally {
                connection.endTransaction()
            }
            changes.update { it + 1 }
        }
    }

    private fun SQLiteDatabase.insertAll(entries: List<StoredDailySteps>) {
        compileStatement("INSERT OR REPLACE INTO daily_steps (day, steps, source) VALUES (?, ?, ?)").use { statement ->
            entries.forEach { entry ->
                statement.bindLong(1, entry.day)
                statement.bindLong(2, entry.steps)
                statement.bindString(3, entry.source.name)
                statement.executeInsert()
            }
        }
    }
}
