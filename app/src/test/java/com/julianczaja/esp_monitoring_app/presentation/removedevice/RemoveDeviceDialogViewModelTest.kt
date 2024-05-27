package com.julianczaja.esp_monitoring_app.presentation.removedevice

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.julianczaja.esp_monitoring_app.data.repository.FakeDeviceRepositoryImpl
import com.julianczaja.esp_monitoring_app.domain.model.Device
import com.julianczaja.esp_monitoring_app.domain.repository.DeviceRepository
import com.julianczaja.esp_monitoring_app.presentation.devices.MainDispatcherRule
import io.mockk.coVerify
import io.mockk.spyk
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class RemoveDeviceDialogViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private fun getViewModel(
        deviceId: Long,
        deviceRepository: DeviceRepository? = null
    ) = RemoveDeviceDialogViewModel(
        savedStateHandle = SavedStateHandle(mapOf("deviceId" to deviceId)),
        deviceRepository = deviceRepository ?: FakeDeviceRepositoryImpl(),
        ioDispatcher = dispatcherRule.testDispatcher
    )

    @Test
    fun `UI state is success when local device found`() = runTest {
        val deviceId = 1L
        val repository = FakeDeviceRepositoryImpl().apply { addNew(Device(deviceId, "name")) }
        val viewModel = getViewModel(deviceId, repository)

        viewModel.uiState.test {
            assertThat(awaitItem()).isInstanceOf(RemoveDeviceDialogViewModel.UiState.Loading::class.java)
            viewModel.init()
            assertThat(awaitItem()).isInstanceOf(RemoveDeviceDialogViewModel.UiState.Success::class.java)
        }
    }

    @Test
    fun `UI state is error when local device found`() = runTest {
        val deviceId = 1L
        val viewModel = getViewModel(deviceId)

        viewModel.uiState.test {
            assertThat(awaitItem()).isInstanceOf(RemoveDeviceDialogViewModel.UiState.Loading::class.java)
            viewModel.init()
            assertThat(awaitItem()).isInstanceOf(RemoveDeviceDialogViewModel.UiState.Error::class.java)
        }
    }

    @Test
    fun `UI state is error when error thrown during device removal`() = runTest {
        val device = Device(1L, "name")
        val viewModel = getViewModel(device.id)

        viewModel.uiState.test {
            assertThat(awaitItem()).isInstanceOf(RemoveDeviceDialogViewModel.UiState.Loading::class.java)
            viewModel.removeDevice(device)
            assertThat(awaitItem()).isInstanceOf(RemoveDeviceDialogViewModel.UiState.Error::class.java)
        }
    }

    @Test
    fun `Event flow emits DEVICE_REMOVED event when device is removed successfully`() = runTest {
        val device = Device(1L, "name")
        val repository = spyk(FakeDeviceRepositoryImpl().apply { addNew(device) })
        val viewModel = getViewModel(device.id, repository)

        viewModel.eventFlow.test {
            viewModel.removeDevice(device)
            assertThat(awaitItem()).isInstanceOf(RemoveDeviceDialogViewModel.Event.DEVICE_REMOVED::class.java)
        }
        coVerify(exactly = 1) { repository.remove(any()) }
    }

    @Test
    fun `Event flow do not emit DEVICE_REMOVED event when error thrown during device removal`() = runTest {
        val device = Device(1L, "name")
        val repository = spyk(FakeDeviceRepositoryImpl())
        val viewModel = getViewModel(device.id, repository)

        viewModel.eventFlow.test {
            viewModel.removeDevice(device)
            expectNoEvents()
        }
        coVerify(exactly = 1) { repository.remove(any()) }
    }
}
