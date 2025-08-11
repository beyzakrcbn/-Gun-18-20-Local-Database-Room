package com.example.offlinesupportapp.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cache")
data class CacheEntity(
    @PrimaryKey val key: String,
    val data: String,
    val timestamp: Long = System.currentTimeMillis(),
    val expiryTime: Long = System.currentTimeMillis() + (24 * 60 * 60 * 1000) // 24 saat
)