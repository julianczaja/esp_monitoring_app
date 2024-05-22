package com.julianczaja.esp_monitoring_app.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp

private const val DROPDOWN_MENU_HEIGHT_DP = 250

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownMenuBox(
    modifier: Modifier = Modifier,
    title: String,
    items: List<String>,
    selectedIndex: Int,
    enabled: Boolean,
    initialIsCollapsed: Boolean = false,
    onItemClicked: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(initialIsCollapsed) }

    ExposedDropdownMenuBox(
        modifier = modifier,
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            modifier = modifier.menuAnchor(),
            value = items[selectedIndex],
            onValueChange = { },
            label = { Text(title) },
            readOnly = true,
            enabled = enabled,
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
        )
        DefaultDropdownMenu(
            isExpanded = expanded,
            items = items,
            selectedIndex = selectedIndex,
            maxHeight = DROPDOWN_MENU_HEIGHT_DP.dp,
            onItemClicked = {
                expanded = false
                onItemClicked(it)
            },
            onDismiss = { expanded = false }
        )
    }
}

//region Preview
@Preview
@Composable
private fun DropdownMenuBoxExpandedPreview() {
    AppBackground(
        Modifier
            .height(400.dp)
            .width(400.dp)
    ) {
        DropdownMenuBox(
            modifier = Modifier.fillMaxWidth(),
            title = "Title",
            items = listOf("Item 1", "Item 2", "Item 3", "Item 4", "Item 5"),
            selectedIndex = 2,
            enabled = true,
            initialIsCollapsed = true,
            onItemClicked = {}
        )
    }
}

@PreviewLightDark
@Composable
private fun DropdownMenuBoxCollapsedPreview() {
    AppBackground {
        DropdownMenuBox(
            title = "Title",
            items = listOf("Item 1", "Item 2", "Item 3", "Item 4", "Item 5"),
            selectedIndex = 2,
            initialIsCollapsed = false,
            enabled = true,
            onItemClicked = {}
        )
    }
}

@PreviewLightDark
@Composable
private fun DropdownMenuBoxCollapsedAndDisabledPreview() {
    AppBackground {
        DropdownMenuBox(
            title = "Title",
            items = listOf("Item 1", "Item 2", "Item 3", "Item 4", "Item 5"),
            selectedIndex = 2,
            initialIsCollapsed = false,
            enabled = false,
            onItemClicked = {}
        )
    }
}
//endregion
