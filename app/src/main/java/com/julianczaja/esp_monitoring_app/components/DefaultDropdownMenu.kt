package com.julianczaja.esp_monitoring_app.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp

@Composable
fun DefaultDropdownMenu(
    isExpanded: Boolean,
    items: List<String>,
    selectedIndex: Int = -1,
    maxHeight: Dp = Dp.Unspecified,
    onItemClicked: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    DropdownMenu(
        expanded = isExpanded,
        onDismissRequest = onDismiss,
        modifier = Modifier
            .heightIn(max = maxHeight)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        items.forEachIndexed { index, label ->
            DropdownMenuItem(
                text = {
                    Text(
                        text = label,
                        fontWeight = if (index == selectedIndex) FontWeight.Bold else null
                    )
                },
                onClick = { onItemClicked(index) }
            )
            if (index < items.size - 1) {
                HorizontalDivider()
            }
        }
    }
}
