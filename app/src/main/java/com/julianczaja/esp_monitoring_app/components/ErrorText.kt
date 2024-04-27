package com.julianczaja.esp_monitoring_app.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign

@Composable
fun ErrorText(
    modifier: Modifier = Modifier,
    text: String,
    textAlign: TextAlign = TextAlign.Center,
) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.headlineSmall,
        textAlign = textAlign,
        modifier = modifier.testTag("ErrorText")
    )
}
