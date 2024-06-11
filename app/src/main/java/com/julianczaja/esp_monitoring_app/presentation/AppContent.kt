package com.julianczaja.esp_monitoring_app.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.rememberNavController
import com.julianczaja.esp_monitoring_app.R
import com.julianczaja.esp_monitoring_app.components.DefaultAppBar
import com.julianczaja.esp_monitoring_app.navigation.EspMonitoringNavHost
import com.julianczaja.esp_monitoring_app.rememberEspMonitoringAppState

@Composable
fun AppContent(modifier: Modifier = Modifier) {

    val appState = rememberEspMonitoringAppState(rememberNavController())
    val snackbarHostState = remember { SnackbarHostState() }
    var appBarTitleId by remember { mutableStateOf(R.string.app_name) }

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            DefaultAppBar(
                title = stringResource(id = appBarTitleId),
                shouldShowNavigationIcon = appState.shouldShowNavigationIcon,
                onBackClick = appState::onBackClick
            )
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
        ) {
            EspMonitoringNavHost(
                onSetAppBarTitle = { appBarTitleId = it },
                navController = appState.navController,
                snackbarHostState = snackbarHostState,
                onBackClick = appState::onBackClick,
                modifier = modifier
                    .padding(paddingValues)
                    .consumeWindowInsets(paddingValues)
            )
        }
    }
}
