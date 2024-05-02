package com.julianczaja.esp_monitoring_app.presentation.devicephotos

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.julianczaja.esp_monitoring_app.DeviceIdArgs
import com.julianczaja.esp_monitoring_app.data.NetworkManager
import com.julianczaja.esp_monitoring_app.data.repository.FakePhotoRepositoryImpl
import com.julianczaja.esp_monitoring_app.domain.model.Photo
import com.julianczaja.esp_monitoring_app.presentation.devicephotos.DevicePhotosScreenViewModel.UiState
import com.julianczaja.esp_monitoring_app.presentation.devices.MainDispatcherRule
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
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
    private lateinit var networkManager: NetworkManager
    private lateinit var viewModel: DevicePhotosScreenViewModel

    private val deviceId = 1L

    @Before
    fun setup() {
        savedStateHandle = SavedStateHandle().apply { set(DeviceIdArgs.KEY, deviceId) }
        photoRepository = spyk(FakePhotoRepositoryImpl())
        networkManager = mockk()
        every { networkManager.isOnline } returns flow { delay(1000); emit(true) }
        viewModel = DevicePhotosScreenViewModel(
            savedStateHandle = savedStateHandle,
            photoRepository = photoRepository,
            networkManager = networkManager,
            ioDispatcher = dispatcherRule.testDispatcher
        )
    }

    @Test
    fun `isRefreshing should be true on start`() = runTest {
        viewModel.devicePhotosUiState.test {
            val uiState = awaitItem()
            assertThat(uiState.isRefreshing).isEqualTo(true)
        }
        coVerify(exactly = 1) { photoRepository.updateAllPhotosRemote(any()) }
    }

    @Test
    fun `local photos should be mapped to dateGroupedPhotos`() = runTest {
        val photo = Photo(
            deviceId = deviceId,
            dateTime = LocalDateTime.of(2022, 1, 1, 1, 1, 1),
            fileName = "fileName",
            url = "url",
            size = "100x500"
        )
        val localPhotos = listOf(photo)
        val dateGroupedPhotos = mapOf(photo.dateTime.toLocalDate() to listOf(photo))

        viewModel.devicePhotosUiState.test {
            var uiState: UiState = awaitItem()
            assertThat(uiState.isRefreshing).isEqualTo(true)

            photoRepository.emitAllPhotosLocalData(localPhotos)
            uiState = awaitItem()
            assertThat(uiState.isRefreshing).isEqualTo(false)
            assertThat(uiState.dateGroupedPhotos).isEqualTo(dateGroupedPhotos)

        }
        verify(exactly = 1) { photoRepository.getAllPhotosLocal(deviceId) }
        coVerify(exactly = 1) { photoRepository.updateAllPhotosRemote(any()) }
    }

    @Test
    fun `event flow emits ShowError when repository thrown exception`() = runTest {

        photoRepository.updateAllPhotosReturnsException = true

        viewModel.eventFlow.test {
            viewModel.devicePhotosUiState.first()
            assertThat(awaitItem()).isInstanceOf(DevicePhotosScreenViewModel.Event.ShowError::class.java)
        }
        coVerify(exactly = 1) { photoRepository.updateAllPhotosRemote(any()) }
    }
}
