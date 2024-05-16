package com.julianczaja.esp_monitoring_app.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.julianczaja.esp_monitoring_app.R
import com.julianczaja.esp_monitoring_app.presentation.theme.spacing


@Composable
fun GrantPermissionButton(
    modifier: Modifier = Modifier,
    @StringRes titleId: Int,
    onButtonClicked: () -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
    ) {
        Text(stringResource(titleId))
        Button(onClick = onButtonClicked) {
            Text(stringResource(id = R.string.grant_permission_button_label))
        }
    }
}