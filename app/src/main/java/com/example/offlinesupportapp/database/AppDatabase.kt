package com.example.offlinesupportapp.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.example.offlinesupportapp.database.dao.CacheDao
import com.example.offlinesupportapp.database.dao.UserDao
import com.example.offlinesupportapp.database.entities.CacheEntity
import com.example.offlinesupportapp.database.entities.UserEntity
//Room veritabanını tanımlayan ve DAO erişim noktalarını sağlayan ana sınıf.
@Database(
    entities = [UserEntity::class, CacheEntity::class],
    version = 2, // Version arttırıldı migration için
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun cacheDao(): CacheDao

    companion object {
        @Volatile //Farklı thread’lerde değişim olduğunda senkronizasyon garantisi verir.
        private var INSTANCE: AppDatabase? = null

        // Migration 1'den 2'ye örneği
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Örnek: Yeni bir kolon ekleme
                database.execSQL("ALTER TABLE users ADD COLUMN lastSyncTime INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): AppDatabase { //Eğer daha önce oluşturulmamışsa veritabanını oluşturur, varsa mevcut örneği döner.
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(  //Room ile app_database adında SQLite veritabanı oluşturulur.
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}