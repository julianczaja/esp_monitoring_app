

package com.julianczaja.esp_monitoring_app.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class Shape(
    val dialogCorners: Dp = 28.dp,
    val photoCorners: Dp = 8.dp,
    val photoDateFilterCorners: Dp = 8.dp,
)

internal val LocalShape = staticCompositionLocalOf { Shape() }

@Suppress("UnusedReceiverParameter")
val MaterialTheme.shape: Shape
    @Composable
    @ReadOnlyComposable
    get() = LocalShape.current
