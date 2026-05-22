package com.hindrax.ss.domain.cyd

import kotlinx.coroutines.flow.Flow

interface CydRepository {
    fun discoverDevices(): Flow<List<CydDevice>>
    suspend fun connect(device: CydDevice): Result<Unit>
    suspend fun disconnect()
    fun observeStatus(): Flow<DeviceStatus>
    fun observeLogs(): Flow<String>
    suspend fun getDeviceInfo(): Result<CydDevice>
    suspend fun executeAction(action: CydAction): Result<Unit>
    suspend fun listFiles(): Result<List<String>>
    suspend fun uploadFile(fileName: String, content: ByteArray): Result<Unit>
    suspend fun downloadFile(fileName: String): Result<ByteArray>
}
