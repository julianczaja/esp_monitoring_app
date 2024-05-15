package com.julianczaja.esp_monitoring_app.presentation.appsettings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.julianczaja.esp_monitoring_app.R
import com.julianczaja.esp_monitoring_app.components.AppBackground
import com.julianczaja.esp_monitoring_app.domain.model.AppSettings
import com.julianczaja.esp_monitoring_app.presentation.appsettings.AppSettingsScreenViewModel.UiState
import com.julianczaja.esp_monitoring_app.presentation.theme.spacing

@Composable
fun AppSettingsScreen(
    snackbarHostState: SnackbarHostState,
    viewModel: AppSettingsScreenViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(true) {
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
    )
}

@Composable
private fun AppSettingsScreenContent(
    modifier: Modifier = Modifier,
    uiState: UiState,
    onBaseUrlUpdate: (String) -> Unit,
    onBaseUrlApply: () -> Unit,
    onBaseUrlRestoreDefault: () -> Unit,
) {
    when (uiState) {
        UiState.Loading -> LoadingScreen(modifier)
        is UiState.Success -> SuccessScreen(
            modifier = modifier,
            uiState = uiState,
            onBaseUrlUpdate = onBaseUrlUpdate,
            onBaseUrlApply = onBaseUrlApply,
            onBaseUrlRestoreDefault = onBaseUrlRestoreDefault
        )
    }
}

@Composable
private fun LoadingScreen(modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        CircularProgressIndicator(Modifier.align(Alignment.Center))
    }
}

@Composable
private fun SuccessScreen(
    modifier: Modifier = Modifier,
    uiState: UiState.Success,
    onBaseUrlUpdate: (String) -> Unit,
    onBaseUrlApply: () -> Unit,
    onBaseUrlRestoreDefault: () -> Unit,
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier.padding(MaterialTheme.spacing.medium),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        OutlinedTextField(
            value = uiState.baseUrlFieldValue,
            onValueChange = onBaseUrlUpdate,
            label = { Text(stringResource(R.string.base_url_label)) },
            isError = uiState.baseUrlFieldError != null,
            singleLine = true,
            supportingText = {
                uiState.baseUrlFieldError?.let { errorId ->
                    Text(
                        text = stringResource(id = errorId),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = MaterialTheme.spacing.small),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            OutlinedButton(onClick = onBaseUrlRestoreDefault) {
                Text(text = stringResource(id = R.string.restore_default_label))
            }
            Button(
                onClick = {
                    onBaseUrlApply()
                    focusManager.clearFocus()
                },
                enabled = uiState.baseUrlFieldError == null
            ) {
                Text(text = stringResource(id = R.string.update_label))
            }
        }
    }
}

//region Preview
@Preview
@Composable
private fun LoadingScreenPreview() {
    AppBackground {
        LoadingScreen()
    }
}

@Preview
@Composable
private fun SuccessScreenNoBaseUrlErrorPreview() {
    AppBackground {
        SuccessScreen(
            uiState = UiState.Success(
                AppSettings("goodBaseUrl", false),
                baseUrlFieldValue = "goodBaseUrl",
                baseUrlFieldError = null
            ),
            onBaseUrlUpdate = {},
            onBaseUrlApply = {},
            onBaseUrlRestoreDefault = {}
        )
    }
}

@Preview
@Composable
private fun SuccessScreenBaseUrlErrorPreview() {
    AppBackground {
        SuccessScreen(
            uiState = UiState.Success(
                AppSettings("badBaseUrl", false),
                baseUrlFieldValue = "badBaseUrl",
                baseUrlFieldError = R.string.base_url_invalid
            ),
            onBaseUrlUpdate = {},
            onBaseUrlApply = {},
            onBaseUrlRestoreDefault = {}
        )
    }
}
//endregion
