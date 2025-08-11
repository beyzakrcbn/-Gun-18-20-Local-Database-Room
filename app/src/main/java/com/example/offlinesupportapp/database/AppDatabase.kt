package com.example.offlinesupportapp.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.example.offlinesupportapp.database.dao.UserDao
import com.example.offlinesupportapp.database.entities.CacheEntity
import com.example.offlinesupportapp.database.entities.UserEntity
import com.example.offlinesupportapp.database.dao.CacheDao

@Database(
    entities = [UserEntity::class, CacheEntity::class],
    version = 1, // İlk sürüm
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
                    "app_database"
                )
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}