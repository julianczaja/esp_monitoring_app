package com.julianczaja.esp_monitoring_app.presentation.savephotos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.julianczaja.esp_monitoring_app.R
import com.julianczaja.esp_monitoring_app.components.AppBackground
import com.julianczaja.esp_monitoring_app.components.DefaultDialog
import com.julianczaja.esp_monitoring_app.components.DialogOneButton
import com.julianczaja.esp_monitoring_app.components.PhotoResultItem
import com.julianczaja.esp_monitoring_app.domain.model.Photo
import com.julianczaja.esp_monitoring_app.presentation.savephotos.SavePhotosDialogViewModel.Event
import com.julianczaja.esp_monitoring_app.presentation.savephotos.SavePhotosDialogViewModel.UiState
import com.julianczaja.esp_monitoring_app.presentation.theme.spacing


@Composable
fun SavePhotosDialog(
    onDismiss: () -> Unit,
    viewModel: SavePhotosDialogViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var viewModelInitiated by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(true) {
        if (!viewModelInitiated) {
            viewModel.save()
            viewModelInitiated = true
        }
        viewModel.eventFlow.collect { event ->
            when (event) {
                Event.PHOTOS_SAVED -> onDismiss()
            }
        }
    }

    SavePhotosDialogContent(
        modifier = Modifier.fillMaxSize(),
        uiState = uiState,
        onDismiss = onDismiss,
        onCancelClicked = viewModel::cancelSaving,
    )
}

@Composable
private fun SavePhotosDialogContent(
    modifier: Modifier = Modifier,
    uiState: UiState,
    onDismiss: () -> Unit,
    onCancelClicked: () -> Unit,
) {
    DefaultDialog(onDismiss) {
        when (uiState) {
            is UiState.Saving -> ProgressScreen(
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
private fun ProgressScreen(
    modifier: Modifier = Modifier,
    uiState: UiState.Saving,
    onCancelClicked: () -> Unit
) {
    Box(modifier) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = stringResource(R.string.saving_photos_label))
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
            painter = painterResource(id = R.drawable.ic_save_24),
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
                    PhotoResultItem(photo = photo, error = error)
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

//region Preview
@PreviewLightDark
@Composable
fun SavingScreenPreview() {
    AppBackground {
        SavePhotosDialogContent(
            modifier = Modifier.fillMaxSize(),
            onDismiss = {},
            uiState = UiState.Saving(progress = .4f),
            onCancelClicked = {}
        )
    }
}

@PreviewLightDark
@Composable
fun ErrorScreenPreview() {
    AppBackground {
        SavePhotosDialogContent(
            modifier = Modifier.fillMaxSize(),
            onDismiss = {},
            uiState = UiState.Error(
                mapOf(
                    Photo.mock(fileName = "1.jpeg") to null,
                    Photo.mock(fileName = "2.jpeg") to R.string.already_saved_label,
                    Photo.mock(fileName = "3.jpeg") to null,
                    Photo.mock(fileName = "4.jpeg") to R.string.unknown_error_message,
                )
            ),
            onCancelClicked = {}
        )
    }
}
//endregion