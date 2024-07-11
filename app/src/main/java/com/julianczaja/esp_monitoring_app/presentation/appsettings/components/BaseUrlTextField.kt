package com.julianczaja.esp_monitoring_app.presentation.appsettings.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.julianczaja.esp_monitoring_app.R
import com.julianczaja.esp_monitoring_app.components.DefaultDropdownMenu
import com.julianczaja.esp_monitoring_app.domain.model.FieldState
import kotlinx.collections.immutable.ImmutableSet


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BaseUrlTextField(
    modifier: Modifier = Modifier,
    fieldState: FieldState<String>,
    history: ImmutableSet<String>,
    onBaseUrlUpdate: (String) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            modifier = modifier,
            value = fieldState.data,
            onValueChange = onBaseUrlUpdate,
            label = { Text(stringResource(R.string.base_url_label)) },
            isError = fieldState.error != null,
            singleLine = true,
            supportingText = {
                fieldState.error?.let { errorId ->
                    Text(
                        text = stringResource(id = errorId),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            trailingIcon = if (history.isNotEmpty()) {
                {
                    ExposedDropdownMenuDefaults.TrailingIcon(
                        modifier = Modifier.menuAnchor(MenuAnchorType.SecondaryEditable),
                        expanded = expanded
                    )

                }
            } else null
        )
        DefaultDropdownMenu(
            isExpanded = expanded,
            items = history,
            selectedIndex = -1,
            maxHeight = 250.dp,
            onItemClicked = {
                expanded = false
                focusManager.clearFocus()
                onBaseUrlUpdate(history.elementAt(it))
            },
            onDismiss = { expanded = false }
        )
    }
}
