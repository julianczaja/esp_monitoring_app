package com.julianczaja.esp_monitoring_app.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.julianczaja.esp_monitoring_app.EspMonitoringNavHost
import com.julianczaja.esp_monitoring_app.R
import com.julianczaja.esp_monitoring_app.navigateToAddNewDeviceDialog
import com.julianczaja.esp_monitoring_app.presentation.theme.AppTheme
import com.julianczaja.esp_monitoring_app.rememberEspMonitoringAppState
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            AppTheme {
                AppBackground {
                    val appState = rememberEspMonitoringAppState(rememberNavController())
                    val snackbarHostState = remember { SnackbarHostState() }

                    Scaffold(
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.onBackground,
                        contentWindowInsets = WindowInsets(0, 0, 0, 0),
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        topBar = {
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
                                                imageVector = Icons.Default.ArrowBack,
                                                tint = MaterialTheme.colorScheme.onSurface,
                                                contentDescription = null
                                            )
                                        }
                                    }
                                }
                            )
                        },
                        floatingActionButton = {
                            if (appState.shouldShowFab) {
                                FloatingActionButton(
                                    onClick = appState.navController::navigateToAddNewDeviceDialog,
                                    modifier = Modifier.safeDrawingPadding()
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_baseline_add_24),
                                        contentDescription = null
                                    )
                                }
                            }

                        }
                    ) { paddingValues ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                        ) {
                            EspMonitoringNavHost(
                                navController = appState.navController,
                                onBackClick = appState::onBackClick,
                                modifier = Modifier
                                    .padding(paddingValues)
                                    .consumedWindowInsets(paddingValues)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize(),
    ) {
        content()
    }
}