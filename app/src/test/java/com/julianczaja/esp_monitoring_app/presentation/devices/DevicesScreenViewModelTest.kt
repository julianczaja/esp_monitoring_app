package com.julianczaja.esp_monitoring_app.presentation.devices

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.julianczaja.esp_monitoring_app.R
import com.julianczaja.esp_monitoring_app.data.repository.FakeDeviceRepositoryImpl
import com.julianczaja.esp_monitoring_app.domain.model.Device
import com.julianczaja.esp_monitoring_app.domain.repository.DeviceRepository
import com.julianczaja.esp_monitoring_app.presentation.devices.DevicesScreenViewModel.DevicesScreenUiState
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test


class DevicesScreenViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private lateinit var deviceRepository: DeviceRepository
    private lateinit var viewModel: DevicesScreenViewModel

    @Before
    fun setup() {
        deviceRepository = FakeDeviceRepositoryImpl()
        viewModel = DevicesScreenViewModel(deviceRepository, dispatcherRule.testDispatcher)
    }

    @Test
    fun `UI State is loading on start`() = runTest {
        viewModel.devicesUiState.test {
            val uiState = awaitItem()
            assertThat(uiState).isInstanceOf(DevicesScreenUiState.Loading::class.java)
        }
    }

    @Test
    fun `UI State error when repository thrown exception`() = runTest {
        (deviceRepository as FakeDeviceRepositoryImpl).getAllDevicesThrowError = true
        viewModel = DevicesScreenViewModel(deviceRepository, dispatcherRule.testDispatcher)

        viewModel.devicesUiState.test {
            val uiState = awaitItem()
            assertThat(uiState).isInstanceOf(DevicesScreenUiState.Error::class.java)
            assertThat((uiState as DevicesScreenUiState.Error).messageId).isEqualTo(R.string.unknown_error_message)
        }
    }

    @Test
    fun `UI State success when device is added`() = runTest {
        val fakeDevice = Device(1L, "Device 1")
        var uiState: DevicesScreenUiState

        viewModel.devicesUiState.test {
            uiState = awaitItem()
            assertThat(uiState).isInstanceOf(DevicesScreenUiState.Loading::class.java)

            deviceRepository.addNew(fakeDevice)
            uiState = awaitItem()
            assertThat(uiState).isInstanceOf(DevicesScreenUiState.Success::class.java)
            assertThat((uiState as DevicesScreenUiState.Success).devices.size).isEqualTo(1)
            assertThat((uiState as DevicesScreenUiState.Success).devices.first()).isEqualTo(fakeDevice)
        }
    }
}
