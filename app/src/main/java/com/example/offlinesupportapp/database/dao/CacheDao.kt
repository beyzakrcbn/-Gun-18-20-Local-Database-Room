package com.example.offlinesupportapp.database.dao

import androidx.room.*
import com.example.offlinesupportapp.database.entities.CacheEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CacheDao {

    @Query("SELECT * FROM cache WHERE key = :key AND expiryTime > :currentTime")
    suspend fun getCacheData(key: String, currentTime: Long = System.currentTimeMillis()): CacheEntity?
    //Anahtara göre önbellek verisini getirir, sadece süresi dolmamış (geçerli) olan veriler döner.

    @Query("SELECT * FROM cache")
    fun getAllCacheData(): Flow<List<CacheEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCache(cache: CacheEntity)

    @Query("DELETE FROM cache WHERE key = :key")
    suspend fun deleteCacheByKey(key: String)

    @Query("DELETE FROM cache WHERE expiryTime < :currentTime")
    suspend fun clearExpiredCache(currentTime: Long = System.currentTimeMillis())
    //Süresi dolmuş önbellek verilerini temizler.

    @Query("DELETE FROM cache")
    suspend fun clearAllCache()
}