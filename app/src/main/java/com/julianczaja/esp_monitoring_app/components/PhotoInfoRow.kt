package com.julianczaja.esp_monitoring_app.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.julianczaja.esp_monitoring_app.presentation.theme.shape
import com.julianczaja.esp_monitoring_app.presentation.theme.spacing


@Composable
fun BoxScope.PhotoInfoRow(infoStrings: List<String>) {
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
            .padding(horizontal = MaterialTheme.spacing.medium, vertical = MaterialTheme.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        infoStrings.forEachIndexed { index, infoString ->
            Text(
                text = infoString,
                style = MaterialTheme.typography.bodySmall
            )
            if (index < infoStrings.size - 1) {
                VerticalDivider(
                    modifier = Modifier
                        .height(16.dp)
                        .padding(horizontal = MaterialTheme.spacing.medium)
                )
            }
        }
    }
}
