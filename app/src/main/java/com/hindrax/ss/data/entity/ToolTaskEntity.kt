package com.hindrax.ss.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tool_tasks")
data class ToolTaskEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    val executionMode: String, // NATIVE, TERMUX_SCRIPT
    val scriptName: String? = null,
    val riskLevel: String,
    val enabled: Boolean,
    val localOnly: Boolean
)
