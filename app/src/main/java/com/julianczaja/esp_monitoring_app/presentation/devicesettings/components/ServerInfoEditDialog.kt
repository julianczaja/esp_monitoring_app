package com.julianczaja.esp_monitoring_app.presentation.devicesettings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.julianczaja.esp_monitoring_app.R
import com.julianczaja.esp_monitoring_app.components.AppBackground
import com.julianczaja.esp_monitoring_app.components.DefaultDialog
import com.julianczaja.esp_monitoring_app.components.DialogTwoButtons
import com.julianczaja.esp_monitoring_app.presentation.theme.spacing

@Composable
fun ServerInfoEditDialog(
    modifier: Modifier = Modifier,
    initialUrl: String = "",
    onDismiss: () -> Unit,
    onApply: (String) -> Unit
) {
    var url by rememberSaveable { mutableStateOf(initialUrl) }

    DefaultDialog(onDismiss = onDismiss) {
        Column(
            modifier = modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.headlineSmall,
                text = stringResource(R.string.server_info_label)
            )
            Spacer(modifier = Modifier.padding(MaterialTheme.spacing.small))

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = url,
                onValueChange = { if (it.length < 64) url = it },
                label = { Text(stringResource(R.string.url_label)) },
                singleLine = false,
                maxLines = 2
            )

            DialogTwoButtons(
                modifier = Modifier,
                onFirstButtonClicked = onDismiss,
                onSecondButtonClicked = { onApply(url) },
                firstButtonLabel = R.string.cancel_label,
                secondButtonLabel = R.string.update_label
            )
        }
    }
}

//region Preview
@PreviewLightDark
@Composable
private fun ServerInfoEditDialogPreview() {
    AppBackground {
        ServerInfoEditDialog(
            onDismiss = {},
            onApply = { url -> }
        )
    }
}
//endregion
