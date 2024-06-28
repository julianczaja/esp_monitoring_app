package com.julianczaja.esp_monitoring_app.presentation.devicephotos

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.testing.invoke
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.julianczaja.esp_monitoring_app.MainDispatcherRule
import com.julianczaja.esp_monitoring_app.data.NetworkManager
import com.julianczaja.esp_monitoring_app.data.repository.FakePhotoRepositoryImpl
import com.julianczaja.esp_monitoring_app.domain.model.Photo
import com.julianczaja.esp_monitoring_app.domain.model.Selectable
import com.julianczaja.esp_monitoring_app.domain.usecase.SelectOrDeselectAllPhotosByDateUseCase
import com.julianczaja.esp_monitoring_app.navigation.DeviceScreen
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
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * These tests use Robolectric because the subject under test (the ViewModel) uses
 * `SavedStateHandle.toRoute` which has a dependency on `android.os.Bundle`.
 *
 * TODO: Remove Robolectric if/when AndroidX Navigation API is updated to remove Android dependency.
 *  See b/340966212.
 */
@RunWith(RobolectricTestRunner::class)
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
        savedStateHandle = SavedStateHandle(route = DeviceScreen(deviceId))
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
            assertThat(awaitItem().isLoading).isEqualTo(false)
        }
        coVerify(exactly = 0) { photoRepository.updateAllPhotosRemote(deviceId) }
    }

    @Test
    fun `isLoading should be true after updatePhotos has been called`() = runTest {
        val remotePhotos = listOf(Photo(1L, LocalDateTime.MAX, "", "", "", ""))
        photoRepository.remotePhotos = remotePhotos

        viewModel.devicePhotosUiState.test {
            assertThat(awaitItem().isLoading).isEqualTo(false)
            viewModel.updatePhotos()
            assertThat(awaitItem().isLoading).isEqualTo(true)
            assertThat(awaitItem().isLoading).isEqualTo(false)
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
        val expectedDateGroupedSelectablePhotos = mapOf(
            photo.dateTime.toLocalDate() to listOf(Selectable(photo, false))
        )

        viewModel.devicePhotosUiState.test {
            with(awaitItem()) { assertThat(dateGroupedSelectablePhotos).isEmpty() }
            photoRepository.emitAllPhotosLocalData(localPhotos)
            with(awaitItem()) { assertThat(dateGroupedSelectablePhotos).isEqualTo(expectedDateGroupedSelectablePhotos) }
        }
        verify(exactly = 1) { photoRepository.getAllPhotosLocal(deviceId) }
    }

    @Test
    fun `event flow emits ShowError when updateAllPhotos thrown exception`() = runTest {
        photoRepository.updateAllPhotosReturnsException = true

        viewModel.eventFlow.test {
            viewModel.updatePhotos()
            assertThat(awaitItem()).isInstanceOf(DevicePhotosScreenViewModel.Event.ShowError::class.java)
        }
        coVerify(exactly = 1) { photoRepository.updateAllPhotosRemote(deviceId) }
    }

    @Test
    fun `event flow emits ShowError when readAllSavedPhotos thrown exception`() = runTest {
        photoRepository.readAllSavedPhotosReturnsException = true

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
        viewModel.updatePhotos()

        viewModel.devicePhotosUiState.test {
            delay(5000L)

            val photos = expectMostRecentItem().dateGroupedSelectablePhotos[localDate].orEmpty()
            assertThat(photos).hasSize(3)

            viewModel.onPhotoLongClick(photos.first())
            viewModel.onSelectDeselectAllClicked(localDate)
            assertThat(expectMostRecentItem().dateGroupedSelectablePhotos[localDate]?.all { it.isSelected }).isTrue()
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
        viewModel.updatePhotos()

        viewModel.devicePhotosUiState.test {
            delay(5000L)
            assertThat(expectMostRecentItem().dateGroupedSelectablePhotos[localDate]).hasSize(3)

            viewModel.onSelectDeselectAllClicked(localDate)
            assertThat(expectMostRecentItem().dateGroupedSelectablePhotos[localDate]?.all { it.isSelected }).isTrue()
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
        viewModel.updatePhotos()

        viewModel.devicePhotosUiState.test {
            delay(5000L)

            val selectablePhotos = expectMostRecentItem().dateGroupedSelectablePhotos[localDate].orEmpty()
            assertThat(selectablePhotos).hasSize(3)

            with(viewModel) {
                onPhotoLongClick(selectablePhotos[0])
                onPhotoClick(selectablePhotos[1])
                onPhotoClick(selectablePhotos[2])
                onSelectDeselectAllClicked(localDate)
            }
            assertThat(expectMostRecentItem().dateGroupedSelectablePhotos[localDate]?.all { !it.isSelected }).isTrue()
        }
    }

    @Test
    fun `all photos should contain sorted local and saved photos`() = runTest {
        val localDate = LocalDate.of(2024, 6, 6)
        photoRepository.remotePhotos = listOf(
            Photo(1L, localDate.atTime(10, 0), "10", "", "", ""),
            Photo(1L, localDate.atTime(11, 0), "11", "", "", "")
        )
        photoRepository.savedPhotos = listOf(
            Photo(1L, localDate.atTime(20, 0), "20", "", "", ""),
            Photo(1L, localDate.atTime(21, 0), "21", "", "", "")
        )
        val expectedPhotos = listOf(
            Selectable(photoRepository.savedPhotos[1], false),
            Selectable(photoRepository.savedPhotos[0], false),
            Selectable(photoRepository.remotePhotos[1], false),
            Selectable(photoRepository.remotePhotos[0], false)
        )
        viewModel.updatePhotos()

        viewModel.devicePhotosUiState.test {
            delay(5000L)
            assertThat(expectMostRecentItem().dateGroupedSelectablePhotos[localDate]).isEqualTo(expectedPhotos)
        }
    }

    @Test
    fun `all photos should contain sorted local and saved photos without duplicates based on file names`() = runTest {
        val localDate = LocalDate.of(2024, 6, 6)
        photoRepository.remotePhotos = listOf(
            Photo(1L, localDate.atTime(10, 0), "10", "", "", ""),
            Photo(1L, localDate.atTime(11, 0), "11", "", "", "")
        )
        photoRepository.savedPhotos = listOf(
            Photo(1L, localDate.atTime(10, 0), "10", "", "", ""),
            Photo(1L, localDate.atTime(21, 0), "21", "", "", "")
        )
        val expectedPhotos = listOf(
            Selectable(photoRepository.savedPhotos[1], false),
            Selectable(photoRepository.remotePhotos[1], false),
            Selectable(photoRepository.remotePhotos[0], false)
        )

        viewModel.updatePhotos()

        viewModel.devicePhotosUiState.test {
            delay(5000L)
            assertThat(expectMostRecentItem().dateGroupedSelectablePhotos[localDate]).isEqualTo(expectedPhotos)
        }
    }

    @Test
    fun `all photos should contain only sorted saved photos when saved only filter is enabled`() = runTest {
        val localDate = LocalDate.of(2024, 6, 6)
        photoRepository.remotePhotos = listOf(
            Photo(1L, localDate.atTime(10, 0), "10", "", "", ""),
            Photo(1L, localDate.atTime(11, 0), "11", "", "", "")
        )
        photoRepository.savedPhotos = listOf(
            Photo(1L, localDate.atTime(10, 0), "20", "", "", ""),
            Photo(1L, localDate.atTime(21, 0), "21", "", "", "")
        )
        val expectedPhotos = listOf(
            Selectable(photoRepository.savedPhotos[1], false),
            Selectable(photoRepository.savedPhotos[0], false)
        )

        viewModel.updatePhotos()
        viewModel.onFilterSavedOnlyClicked(true)

        viewModel.devicePhotosUiState.test {
            delay(5000L)
            assertThat(expectMostRecentItem().dateGroupedSelectablePhotos[localDate]).isEqualTo(expectedPhotos)
        }
    }
}
