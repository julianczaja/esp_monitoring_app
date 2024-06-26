package com.julianczaja.esp_monitoring_app.presentation.addeditdevice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.julianczaja.esp_monitoring_app.R
import com.julianczaja.esp_monitoring_app.components.AppBackground
import com.julianczaja.esp_monitoring_app.presentation.addeditdevice.AddEditDeviceScreenViewModel.Event
import com.julianczaja.esp_monitoring_app.presentation.addeditdevice.AddEditDeviceScreenViewModel.Mode
import com.julianczaja.esp_monitoring_app.presentation.theme.spacing


@Composable
fun AddEditDeviceScreen(
    onSetAppBarTitle: (Int) -> Unit,
    snackbarHostState: SnackbarHostState,
    onDismiss: () -> Unit,
    viewModel: AddEditDeviceScreenViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    var viewModelInitiated by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(true) {
        onSetAppBarTitle(
            when (viewModel.mode) {
                Mode.Add -> R.string.add_device_screen_title
                Mode.Edit -> R.string.edit_device_screen_title
            }
        )

        if (!viewModelInitiated) {
            viewModel.init()
            viewModelInitiated = true
        }
    }

    val applyButtonLabelId = rememberSaveable {
        when (viewModel.mode) {
            Mode.Edit -> R.string.update_label
            Mode.Add -> R.string.add_label
        }
    }

    val id by viewModel.id.collectAsStateWithLifecycle()
    val idError by viewModel.idError.collectAsStateWithLifecycle()
    val isIdEnabled = viewModel.isIdEnabled
    val name by viewModel.name.collectAsStateWithLifecycle()
    val nameError by viewModel.nameError.collectAsStateWithLifecycle()

    LaunchedEffect(true) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                Event.DeviceAdded, Event.DeviceUpdated -> onDismiss()
                is Event.ShowError -> snackbarHostState.showSnackbar(
                    message = context.getString(event.messageId),
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    AddEditDeviceScreenContent(
        modifier = Modifier.fillMaxSize(),
        applyButtonLabelId = applyButtonLabelId,
        id = id,
        idError = idError,
        isIdEnabled = isIdEnabled,
        name = name,
        nameError = nameError,
        updateName = viewModel::updateName,
        updateId = viewModel::updateId,
        apply = viewModel::apply
    )
}

@Composable
fun AddEditDeviceScreenContent(
    modifier: Modifier = Modifier,
    applyButtonLabelId: Int,
    id: String,
    idError: Int?,
    isIdEnabled: Boolean,
    name: String,
    nameError: Int?,
    updateName: (String) -> Unit,
    updateId: (String) -> Unit,
    apply: () -> Unit,
) {
    Column(
        modifier = modifier
            .padding(MaterialTheme.spacing.large)
            .verticalScroll(rememberScrollState())
            .safeDrawingPadding()
            .imePadding(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier)
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
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
                singleLine = true
            )
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = MaterialTheme.spacing.medium),
                value = id,
                enabled = isIdEnabled,
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
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
        }
        Button(
            onClick = apply,
            enabled = (idError == null) && (nameError == null) && id.isNotEmpty() && name.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(0.5f)
        ) {
            Text(text = stringResource(id = applyButtonLabelId))
        }
    }
}

//region Preview
@PreviewLightDark
@Composable
private fun AddEditDeviceScreenContentPreview() {
    AppBackground {
        AddEditDeviceScreenContent(
            applyButtonLabelId = R.string.add_label,
            id = "12",
            idError = null,
            isIdEnabled = true,
            name = "Device name",
            nameError = null,
            updateId = {},
            updateName = {},
            apply = {}
        )
    }
}

@PreviewLightDark
@Composable
private fun AddEditDeviceScreenContentWithErrorsPreview() {
    AppBackground {
        AddEditDeviceScreenContent(
            applyButtonLabelId = R.string.update_label,
            id = "12",
            idError = R.string.add_device_error_id_exists,
            isIdEnabled = false,
            name = "-?",
            nameError = R.string.add_device_error_wrong_name,
            updateId = {},
            updateName = {},
            apply = {}
        )
    }
}
//endregion
