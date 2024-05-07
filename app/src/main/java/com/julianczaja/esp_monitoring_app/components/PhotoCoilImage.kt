package com.julianczaja.esp_monitoring_app.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest


@Composable
fun PhotoCoilImage(
    modifier: Modifier = Modifier,
    url: String,
    height: Dp,
) {
    val context = LocalContext.current

    SubcomposeAsyncImage(
        modifier = modifier.height(height),
        contentScale = ContentScale.Fit,
        contentDescription = null,
        model = ImageRequest.Builder(context)
            .data(url)
            .crossfade(true)
            .build(),
        loading = {
            Box(Modifier.fillMaxSize()) {
                DefaultProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        },
    )
}
