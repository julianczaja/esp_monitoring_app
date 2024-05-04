package com.julianczaja.esp_monitoring_app.presentation.devices


import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.julianczaja.esp_monitoring_app.R
import com.julianczaja.esp_monitoring_app.domain.model.Device
import com.julianczaja.esp_monitoring_app.presentation.devices.DevicesScreenViewModel.UiState
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog

@Config(instrumentedPackages = ["androidx.loader.content"]) // https://github.com/robolectric/robolectric/issues/6593
@RunWith(RobolectricTestRunner::class)
class DevicesScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Before
    @Throws(Exception::class)
    fun setUp() {
        ShadowLog.stream = System.out // redirect Logcat to console
    }

    @Test
    fun uiState_success_DevicesScreen() {
        val deviceName = "Device 1"

        composeTestRule.setContent {
            DevicesScreenContent(
                uiState = UiState.Success(listOf(Device(1L, deviceName))),
                onDeviceClicked = {},
                onRemoveDeviceClicked = {},
                onEditDeviceClicked = {}
            )
        }
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

    @Test
    fun uiState_loading_DevicesScreen() {
        composeTestRule.setContent {
            DevicesScreenContent(
                uiState = UiState.Loading,
                onDeviceClicked = {},
                onRemoveDeviceClicked = {},
                onEditDeviceClicked = {}
            )
        }
        composeTestRule
            .onNodeWithTag("DefaultProgressIndicator")
            .assertIsDisplayed()

        composeTestRule
            .onAllNodesWithTag("ErrorText")
            .assertCountEquals(0)
    }

    @Test
    fun uiState_error_DevicesScreen() {
        composeTestRule.setContent {
            DevicesScreenContent(
                uiState = UiState.Error(R.string.unknown_error_message),
                onDeviceClicked = {},
                onRemoveDeviceClicked = {},
                onEditDeviceClicked = {}
            )
        }
        composeTestRule
            .onNodeWithTag("ErrorText")
            .assertIsDisplayed()
            .assertTextEquals(composeTestRule.activity.getString(R.string.unknown_error_message))

        composeTestRule
            .onAllNodesWithTag("DefaultProgressIndicator")
            .assertCountEquals(0)
    }
}
