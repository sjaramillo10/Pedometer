package dev.sjaramillo.pedometer.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [DailySteps::class], version = 2)
abstract class PedometerDatabase : RoomDatabase() {
    abstract fun dailyStepsDao(): DailyStepsDao

    companion object {
        @Volatile // For Singleton instantiation
        private var instance: PedometerDatabase? = null

        fun getInstance(context: Context): PedometerDatabase =
            instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }

        private fun buildDatabase(context: Context): PedometerDatabase =
            Room
                .databaseBuilder(context, PedometerDatabase::class.java, "pedometer-db")
                .addMigrations(MIGRATION_1_2)
                .build()

        val MIGRATION_1_2 =
            object : Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("DELETE FROM daily_steps")
                }
            }
    }
}
