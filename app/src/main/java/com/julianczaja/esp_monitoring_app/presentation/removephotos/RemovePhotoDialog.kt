package com.julianczaja.esp_monitoring_app.presentation.removephotos

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.julianczaja.esp_monitoring_app.R
import com.julianczaja.esp_monitoring_app.components.AppBackground
import com.julianczaja.esp_monitoring_app.components.DefaultDialog
import com.julianczaja.esp_monitoring_app.components.DialogOneButton
import com.julianczaja.esp_monitoring_app.components.DialogTwoButtons
import com.julianczaja.esp_monitoring_app.domain.model.Photo
import com.julianczaja.esp_monitoring_app.presentation.removephotos.RemovePhotosDialogViewModel.Event
import com.julianczaja.esp_monitoring_app.presentation.removephotos.RemovePhotosDialogViewModel.UiState
import com.julianczaja.esp_monitoring_app.presentation.removephotos.components.RemovePhotoResultItem
import com.julianczaja.esp_monitoring_app.presentation.theme.spacing
import kotlin.collections.component1
import kotlin.collections.component2


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
        modifier = Modifier.fillMaxSize(),
        uiState = uiState,
        onDismiss = onDismiss,
        onRemoveSavedChanged = viewModel::onRemoveSavedChanged,
        onRemoveClicked = viewModel::removePhotos,
        onCancelClicked = viewModel::cancelRemoval,
    )
}

@Composable
private fun RemovePhotosDialogContent(
    modifier: Modifier = Modifier,
    uiState: UiState,
    onDismiss: () -> Unit,
    onRemoveSavedChanged: (Boolean) -> Unit,
    onRemoveClicked: () -> Unit,
    onCancelClicked: () -> Unit,
) {
    DefaultDialog(onDismiss) {
        when (uiState) {
            is UiState.Confirm -> ConfirmScreen(
                modifier = modifier,
                uiState = uiState,
                onDismiss = onDismiss,
                onRemoveSavedChanged = onRemoveSavedChanged,
                onRemoveClick = onRemoveClicked,
            )

            is UiState.Removing -> ProgressScreen(
                modifier = modifier,
                uiState = uiState,
                onCancelClicked = onCancelClicked
            )

            is UiState.Error -> ErrorScreen(
                modifier = modifier,
                uiState = uiState,
                onDismiss = onDismiss
            )
        }
    }
}

@Composable
private fun ConfirmScreen(
    modifier: Modifier = Modifier,
    uiState: UiState.Confirm,
    onDismiss: () -> Unit,
    onRemoveSavedChanged: (Boolean) -> Unit,
    onRemoveClick: () -> Unit,
) {
    val context = LocalContext.current
    val spanStyle = SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)

    val uniquePhotosCount = remember { uiState.photos.distinctBy { it.fileName }.size }
    val titleText = remember {
        when (uniquePhotosCount) {
            1 -> context.getString(R.string.remove_photo_title)
            else -> context.getString(R.string.remove_photos_title)
        }
    }

    val savedPhotosCount = remember { uiState.photos.count { it.isSaved } }

    val explanationText = remember {
        when (uiState.photos.size) {
            1 -> with(uiState.photos.first()) {
                when (isSaved) {
                    true -> context.getString(R.string.remove_saved_photo_explanation, fileName)
                    false -> context.getString(R.string.remove_photo_explanation, fileName)
                }
            }

            else -> when {
                uiState.photos.size == savedPhotosCount -> context.getString(
                    R.string.remove_saved_photos_explanation,
                    savedPhotosCount
                )

                else -> context.getString(
                    R.string.remove_photos_explanation,
                    uiState.photos.count { !it.isSaved }
                )
            }
        }
    }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_delete),
            tint = MaterialTheme.colorScheme.secondary,
            contentDescription = null
        )
        Text(
            text = titleText,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = MaterialTheme.spacing.large)
        )
        Text(
            text = convertExplanationStringToStyledText(explanationText, spanStyle),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = MaterialTheme.spacing.large)
        )
        if (uiState.shouldShowRemoveSaved) {
            val removeAlsoSavedText = when (savedPhotosCount) {
                1 -> stringResource(R.string.remove_photo_saved_locally_label)
                else -> stringResource(R.string.remove_photos_saved_locally_label, savedPhotosCount)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = MaterialTheme.spacing.large),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = uiState.removeSaved, onCheckedChange = onRemoveSavedChanged)
                Text(
                    modifier = Modifier.clickable { onRemoveSavedChanged(!uiState.removeSaved) },
                    text = convertExplanationStringToStyledText(removeAlsoSavedText, spanStyle),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        DialogTwoButtons(
            modifier = Modifier.padding(top = MaterialTheme.spacing.veryLarge),
            onFirstButtonClicked = onDismiss,
            onSecondButtonClicked = onRemoveClick
        )
    }
}

@Composable
private fun ProgressScreen(
    modifier: Modifier = Modifier,
    uiState: UiState.Removing,
    onCancelClicked: () -> Unit
) {
    Box(modifier) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = stringResource(R.string.removing_photos_label))
            LinearProgressIndicator(
                modifier = Modifier
                    .padding(MaterialTheme.spacing.medium)
                    .height(MaterialTheme.spacing.medium)
                    .fillMaxWidth(),
                progress = { uiState.progress },
            )
        }
        TextButton(
            modifier = Modifier.align(Alignment.BottomEnd),
            onClick = onCancelClicked
        ) {
            Text(text = stringResource(id = R.string.cancel_label))
        }
    }
}

@Composable
private fun ErrorScreen(
    modifier: Modifier = Modifier,
    uiState: UiState.Error,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_delete),
            tint = MaterialTheme.colorScheme.secondary,
            contentDescription = null
        )
        Text(
            text = stringResource(R.string.results_label),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = MaterialTheme.spacing.large)
        )

        HorizontalDivider(
            modifier = Modifier.padding(top = MaterialTheme.spacing.large)
        )
        LazyColumn(
            modifier = Modifier.height(200.dp),
            contentPadding = PaddingValues(MaterialTheme.spacing.small),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            uiState.results.forEach { (photo, error) ->
                item {
                    RemovePhotoResultItem(photo = photo, error = error)
                }
            }
        }
        HorizontalDivider()

        DialogOneButton(
            modifier = Modifier.padding(top = MaterialTheme.spacing.large),
            onButtonClicked = onDismiss,
            buttonLabel = R.string.ok_label
        )
    }
}

private fun convertExplanationStringToStyledText(explanationText: String, spanStyle: SpanStyle): AnnotatedString {
    return buildAnnotatedString {
        val from = explanationText.indexOf("*")
        val to = explanationText.indexOf("*", startIndex = from + 1)
        append(explanationText.replace("*", ""))
        addStyle(spanStyle, start = from, end = to)
    }
}


//region Preview
@PreviewLightDark
@Composable
fun ConfirmScreenSinglePhotoPreview() {
    AppBackground {
        RemovePhotosDialogContent(
            modifier = Modifier.fillMaxSize(),
            onDismiss = {},
            uiState = UiState.Confirm(
                photos = listOf(Photo.mock()),
                shouldShowRemoveSaved = false,
                removeSaved = false
            ),
            onRemoveSavedChanged = {},
            onRemoveClicked = {},
            onCancelClicked = {}
        )
    }
}

@Preview
@Composable
fun ConfirmScreenMultipleSavedPhotosPreview() {
    AppBackground {
        RemovePhotosDialogContent(
            modifier = Modifier.fillMaxSize(),
            onDismiss = {},
            uiState = UiState.Confirm(
                listOf(
                    Photo.mock(fileName = "1.jpg", isSaved = true),
                    Photo.mock(fileName = "2.jpg", isSaved = true),
                    Photo.mock(fileName = "3.jpg", isSaved = true),
                ),
                shouldShowRemoveSaved = false,
                removeSaved = true
            ),
            onRemoveSavedChanged = {},
            onRemoveClicked = {},
            onCancelClicked = {}
        )
    }
}

@Preview
@Composable
fun ConfirmScreenSingleSavedAndNotSavedPhotoPreview() {
    AppBackground {
        RemovePhotosDialogContent(
            modifier = Modifier.fillMaxSize(),
            onDismiss = {},
            uiState = UiState.Confirm(
                listOf(
                    Photo.mock(fileName = "1.jpg", isSaved = false),
                    Photo.mock(fileName = "2.jpg", isSaved = true),
                ),
                shouldShowRemoveSaved = true,
                removeSaved = true
            ),
            onRemoveSavedChanged = {},
            onRemoveClicked = {},
            onCancelClicked = {}
        )
    }
}

@Preview
@Composable
fun ConfirmScreenMultipleNotSavedPhotosPreview() {
    AppBackground {
        RemovePhotosDialogContent(
            modifier = Modifier.fillMaxSize(),
            onDismiss = {},
            uiState = UiState.Confirm(
                listOf(
                    Photo.mock(fileName = "1.jpg", isSaved = false),
                    Photo.mock(fileName = "2.jpg", isSaved = false),
                    Photo.mock(fileName = "3.jpg", isSaved = false),
                ),
                shouldShowRemoveSaved = false,
                removeSaved = false
            ),
            onRemoveSavedChanged = {},
            onRemoveClicked = {},
            onCancelClicked = {}
        )
    }
}

@Preview
@Composable
fun ConfirmScreenMultipleSavedAndNotSavedPhotosPreview() {
    AppBackground {
        RemovePhotosDialogContent(
            modifier = Modifier.fillMaxSize(),
            onDismiss = {},
            uiState = UiState.Confirm(
                listOf(
                    Photo.mock(fileName = "1.jpg", isSaved = false),
                    Photo.mock(fileName = "1.jpg", isSaved = true),
                    Photo.mock(fileName = "2.jpg", isSaved = false),
                    Photo.mock(fileName = "3.jpg", isSaved = false),
                    Photo.mock(fileName = "4.jpg", isSaved = true),
                ),
                shouldShowRemoveSaved = true,
                removeSaved = false
            ),
            onRemoveSavedChanged = {},
            onRemoveClicked = {},
            onCancelClicked = {}
        )
    }
}

@PreviewLightDark
@Composable
fun ErrorScreenPreview() {
    AppBackground {
        RemovePhotosDialogContent(
            modifier = Modifier.fillMaxSize(),
            onDismiss = {},
            uiState = UiState.Error(
                mapOf(
                    Photo.mock(fileName = "1.jpeg", isSaved = false) to null,
                    Photo.mock(fileName = "1.jpeg", isSaved = true) to null,
                    Photo.mock(fileName = "2.jpeg", isSaved = false) to R.string.internal_app_error_message,
                    Photo.mock(fileName = "3.jpeg", isSaved = false) to null,
                    Photo.mock(fileName = "4.jpeg", isSaved = true) to R.string.remove_photo_security_error_message,
                    Photo.mock(fileName = "5.jpeg", isSaved = false) to R.string.internal_app_error_message,
                )
            ),
            onRemoveSavedChanged = {},
            onRemoveClicked = {},
            onCancelClicked = {}
        )
    }
}

@Preview
@Composable
fun ProgressScreenPreview() {
    AppBackground {
        RemovePhotosDialogContent(
            modifier = Modifier.fillMaxSize(),
            onDismiss = {},
            uiState = UiState.Removing(progress = .4f),
            onRemoveSavedChanged = {},
            onRemoveClicked = {},
            onCancelClicked = {},
        )
    }
}
//endregion