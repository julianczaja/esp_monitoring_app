package com.julianczaja.esp_monitoring_app.components

import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.runtime.Composable


fun LazyGridScope.header(
    key: Any? = null,
    content: @Composable LazyGridItemScope.() -> Unit,
) {
    item(
        key = key,
        span = { GridItemSpan(this.maxLineSpan) },
        content = content
    )
}