package com.julianczaja.esp_monitoring_app.presentation.photopreview

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.ScaleFactor
import androidx.compose.ui.layout.lerp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.constraintlayout.compose.ChainStyle
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import coil.size.Scale
import coil.size.Size
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.calculateCurrentOffsetForPage
import com.google.accompanist.pager.rememberPagerState
import com.julianczaja.esp_monitoring_app.components.DefaultProgressIndicator
import com.julianczaja.esp_monitoring_app.presentation.theme.shape
import com.julianczaja.esp_monitoring_app.toPrettyString
import kotlin.math.absoluteValue


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

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PhotoPreviewDialogContent(
    uiState: PhotoPreviewUiState,
    onDismiss: () -> Unit,
) {
    val orientation = LocalConfiguration.current.orientation

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val columnModifier = if (orientation == Configuration.ORIENTATION_LANDSCAPE)
            Modifier
                .fillMaxWidth(.7f)
                .fillMaxHeight(.9f)
        else
            Modifier
                .fillMaxWidth(.9f)
                .fillMaxHeight(.7f)
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = columnModifier.clip(RoundedCornerShape(MaterialTheme.shape.dialogCorners))
        ) {
            when (uiState) {
                PhotoPreviewUiState.Loading -> {

                }

                is PhotoPreviewUiState.Error -> {

                }

                is PhotoPreviewUiState.Success -> PhotoPreview(uiState)
            }
        }
    }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun ColumnScope.PhotoPreview(
    uiState: PhotoPreviewUiState.Success,
) {
    val pagerState = rememberPagerState(initialPage = uiState.initialPhotoIndex)
    val context = LocalContext.current

    HorizontalPager(
        count = uiState.photos.size,
        state = pagerState,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) { page ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 16.dp)
                .graphicsLayer {
                    val pageOffset = calculateCurrentOffsetForPage(page).absoluteValue

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
                modifier = Modifier.fillMaxSize(),
                model = ImageRequest.Builder(context)
                    .data(uiState.photos[page].url)
                    .size(Size.ORIGINAL)
                    .scale(Scale.FIT)
                    .crossfade(true)
                    .build(),
                contentScale = ContentScale.Fit,
                contentDescription = null,
                loading = {
                    Box(Modifier.fillMaxSize()) {
                        DefaultProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                },
                success = {
                    val painter = painter

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        ConstraintLayout(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxSize()
                        ) {
                            val (image, imageInfoSize, imageInfoDate) = createRefs()

                            Image(
                                painter = painter,
                                contentDescription = null,
                                modifier = Modifier
                                    .constrainAs(image) {
                                        top.linkTo(parent.top)
                                        start.linkTo(parent.start)
                                        end.linkTo(parent.end)
                                        height = Dimension.preferredWrapContent
                                        width = Dimension.fillToConstraints
                                    }
                            )
                            Text(
                                text = "Size: ${uiState.photos[page].size}",
                                modifier = Modifier
                                    .padding(top = 8.dp)
                                    .constrainAs(imageInfoSize) {
                                        top.linkTo(image.bottom)
                                        bottom.linkTo(imageInfoDate.top)
                                        start.linkTo(parent.start)
                                        end.linkTo(parent.end)
                                    }
                            )
                            Text(
                                text = "Date: ${uiState.photos[page].dateTime.toPrettyString()}",
                                modifier = Modifier
                                    .constrainAs(imageInfoDate) {
                                        bottom.linkTo(parent.bottom)
                                        start.linkTo(parent.start)
                                        end.linkTo(parent.end)
                                    }
                            )

                            createVerticalChain(image, imageInfoSize, imageInfoDate, chainStyle = ChainStyle.Packed)
                        }
                    }
                },
            )
        }
    }
}
