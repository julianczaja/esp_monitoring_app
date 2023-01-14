package com.julianczaja.esp_monitoring_app.presentation.adddevice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.julianczaja.esp_monitoring_app.R
import com.julianczaja.esp_monitoring_app.presentation.theme.spacing


@OptIn(ExperimentalMaterial3Api::class)
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
                AddDeviceDialogViewModel.Event.DEVICE_ADDED -> onDismiss()
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss
    ) {
        Card(
            shape = RoundedCornerShape(MaterialTheme.spacing.medium)
        ) {
            LazyColumn(
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(250.dp)
                    .height(300.dp)
                    .padding(MaterialTheme.spacing.large)
            ) {
                item {
                    Text(text = stringResource(R.string.add_new_device_label))
                }
                item {
                    OutlinedTextField(
                        value = name,
                        label = { Text(stringResource(R.string.device_name_label)) },
                        onValueChange = viewModel::updateName,
                        isError = !nameError.isNullOrEmpty(),
                        supportingText = {
                            nameError?.let {
                                Text(
                                    text = it,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding()
                                )
                            }
                        }
                    )
                }
                item {
                    OutlinedTextField(
                        value = id,
                        label = { Text(stringResource(R.string.device_id_label)) },
                        onValueChange = viewModel::updateId,
                        isError = !idError.isNullOrEmpty(),
                        supportingText = {
                            idError?.let {
                                Text(
                                    text = it,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding()
                                )
                            }
                        }
                    )
                }
                item {
                    Button(
                        onClick = viewModel::addDevice,
                        enabled = (idError == null) && (nameError == null) && id.isNotEmpty() && name.isNotEmpty(),
                        modifier = Modifier.padding(top = MaterialTheme.spacing.large)
                    ) {
                        Text(stringResource(R.string.add_label))
                    }
                }
            }
        }
    }
}
