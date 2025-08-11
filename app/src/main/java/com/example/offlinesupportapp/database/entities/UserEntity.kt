package com.example.offlinesupportapp.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val id: Int,
    val name: String,
    val email: String,
    val phone: String? = null,
    val website: String? = null,
    val isFromCache: Boolean = false
)