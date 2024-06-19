package com.julianczaja.esp_monitoring_app.presentation.photopreview

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.julianczaja.esp_monitoring_app.components.DefaultProgressIndicator
import com.julianczaja.esp_monitoring_app.components.ErrorText
import com.julianczaja.esp_monitoring_app.data.utils.toPrettyString
import com.julianczaja.esp_monitoring_app.domain.model.Photo
import com.julianczaja.esp_monitoring_app.presentation.photopreview.PhotoPreviewDialogViewModel.UiState
import com.julianczaja.esp_monitoring_app.presentation.theme.shape
import com.julianczaja.esp_monitoring_app.presentation.theme.spacing

@Composable
fun PhotoPreviewDialog(
    onDismiss: () -> Unit,
    viewModel: PhotoPreviewDialogViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    PhotoPreviewDialogContent(
        uiState = uiState,
        onDismiss = onDismiss,
    )
}

@Composable
fun PhotoPreviewDialogContent(
    uiState: UiState,
    onDismiss: () -> Unit,
) {
    val orientation = LocalConfiguration.current.orientation
    val columnModifier = remember(orientation) {
        when (orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> Modifier
                .fillMaxWidth(.7f)
                .fillMaxHeight(.9f)

            else -> Modifier
                .fillMaxWidth(.9f)
                .fillMaxHeight(.7f)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Card(
            modifier = columnModifier,
            shape = RoundedCornerShape(MaterialTheme.shape.dialogCorners)
        ) {
            when (uiState) {
                is UiState.Success -> PhotoPreview(uiState)
                is UiState.Loading -> Box(modifier = Modifier.fillMaxSize()) {
                    DefaultProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                is UiState.Error -> Box(modifier = Modifier.fillMaxSize()) {
                    ErrorText(text = stringResource(uiState.messageId), modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }
}

@Composable
fun PhotoPreview(
    uiState: UiState.Success,
) {
    val pagerState = rememberPagerState(
        pageCount = { uiState.photos.size },
        initialPage = uiState.initialPhotoIndex
    )

    HorizontalPager(
        modifier = Modifier.fillMaxSize(),
        state = pagerState
    ) { page ->
        ZoomableCoilPhoto(
            modifier = Modifier.fillMaxSize(),
            photo = uiState.photos[page]
        )
    }
}

@Composable
private fun ZoomableCoilPhoto(
    modifier: Modifier = Modifier,
    photo: Photo
) {
    val context = LocalContext.current
    var zoom by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val isZoomed = remember {
        derivedStateOf { zoom > 1f }
    }

    SubcomposeAsyncImage(
        modifier = modifier
            .padding(MaterialTheme.spacing.large)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    do {
                        val event = awaitPointerEvent()

                        val oldZoom = zoom
                        val centroid = event.calculateCentroid()
                        val pan = event.calculatePan()

                        zoom = (zoom * event.calculateZoom()).coerceIn(1f, 5f)

                        if (isZoomed.value) {
                            val newOffset = (offset + centroid / oldZoom) - (centroid / zoom + pan / oldZoom)
                            if (newOffset != Offset.Unspecified) offset = newOffset
                        } else {
                            offset = Offset.Zero
                        }

                        if (isZoomed.value) {
                            event.changes.forEach { pointerInputChange: PointerInputChange ->
                                pointerInputChange.consume()
                            }
                        }

                    } while (event.changes.any { it.pressed })
                }
            }
            .graphicsLayer(
                translationX = -offset.x * zoom,
                translationY = -offset.y * zoom,
                scaleX = zoom,
                scaleY = zoom,
                transformOrigin = TransformOrigin(0f, 0f)
            ),
        model = ImageRequest.Builder(context)
            .data(photo.url)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.DISABLED)
            .build(),
        contentDescription = null,
        loading = {
            Box(modifier) {
                DefaultProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        },
        success = {
            Box(
                modifier = Modifier
                    .wrapContentSize()
                    .clip(RoundedCornerShape(MaterialTheme.shape.photoCorners))
            ) {
                Image(
                    modifier = Modifier.align(Alignment.Center),
                    painter = it.painter,
                    contentDescription = null
                )
                if (!isZoomed.value) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .clip(
                                RoundedCornerShape(
                                    topEnd = MaterialTheme.shape.photoCorners,
                                    topStart = MaterialTheme.shape.photoCorners
                                )
                            )
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = .75f))
                            .padding(MaterialTheme.spacing.medium),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = photo.dateTime.toPrettyString(),
                            style = MaterialTheme.typography.bodySmall
                        )
                        VerticalDivider(
                            modifier = Modifier
                                .height(16.dp)
                                .padding(horizontal = MaterialTheme.spacing.medium)
                        )
                        Text(
                            text = photo.size,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    )
}
