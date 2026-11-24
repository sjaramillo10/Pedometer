package dev.sjaramillo.nemi.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [StoredDailySteps::class], version = 1)
abstract class NemiDatabase : RoomDatabase() {
    abstract fun dailyStepsDao(): DailyStepsDao

    companion object {
        @Volatile // For Singleton instantiation
        private var instance: NemiDatabase? = null

        fun getInstance(context: Context): NemiDatabase =
            instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }

        private fun buildDatabase(context: Context): NemiDatabase =
            Room
                .databaseBuilder(context, NemiDatabase::class.java, "nemi-db")
                .build()
    }
}
