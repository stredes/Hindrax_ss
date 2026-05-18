package com.hindrax.ss.core.security

import com.hindrax.ss.core.model.AuditTask
import com.hindrax.ss.core.model.Target
import com.hindrax.ss.core.model.TargetType

object TargetPolicy {
    fun isTaskAllowedForTarget(task: AuditTask, target: Target): Boolean {
        // Basic rules for MVP
        return when (task.category.uppercase()) {
            "APK" -> target.type is TargetType.APK
            "FILES" -> target.type is TargetType.LocalFile || target.type is TargetType.APK
            "NETWORK" -> target.type is TargetType.PrivateIP || target.type is TargetType.PublicIP || target.type is TargetType.AuthorizedDomain
            "WEB" -> target.type is TargetType.AuthorizedDomain || target.type is TargetType.PublicIP
            "OSINT" -> target.type is TargetType.AuthorizedDomain
            else -> true
        }
    }
}
