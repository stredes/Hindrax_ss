package com.hindrax.ss.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audit_sessions")
data class AuditSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val taskType: String,
    val target: String,
    val targetType: String,
    val authorizationMode: String,
    val status: String,
    val startedAt: Long,
    val finishedAt: Long? = null,
    val summary: String? = null
)
