package com.julianczaja.esp_monitoring_app.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.julianczaja.esp_monitoring_app.presentation.theme.spacing


@Composable
fun SwitchWithLabel(
    modifier: Modifier,
    label: String,
    isChecked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Switch,
                onClick = {
                    onCheckedChange(!isChecked)
                }
            )

    ) {
        Text(
            modifier = Modifier.weight(.8f),
            text = label,
            overflow = TextOverflow.Ellipsis,
            maxLines = 2
        )
        Spacer(modifier = Modifier.padding(start = MaterialTheme.spacing.medium))
        Switch(
            modifier = Modifier.weight(.2f),
            checked = isChecked,
            enabled = enabled,
            onCheckedChange = {
                onCheckedChange(it)
            }
        )
    }
}

//region Preview
@PreviewLightDark
@Composable
private fun SwitchWithLabelUnCheckedPreview() {
    AppBackground {
        SwitchWithLabel(
            modifier = Modifier.fillMaxWidth(),
            label = "Label",
            isChecked = false,
            enabled = true,
            onCheckedChange = {}
        )
    }
}

@PreviewLightDark
@Composable
private fun SwitchWithLabelCheckedPreview() {
    AppBackground {
        SwitchWithLabel(
            modifier = Modifier.fillMaxWidth(),
            label = "Label",
            isChecked = true,
            enabled = true,
            onCheckedChange = {}
        )
    }
}

@PreviewLightDark
@Composable
private fun SwitchWithLabelCheckedDisabledPreview() {
    AppBackground {
        SwitchWithLabel(
            modifier = Modifier.fillMaxWidth(),
            label = "Label",
            isChecked = true,
            enabled = false,
            onCheckedChange = {}
        )
    }
}

@Preview
@Composable
private fun SwitchWithLabelLongLabelPreview() {
    val label = "Looooooooooooooooooooooooooooooong labeeeeeeeeeeeeeeeeeeeeeeeeeel eeeeeeeeeel ellllllllleeeeeeeeee"
    AppBackground {
        SwitchWithLabel(
            modifier = Modifier.fillMaxWidth(),
            label = label,
            isChecked = false,
            enabled = true,
            onCheckedChange = {}
        )
    }
}
//endregion
