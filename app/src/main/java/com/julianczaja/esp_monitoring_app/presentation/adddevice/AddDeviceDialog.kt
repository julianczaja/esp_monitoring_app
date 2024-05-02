package com.julianczaja.esp_monitoring_app.presentation.adddevice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.julianczaja.esp_monitoring_app.R
import com.julianczaja.esp_monitoring_app.components.DefaultDialog
import com.julianczaja.esp_monitoring_app.components.DialogTwoButtons
import com.julianczaja.esp_monitoring_app.presentation.adddevice.AddDeviceDialogViewModel.Event
import com.julianczaja.esp_monitoring_app.presentation.theme.AppTheme
import com.julianczaja.esp_monitoring_app.presentation.theme.spacing


@Composable
fun AddNewDeviceDialog(
    onDismiss: () -> Unit,
    viewModel: AddDeviceDialogViewModel = hiltViewModel(),
) {
    val id by viewModel.id.collectAsStateWithLifecycle()
    val idError by viewModel.idError.collectAsStateWithLifecycle()
    val name by viewModel.name.collectAsStateWithLifecycle()
    val nameError by viewModel.nameError.collectAsStateWithLifecycle()

    LaunchedEffect(true) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                Event.DEVICE_ADDED -> onDismiss()
            }
        }
    }

    AddNewDeviceDialogContent(
        onDismiss = onDismiss,
        id = id,
        idError = idError,
        name = name,
        nameError = nameError,
        updateName = viewModel::updateName,
        updateId = viewModel::updateId,
        addDevice = viewModel::addDevice
    )
}

@Composable
fun AddNewDeviceDialogContent(
    onDismiss: () -> Unit,
    id: String,
    idError: Int?,
    name: String,
    nameError: Int?,
    updateName: (String) -> Unit,
    updateId: (String) -> Unit,
    addDevice: () -> Unit,
) {
    DefaultDialog(onDismiss) {
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Text(text = stringResource(R.string.add_new_device_label), style = MaterialTheme.typography.headlineSmall)
            OutlinedTextField(
                value = name,
                label = { Text(stringResource(R.string.device_name_label)) },
                onValueChange = updateName,
                isError = nameError != null,
                supportingText = {
                    nameError?.let {
                        Text(
                            text = stringResource(id = it),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                singleLine = true,
                modifier = Modifier.padding(top = MaterialTheme.spacing.veryLarge)
            )
            OutlinedTextField(
                value = id,
                label = { Text(stringResource(R.string.device_id_label)) },
                onValueChange = updateId,
                isError = idError != null,
                supportingText = {
                    idError?.let {
                        Text(
                            text = stringResource(id = it),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                singleLine = true,
                modifier = Modifier.padding(top = MaterialTheme.spacing.medium)
            )
            DialogTwoButtons(
                onFirstButtonClicked = onDismiss,
                onSecondButtonClicked = addDevice,
                secondButtonEnabled = (idError == null) && (nameError == null) && id.isNotEmpty() && name.isNotEmpty(),
                modifier = Modifier.padding(top = MaterialTheme.spacing.veryLarge)
            )
        }
    }
}

@Preview
@Composable
private fun AddNewDeviceDialogContentPreview() {
    AppTheme {
        AddNewDeviceDialogContent(
            onDismiss = {},
            id = "12",
            idError = null,
            name = "Device name",
            nameError = null,
            updateId = {},
            updateName = {},
            addDevice = {}
        )
    }
}

@Preview
@Composable
private fun AddNewDeviceDialogContentWithErrorsPreview() {
    AppTheme {
        AddNewDeviceDialogContent(
            onDismiss = {},
            id = "12",
            idError = R.string.add_device_error_id_exists,
            name = "-?",
            nameError = R.string.add_device_error_wrong_name,
            updateId = {},
            updateName = {},
            addDevice = {}
        )
    }
}
