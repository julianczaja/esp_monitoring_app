package com.julianczaja.esp_monitoring_app.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.julianczaja.esp_monitoring_app.R
import com.julianczaja.esp_monitoring_app.presentation.theme.spacing


@Composable
fun Notice(
    modifier: Modifier = Modifier,
    text: String,
    actionText: String,
    onIgnoreClicked: () -> Unit,
    onActionClicked: () -> Unit,
) {
    Card(
        modifier = modifier,
        colors = CardColors(
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            containerColor = MaterialTheme.colorScheme.errorContainer,
            disabledContainerColor = CardDefaults.cardColors().disabledContainerColor,
            disabledContentColor = CardDefaults.cardColors().disabledContentColor
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.spacing.medium),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_alert), contentDescription = null
                )
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.medium))
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onIgnoreClicked) {
                    Text(
                        text = stringResource(R.string.ignore_action),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                TextButton(onClick = onActionClicked) {
                    Text(
                        text = actionText,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}


//region Preview
@PreviewLightDark
@Composable
private fun NoticePreview() {
    AppBackground(Modifier.width(350.dp)) {
        Notice(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            text = stringResource(id = R.string.storage_timelapses_permission_notice),
            actionText = stringResource(id = R.string.grant_permission_button_label),
            onActionClicked = {},
            onIgnoreClicked = {}
        )
    }
}
//endregion
