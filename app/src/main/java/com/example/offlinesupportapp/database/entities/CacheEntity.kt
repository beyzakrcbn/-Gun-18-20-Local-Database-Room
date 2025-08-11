package com.example.offlinesupportapp.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cache_info")
data class CacheEntity(
    @PrimaryKey
    val key: String,
    val lastSyncTime: Long,
    val dataType: String
)