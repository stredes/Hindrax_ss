package com.hindrax.ss.presentation.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hindrax.ss.data.db.InventoryDao
import com.hindrax.ss.data.entity.InventoryEntity
import com.hindrax.ss.data.repository.ChatRepository
import com.hindrax.ss.domain.tasks.model.InventoryItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InventoryUiState(
    val items: List<InventoryItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val inventoryDao: InventoryDao,
    private val chatRepository: ChatRepository
) : ViewModel() {

    val uiState: StateFlow<InventoryUiState> = inventoryDao.observeInventory()
        .map { entities -> 
            InventoryUiState(items = entities.map { it.toDomain() }) 
        }
        .catch { e -> emit(InventoryUiState(error = e.message)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), InventoryUiState(isLoading = true))

    fun addItem(name: String, category: String, minQty: Double, unit: String) {
        viewModelScope.launch {
            val item = InventoryEntity(
                name = name,
                category = category,
                currentQuantity = 0.0,
                minQuantity = minQty,
                unit = unit,
                updatedAt = System.currentTimeMillis()
            )
            val id = inventoryDao.insert(item)
            inventoryDao.getById(id)?.let { chatRepository.broadcastInventory(it) }
        }
    }

    fun updateQuantity(id: Long, delta: Double) {
        viewModelScope.launch {
            val item = inventoryDao.getById(id) ?: return@launch
            inventoryDao.updateQuantity(id, item.currentQuantity + delta, System.currentTimeMillis())
            inventoryDao.getById(id)?.let { chatRepository.broadcastInventory(it) }
        }
    }

    fun deleteItem(item: InventoryItem) {
        viewModelScope.launch {
            inventoryDao.delete(item.toEntity())
        }
    }
}

fun InventoryEntity.toDomain() = InventoryItem(id, name, category, currentQuantity, minQuantity, unit, updatedAt)
fun InventoryItem.toEntity() = InventoryEntity(id, name, category, currentQuantity, minQuantity, unit, updatedAt)
