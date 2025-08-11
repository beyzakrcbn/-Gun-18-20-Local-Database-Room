package com.example.offlinesupportapp.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.offlinesupportapp.database.entities.CacheEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CacheDao {

    @Query("SELECT * FROM cache WHERE `key` = :key")
    suspend fun getCacheData(key: String): CacheEntity?

    @Query("SELECT * FROM cache")
    fun getAllCacheData(): Flow<List<CacheEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCache(cache: CacheEntity)

    @Query("DELETE FROM cache WHERE `key` = :key")
    suspend fun deleteCacheByKey(key: String)

    @Query("DELETE FROM cache")
    suspend fun clearAllCache()
}