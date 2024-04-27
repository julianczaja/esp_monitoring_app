package com.julianczaja.esp_monitoring_app.presentation.devicephotos

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.julianczaja.esp_monitoring_app.DeviceIdArgs
import com.julianczaja.esp_monitoring_app.data.repository.FakePhotoRepositoryImpl
import com.julianczaja.esp_monitoring_app.domain.model.Photo
import com.julianczaja.esp_monitoring_app.presentation.devices.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDateTime


class DevicePhotosScreenViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var photoRepository: FakePhotoRepositoryImpl
    private lateinit var viewModel: DevicePhotosScreenViewModel

    private val deviceId = 1L

    @Before
    fun setup() {
        savedStateHandle = SavedStateHandle().apply { set(DeviceIdArgs.KEY, deviceId) }
        photoRepository = FakePhotoRepositoryImpl()
        viewModel = DevicePhotosScreenViewModel(savedStateHandle, photoRepository)
    }

    @Test
    fun `UI State is loading on start`() = runTest {
        viewModel.devicePhotosUiState.test {
            val uiState = awaitItem()
            assertThat(uiState.devicePhotosUiState).isInstanceOf(DevicePhotosState.Loading::class.java)
            assertThat(uiState.isRefreshing).isEqualTo(true)
        }
    }

    @Test
    fun `UI State is success when local photos returned`() = runTest {
        val fakePhoto = Photo(
            deviceId = deviceId,
            dateTime = LocalDateTime.of(2022, 1, 1, 1, 1, 1),
            fileName = "fileName",
            url = "url",
            size = "100x500"
        )
        var uiState: DevicePhotosScreenUiState

        viewModel.devicePhotosUiState.test {
            uiState = awaitItem()
            assertThat(uiState.devicePhotosUiState).isInstanceOf(DevicePhotosState.Loading::class.java)
            assertThat(uiState.isRefreshing).isEqualTo(true)

            photoRepository.emitAllPhotosLocalData(listOf(fakePhoto))
            uiState = awaitItem()
            assertThat(uiState.devicePhotosUiState).isInstanceOf(DevicePhotosState.Success::class.java)
            ((uiState.devicePhotosUiState as DevicePhotosState.Success).dateGroupedPhotos.values).let { photos ->
                assertThat(photos.size).isEqualTo(1)
                assertThat(photos.first()).contains(fakePhoto)
            }
        }
    }

    @Test
    fun `UI State is error when repository thrown exception`() = runTest {
        var uiState: DevicePhotosScreenUiState

        viewModel.devicePhotosUiState.test {
            uiState = awaitItem()
            assertThat(uiState.devicePhotosUiState).isInstanceOf(DevicePhotosState.Loading::class.java)
            assertThat(uiState.isRefreshing).isEqualTo(true)

            photoRepository.setUpdateAllPhotosRemoteReturnData(Result.failure(Exception()))
            uiState = awaitItem()
            assertThat(uiState.devicePhotosUiState).isInstanceOf(DevicePhotosState.Error::class.java)
            assertThat(uiState.isRefreshing).isEqualTo(true)

            uiState = awaitItem()
            assertThat(uiState.devicePhotosUiState).isInstanceOf(DevicePhotosState.Error::class.java)
            assertThat(uiState.isRefreshing).isEqualTo(false)
        }
    }
}
