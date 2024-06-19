package com.julianczaja.esp_monitoring_app.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.julianczaja.esp_monitoring_app.R
import com.julianczaja.esp_monitoring_app.presentation.theme.spacing


@Composable
fun SelectedEditBar(
    isSelectionMode: Boolean,
    selectedCount: Int,
    createTimelapseFromSelectedPhotos: () -> Unit,
    removeSelectedPhotos: () -> Unit,
    saveSelectedPhotos: (() -> Unit)? = null,
    resetSelections: () -> Unit
) {
    AnimatedVisibility(
        modifier = Modifier.zIndex(0f),
        visible = isSelectionMode,
        enter = slideInVertically(initialOffsetY = { -it/2 }),
        exit = slideOutVertically(targetOffsetY = { -it })
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceBright)
                    .padding(horizontal = MaterialTheme.spacing.medium),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = stringResource(R.string.selected_photos_count_format, selectedCount))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = createTimelapseFromSelectedPhotos) {
                        Icon(
                            painterResource(id = R.drawable.ic_timelapse),
                            contentDescription = null
                        )
                    }
                    IconButton(onClick = removeSelectedPhotos) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_delete),
                            contentDescription = null
                        )
                    }
                    saveSelectedPhotos?.let { action ->
                        IconButton(onClick = action) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_save_24),
                                contentDescription = null
                            )
                        }
                    }
                    IconButton(onClick = resetSelections) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_x),
                            contentDescription = null
                        )
                    }
                }
            }
            HorizontalDivider()
        }
    }
}

//region Preview
@PreviewLightDark
@Composable
private fun SelectedEditBarPreview() {
    AppBackground(Modifier.height(100.dp)) {
        SelectedEditBar(
            isSelectionMode = true,
            selectedCount = 2,
            createTimelapseFromSelectedPhotos = {},
            removeSelectedPhotos = {},
            saveSelectedPhotos = {},
            resetSelections = {}
        )
    }
}
//endregion
