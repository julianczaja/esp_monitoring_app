package com.julianczaja.esp_monitoring_app.presentation.removephotos

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
import com.julianczaja.esp_monitoring_app.presentation.removephotos.RemovePhotosDialogViewModel.Event
import com.julianczaja.esp_monitoring_app.presentation.removephotos.RemovePhotosDialogViewModel.UiState
import com.julianczaja.esp_monitoring_app.presentation.theme.AppTheme
import com.julianczaja.esp_monitoring_app.presentation.theme.spacing


@Composable
fun RemovePhotosDialog(
    onDismiss: () -> Unit,
    viewModel: RemovePhotosDialogViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(true) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                Event.PHOTOS_REMOVED -> onDismiss()
            }
        }
    }

    RemovePhotosDialogContent(
        uiState = uiState,
        onDismiss = onDismiss,
        onRemoveClicked = viewModel::removePhotos,
        convertExplanationStringToStyledText = viewModel::convertExplanationStringToStyledText
    )
}


@Composable
private fun RemovePhotosDialogContent(
    uiState: UiState,
    onDismiss: () -> Unit,
    onRemoveClicked: () -> Unit,
    convertExplanationStringToStyledText: (String, SpanStyle) -> AnnotatedString,
) {
    DefaultDialog(onDismiss) {
        when (uiState) {
            is UiState.Error -> RemoveNewPhotoDialogContentError(uiState, onDismiss)
            is UiState.Success -> RemoveNewPhotoDialogContentSuccess(
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
private fun RemoveNewPhotoDialogContentSuccess(
    uiState: UiState.Success,
    onDismiss: () -> Unit,
    onRemoveClick: () -> Unit,
    convertExplanationStringToStyledText: (String, SpanStyle) -> AnnotatedString,
) {
    val titleText = when (uiState.photosFileNames.size) {
        1 -> stringResource(R.string.remove_photo_title)
        else -> stringResource(R.string.remove_photos_title)
    }
    val explanationText = when (uiState.photosFileNames.size) {
        1 -> stringResource(R.string.remove_photo_explanation, uiState.photosFileNames.first())
        else -> stringResource(R.string.remove_photos_explanation, uiState.photosFileNames.size)
    }
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
            text = titleText,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = MaterialTheme.spacing.large)
        )
        Text(
            text = convertExplanationStringToStyledText(
                explanationText, SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
            ),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = MaterialTheme.spacing.large)
        )
        DialogTwoButtons(
            onFirstButtonClicked = onDismiss,
            onSecondButtonClicked = onRemoveClick,
            modifier = Modifier.padding(top = MaterialTheme.spacing.veryLarge)
        )
    }
}

@Composable
private fun RemoveNewPhotoDialogContentError(
    uiState: UiState.Error,
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

//region Preview
@Preview
@Composable
fun RemovePhotosDialogContentSuccessPreviewMultiplePhotos() {
    AppTheme {
        val secondaryColor = MaterialTheme.colorScheme.secondary
        RemovePhotosDialogContent(
            onDismiss = {},
            uiState = UiState.Success(
                listOf(
                    "fileName1.jpg", "fileName2.jpg", "fileName3.jpg"
                )
            ),
            onRemoveClicked = {},
            convertExplanationStringToStyledText = { _, _ ->
                buildAnnotatedString {
                    append("This will remove 3 photos also from server. This action is not recoverable.")
                    addStyle(SpanStyle(fontWeight = FontWeight.Bold, color = secondaryColor), 17, 18)
                }
            }
        )
    }
}

@Preview
@Composable
fun RemovePhotosDialogContentSuccessPreviewSinglePhoto() {
    AppTheme {
        val secondaryColor = MaterialTheme.colorScheme.secondary
        RemovePhotosDialogContent(
            onDismiss = {},
            uiState = UiState.Success(
                listOf(
                    "fileName.jpg"
                )
            ),
            onRemoveClicked = {},
            convertExplanationStringToStyledText = { _, _ ->
                buildAnnotatedString {
                    append("This will remove photo \"fileName.jpg\" also from server. This action is not recoverable.")
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
        RemovePhotosDialogContent(
            onDismiss = {},
            uiState = UiState.Error(R.string.internal_app_error_message),
            onRemoveClicked = {},
            convertExplanationStringToStyledText = { _, _ -> AnnotatedString("") }
        )
    }
}

@Preview
@Composable
fun RemoveNewPhotoDialogContentLoadingPreview() {
    AppTheme {
        RemovePhotosDialogContent(
            onDismiss = {},
            uiState = UiState.Loading,
            onRemoveClicked = {},
            convertExplanationStringToStyledText = { _, _ -> AnnotatedString("") }
        )
    }
}
//endregion