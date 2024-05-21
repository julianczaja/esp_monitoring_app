package com.julianczaja.esp_monitoring_app.domain.model

import com.julianczaja.esp_monitoring_app.common.Constants
import com.juul.kable.AndroidAdvertisement

data class BleAdvertisement(
    val name: String?,
    val address: String,
    val isEspMonitoringDevice: Boolean,
    val isConnectable: Boolean,
    val rssi: Int,
)

fun AndroidAdvertisement.toBleAdvertisement() = BleAdvertisement(
    name = name ?: peripheralName,
    address = address,
    isEspMonitoringDevice = name == Constants.DEFAULT_DEVICE_NAME,
    isConnectable = isConnectable ?: false,
    rssi = rssi
)
