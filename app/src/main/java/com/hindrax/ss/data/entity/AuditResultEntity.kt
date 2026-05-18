package com.hindrax.ss.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "audit_results",
    foreignKeys = [
        ForeignKey(
            entity = AuditSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["sessionId"])]
)
data class AuditResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val severity: String,
    val category: String,
    val findingTitle: String,
    val findingBody: String,
    val evidence: String? = null,
    val recommendation: String? = null
)
