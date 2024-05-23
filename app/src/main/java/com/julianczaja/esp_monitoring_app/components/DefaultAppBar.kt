package com.julianczaja.esp_monitoring_app.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.julianczaja.esp_monitoring_app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DefaultAppBar(
    shouldShowNavigationIcon: Boolean,
    onBackClick: () -> Unit
) {
    CenterAlignedTopAppBar(
        title = {
            Text(text = stringResource(id = R.string.app_name))
        },
        navigationIcon = {
            if (shouldShowNavigationIcon) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        tint = MaterialTheme.colorScheme.onSurface,
                        contentDescription = null
                    )
                }
            }
        }
    )
}

@PreviewLightDark
@Composable
private fun DefaultAppBarPreview() {
    AppBackground {
        DefaultAppBar(shouldShowNavigationIcon = true, onBackClick = {})
    }
}
