package com.julianczaja.esp_monitoring_app.components

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.julianczaja.esp_monitoring_app.R
import com.julianczaja.esp_monitoring_app.presentation.theme.spacing

private const val STATE_BAR_HEIGHT_DP = 40

@Composable
fun StateBar(
    isVisible: Boolean,
    @StringRes title: Int,
    onButtonClicked: (() -> Unit)? = null,
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { -it })
    ) {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.errorContainer)
                .height(STATE_BAR_HEIGHT_DP.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = MaterialTheme.spacing.medium),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(id = title),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                if (onButtonClicked != null) {
                    TextButton(onClick = onButtonClicked) {
                        Text(
                            text = stringResource(id = R.string.enable_label),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
            HorizontalDivider()
        }
    }
}

//region Preview
@PreviewLightDark
@Composable
private fun StateBarWithoutButtonPreview() {
    AppBackground(Modifier.height(100.dp)) {
        Column {
            StateBar(isVisible = true, title = R.string.location_disabled_label, onButtonClicked = null)
        }
    }
}

@PreviewLightDark
@Composable
private fun StateBarWithButtonPreview() {
    AppBackground(Modifier.height(100.dp)) {
        Column {
            StateBar(isVisible = true, title = R.string.location_disabled_label, onButtonClicked = {})
            StateBar(isVisible = true, title = R.string.location_disabled_label, onButtonClicked = {})
        }
    }
}
//endregion