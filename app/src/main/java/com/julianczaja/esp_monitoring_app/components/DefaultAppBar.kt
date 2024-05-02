package com.julianczaja.esp_monitoring_app.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.julianczaja.esp_monitoring_app.EspMonitoringAppState
import com.julianczaja.esp_monitoring_app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DefaultAppBar(appState: EspMonitoringAppState) {
    CenterAlignedTopAppBar(
        title = {
            Text(text = stringResource(id = R.string.app_name))
        },
        navigationIcon = {
            if (appState.shouldShowNavigationIcon) {
                IconButton(
                    onClick = appState::onBackClick
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        tint = MaterialTheme.colorScheme.onSurface,
                        contentDescription = null
                    )
                }
            }
        }
    )
}
