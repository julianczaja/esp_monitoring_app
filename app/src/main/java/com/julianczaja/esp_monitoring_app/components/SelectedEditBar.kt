package com.julianczaja.esp_monitoring_app.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.julianczaja.esp_monitoring_app.R
import com.julianczaja.esp_monitoring_app.presentation.theme.spacing


@Composable
fun ColumnScope.SelectedEditBar(
    isSelectionMode: Boolean,
    removeSelectedPhotos: () -> Unit,
    saveSelectedPhotos: () -> Unit,
    resetSelections: () -> Unit
) {
    AnimatedVisibility(visible = isSelectionMode) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceBright)
                    .padding(horizontal = MaterialTheme.spacing.medium),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small, Alignment.End),
            ) {
                IconButton(onClick = removeSelectedPhotos) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null
                    )
                }
                IconButton(onClick = saveSelectedPhotos) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_baseline_save_24),
                        contentDescription = null
                    )
                }
                IconButton(onClick = resetSelections) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = null
                    )
                }
            }
            HorizontalDivider()
        }
    }
}

//region Preview
@Preview(heightDp = 100)
@Composable
private fun SelectedEditBarPreview() {
    AppBackground {
        Column {
            SelectedEditBar(
                isSelectionMode = true,
                removeSelectedPhotos = {},
                saveSelectedPhotos = {},
                resetSelections = {}
            )
        }
    }
}
//endregion