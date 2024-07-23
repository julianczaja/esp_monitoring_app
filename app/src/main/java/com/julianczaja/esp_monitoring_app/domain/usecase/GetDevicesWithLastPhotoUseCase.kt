package com.julianczaja.esp_monitoring_app.domain.usecase

import com.julianczaja.esp_monitoring_app.domain.model.Device
import com.julianczaja.esp_monitoring_app.domain.model.Photo
import com.julianczaja.esp_monitoring_app.domain.repository.DeviceRepository
import com.julianczaja.esp_monitoring_app.domain.repository.PhotoRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject


class GetDevicesWithLastPhotoUseCase @Inject constructor(
    private val deviceRepository: DeviceRepository,
    private val photoRepository: PhotoRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke() = deviceRepository.getAllDevices()
        .flatMapLatest { devices ->
            if (devices.isEmpty()) return@flatMapLatest flowOf(emptyMap<Device, Photo?>())

            val flows = devices.map { device ->
                photoRepository.getLastPhotoLocal(device.id).map { photo ->
                    device to photo
                }
            }
            return@flatMapLatest combine(flows) { it.toMap() }
        }
}
