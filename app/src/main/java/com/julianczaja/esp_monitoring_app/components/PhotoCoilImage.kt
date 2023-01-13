package com.julianczaja.esp_monitoring_app.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.julianczaja.esp_monitoring_app.presentation.theme.spacing


@Composable
fun PhotoCoilImage(
    modifier: Modifier = Modifier,
    url: String,
    height: Dp,
    cornerSize: Dp = MaterialTheme.spacing.medium,
    onClick: () -> Unit,
) {
    val context = LocalContext.current

    SubcomposeAsyncImage(
        model = ImageRequest.Builder(context)
            .data(url)
            .crossfade(true)
            .build(),
        contentScale = ContentScale.Fit,
        contentDescription = null,
        loading = {
            Box(Modifier.fillMaxSize()) {
                DefaultProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        },
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(cornerSize))
            .clickable(onClick = onClick)
    )
}