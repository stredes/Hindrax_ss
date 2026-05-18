package com.hindrax.ss.domain.cyd

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class DiscoverCydDevicesUseCase @Inject constructor(
    private val repository: CydRepository
) {
    operator fun invoke(): Flow<List<CydDevice>> = repository.discoverDevices()
}

class ConnectToCydUseCase @Inject constructor(
    private val repository: CydRepository
) {
    suspend operator fun invoke(device: CydDevice): Result<Unit> = repository.connect(device)
}

class ExecuteRemoteActionUseCase @Inject constructor(
    private val repository: CydRepository
) {
    suspend operator fun invoke(action: CydAction): Result<Unit> = repository.executeAction(action)
}

class ObserveCydLogsUseCase @Inject constructor(
    private val repository: CydRepository
) {
    operator fun invoke(): Flow<String> = repository.observeLogs()
}

class GetCydStatusUseCase @Inject constructor(
    private val repository: CydRepository
) {
    operator fun invoke(): Flow<DeviceStatus> = repository.observeStatus()
}
