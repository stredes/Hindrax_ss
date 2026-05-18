package com.hindrax.ss.core.model

sealed class TargetType {
    object PrivateIP : TargetType()
    object PublicIP : TargetType()
    object AuthorizedDomain : TargetType()
    object LocalFile : TargetType()
    object APK : TargetType()
    object Unknown : TargetType()
}

data class Target(
    val value: String,
    val type: TargetType
) {
    fun isPrivateOrLocal(): Boolean {
        return type is TargetType.PrivateIP || type is TargetType.LocalFile || type is TargetType.APK
    }
    
    fun isPublic(): Boolean = type is TargetType.PublicIP || type is TargetType.AuthorizedDomain
}
