package com.julianczaja.esp_monitoring_app.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.julianczaja.esp_monitoring_app.presentation.theme.AppTheme


@Composable
fun AppBackground(
    modifier: Modifier = Modifier,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    AppTheme(
        dynamicColor = dynamicColor
    ) {
        Surface(
            color = MaterialTheme.colorScheme.background,
            modifier = modifier,
        ) {
            content()
        }
    }
}
