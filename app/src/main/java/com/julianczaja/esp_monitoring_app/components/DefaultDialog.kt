package com.julianczaja.esp_monitoring_app.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.julianczaja.esp_monitoring_app.presentation.theme.AppTheme
import com.julianczaja.esp_monitoring_app.presentation.theme.shape
import com.julianczaja.esp_monitoring_app.presentation.theme.spacing


@Composable
fun DefaultDialog(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss
    ) {
        Card(
            shape = RoundedCornerShape(MaterialTheme.shape.dialogCorners),
            modifier = Modifier
                .height(IntrinsicSize.Min)
                .defaultMinSize(minWidth = 280.dp, minHeight = 250.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(MaterialTheme.spacing.veryLarge)
            ) {
                content.invoke()
            }
        }
    }
}

@Preview
@Composable
private fun  DefaultDialogPreview() {
    AppTheme {
        DefaultDialog(onDismiss = { }) {
            Text(text = "Dialog")
        }
    }
}