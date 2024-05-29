package com.julianczaja.esp_monitoring_app.presentation.photopreview

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.ScaleFactor
import androidx.compose.ui.layout.lerp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.constraintlayout.compose.ChainStyle
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Scale
import com.julianczaja.esp_monitoring_app.R
import com.julianczaja.esp_monitoring_app.components.DefaultProgressIndicator
import com.julianczaja.esp_monitoring_app.components.ErrorText
import com.julianczaja.esp_monitoring_app.data.utils.toPrettyString
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val columnModifier = when (orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> Modifier
                .fillMaxWidth(.7f)
                .fillMaxHeight(.9f)

            else -> Modifier
                .fillMaxWidth(.9f)
                .fillMaxHeight(.7f)
        }

        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = columnModifier.clip(RoundedCornerShape(MaterialTheme.shape.dialogCorners))
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
    val context = LocalContext.current
    val pagerState = rememberPagerState(
        pageCount = { uiState.photos.size },
        initialPage = uiState.initialPhotoIndex
    )
    HorizontalPager(
        state = pagerState,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) { page ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = MaterialTheme.spacing.large)
                .graphicsLayer {
                    val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction

                    lerp(
                        start = ScaleFactor(0.65f, 0.65f),
                        stop = ScaleFactor(1f, 1f),
                        fraction = 1f - pageOffset.coerceIn(0f, 1f)
                    ).also { scale ->
                        scaleX = scale.scaleX
                        scaleY = scale.scaleY
                    }

                    lerp(
                        start = ScaleFactor(0f, 0f),
                        stop = ScaleFactor(1f, 1f),
                        fraction = 1f - pageOffset.coerceIn(0f, 1f)
                    ).also {
                        alpha = it.scaleX
                    }
                }
        ) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(uiState.photos[page].url)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.DISABLED)
                    .scale(Scale.FILL)
                    .crossfade(true)
                    .build(),
                contentScale = ContentScale.FillWidth,
                contentDescription = null,
                loading = {
                    Box(Modifier.fillMaxSize()) {
                        DefaultProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                },
                success = {
                    ConstraintLayout(
                        modifier = Modifier
                            .padding(MaterialTheme.spacing.medium)
                            .fillMaxSize()
                    ) {
                        val (image, imageInfoSize, imageInfoDate) = createRefs()

                        Image(
                            painter = it.painter,
                            contentDescription = null,
                            modifier = Modifier
                                .constrainAs(image) {
                                    top.linkTo(parent.top)
                                    start.linkTo(parent.start)
                                    end.linkTo(parent.end)
                                    height = Dimension.fillToConstraints
                                    width = Dimension.fillToConstraints
                                }
                        )
                        Text(
                            text = stringResource(
                                R.string.photo_preview_size_label,
                                uiState.photos[page].size
                            ),
                            modifier = Modifier
                                .padding(top = MaterialTheme.spacing.medium)
                                .constrainAs(imageInfoSize) {
                                    top.linkTo(image.bottom)
                                    bottom.linkTo(imageInfoDate.top)
                                    start.linkTo(parent.start)
                                    end.linkTo(parent.end)
                                }
                        )
                        Text(
                            text = stringResource(
                                R.string.photo_preview_date_label,
                                uiState.photos[page].dateTime.toPrettyString()
                            ),
                            modifier = Modifier
                                .constrainAs(imageInfoDate) {
                                    bottom.linkTo(parent.bottom)
                                    start.linkTo(parent.start)
                                    end.linkTo(parent.end)
                                }
                        )
                        createVerticalChain(
                            image,
                            imageInfoSize,
                            imageInfoDate,
                            chainStyle = ChainStyle.Packed
                        )
                    }
                },
            )
        }
    }
}
