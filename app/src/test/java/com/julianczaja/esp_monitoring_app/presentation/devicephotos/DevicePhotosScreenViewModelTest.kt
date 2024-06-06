package com.julianczaja.esp_monitoring_app.presentation.devicephotos

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.julianczaja.esp_monitoring_app.MainDispatcherRule
import com.julianczaja.esp_monitoring_app.data.NetworkManager
import com.julianczaja.esp_monitoring_app.data.repository.FakePhotoRepositoryImpl
import com.julianczaja.esp_monitoring_app.domain.model.Photo
import com.julianczaja.esp_monitoring_app.domain.model.SelectablePhoto
import com.julianczaja.esp_monitoring_app.domain.usecase.SelectOrDeselectAllPhotosByDateUseCase
import com.julianczaja.esp_monitoring_app.navigation.DeviceIdArgs
import com.julianczaja.esp_monitoring_app.presentation.devicephotos.DevicePhotosScreenViewModel.UiState
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
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
            selectOrDeselectAllPhotosByDateUseCase = SelectOrDeselectAllPhotosByDateUseCase(),
            ioDispatcher = dispatcherRule.testDispatcher
        )
    }

    @Test
    fun `isRefreshing should be false on start`() = runTest {
        viewModel.devicePhotosUiState.test {
            val uiState = awaitItem()
            assertThat(uiState.isLoading).isEqualTo(false)
        }
        coVerify(exactly = 0) { photoRepository.updateAllPhotosRemote(deviceId) }
    }

    @Test
    fun `isLoading should be true after updatePhotos has been called`() = runTest {
        val remotePhotos = listOf(Photo(1L, LocalDateTime.MAX, "", "", "", ""))
        photoRepository.remotePhotos = remotePhotos

        viewModel.devicePhotosUiState.test {
            var uiState = awaitItem()
            assertThat(uiState.isLoading).isEqualTo(false)
            viewModel.updatePhotos()
            uiState = awaitItem()
            assertThat(uiState.isLoading).isEqualTo(true)
            uiState = awaitItem()
            assertThat(uiState.isLoading).isEqualTo(false)
            cancelAndIgnoreRemainingEvents()
        }
        coVerify(exactly = 1) { photoRepository.updateAllPhotosRemote(deviceId) }
    }

    @Test
    fun `local photos should be mapped to dateGroupedSelectablePhotos`() = runTest {
        val photo = Photo(
            deviceId = deviceId,
            dateTime = LocalDateTime.of(2022, 1, 1, 1, 1, 1),
            fileName = "fileName",
            size = "100x500",
            url = "url",
            thumbnailUrl = "thumbnailUrl"
        )
        val localPhotos = listOf(photo)
        val dateGroupedSelectablePhotos = mapOf(
            photo.dateTime.toLocalDate() to listOf(SelectablePhoto(photo, false))
        )

        viewModel.devicePhotosUiState.test {
            var uiState: UiState = awaitItem()
            assertThat(uiState.dateGroupedSelectablePhotos).isEqualTo(emptyMap<LocalDate, List<SelectablePhoto>>())
            photoRepository.emitAllPhotosLocalData(localPhotos)
            uiState = awaitItem()
            assertThat(uiState.dateGroupedSelectablePhotos).isEqualTo(dateGroupedSelectablePhotos)
        }
        verify(exactly = 1) { photoRepository.getAllPhotosLocal(deviceId) }
    }

    @Test
    fun `event flow emits ShowError when repository thrown exception`() = runTest {
        photoRepository.updateAllPhotosReturnsException = true

        viewModel.eventFlow.test {
            viewModel.updatePhotos()
            assertThat(awaitItem()).isInstanceOf(DevicePhotosScreenViewModel.Event.ShowError::class.java)
        }
        coVerify(exactly = 1) { photoRepository.updateAllPhotosRemote(deviceId) }
    }

    @Test
    fun `should select all photos if only one photos with given date is selected`() = runTest {
        val localDate = LocalDate.of(2024, 6, 6)
        val remotePhotos = listOf(
            Photo(1L, localDate.atTime(10, 0), "", "", "", ""),
            Photo(2L, localDate.atTime(11, 0), "", "", "", ""),
            Photo(3L, localDate.atTime(12, 0), "", "", "", ""),
        )
        photoRepository.remotePhotos = remotePhotos

        viewModel.devicePhotosUiState.test {
            assertThat(awaitItem().dateGroupedSelectablePhotos).isEmpty()
            viewModel.updatePhotos()

            var uiState = awaitItem()
            val selectablePhotos = uiState.dateGroupedSelectablePhotos[localDate].orEmpty()
            assertThat(selectablePhotos).hasSize(3)
            viewModel.onPhotoLongClick(selectablePhotos.first())
            viewModel.onSelectDeselectAllClicked(localDate)
            uiState = expectMostRecentItem()
            assertThat(uiState.dateGroupedSelectablePhotos[localDate]?.all { it.isSelected }).isTrue()
        }
    }

    @Test
    fun `should select all photos if no photo with given date is selected`() = runTest {
        val localDate = LocalDate.of(2024, 6, 6)
        val remotePhotos = listOf(
            Photo(1L, localDate.atTime(10, 0), "", "", "", ""),
            Photo(2L, localDate.atTime(11, 0), "", "", "", ""),
            Photo(3L, localDate.atTime(12, 0), "", "", "", ""),
        )
        photoRepository.remotePhotos = remotePhotos

        viewModel.devicePhotosUiState.test {
            assertThat(awaitItem().dateGroupedSelectablePhotos).isEmpty()
            viewModel.updatePhotos()

            var uiState = awaitItem()
            val selectablePhotos = uiState.dateGroupedSelectablePhotos[localDate].orEmpty()
            assertThat(selectablePhotos).hasSize(3)
            viewModel.onSelectDeselectAllClicked(localDate)
            uiState = expectMostRecentItem()
            assertThat(uiState.dateGroupedSelectablePhotos[localDate]?.all { it.isSelected }).isTrue()
        }
    }

    @Test
    fun `should deselect all photos if all photos with given date are selected`() = runTest {
        val localDate = LocalDate.of(2024, 6, 6)
        val remotePhotos = listOf(
            Photo(1L, localDate.atTime(10, 0), "", "", "", ""),
            Photo(2L, localDate.atTime(11, 0), "", "", "", ""),
            Photo(3L, localDate.atTime(12, 0), "", "", "", ""),
        )
        photoRepository.remotePhotos = remotePhotos

        viewModel.devicePhotosUiState.test {
            assertThat(awaitItem().dateGroupedSelectablePhotos).isEmpty()
            viewModel.updatePhotos()

            var uiState = awaitItem()
            val selectablePhotos = uiState.dateGroupedSelectablePhotos[localDate].orEmpty()
            assertThat(selectablePhotos).hasSize(3)
            with(viewModel) {
                onPhotoLongClick(selectablePhotos[0])
                onPhotoLongClick(selectablePhotos[1])
                onPhotoLongClick(selectablePhotos[2])
                onSelectDeselectAllClicked(localDate)
            }
            uiState = expectMostRecentItem()
            assertThat(uiState.dateGroupedSelectablePhotos[localDate]?.all { !it.isSelected }).isTrue()
        }
    }
}
