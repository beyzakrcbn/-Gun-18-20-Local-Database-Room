package com.example.offlinesupportapp.database.dao

import androidx.room.*
import com.example.offlinesupportapp.database.entities.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Query("SELECT * FROM users ORDER BY name ASC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE isOnline = 1")
    fun getOnlineUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE isCached = 1")
    fun getCachedUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserById(userId: Int): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Delete
    suspend fun deleteUser(user: UserEntity)

    @Query("DELETE FROM users")
    suspend fun clearAllUsers()

    @Query("UPDATE users SET isOnline = :isOnline WHERE id = :userId")
    suspend fun updateUserOnlineStatus(userId: Int, isOnline: Boolean)

    @Query("UPDATE users SET isCached = :isCached WHERE id = :userId")
    suspend fun updateUserCacheStatus(userId: Int, isCached: Boolean)
}