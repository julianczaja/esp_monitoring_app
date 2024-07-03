package com.julianczaja.esp_monitoring_app.presentation.photopreview.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.core.net.toUri
import com.julianczaja.esp_monitoring_app.R
import com.julianczaja.esp_monitoring_app.domain.model.Photo
import com.julianczaja.esp_monitoring_app.presentation.theme.shape


@Composable
fun BoxScope.PhotoActionsRow(
    photo: Photo
) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .clip(RoundedCornerShape(bottomStart = MaterialTheme.shape.photoCorners))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = .75f)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (photo.isSaved) {
            IconButton(onClick = { sharePhoto(context, photo.url.toUri()) }) {
                Icon(painter = painterResource(id = R.drawable.ic_share), contentDescription = null)
            }
        }
        IconButton(onClick = { openPhotoInExternalGallery(context, photo.url.toUri()) }) {
            Icon(painter = painterResource(id = R.drawable.ic_open_external), contentDescription = null)
        }
    }
}

private fun openPhotoInExternalGallery(context: Context, uri: Uri) {
    val intent = Intent().apply {
        action = Intent.ACTION_VIEW
        setDataAndType(uri, "image/jpeg")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(intent)
}

private fun sharePhoto(context: Context, uri: Uri) {
    val intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_STREAM, uri)
        type = "image/jpeg"
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_image_label)))
}
