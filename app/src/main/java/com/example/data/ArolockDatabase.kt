package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [TrekEntity::class], version = 1, exportSchema = false)
abstract class ArolockDatabase : RoomDatabase() {

    abstract fun trekDao(): TrekDao

    companion object {
        @Volatile
        private var INSTANCE: ArolockDatabase? = null

        fun getDatabase(context: Context): ArolockDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ArolockDatabase::class.java,
                    "arolock_trails.db"
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
