package com.hindrax.ss.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "allowed_targets")
data class AllowedTargetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val targetValue: String,
    val targetType: String,
    val label: String? = null,
    val authorizationNote: String,
    val createdAt: Long,
    val expiresAt: Long? = null
)
