package com.example.offlinesupportapp.repository

import com.example.offlinesupportapp.database.dao.UserDao
import com.example.offlinesupportapp.database.dao.CacheDao
import com.example.offlinesupportapp.database.entities.UserEntity
import com.example.offlinesupportapp.database.entities.CacheEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

//Öncelikle yerel veritabanı (Room) kullanılır, gerektiğinde ağdan veri çekilir ve senkronize edilir.

@Singleton
class UserRepository @Inject constructor( //Hilt/Dagger ile dependency injection sağlanır.
    private val userDao: UserDao,
    private val cacheDao: CacheDao
) {

    // Tüm kullanıcılar (Flow ile anlık güncellenir).
    fun getAllUsers(): Flow<List<UserEntity>> = userDao.getAllUsers()

    fun getOnlineUsers(): Flow<List<UserEntity>> = userDao.getOnlineUsers()

    fun getCachedUsers(): Flow<List<UserEntity>> = userDao.getCachedUsers() //önbellek

    suspend fun refreshUsers(): Result<List<UserEntity>> {
        return try {
            // Simüle edilmiş ağ çağrısı
            val networkUsers = fetchUsersFromNetwork()

            // Önce mevcut kullanıcıları offline olarak işaretle
            val existingUsers = userDao.getAllUsers().first()
            existingUsers.forEach { user ->
                userDao.updateUserOnlineStatus(user.id, false)
            }

            // Yeni verileri ekle/güncelle
            val updatedUsers = networkUsers.map { user ->
                user.copy(isOnline = true, isCached = true)
            }
            userDao.insertUsers(updatedUsers)

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

    suspend fun syncUser(userId: Int) {
        try {
            // Tek kullanıcı senkronizasyonu
            val user = userDao.getUserById(userId)
            user?.let {
                val updatedUser = it.copy(
                    lastSyncTime = System.currentTimeMillis(),
                    isCached = true
                )
                userDao.updateUser(updatedUser)
            }
        } catch (e: Exception) {
            // Hata yönetimi
        }
    }

    suspend fun clearCache() {
        cacheDao.clearAllCache()
        // Kullanıcıları cache durumunu sıfırla
        val users = userDao.getAllUsers().first()
        users.forEach { user ->
            userDao.updateUserCacheStatus(user.id, false)
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

    suspend fun isDataStale(): Boolean {  //Verinin 2 dakikadan eski olup olmadığını kontrol eder.
        val lastSync = getLastSyncTime() ?: return true
        val currentTime = System.currentTimeMillis()
        val twoMinutesAgo = currentTime - (2 * 60 * 1000) // 2 dakika
        return lastSync < twoMinutesAgo
    }
}