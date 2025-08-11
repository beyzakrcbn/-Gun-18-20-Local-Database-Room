package com.example.offlinesupportapp.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.offlinesupportapp.database.dao.CacheDao
import com.example.offlinesupportapp.database.dao.UserDao
import com.example.offlinesupportapp.database.entities.CacheEntity
import com.example.offlinesupportapp.database.entities.UserEntity

@Database(
    entities = [UserEntity::class, CacheEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun cacheDao(): CacheDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "offline_support_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}