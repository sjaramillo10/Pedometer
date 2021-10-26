package dev.sjaramillo.pedometer.data

import android.content.Context
import androidx.room.RoomDatabase
import androidx.room.Database
import androidx.room.Room

@Database(entities = [DailySteps::class], version = 1)
abstract class PedometerDatabase : RoomDatabase() {
    abstract fun dailyStepsDao(): DailyStepsDao

    companion object {
        @Volatile // For Singleton instantiation
        private var instance: PedometerDatabase? = null

        fun getInstance(context: Context): PedometerDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }
        }

        private fun buildDatabase(context: Context): PedometerDatabase {
            return Room.databaseBuilder(context, PedometerDatabase::class.java, "pedometer-db")
                .build()
        }
    }
}
