package com.julianczaja.esp_monitoring_app.presentation.removephoto

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.julianczaja.esp_monitoring_app.R
import com.julianczaja.esp_monitoring_app.components.DefaultDialog
import com.julianczaja.esp_monitoring_app.components.DefaultProgressIndicator
import com.julianczaja.esp_monitoring_app.components.DialogOneButton
import com.julianczaja.esp_monitoring_app.components.DialogTwoButtons
import com.julianczaja.esp_monitoring_app.components.ErrorText
import com.julianczaja.esp_monitoring_app.domain.model.Photo
import com.julianczaja.esp_monitoring_app.presentation.theme.AppTheme
import com.julianczaja.esp_monitoring_app.presentation.theme.spacing
import java.time.LocalDateTime


@Composable
fun RemovePhotoDialog(
    onDismiss: () -> Unit,
    viewModel: RemovePhotoDialogViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(true) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                RemovePhotoDialogViewModel.Event.PHOTO_REMOVED -> onDismiss()
            }
        }
    }

    RemoveNewPhotoDialogContent(
        uiState = uiState,
        onDismiss = onDismiss,
        onRemoveClicked = viewModel::removePhoto,
        convertExplanationStringToStyledText = viewModel::convertExplanationStringToStyledText
    )
}


@Composable
private fun RemoveNewPhotoDialogContent(
    uiState: RemovePhotoScreenUiState,
    onDismiss: () -> Unit,
    onRemoveClicked: (Photo) -> Unit,
    convertExplanationStringToStyledText: (String, SpanStyle) -> AnnotatedString,
) {
    DefaultDialog(onDismiss) {
        when (uiState) {
            is RemovePhotoScreenUiState.Error -> RemoveNewPhotoDialogContentError(uiState, onDismiss)
            is RemovePhotoScreenUiState.Success -> RemoveNewPhotoDialogContentSuccess(
                uiState = uiState,
                onDismiss = onDismiss,
                onRemoveClick = onRemoveClicked,
                convertExplanationStringToStyledText = convertExplanationStringToStyledText
            )

            RemovePhotoScreenUiState.Loading -> DefaultProgressIndicator()
        }
    }
}

@Composable
private fun RemoveNewPhotoDialogContentSuccess(
    uiState: RemovePhotoScreenUiState.Success,
    onDismiss: () -> Unit,
    onRemoveClick: (Photo) -> Unit,
    convertExplanationStringToStyledText: (String, SpanStyle) -> AnnotatedString,
) {
    Column(
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
    ) {
        Icon(
            imageVector = Icons.Outlined.Delete,
            tint = MaterialTheme.colorScheme.secondary,
            contentDescription = null
        )
        Text(
            text = stringResource(R.string.remove_photo_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = MaterialTheme.spacing.large)
        )
        Text(
            text = convertExplanationStringToStyledText(
                stringResource(id = R.string.remove_photo_explanation, uiState.photo.fileName),
                SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
            ),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = MaterialTheme.spacing.large)
        )
        DialogTwoButtons(
            onFirstButtonClicked = onDismiss,
            onSecondButtonClicked = { onRemoveClick(uiState.photo) },
            modifier = Modifier.padding(top = MaterialTheme.spacing.veryLarge)
        )
    }
}

@Composable
private fun RemoveNewPhotoDialogContentError(
    uiState: RemovePhotoScreenUiState.Error,
    onDismiss: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.weight(1f)
        ) {
            ErrorText(text = stringResource(uiState.messageId))
        }
        DialogOneButton(
            onButtonClicked = onDismiss,
            modifier = Modifier.padding(top = MaterialTheme.spacing.veryLarge)
        )
    }
}

@Preview
@Composable
fun RemoveNewPhotoDialogContentSuccessPreview() {
    AppTheme {
        val secondaryColor = MaterialTheme.colorScheme.secondary
        RemoveNewPhotoDialogContent(
            onDismiss = {},
            uiState = RemovePhotoScreenUiState.Success(
                Photo(
                    12L,
                    LocalDateTime.now(),
                    "fileName.jpg",
                    "1600x1200",
                    "www.a.com"
                )
            ),
            onRemoveClicked = {},
            convertExplanationStringToStyledText = { _, _ ->
                buildAnnotatedString {
                    append("This will remove photo \"PhotoFileName\" also from server. This action is not recoverable.")
                    addStyle(SpanStyle(fontWeight = FontWeight.Bold, color = secondaryColor), 23, 38)
                }
            }
        )
    }
}

@Preview
@Composable
fun RemoveNewPhotoDialogContentErrorPreview() {
    AppTheme {
        RemoveNewPhotoDialogContent(
            onDismiss = {},
            uiState = RemovePhotoScreenUiState.Error(R.string.internal_app_error_message),
            onRemoveClicked = {},
            convertExplanationStringToStyledText = { _, _ -> AnnotatedString("") }
        )
    }
}

@Preview
@Composable
fun RemoveNewPhotoDialogContentLoadingPreview() {
    AppTheme {
        RemoveNewPhotoDialogContent(
            onDismiss = {},
            uiState = RemovePhotoScreenUiState.Loading,
            onRemoveClicked = {},
            convertExplanationStringToStyledText = { _, _ -> AnnotatedString("") }
        )
    }
}
