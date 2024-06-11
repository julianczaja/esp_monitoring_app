package com.julianczaja.esp_monitoring_app.domain

import com.julianczaja.esp_monitoring_app.data.utils.toBoolean
import com.julianczaja.esp_monitoring_app.data.utils.toInt
import com.julianczaja.esp_monitoring_app.domain.model.DeviceSettings
import com.julianczaja.esp_monitoring_app.domain.model.EspCameraFrameSize
import com.julianczaja.esp_monitoring_app.domain.model.EspCameraPhotoInterval
import com.julianczaja.esp_monitoring_app.domain.model.EspCameraSpecialEffect
import com.julianczaja.esp_monitoring_app.domain.model.EspCameraWhiteBalanceMode
import com.julianczaja.esp_monitoring_app.domain.model.WifiCredentials
import com.juul.kable.Characteristic
import com.juul.kable.Peripheral
import com.juul.kable.WriteType
import com.juul.kable.characteristicOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber
import kotlin.time.measureTime

class MonitoringDevice(
    private val peripheral: Peripheral,
) : Peripheral by peripheral {

    private companion object {
        const val INFO_SERVICE_UUID = "691ff8e2-b4d3-4c2e-b72f-95b6e5acd64b"
        const val DEVICE_ID_CHARACTERISTIC_UUID = "17a4e7f7-f645-4f67-a618-98037cb4372a"

        const val WIFI_CREDENTIALS_SERVICE_UUID = "9717095f-7084-4a9c-a29c-7d030d8a86d2"
        const val WIFI_SSID_CHARACTERISTIC_UUID = "1b5f00db-42f9-4cbd-a6ba-0e19a9192118"
        const val WIFI_PASSWORD_CHARACTERISTIC_UUID = "bf4c3e01-11d0-40b4-9109-51202baebd28"

        const val SETTINGS_SERVICE_UUID = "ecb44d46-93cf-45cc-bd34-b82205e80d7b"
        const val FRAME_SIZE_CHARACTERISTIC_UUID = "2c0980bd-efc9-49e2-8043-ad94bf4bf81e"
        const val PHOTO_INTERVAL_CHARACTERISTIC_UUID = "184711ee-5964-4f31-9d2b-5405da58a7d9"
        const val SPECIAL_EFFECT_CHARACTERISTIC_UUID = "805fad03-b784-4bd0-8499-c86a2bc6f8d4"
        const val WHITE_BALANCE_CHARACTERISTIC_UUID = "b243a39d-a337-4516-b183-d6cdbcc524e9"
        const val QUALITY_CHARACTERISTIC_UUID = "06135106-f60d-4d46-858d-b8988f33aafa"
        const val BRIGHTNESS_CHARACTERISTIC_UUID = "1d7e8059-f231-44f2-be7c-9cb51855c30b"
        const val CONTRAST_CHARACTERISTIC_UUID = "2cd319ae-4bd4-4914-8571-80262adcbe16"
        const val SATURATION_CHARACTERISTIC_UUID = "f974b4b3-d1c0-4170-87e0-bfe19a1caf22"
        const val FLASH_ON_CHARACTERISTIC_UUID = "e8da265f-dba8-495e-b056-888b36bf9298"
        const val VERTICAL_FLIP_CHARACTERISTIC_UUID = "597b76be-0816-4564-872c-0355df5d12fe"
        const val HORIZONTAL_MIRROR_CHARACTERISTIC_UUID = "ae21155a-7cde-41d7-ad70-2b7daac1de59"

        const val WAIT_TIME_MS_AFTER_WRITE = 100L
    }

    private val deviceIdCharacteristic = infoCharacteristicOf(DEVICE_ID_CHARACTERISTIC_UUID)

    private val wifiSsidCharacteristic = wifiCredentialsCharacteristicOf(WIFI_SSID_CHARACTERISTIC_UUID)
    private val wifiPasswordCharacteristic = wifiCredentialsCharacteristicOf(WIFI_PASSWORD_CHARACTERISTIC_UUID)

    private val frameSizeCharacteristic = settingsCharacteristicOf(FRAME_SIZE_CHARACTERISTIC_UUID)
    private val photoIntervalCharacteristic = settingsCharacteristicOf(PHOTO_INTERVAL_CHARACTERISTIC_UUID)
    private val specialEffectCharacteristic = settingsCharacteristicOf(SPECIAL_EFFECT_CHARACTERISTIC_UUID)
    private val whiteBalanceCharacteristic = settingsCharacteristicOf(WHITE_BALANCE_CHARACTERISTIC_UUID)
    private val qualityCharacteristic = settingsCharacteristicOf(QUALITY_CHARACTERISTIC_UUID)
    private val brightnessCharacteristic = settingsCharacteristicOf(BRIGHTNESS_CHARACTERISTIC_UUID)
    private val contrastCharacteristic = settingsCharacteristicOf(CONTRAST_CHARACTERISTIC_UUID)
    private val saturationCharacteristic = settingsCharacteristicOf(SATURATION_CHARACTERISTIC_UUID)
    private val flashOnCharacteristic = settingsCharacteristicOf(FLASH_ON_CHARACTERISTIC_UUID)
    private val verticalFlipCharacteristic = settingsCharacteristicOf(VERTICAL_FLIP_CHARACTERISTIC_UUID)
    private val horizontalMirrorCharacteristic = settingsCharacteristicOf(HORIZONTAL_MIRROR_CHARACTERISTIC_UUID)

    private val _isBusy = MutableStateFlow(true)
    val isBusy = _isBusy.asStateFlow()

    private val _deviceSettings = MutableStateFlow(DeviceSettings())
    val deviceSettings = _deviceSettings.asStateFlow()

    private var isInitiated = false

    suspend fun init() {
        if (!isInitiated) {
            readDeviceData()
            isInitiated = true
        }
    }

    private fun infoCharacteristicOf(characteristic: String): Characteristic =
        characteristicOf(INFO_SERVICE_UUID, characteristic)

    private fun wifiCredentialsCharacteristicOf(characteristic: String): Characteristic =
        characteristicOf(WIFI_CREDENTIALS_SERVICE_UUID, characteristic)

    private fun settingsCharacteristicOf(characteristic: String): Characteristic =
        characteristicOf(SETTINGS_SERVICE_UUID, characteristic)

    private suspend fun readDeviceData() {
        Timber.d("readSettings")
        try {
            _isBusy.update { true }
            val timeTaken = measureTime {
                readDeviceId()
                readWifiSsid()
                readFrameSize()
                readPhotoInterval()
                readSpecialEffect()
                readWhiteBalanceMode()
                readQuality()
                readBrightness()
                readContrast()
                readSaturation()
                readFlashOn()
                readVerticalFlip()
                readHorizontalMirror()
            }.inWholeMilliseconds
            Timber.d("readSettings took $timeTaken ms")
            _isBusy.update { false }
        } catch (e: Exception) {
            _isBusy.update { false }
            throw e
        }
    }

    suspend fun updateSettings(newSettings: DeviceSettings) {
        Timber.d("updateSettings = $newSettings")
        _isBusy.update { true }
        val timeTaken = measureTime {
            if (_deviceSettings.value.frameSize != newSettings.frameSize) writeFrameSize(newSettings.frameSize)
            if (_deviceSettings.value.photoInterval != newSettings.photoInterval) writePhotoInterval(newSettings.photoInterval)
            if (_deviceSettings.value.specialEffect != newSettings.specialEffect) writeSpecialEffect(newSettings.specialEffect)
            if (_deviceSettings.value.whiteBalanceMode != newSettings.whiteBalanceMode) writeWhiteBalanceMode(newSettings.whiteBalanceMode)
            if (_deviceSettings.value.jpegQuality != newSettings.jpegQuality) writeQuality(newSettings.jpegQuality)
            if (_deviceSettings.value.brightness != newSettings.brightness) writeBrightness(newSettings.brightness)
            if (_deviceSettings.value.contrast != newSettings.contrast) writeContrast(newSettings.contrast)
            if (_deviceSettings.value.saturation != newSettings.saturation) writeSaturation(newSettings.saturation)
            if (_deviceSettings.value.flashOn != newSettings.flashOn) writeFlashOn(newSettings.flashOn)
            if (_deviceSettings.value.verticalFlip != newSettings.verticalFlip) writeVerticalFlip(newSettings.verticalFlip)
            if (_deviceSettings.value.horizontalMirror != newSettings.horizontalMirror) writeHorizontalMirror(newSettings.horizontalMirror)
        }
        Timber.d("updateSettings took ${timeTaken.inWholeMilliseconds} ms")
        _isBusy.update { false }
    }

    suspend fun updateWifiCredentials(wifiCredentials: WifiCredentials) {
        _isBusy.update { true }
        writeWifiCredentials(wifiCredentials)
        _isBusy.update { false }
    }

    //region read/write
    private suspend fun readDeviceId() {
        val deviceId = readIntCharacteristic(deviceIdCharacteristic).toLong()
        _deviceSettings.update { it.copy(deviceId = deviceId) }
    }

    private suspend fun readWifiSsid() {
        val newWifiSsid = readStringCharacteristic(wifiSsidCharacteristic)
        _deviceSettings.update { it.copy(wifiSsid = newWifiSsid) }
    }

    private suspend fun writeWifiCredentials(wifiCredentials: WifiCredentials) {
        writeStringCharacteristic(wifiSsidCharacteristic, wifiCredentials.ssid)
        delay(WAIT_TIME_MS_AFTER_WRITE)
        writeStringCharacteristic(wifiPasswordCharacteristic, wifiCredentials.password)
        delay(WAIT_TIME_MS_AFTER_WRITE)
        readWifiSsid()
    }

    private suspend fun readFrameSize() {
        val newFrameSize = readIntCharacteristic(frameSizeCharacteristic)
        _deviceSettings.update { it.copy(frameSize = EspCameraFrameSize.entries[newFrameSize]) }
    }

    private suspend fun writeFrameSize(frameSize: EspCameraFrameSize) {
        writeIntCharacteristic(frameSizeCharacteristic, frameSize.ordinal)
        delay(WAIT_TIME_MS_AFTER_WRITE)
        readFrameSize()
    }

    private suspend fun readPhotoInterval() {
        val newPhotoIntervalSec = readIntCharacteristic(photoIntervalCharacteristic)
        _deviceSettings.update { it.copy(photoInterval = EspCameraPhotoInterval.entries[newPhotoIntervalSec]) }
    }

    private suspend fun writePhotoInterval(photoIntervalSec: EspCameraPhotoInterval) {
        writeIntCharacteristic(photoIntervalCharacteristic, photoIntervalSec.ordinal)
        delay(WAIT_TIME_MS_AFTER_WRITE)
        readPhotoInterval()
    }

    private suspend fun readSpecialEffect() {
        val newSpecialEffect = readIntCharacteristic(specialEffectCharacteristic)
        _deviceSettings.update { it.copy(specialEffect = EspCameraSpecialEffect.entries[newSpecialEffect]) }
    }

    private suspend fun writeSpecialEffect(specialEffect: EspCameraSpecialEffect) {
        writeIntCharacteristic(specialEffectCharacteristic, specialEffect.ordinal)
        delay(WAIT_TIME_MS_AFTER_WRITE)
        readSpecialEffect()
    }

    private suspend fun readWhiteBalanceMode() {
        val newWhiteBalanceMode = readIntCharacteristic(whiteBalanceCharacteristic)
        _deviceSettings.update { it.copy(whiteBalanceMode = EspCameraWhiteBalanceMode.entries[newWhiteBalanceMode]) }
    }

    private suspend fun writeWhiteBalanceMode(whiteBalanceMode: EspCameraWhiteBalanceMode) {
        writeIntCharacteristic(whiteBalanceCharacteristic, whiteBalanceMode.ordinal)
        delay(WAIT_TIME_MS_AFTER_WRITE)
        readWhiteBalanceMode()
    }

    private suspend fun readQuality() {
        val newQuality = readIntCharacteristic(qualityCharacteristic)
        _deviceSettings.update { it.copy(jpegQuality = newQuality) }
    }

    private suspend fun writeQuality(quality: Int) {
        writeIntCharacteristic(qualityCharacteristic, quality)
        delay(WAIT_TIME_MS_AFTER_WRITE)
        readQuality()
    }

    private suspend fun readBrightness() {
        val newBrightness = readIntCharacteristic(brightnessCharacteristic)
        _deviceSettings.update { it.copy(brightness = newBrightness) }
    }

    private suspend fun writeBrightness(brightness: Int) {
        writeIntCharacteristic(brightnessCharacteristic, brightness)
        delay(WAIT_TIME_MS_AFTER_WRITE)
        readBrightness()
    }

    private suspend fun readContrast() {
        val newContrast = readIntCharacteristic(contrastCharacteristic)
        _deviceSettings.update { it.copy(contrast = newContrast) }
    }

    private suspend fun writeContrast(contrast: Int) {
        writeIntCharacteristic(contrastCharacteristic, contrast)
        delay(WAIT_TIME_MS_AFTER_WRITE)
        readContrast()
    }

    private suspend fun readSaturation() {
        val newSaturation = readIntCharacteristic(saturationCharacteristic)
        _deviceSettings.update { it.copy(saturation = newSaturation) }
    }

    private suspend fun writeSaturation(saturation: Int) {
        writeIntCharacteristic(saturationCharacteristic, saturation)
        delay(WAIT_TIME_MS_AFTER_WRITE)
        readSaturation()
    }

    private suspend fun readFlashOn() {
        val newFlashOn = readIntCharacteristic(flashOnCharacteristic)
        _deviceSettings.update { it.copy(flashOn = newFlashOn.toBoolean()) }
    }

    private suspend fun writeFlashOn(flashOn: Boolean) {
        writeIntCharacteristic(flashOnCharacteristic, flashOn.toInt())
        delay(WAIT_TIME_MS_AFTER_WRITE)
        readFlashOn()
    }

    private suspend fun readVerticalFlip() {
        val newVerticalFlip = readIntCharacteristic(verticalFlipCharacteristic)
        _deviceSettings.update { it.copy(verticalFlip = newVerticalFlip.toBoolean()) }
    }

    private suspend fun writeVerticalFlip(verticalFlip: Boolean) {
        writeIntCharacteristic(verticalFlipCharacteristic, verticalFlip.toInt())
        delay(WAIT_TIME_MS_AFTER_WRITE)
        readVerticalFlip()
    }

    private suspend fun readHorizontalMirror() {
        val newHorizontalMirror = readIntCharacteristic(horizontalMirrorCharacteristic)
        _deviceSettings.update { it.copy(horizontalMirror = newHorizontalMirror.toBoolean()) }
    }

    private suspend fun writeHorizontalMirror(horizontalMirror: Boolean) {
        writeIntCharacteristic(horizontalMirrorCharacteristic, horizontalMirror.toInt())
        delay(WAIT_TIME_MS_AFTER_WRITE)
        readHorizontalMirror()
    }

    private suspend fun readIntCharacteristic(characteristic: Characteristic): Int =
        String(read(characteristic)).toInt()
            .also { Timber.d("read $characteristic: $it") }

    private suspend fun writeIntCharacteristic(characteristic: Characteristic, value: Int): Unit =
        write(characteristic, value.toString().toByteArray(), WriteType.WithResponse)
            .also { Timber.d("write $characteristic: $value") }

    private suspend fun readStringCharacteristic(characteristic: Characteristic): String =
        String(read(characteristic))
            .also { Timber.d("read $characteristic: $it") }

    private suspend fun writeStringCharacteristic(characteristic: Characteristic, value: String): Unit =
        write(characteristic, value.toByteArray(), WriteType.WithResponse)
            .also { Timber.d("write $characteristic: $value") }
    //endregion
}
