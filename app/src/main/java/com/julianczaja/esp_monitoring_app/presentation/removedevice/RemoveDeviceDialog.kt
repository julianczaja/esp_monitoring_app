package com.julianczaja.esp_monitoring_app.presentation.removedevice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.julianczaja.esp_monitoring_app.R
import com.julianczaja.esp_monitoring_app.components.AppBackground
import com.julianczaja.esp_monitoring_app.components.DefaultDialog
import com.julianczaja.esp_monitoring_app.components.DefaultProgressIndicator
import com.julianczaja.esp_monitoring_app.components.DialogOneButton
import com.julianczaja.esp_monitoring_app.components.DialogTwoButtons
import com.julianczaja.esp_monitoring_app.components.ErrorText
import com.julianczaja.esp_monitoring_app.domain.model.Device
import com.julianczaja.esp_monitoring_app.presentation.removedevice.RemoveDeviceDialogViewModel.Event
import com.julianczaja.esp_monitoring_app.presentation.removedevice.RemoveDeviceDialogViewModel.UiState
import com.julianczaja.esp_monitoring_app.presentation.theme.spacing


@Composable
fun RemoveDeviceDialog(
    onDismiss: () -> Unit,
    viewModel: RemoveDeviceDialogViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var viewModelInitiated by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(true) {
        if (!viewModelInitiated) {
            viewModel.init()
            viewModelInitiated = true
        }
    }

    LaunchedEffect(true) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                Event.DEVICE_REMOVED -> onDismiss()
            }
        }
    }

    RemoveNewDeviceDialogContent(
        uiState = uiState,
        onDismiss = onDismiss,
        onRemoveClicked = viewModel::removeDevice,
        convertExplanationStringToStyledText = viewModel::convertExplanationStringToStyledText
    )
}


@Composable
private fun RemoveNewDeviceDialogContent(
    uiState: UiState,
    onDismiss: () -> Unit,
    onRemoveClicked: (Device) -> Unit,
    convertExplanationStringToStyledText: (String, SpanStyle) -> AnnotatedString,
) {
    DefaultDialog(onDismiss) {
        when (uiState) {
            is UiState.Error -> RemoveNewDeviceDialogContentError(uiState, onDismiss)
            is UiState.Success -> RemoveNewDeviceDialogContentSuccess(
                uiState = uiState,
                onDismiss = onDismiss,
                onRemoveClick = onRemoveClicked,
                convertExplanationStringToStyledText = convertExplanationStringToStyledText
            )
            UiState.Loading -> DefaultProgressIndicator()
        }
    }
}

@Composable
private fun RemoveNewDeviceDialogContentSuccess(
    uiState: UiState.Success,
    onDismiss: () -> Unit,
    onRemoveClick: (Device) -> Unit,
    convertExplanationStringToStyledText: (String, SpanStyle) -> AnnotatedString,
) {
    Column(
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_delete),
            tint = MaterialTheme.colorScheme.secondary,
            contentDescription = null
        )
        Text(
            text = stringResource(R.string.remove_device_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = MaterialTheme.spacing.large)
        )
        Text(
            text = convertExplanationStringToStyledText(
                stringResource(id = R.string.remove_device_explanation, uiState.device.name, uiState.device.id),
                SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
            ),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = MaterialTheme.spacing.large)
        )
        DialogTwoButtons(
            onFirstButtonClicked = onDismiss,
            onSecondButtonClicked = { onRemoveClick(uiState.device) },
            modifier = Modifier.padding(top = MaterialTheme.spacing.veryLarge)
        )
    }
}

@Composable
private fun RemoveNewDeviceDialogContentError(
    uiState: UiState.Error,
    onDismiss: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
    ) {
        ErrorText(text = stringResource(uiState.messageId))
        DialogOneButton(
            onButtonClicked = onDismiss,
            modifier = Modifier.padding(top = MaterialTheme.spacing.veryLarge)
        )
    }
}

@PreviewLightDark
@Composable
fun RemoveNewDeviceDialogContentSuccessPreview() {
    AppBackground {
        val secondaryColor = MaterialTheme.colorScheme.secondary
        RemoveNewDeviceDialogContent(
            onDismiss = {},
            uiState = UiState.Success(Device(12L, "Device name")),
            onRemoveClicked = {},
            convertExplanationStringToStyledText = { _, _ ->
                buildAnnotatedString {
                    append("This will remove device \"Device name\" (id: 12) and all its photos from phone memory. Photos still will be stored on server.")
                    addStyle(SpanStyle(fontWeight = FontWeight.Bold, color = secondaryColor), 25, 37)
                }
            }
        )
    }
}

@Preview
@Composable
fun RemoveNewDeviceDialogContentErrorPreview() {
    AppBackground {
        RemoveNewDeviceDialogContent(
            onDismiss = {},
            uiState = UiState.Error(R.string.internal_app_error_message),
            onRemoveClicked = {},
            convertExplanationStringToStyledText = { _, _ -> AnnotatedString("") }
        )
    }
}

@Preview
@Composable
fun RemoveNewDeviceDialogContentLoadingPreview() {
    AppBackground {
        RemoveNewDeviceDialogContent(
            onDismiss = {},
            uiState = UiState.Loading,
            onRemoveClicked = {},
            convertExplanationStringToStyledText = { _, _ -> AnnotatedString("") }
        )
    }
}
