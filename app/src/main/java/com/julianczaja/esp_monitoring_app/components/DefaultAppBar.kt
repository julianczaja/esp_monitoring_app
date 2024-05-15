package com.julianczaja.esp_monitoring_app.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
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
import com.julianczaja.esp_monitoring_app.navigation.navigateToAppSettings

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
        },
        actions = {
            AnimatedVisibility(visible = appState.shouldShowSettingsIcon) {
                IconButton(onClick = appState.navController::navigateToAppSettings) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        tint = MaterialTheme.colorScheme.onSurface,
                        contentDescription = null
                    )
                }
            }
        }
    )
}
