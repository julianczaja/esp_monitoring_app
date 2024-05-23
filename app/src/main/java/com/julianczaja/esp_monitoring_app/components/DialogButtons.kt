package com.julianczaja.esp_monitoring_app.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.julianczaja.esp_monitoring_app.R


@Composable
fun DialogTwoButtons(
    modifier: Modifier = Modifier,
    onFirstButtonClicked: () -> Unit,
    onSecondButtonClicked: () -> Unit,
    firstButtonEnabled: Boolean = true,
    secondButtonEnabled: Boolean = true,
    @StringRes firstButtonLabel: Int = R.string.cancel_label,
    @StringRes secondButtonLabel: Int = R.string.ok_label,
) {
    Row(
        horizontalArrangement = Arrangement.End,
        modifier = modifier.fillMaxWidth()
    ) {
        TextButton(onClick = onFirstButtonClicked, enabled = firstButtonEnabled) {
            Text(text = stringResource(id = firstButtonLabel), style = MaterialTheme.typography.labelLarge)
        }
        TextButton(onClick = onSecondButtonClicked, enabled = secondButtonEnabled) {
            Text(text = stringResource(id = secondButtonLabel), style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun DialogOneButton(
    modifier: Modifier = Modifier,
    onButtonClicked: () -> Unit,
    buttonEnabled: Boolean = true,
    @StringRes buttonLabel: Int = R.string.cancel_label
) {
    Row(
        horizontalArrangement = Arrangement.End,
        modifier = modifier.fillMaxWidth()
    ) {
        TextButton(onClick = onButtonClicked, enabled = buttonEnabled) {
            Text(text = stringResource(id = buttonLabel), style = MaterialTheme.typography.labelLarge)
        }
    }
}

//region Preview
@PreviewLightDark
@Composable
private fun DialogTwoButtonsPreview() {
    AppBackground {
        DialogTwoButtons(onFirstButtonClicked = {}, onSecondButtonClicked = {})
    }
}

@PreviewLightDark
@Composable
private fun DialogOneButtonPreview() {
    AppBackground {
        DialogOneButton(onButtonClicked = {})
    }
}
//endregion
