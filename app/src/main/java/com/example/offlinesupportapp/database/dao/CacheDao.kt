package com.example.offlinesupportapp.database.dao

import androidx.room.*
import com.example.offlinesupportapp.database.entities.CacheEntity
import kotlinx.coroutines.flow.Flow


@Dao
interface CacheDao {

    @Query("SELECT * FROM cache_info WHERE `key` = :key")
    suspend fun getCacheInfo(key: String): CacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCacheInfo(cacheInfo: CacheEntity)

    @Query("DELETE FROM cache_info")
    suspend fun clearAllCache()

    @Query("DELETE FROM cache_info WHERE `key` = :key")
    suspend fun deleteCacheInfo(key: String)

    @Query("SELECT * FROM cache_info")
    fun getAllCacheInfo(): Flow<List<CacheEntity>>
}