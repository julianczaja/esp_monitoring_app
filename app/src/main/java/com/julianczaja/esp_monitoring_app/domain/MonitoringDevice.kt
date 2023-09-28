package com.julianczaja.esp_monitoring_app.domain

import com.julianczaja.esp_monitoring_app.domain.model.DeviceSettings
import com.juul.kable.Peripheral
import com.juul.kable.WriteType
import com.juul.kable.characteristicOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber

class MonitoringDevice(
    private val deviceId: Long,
    private val peripheral: Peripheral,
) : Peripheral by peripheral {

    companion object {
        const val INFO_SERVICE_UUID = "691ff8e2-b4d3-4c2e-b72f-95b6e5acd64b"
        const val SETTINGS_SERVICE_UUID = "ecb44d46-93cf-45cc-bd34-b82205e80d7b"
        const val DEVICE_ID_CHARACTERISTIC_UUID = "17a4e7f7-f645-4f67-a618-98037cb4372a"
        const val FRAME_SIZE_CHARACTERISTIC_UUID = "2c0980bd-efc9-49e2-8043-ad94bf4bf81e"
        const val QUALITY_CHARACTERISTIC_UUID = "06135106-f60d-4d46-858d-b8988f33aafa"
        const val BRIGHTNESS_CHARACTERISTIC_UUID = "1d7e8059-f231-44f2-be7c-9cb51855c30b"
    }

    private val brightnessCharacteristic = characteristicOf(
        service = SETTINGS_SERVICE_UUID,
        characteristic = BRIGHTNESS_CHARACTERISTIC_UUID,
    )

    val deviceSettings = MutableStateFlow(DeviceSettings(deviceId))

    suspend fun updateBrightness() {
        val newBrightness = String(read(brightnessCharacteristic)).toInt()
        deviceSettings.update { it.copy(brightness = newBrightness) }
        Timber.e("updateBrightness = $newBrightness")
    }

    suspend fun setBrightness(brightness: Int) {
        Timber.e("setBrightness = $brightness")
        write(brightnessCharacteristic, "$brightness".toByteArray(), WriteType.WithResponse)
        delay(200)
        updateBrightness()
    }
}
