package com.julianczaja.esp_monitoring_app

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.julianczaja.esp_monitoring_app.di.AppModule
import com.julianczaja.esp_monitoring_app.di.DispatchersModule
import com.julianczaja.esp_monitoring_app.presentation.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

@HiltAndroidTest
@UninstallModules(AppModule::class, DispatchersModule::class)
class MainActivityTest {

    private val hiltRule = HiltAndroidRule(this)
    private val composeTestRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rule: RuleChain = RuleChain
        .outerRule(hiltRule)
        .around(composeTestRule)

    @Test
    fun aaaTest() {
        composeTestRule.waitForIdle()

        composeTestRule.onAllNodesWithTag("DeviceItemCard").assertCountEquals(0)

        composeTestRule.onNodeWithTag("add_new_device_button")
            .assertExists()
            .performClick()

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("device_name_text_filed")
            .assertExists()
            .performClick()
            .performTextInput("Device 1")

        composeTestRule.onNodeWithTag("device_id_text_filed")
            .assertExists()
            .performClick()
            .performTextInput("1")

        composeTestRule.onNodeWithTag("apply_button")
            .assertExists()
            .performClick()

        composeTestRule.waitForIdle()

        composeTestRule.onAllNodesWithTag("DeviceItemCard")
            .assertCountEquals(1)
            .onFirst()
            .assertHasClickAction()
    }
}
