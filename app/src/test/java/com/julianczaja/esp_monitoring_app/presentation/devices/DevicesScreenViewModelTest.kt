package com.julianczaja.esp_monitoring_app.presentation.devices

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.julianczaja.esp_monitoring_app.MainDispatcherRule
import com.julianczaja.esp_monitoring_app.R
import com.julianczaja.esp_monitoring_app.data.repository.FakeDeviceRepositoryImpl
import com.julianczaja.esp_monitoring_app.data.repository.FakePhotoRepositoryImpl
import com.julianczaja.esp_monitoring_app.domain.model.Device
import com.julianczaja.esp_monitoring_app.domain.model.Photo
import com.julianczaja.esp_monitoring_app.presentation.devices.DevicesScreenViewModel.UiState
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test


class DevicesScreenViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private lateinit var deviceRepository: FakeDeviceRepositoryImpl
    private lateinit var photoRepository: FakePhotoRepositoryImpl
    private lateinit var viewModel: DevicesScreenViewModel

    @Before
    fun setup() {
        deviceRepository = FakeDeviceRepositoryImpl()
        photoRepository = FakePhotoRepositoryImpl()
        viewModel = DevicesScreenViewModel(deviceRepository, photoRepository, dispatcherRule.testDispatcher)
    }

    @Test
    fun `UI State is loading on start`() = runTest {
        viewModel.uiState.test {
            val uiState = awaitItem()
            assertThat(uiState).isInstanceOf(UiState.Loading::class.java)
        }
    }

    @Test
    fun `UI State error when device repository thrown exception`() = runTest {
        deviceRepository.getAllDevicesThrowsError = true
        viewModel = DevicesScreenViewModel(deviceRepository, photoRepository, dispatcherRule.testDispatcher)

        viewModel.uiState.test {
            val uiState = awaitItem()
            assertThat(uiState).isInstanceOf(UiState.Error::class.java)
            assertThat((uiState as UiState.Error).messageId).isEqualTo(R.string.unknown_error_message)
        }
    }

    @Test
    fun `UI State error when devices are not empty and photo repository thrown exception`() = runTest {
        deviceRepository.emitAllDevicesData(listOf(Device(1L, "name")))
        photoRepository.getLastPhotoLocalThrowsError = true
        viewModel = DevicesScreenViewModel(deviceRepository, photoRepository, dispatcherRule.testDispatcher)

        viewModel.uiState.test {
            val uiState = awaitItem()
            assertThat(uiState).isInstanceOf(UiState.Error::class.java)
            assertThat((uiState as UiState.Error).messageId).isEqualTo(R.string.unknown_error_message)
        }
    }

    @Test
    fun `UI State success when device is added`() = runTest {
        val fakeDevice = Device(1L, "Device 1")
        val fakePhoto = Photo(1L, mockk(), "", "", "", "")
        var uiState: UiState

        viewModel.uiState.test {
            uiState = awaitItem()
            assertThat(uiState).isInstanceOf(UiState.Loading::class.java)

            deviceRepository.emitAllDevicesData(emptyList())
            uiState = awaitItem()
            assertThat(uiState).isInstanceOf(UiState.Success::class.java)
            assertThat((uiState as UiState.Success).devicesWithLastPhoto.size).isEqualTo(0)

            photoRepository.emitAllPhotosLocalData(listOf(fakePhoto))
            deviceRepository.addNew(fakeDevice)
            uiState = awaitItem()
            assertThat(uiState).isInstanceOf(UiState.Success::class.java)
            assertThat((uiState as UiState.Success).devicesWithLastPhoto.size).isEqualTo(1)
            assertThat((uiState as UiState.Success).devicesWithLastPhoto.keys.first()).isEqualTo(fakeDevice)
            assertThat((uiState as UiState.Success).devicesWithLastPhoto.values.first()).isEqualTo(fakePhoto)
        }
    }

    @Test
    fun `UI State success when device is added without last photo`() = runTest {
        val fakeDevice = Device(1L, "Device 1")
        var uiState: UiState

        deviceRepository.emitAllDevicesData(listOf(fakeDevice))
        photoRepository.emitAllPhotosLocalData(emptyList())

        viewModel.uiState.test {
            uiState = awaitItem()
            assertThat(uiState).isInstanceOf(UiState.Success::class.java)
            assertThat((uiState as UiState.Success).devicesWithLastPhoto.size).isEqualTo(1)
            assertThat((uiState as UiState.Success).devicesWithLastPhoto.keys.first()).isEqualTo(fakeDevice)
            assertThat((uiState as UiState.Success).devicesWithLastPhoto.values.first()).isEqualTo(null)
        }
    }

    @Test
    fun `UI State success when device is removed`() = runTest {
        val fakeDevice = Device(1L, "Device 1")
        val fakePhoto = Photo(1L, mockk(), "", "", "", "")
        var uiState: UiState

        deviceRepository.emitAllDevicesData(listOf(fakeDevice))
        photoRepository.emitAllPhotosLocalData(listOf(fakePhoto))

        viewModel.uiState.test {
            uiState = awaitItem()
            assertThat(uiState).isInstanceOf(UiState.Success::class.java)
            assertThat((uiState as UiState.Success).devicesWithLastPhoto.size).isEqualTo(1)
            assertThat((uiState as UiState.Success).devicesWithLastPhoto.keys.first()).isEqualTo(fakeDevice)
            assertThat((uiState as UiState.Success).devicesWithLastPhoto.values.first()).isEqualTo(fakePhoto)

            deviceRepository.remove(fakeDevice)
            uiState = awaitItem()
            assertThat(uiState).isInstanceOf(UiState.Success::class.java)
            assertThat((uiState as UiState.Success).devicesWithLastPhoto.size).isEqualTo(0)
        }
    }
}
