package com.julianczaja.esp_monitoring_app.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.julianczaja.esp_monitoring_app.R
import com.julianczaja.esp_monitoring_app.domain.model.PermissionState
import com.julianczaja.esp_monitoring_app.presentation.theme.spacing


@Composable
fun PermissionRationaleDialog(
    @StringRes title: Int,
    @StringRes bodyRationale: Int,
    @StringRes bodyDenied: Int,
    permissionState: PermissionState,
    onRequestPermission: () -> Unit,
    onDismiss: () -> Unit,
) {
    DefaultDialog(onDismiss = onDismiss) {
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                tint = MaterialTheme.colorScheme.secondary,
                contentDescription = null
            )
            Text(
                text = stringResource(title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = MaterialTheme.spacing.large)
            )
            Text(
                text = stringResource(
                    if (permissionState == PermissionState.RATIONALE_NEEDED) bodyRationale else bodyDenied
                ),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = MaterialTheme.spacing.large)
            )
            DialogTwoButtons(
                modifier = Modifier,
                onFirstButtonClicked = onDismiss,
                onSecondButtonClicked = onRequestPermission,
                firstButtonLabel = R.string.cancel_label,
                secondButtonLabel = if (permissionState == PermissionState.RATIONALE_NEEDED)
                    R.string.grant_permission_button_label
                else
                    R.string.open_settings_label
            )
        }
    }
}
