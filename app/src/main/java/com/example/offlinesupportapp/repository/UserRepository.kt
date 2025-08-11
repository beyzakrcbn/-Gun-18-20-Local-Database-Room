package com.example.offlinesupportapp.repository

import com.example.offlinesupportapp.database.dao.UserDao
import com.example.offlinesupportapp.database.dao.CacheDao  // Bu import eksikti
import com.example.offlinesupportapp.database.entities.UserEntity
import com.example.offlinesupportapp.database.entities.CacheEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.delay

class UserRepository(
    private val userDao: UserDao,
    private val cacheDao: CacheDao
) {

    // Offline-first approach: Önce yerel veriler
    fun getAllUsers(): Flow<List<UserEntity>> = userDao.getAllUsers()

    suspend fun refreshUsers(): Result<List<UserEntity>> {
        return try {
            // Simüle edilmiş ağ çağrısı
            val networkUsers = fetchUsersFromNetwork()

            // Önce mevcut kullanıcıları offline olarak işaretle
            val existingUsers = userDao.getAllUsers().first()
            existingUsers.forEach { user ->
                val updatedUser = user.copy(isOnline = false)
                userDao.insertUser(updatedUser) // insertUser kullanıyoruz
            }

            // Yeni verileri ekle/güncelle
            val updatedUsers = networkUsers.map { user ->
                user.copy(isOnline = true, isCached = true)
            }

            // Her kullanıcıyı tek tek insert et
            updatedUsers.forEach { user ->
                userDao.insertUser(user)
            }

            // Cache'i güncelle
            cacheLastSyncTime()

            Result.success(updatedUsers)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun fetchUsersFromNetwork(): List<UserEntity> {
        // Simüle edilmiş ağ gecikmesi
        delay(1000)

        // Simüle edilmiş ağ verisi
        return listOf(
            UserEntity(1, "Alice Brown", "alice@example.com", true, true),
            UserEntity(2, "David Wilson", "david@example.com", true, true),
            UserEntity(3, "John Doe", "john@example.com", false, true),
            UserEntity(4, "Jane Smith", "jane@example.com", false, true),
            UserEntity(5, "Bob Johnson", "bob@example.com", false, true)
        )
    }

    suspend fun clearCache() {
        cacheDao.clearAllCache()
        // Kullanıcıları cache durumunu sıfırla
        val users = userDao.getAllUsers().first()
        users.forEach { user ->
            val updatedUser = user.copy(isCached = false)
            userDao.insertUser(updatedUser)
        }
    }

    private suspend fun cacheLastSyncTime() {
        val syncCache = CacheEntity(
            key = "last_sync_time",
            data = System.currentTimeMillis().toString()
        )
        cacheDao.insertCache(syncCache)
    }

    suspend fun getLastSyncTime(): Long? {
        return cacheDao.getCacheData("last_sync_time")?.data?.toLongOrNull()
    }

    suspend fun isDataStale(): Boolean {
        val lastSync = getLastSyncTime() ?: return true
        val currentTime = System.currentTimeMillis()
        val twoMinutesAgo = currentTime - (2 * 60 * 1000) // 2 dakika
        return lastSync < twoMinutesAgo
    }
}