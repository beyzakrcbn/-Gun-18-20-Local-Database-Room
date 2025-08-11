package com.example.offlinesupportapp.repository

import com.example.offlinesupportapp.database.dao.CacheDao
import com.example.offlinesupportapp.database.dao.UserDao
import com.example.offlinesupportapp.database.entities.CacheEntity
import com.example.offlinesupportapp.database.entities.UserEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.delay
import kotlin.random.Random

class UserRepository(
    private val userDao: UserDao,
    private val cacheDao: CacheDao
) {
    fun getAllUsers(): Flow<List<UserEntity>> = userDao.getAllUsers()

    suspend fun refreshUsers(): Result<List<UserEntity>> {
        return try {
            // Simulate API call delay
            delay(1000)

            // Simulate random network failure (30% chance)
            if (Random.nextFloat() < 0.3f) {
                return Result.failure(Exception("Network connection failed"))
            }

            // Mock API data - simulating JSONPlaceholder API response
            val mockUsers = listOf(
                UserEntity(1, "John Doe", "john@example.com", "555-0105", "john.web"),
                UserEntity(2, "Jane Smith", "jane@example.com", "555-0104", "jane.app"),
                UserEntity(3, "Bob Johnson", "bob@example.com", "555-0102", "bob.tech"),
                UserEntity(4, "Alice Brown", "alice@example.com", "555-0101", "alice.dev"),
                UserEntity(5, "David Wilson", "david@example.com", "555-0103", "david.info"),
                UserEntity(6, "Sarah Connor", "sarah@example.com", "555-0106", "sarah.dev"),
                UserEntity(7, "Mike Johnson", "mike@example.com", "555-0107", "mike.tech"),
                UserEntity(8, "Lisa Anderson", "lisa@example.com", "555-0108", "lisa.design"),
                UserEntity(9, "Tom Wilson", "tom@example.com", "555-0109", "tom.marketing"),
                UserEntity(10, "Emma Davis", "emma@example.com", "555-0110", "emma.sales"),
                UserEntity(11, "Chris Brown", "chris@example.com", "555-0111", "chris.support"),
                UserEntity(12, "Anna Taylor", "anna@example.com", "555-0112", "anna.hr")
            )

            // Save to database
            userDao.insertUsers(mockUsers)

            // Update cache info
            cacheDao.insertCacheInfo(
                CacheEntity(
                    key = "users_cache",
                    lastSyncTime = System.currentTimeMillis(),
                    dataType = "users"
                )
            )

            Result.success(mockUsers)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun clearCache() {
        userDao.deleteAllUsers()
        cacheDao.clearAllCache()
    }

    suspend fun getCacheInfo(): CacheEntity? {
        return cacheDao.getCacheInfo("users_cache")
    }

    suspend fun getUserCount(): Int {
        return userDao.getUserCount()
    }
}