package com.julianczaja.esp_monitoring_app.presentation.devicetimelapses.components

import androidx.annotation.OptIn
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.julianczaja.esp_monitoring_app.domain.model.TimelapseData
import com.julianczaja.esp_monitoring_app.presentation.theme.spacing


@OptIn(UnstableApi::class)
@Composable
fun TimelapsePreviewDialog(
    modifier: Modifier = Modifier,
    timelapseData: TimelapseData,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .build()
            .apply {
                repeatMode = Player.REPEAT_MODE_ALL
                playWhenReady = true
            }
    }
    DisposableEffect(Unit) {
        exoPlayer.apply {
            setMediaItem(MediaItem.fromUri(timelapseData.path))
            prepare()
        }

        onDispose {
            exoPlayer.release()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier) {
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(exoPlayer.videoSize.pixelWidthHeightRatio)
                    .clip(RoundedCornerShape(MaterialTheme.spacing.medium)),
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = true
                        hideController()
                        setShowRewindButton(false)
                        setShowFastForwardButton(false)
                        setShowNextButton(false)
                        setShowPreviousButton(false)
                    }
                }
            )
        }
    }
}
