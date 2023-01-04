package com.julianczaja.esp_monitoring_app.presentation.deviceconfiguration

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.PagerScope
import com.julianczaja.esp_monitoring_app.R

@OptIn(ExperimentalPagerApi::class)
@Composable
fun PagerScope.DeviceSettingsScreen() {
    DeviceSettingsScreenContent()
}

@OptIn(ExperimentalPagerApi::class)
@Composable
private fun PagerScope.DeviceSettingsScreenContent() {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = stringResource(id = R.string.work_in_progress),
            modifier = Modifier.align(Alignment.Center)
        )
    }
}
