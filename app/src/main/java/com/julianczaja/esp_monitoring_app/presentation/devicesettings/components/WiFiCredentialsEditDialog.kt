package com.julianczaja.esp_monitoring_app.presentation.devicesettings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.julianczaja.esp_monitoring_app.R
import com.julianczaja.esp_monitoring_app.components.AppBackground
import com.julianczaja.esp_monitoring_app.components.DefaultDialog
import com.julianczaja.esp_monitoring_app.components.DialogTwoButtons
import com.julianczaja.esp_monitoring_app.presentation.theme.spacing


@Composable
fun WiFiCredentialsEditDialog(
    modifier: Modifier = Modifier,
    initialSsid: String = "",
    onDismiss: () -> Unit,
    onApply: (String, String) -> Unit
) {
    var ssid by rememberSaveable { mutableStateOf(initialSsid) }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    DefaultDialog(onDismiss = { /*TODO*/ }) {
        Column(
            modifier = modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.headlineSmall,
                text = stringResource(R.string.wifi_credentials_label)
            )
            Spacer(modifier = Modifier.padding(MaterialTheme.spacing.small))

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = ssid,
                onValueChange = { if (it.length < 30) ssid = it },
                label = { Text(stringResource(R.string.ssid_label)) },
                singleLine = true,
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = password,
                onValueChange = { if (it.length < 30) password = it },
                label = { Text(stringResource(R.string.password_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                trailingIcon = {
                    val drawableId = when (passwordVisible) {
                        true -> R.drawable.ic_eye
                        false -> R.drawable.ic_eye_off
                    }

                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(painter = painterResource(id = drawableId), null)
                    }
                }
            )
            DialogTwoButtons(
                modifier = Modifier,
                onFirstButtonClicked = onDismiss,
                onSecondButtonClicked = { onApply(ssid, password) },
                firstButtonLabel = R.string.cancel_label,
                secondButtonLabel = R.string.update_label
            )
        }
    }
}

//region Preview
@PreviewLightDark
@Composable
private fun WiFiCredentialsEditDialogPreview() {
    AppBackground {
        WiFiCredentialsEditDialog(
            onDismiss = {},
            onApply = { ssid, password -> }
        )
    }
}
//endregion