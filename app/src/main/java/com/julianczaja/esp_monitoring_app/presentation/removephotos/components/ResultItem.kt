package com.julianczaja.esp_monitoring_app.presentation.removephotos.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.julianczaja.esp_monitoring_app.R
import com.julianczaja.esp_monitoring_app.components.AppBackground
import com.julianczaja.esp_monitoring_app.domain.model.Photo
import com.julianczaja.esp_monitoring_app.presentation.theme.color_green
import com.julianczaja.esp_monitoring_app.presentation.theme.color_red
import com.julianczaja.esp_monitoring_app.presentation.theme.spacing


@Composable
fun RemovePhotoResultItem(
    modifier: Modifier = Modifier,
    photo: Photo,
    error: Int?
) {
    val context = LocalContext.current
    val text = remember {
        StringBuilder()
            .append(photo.fileName)
            .apply { if (photo.isSaved) append(" (${context.getString(R.string.saved_label)})") }
            .append(": ")
            .append(
                when (error) {
                    null -> context.getString(R.string.success_label)
                    else -> context.getString(error)
                }
            )
            .toString()
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (error) {
            null -> Icon(
                painter = painterResource(id = R.drawable.ic_check),
                tint = color_green,
                contentDescription = null
            )

            else -> Icon(
                painter = painterResource(id = R.drawable.ic_x),
                tint = color_red,
                contentDescription = null
            )

        }
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}

//region Preview
@Preview
@Composable
private fun SuccessPreview() {
    AppBackground {
        RemovePhotoResultItem(
            photo = Photo.mock(),
            error = null
        )
    }
}

@Preview
@Composable
private fun SavedPhotoSuccessPreview() {
    AppBackground {
        RemovePhotoResultItem(
            photo = Photo.mock(isSaved = true),
            error = null
        )
    }
}

@Preview
@Composable
private fun ErrorPreview() {
    AppBackground {
        RemovePhotoResultItem(
            photo = Photo.mock(),
            error = R.string.unknown_error_message
        )
    }
}

@Preview
@Composable
private fun ErrorLongFileNamePreview() {
    AppBackground(Modifier.width(250.dp)) {
        RemovePhotoResultItem(
            photo = Photo.mock(fileName = "looooooooooooooooooooooong file name"),
            error = R.string.unknown_error_message
        )
    }
}

@Preview
@Composable
private fun SavedPhotoErrorPreview() {
    AppBackground {
        RemovePhotoResultItem(
            photo = Photo.mock(isSaved = true),
            error = R.string.unknown_error_message
        )
    }
}
//endregion