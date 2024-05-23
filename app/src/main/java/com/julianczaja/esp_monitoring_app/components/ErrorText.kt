package com.julianczaja.esp_monitoring_app.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.julianczaja.esp_monitoring_app.presentation.theme.spacing

@Composable
fun ErrorText(
    modifier: Modifier = Modifier,
    text: String,
    textAlign: TextAlign = TextAlign.Center,
) {
    Text(
        modifier = modifier.testTag("ErrorText"),
        text = text,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.headlineSmall,
        textAlign = textAlign
    )
}

//region Preview
@PreviewLightDark
@Composable
private fun ErrorTextPreview() {
    AppBackground {
        Box(modifier = Modifier.padding(MaterialTheme.spacing.large)) {
            ErrorText(text = "Some error text message")
        }
    }
}
//endregion
