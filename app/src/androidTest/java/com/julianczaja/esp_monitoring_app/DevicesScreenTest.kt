package com.julianczaja.esp_monitoring_app

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.filter
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.julianczaja.esp_monitoring_app.components.AppBackground
import com.julianczaja.esp_monitoring_app.domain.model.Device
import com.julianczaja.esp_monitoring_app.domain.model.Photo
import com.julianczaja.esp_monitoring_app.presentation.devices.DevicesScreenContent
import com.julianczaja.esp_monitoring_app.presentation.devices.DevicesScreenViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DevicesScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun aaaTest() {
        val deviceName = "Device 1"
        val devices = mapOf(Device(id = 1L, name = deviceName) to null)
        startDevicesScreen(devices)

        composeTestRule
            .onAllNodesWithTag("DeviceItemCard")
            .assertCountEquals(1)
            .filter(hasText(deviceName))
            .assertCountEquals(1)
            .onFirst()
            .assertIsDisplayed()
            .assertHasClickAction()

        composeTestRule
            .onAllNodesWithTag("ErrorText")
            .assertCountEquals(0)
        composeTestRule
            .onAllNodesWithTag("DefaultProgressIndicator")
            .assertCountEquals(0)
    }

    private fun startDevicesScreen(devices: Map<Device, Photo?>) {
        composeTestRule.setContent {
            AppBackground {
                DevicesScreenContent(
                    uiState = DevicesScreenViewModel.UiState.Success(devices),
                    onDeviceClicked = {},
                    onRemoveDeviceClicked = {},
                    onAddDeviceClicked = {},
                    onEditDeviceClicked = {},
                    onAppSettingsClicked = {},
                    onDeviceSettingsClicked = {}
                )
            }
        }
    }
}
