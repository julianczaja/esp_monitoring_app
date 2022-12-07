package com.julianczaja.esp_monitoring_app.presentation.device

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.julianczaja.esp_monitoring_app.R
import com.julianczaja.esp_monitoring_app.components.PhotoCoilImage
import com.julianczaja.esp_monitoring_app.domain.model.Photo
import com.julianczaja.esp_monitoring_app.presentation.theme.spacing
import com.julianczaja.esp_monitoring_app.toPrettyString

const val DEFAULT_PHOTO_HEIGHT = 150

@OptIn(ExperimentalLifecycleComposeApi::class)
@Composable
fun DeviceScreen(
    viewModel: DeviceScreenViewModel = hiltViewModel(),
) {
    val uiState by viewModel.deviceUiState.collectAsStateWithLifecycle()

    DeviceScreenContent(
        uiState = uiState,
        updatePhotos = viewModel::updatePhotos
    )
}

@Composable
fun DeviceScreenContent(
    uiState: DeviceScreenUiState,
    updatePhotos: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
    ) {
        when (uiState) {
            DeviceScreenUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }
            is DeviceScreenUiState.Success -> if (uiState.photos.isNotEmpty()) {
                PhotosGrid(uiState.photos)
            } else {
                Text(
                    text = "There are no photos taken by this device :(",
                    style = MaterialTheme.typography.labelMedium
                )
            }
            is DeviceScreenUiState.Error -> {
                Box(Modifier.fillMaxSize()) {
                    Text(
                        text = uiState.message ?: stringResource(id = R.string.unknown_error_message),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
        Button(
            onClick = updatePhotos,
            modifier = Modifier
                .height(IntrinsicSize.Min)
                .width(200.dp)
                .padding(MaterialTheme.spacing.medium)
        ) {
            Text(text = stringResource(R.string.update_photos_label))
        }
    }
}

@Composable
private fun ColumnScope.PhotosGrid(
    photos: List<Photo>,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(DEFAULT_PHOTO_HEIGHT.dp),
        contentPadding = PaddingValues(MaterialTheme.spacing.medium),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium, Alignment.Top),
        modifier = Modifier.weight(1f)
    ) {
        items(items = photos, key = { it.dateTime }) {
            Box {
                PhotoCoilImage(
                    url = it.url,
                    height = DEFAULT_PHOTO_HEIGHT.dp,
                    onClick = {
                        // TODO
                    }
                )
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.75f))
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                ) {
                    Text(
                        text = it.dateTime.toPrettyString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}

