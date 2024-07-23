package com.julianczaja.esp_monitoring_app.presentation.appsettings

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.annotation.ExperimentalCoilApi
import coil.imageLoader
import com.julianczaja.esp_monitoring_app.R
import com.julianczaja.esp_monitoring_app.components.AppBackground
import com.julianczaja.esp_monitoring_app.components.DefaultProgressIndicator
import com.julianczaja.esp_monitoring_app.components.SwitchWithLabel
import com.julianczaja.esp_monitoring_app.domain.model.FieldState
import com.julianczaja.esp_monitoring_app.presentation.appsettings.AppSettingsScreenViewModel.UiState
import com.julianczaja.esp_monitoring_app.presentation.appsettings.components.BaseUrlTextField
import com.julianczaja.esp_monitoring_app.presentation.theme.spacing
import kotlinx.collections.immutable.persistentSetOf

@Composable
fun AppSettingsScreen(
    onSetAppBarTitle: (Int) -> Unit,
    snackbarHostState: SnackbarHostState,
    viewModel: AppSettingsScreenViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(true) {
        onSetAppBarTitle(R.string.app_settings_screen_title)
        viewModel.eventFlow.collect { event ->
            when (event) {
                AppSettingsScreenViewModel.Event.BaseUrlSaved -> snackbarHostState.showSnackbar(
                    message = context.getString(R.string.base_url_saved),
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    AppSettingsScreenContent(
        uiState = uiState,
        modifier = Modifier.fillMaxSize(),
        onBaseUrlUpdate = viewModel::setBaseUrl,
        onBaseUrlApply = viewModel::applyBaseUrl,
        onBaseUrlRestoreDefault = viewModel::onBaseUrlRestoreDefault,
        onDynamicColorChanged = viewModel::setDynamicColor,
        onRefreshWidgetsClicked = viewModel::onRefreshWidgetsClicked,
    )
}

@Composable
private fun AppSettingsScreenContent(
    modifier: Modifier = Modifier,
    uiState: UiState,
    onBaseUrlUpdate: (String) -> Unit,
    onBaseUrlApply: () -> Unit,
    onBaseUrlRestoreDefault: () -> Unit,
    onDynamicColorChanged: (Boolean) -> Unit,
    onRefreshWidgetsClicked: () -> Unit,
) {
    when (uiState) {
        UiState.Loading -> LoadingScreen(modifier)
        is UiState.Success -> SuccessScreen(
            modifier = modifier,
            uiState = uiState,
            onBaseUrlUpdate = onBaseUrlUpdate,
            onBaseUrlApply = onBaseUrlApply,
            onBaseUrlRestoreDefault = onBaseUrlRestoreDefault,
            onDynamicColorChanged = onDynamicColorChanged,
            onRefreshWidgetsClicked = onRefreshWidgetsClicked,
        )
    }
}

@Composable
private fun LoadingScreen(modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        DefaultProgressIndicator(modifier = Modifier.align(Alignment.Center))
    }
}

@OptIn(ExperimentalCoilApi::class)
@Composable
private fun SuccessScreen(
    modifier: Modifier = Modifier,
    uiState: UiState.Success,
    onBaseUrlUpdate: (String) -> Unit,
    onBaseUrlApply: () -> Unit,
    onBaseUrlRestoreDefault: () -> Unit,
    onDynamicColorChanged: (Boolean) -> Unit,
    onRefreshWidgetsClicked: () -> Unit,
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val shouldShowDynamicColorSettings = remember { Build.VERSION.SDK_INT >= 31 }

    Column(
        modifier = modifier.padding(MaterialTheme.spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        BaseUrlTextField(
            modifier = Modifier.fillMaxWidth(),
            fieldState = uiState.baseUrlFieldState,
            history = uiState.baseUrlHistory,
            onBaseUrlUpdate = onBaseUrlUpdate
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = MaterialTheme.spacing.small),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(onClick = onBaseUrlRestoreDefault) {
                Text(text = stringResource(id = R.string.restore_default_label))
            }
            Button(
                onClick = {
                    onBaseUrlApply()
                    focusManager.clearFocus()
                },
                enabled = uiState.baseUrlFieldState.error == null
            ) {
                Text(text = stringResource(id = R.string.update_label))
            }
        }

        HorizontalDivider(Modifier.padding(vertical = MaterialTheme.spacing.large))

        if (shouldShowDynamicColorSettings) {
            SwitchWithLabel(
                modifier = Modifier.fillMaxWidth(),
                label = stringResource(R.string.dynamic_color_label),
                isChecked = uiState.isDynamicColor,
                onCheckedChange = onDynamicColorChanged,
                enabled = true
            )
        }

        HorizontalDivider(Modifier.padding(vertical = MaterialTheme.spacing.large))

        Button(
            onClick = {
                context.imageLoader.run {
                    diskCache?.clear()
                    memoryCache?.clear()
                }
            }
        ) {
            Text(text = stringResource(R.string.clear_photos_cache_label))
        }

        Button(onClick = onRefreshWidgetsClicked) {
            Text(stringResource(R.string.refresh_widgets_label))
        }
    }
}

//region Preview
@Preview
@Composable
private fun LoadingScreenPreview() {
    AppBackground(Modifier.size(300.dp)) {
        LoadingScreen()
    }
}

@Preview
@Composable
private fun SuccessScreenNoBaseUrlErrorPreview() {
    AppBackground {
        SuccessScreen(
            uiState = UiState.Success(
                baseUrlFieldState = FieldState("goodBaseUrl", null),
                baseUrlHistory = persistentSetOf("goodBaseUrl1", "goodBaseUrl2", "goodBaseUrl3"),
                isDynamicColor = false
            ),
            onBaseUrlUpdate = {},
            onBaseUrlApply = {},
            onBaseUrlRestoreDefault = {},
            onDynamicColorChanged = {},
            onRefreshWidgetsClicked = {},
        )
    }
}

@Preview
@Composable
private fun SuccessScreenBaseUrlErrorPreview() {
    AppBackground {
        SuccessScreen(
            uiState = UiState.Success(
                baseUrlFieldState = FieldState("goodBaseUrl", R.string.base_url_invalid),
                baseUrlHistory = persistentSetOf("goodBaseUrl1", "goodBaseUrl2", "goodBaseUrl3"),
                isDynamicColor = true
            ),
            onBaseUrlUpdate = {},
            onBaseUrlApply = {},
            onBaseUrlRestoreDefault = {},
            onDynamicColorChanged = {},
            onRefreshWidgetsClicked = {},
        )
    }
}
//endregion
