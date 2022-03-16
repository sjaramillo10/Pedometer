package dev.sjaramillo.pedometer.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

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
                .allowMainThreadQueries()
                .build()
        }
    }
}
