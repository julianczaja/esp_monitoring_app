package com.julianczaja.esp_monitoring_app.presentation.devices

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.julianczaja.esp_monitoring_app.R
import com.julianczaja.esp_monitoring_app.data.repository.FakeDeviceRepositoryImpl
import com.julianczaja.esp_monitoring_app.domain.model.Device
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test


class DevicesScreenViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    @Test
    fun `UI State is loading on start`() = runTest {
        val fakeDeviceRepository = FakeDeviceRepositoryImpl()
        val viewModel = DevicesScreenViewModel(fakeDeviceRepository)

        viewModel.devicesUiState.test {
            val uiState = awaitItem()
            assertThat(uiState).isInstanceOf(DevicesScreenUiState.Loading::class.java)
        }
    }

    @Test
    fun `UI State error when repository thrown exception`() = runTest {
        val fakeDeviceRepository = FakeDeviceRepositoryImpl().apply { getAllDevicesThrowError = true }
        val viewModel = DevicesScreenViewModel(fakeDeviceRepository)

        viewModel.devicesUiState.test {
            val uiState = awaitItem()
            assertThat(uiState).isInstanceOf(DevicesScreenUiState.Error::class.java)
            assertThat((uiState as DevicesScreenUiState.Error).messageId).isEqualTo(R.string.unknown_error_message)
        }
    }

    @Test
    fun `UI State success when device is added`() = runTest {
        val fakeDevice = Device(1L, "Device 1")
        val fakeDeviceRepository = FakeDeviceRepositoryImpl()
        val viewModel = DevicesScreenViewModel(fakeDeviceRepository)
        var uiState: DevicesScreenUiState

        viewModel.devicesUiState.test {
            uiState = awaitItem()
            assertThat(uiState).isInstanceOf(DevicesScreenUiState.Loading::class.java)

            fakeDeviceRepository.addNew(fakeDevice)
            uiState = awaitItem()
            assertThat(uiState).isInstanceOf(DevicesScreenUiState.Success::class.java)
            assertThat((uiState as DevicesScreenUiState.Success).devices.size).isEqualTo(1)
            assertThat((uiState as DevicesScreenUiState.Success).devices.first()).isEqualTo(fakeDevice)
        }
    }
}
