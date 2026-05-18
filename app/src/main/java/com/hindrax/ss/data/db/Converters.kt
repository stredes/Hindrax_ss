package com.hindrax.ss.data.db

import androidx.room.TypeConverter
import com.hindrax.ss.domain.tasks.model.ChecklistItem
import com.hindrax.ss.domain.tasks.model.TaskStatus
import com.hindrax.ss.domain.tasks.model.TaskType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    @TypeConverter
    fun fromTaskStatus(status: TaskStatus): String {
        return status.name
    }

    @TypeConverter
    fun toTaskStatus(status: String): TaskStatus {
        return TaskStatus.valueOf(status)
    }

    @TypeConverter
    fun fromTaskType(type: TaskType): String {
        return type.name
    }

    @TypeConverter
    fun toTaskType(type: String): TaskType {
        return TaskType.valueOf(type)
    }

    @TypeConverter
    fun fromChecklist(checklist: List<ChecklistItem>): String {
        return Json.encodeToString(checklist)
    }

    @TypeConverter
    fun toChecklist(checklistJson: String): List<ChecklistItem> {
        return try {
            Json.decodeFromString(checklistJson)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
