package com.hindrax.ss.features.targets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hindrax.ss.data.entity.AllowedTargetEntity
import com.hindrax.ss.data.repository.TargetRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AllowedTargetsViewModel(
    private val targetRepository: TargetRepository
) : ViewModel() {
    val allowedTargets: StateFlow<List<AllowedTargetEntity>> = targetRepository.getAllAllowedTargets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addTarget(value: String, type: String, note: String) {
        viewModelScope.launch {
            val newTarget = AllowedTargetEntity(
                targetValue = value,
                targetType = type,
                authorizationNote = note,
                createdAt = System.currentTimeMillis(),
                expiresAt = System.currentTimeMillis() + (1000L * 60 * 60 * 24 * 30) // 30 days default
            )
            targetRepository.addAllowedTarget(newTarget)
        }
    }

    fun removeTarget(target: AllowedTargetEntity) {
        viewModelScope.launch {
            targetRepository.removeAllowedTarget(target)
        }
    }
}
