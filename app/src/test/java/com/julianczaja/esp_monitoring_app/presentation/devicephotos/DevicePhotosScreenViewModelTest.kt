package com.julianczaja.esp_monitoring_app.presentation.devicephotos

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.testing.invoke
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.julianczaja.esp_monitoring_app.MainDispatcherRule
import com.julianczaja.esp_monitoring_app.data.NetworkManager
import com.julianczaja.esp_monitoring_app.data.repository.FakePhotoRepositoryImpl
import com.julianczaja.esp_monitoring_app.domain.TimelapseCreator
import com.julianczaja.esp_monitoring_app.domain.model.Photo
import com.julianczaja.esp_monitoring_app.domain.model.PhotosFilterMode
import com.julianczaja.esp_monitoring_app.domain.model.Selectable
import com.julianczaja.esp_monitoring_app.domain.usecase.SelectOrDeselectAllPhotosByDateUseCase
import com.julianczaja.esp_monitoring_app.navigation.DeviceScreen
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
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

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
    private lateinit var timelapseCreator: TimelapseCreator
    private lateinit var viewModel: DevicePhotosScreenViewModel

    private val deviceId = 1L

    @Before
    fun setup() {
        savedStateHandle = SavedStateHandle(route = DeviceScreen(deviceId))
        photoRepository = spyk(FakePhotoRepositoryImpl())
        photoRepository.tryEmitAllSavedPhotosData(Result.success(emptyList()))
        networkManager = mockk()
        timelapseCreator = mockk()
        every { networkManager.isOnline } returns flow { delay(1000); emit(true) }
        viewModel = DevicePhotosScreenViewModel(
            savedStateHandle = savedStateHandle,
            photoRepository = photoRepository,
            networkManager = networkManager,
            timelapseCreator = timelapseCreator,
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
        val remotePhotos = listOf(Photo.mock())
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
        val photo = Photo.mock(deviceId = deviceId)
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
        photoRepository.emitAllPhotosLocalData(emptyList())
        photoRepository.emitAllSavedPhotosData(Result.failure(Exception("error")))

        viewModel.eventFlow.test {
            viewModel.devicePhotosUiState.first()
            assertThat(awaitItem()).isInstanceOf(DevicePhotosScreenViewModel.Event.ShowError::class.java)
        }
    }

    @Test
    fun `saved photos list is empty when readAllSavedPhotos thrown exception`() = runTest {
        photoRepository.emitAllPhotosLocalData(emptyList())
        photoRepository.emitAllSavedPhotosData(Result.failure(Exception("error")))

        viewModel.devicePhotosUiState.test {
            with(awaitItem()) { assertThat(dateGroupedSelectablePhotos).isEmpty() }
        }
    }

    @Test
    fun `should select all photos if only one photo with given date is selected`() = runTest {
        val localDate = LocalDate.of(2024, 6, 6)
        val remotePhotos = listOf(
            Photo.mock(dateTime = localDate.atTime(10, 0)),
            Photo.mock(dateTime = localDate.atTime(11, 0)),
            Photo.mock(dateTime = localDate.atTime(12, 0)),
        )

        photoRepository.emitAllPhotosLocalData(remotePhotos)
        photoRepository.emitAllSavedPhotosData(Result.success(emptyList()))

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
            Photo.mock(dateTime = localDate.atTime(10, 0)),
            Photo.mock(dateTime = localDate.atTime(11, 0)),
            Photo.mock(dateTime = localDate.atTime(12, 0)),
        )

        photoRepository.emitAllPhotosLocalData(remotePhotos)

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
            Photo.mock(dateTime = localDate.atTime(10, 0)),
            Photo.mock(dateTime = localDate.atTime(11, 0)),
            Photo.mock(dateTime = localDate.atTime(12, 0)),
        )

        photoRepository.emitAllPhotosLocalData(remotePhotos)

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
        val remotePhotos = listOf(
            Photo.mock(dateTime = localDate.atTime(10, 0), fileName = "fileName1"),
            Photo.mock(dateTime = localDate.atTime(11, 0), fileName = "fileName2")
        )
        val savedPhotos = listOf(
            Photo.mock(dateTime = localDate.atTime(20, 0), fileName = "fileName3"),
            Photo.mock(dateTime = localDate.atTime(21, 0), fileName = "fileName4")
        )
        val expectedPhotos = listOf(
            Selectable(savedPhotos[1], false),
            Selectable(savedPhotos[0], false),
            Selectable(remotePhotos[1], false),
            Selectable(remotePhotos[0], false)
        )

        photoRepository.emitAllPhotosLocalData(remotePhotos)
        photoRepository.emitAllSavedPhotosData(Result.success(savedPhotos))

        viewModel.devicePhotosUiState.test {
            delay(5000L)
            assertThat(expectMostRecentItem().dateGroupedSelectablePhotos[localDate]).isEqualTo(expectedPhotos)
        }
    }

    @Test
    fun `all photos should contain sorted server and saved photos when filter mode is set to all`() = runTest {
        val localDate = LocalDate.of(2024, 6, 6)
        val remotePhotos = listOf(
            Photo.mock(dateTime = localDate.atTime(10, 0), fileName = "fileName1"),
            Photo.mock(dateTime = localDate.atTime(11, 0), fileName = "fileName2")
        )
        val savedPhotos = listOf(
            Photo.mock(dateTime = localDate.atTime(20, 0), fileName = "fileName3"),
            Photo.mock(dateTime = localDate.atTime(21, 0), fileName = "fileName4")
        )
        val expectedPhotos = listOf(
            Selectable(savedPhotos[1], false),
            Selectable(savedPhotos[0], false),
            Selectable(remotePhotos[1], false),
            Selectable(remotePhotos[0], false)
        )

        photoRepository.emitAllPhotosLocalData(remotePhotos)
        photoRepository.emitAllSavedPhotosData(Result.success(savedPhotos))

        viewModel.devicePhotosUiState.test {
            delay(5000L)
            with(expectMostRecentItem()) {
                assertThat(filterMode).isEqualTo(PhotosFilterMode.ALL)
                assertThat(dateGroupedSelectablePhotos[localDate]).isEqualTo(expectedPhotos)
            }
        }
    }

    @Test
    fun `all photos should contain only sorted saved photos when filter mode is set to saved only`() = runTest {
        val localDate = LocalDate.of(2024, 6, 6)
        val remotePhotos = listOf(
            Photo.mock(dateTime = localDate.atTime(10, 0), fileName = "fileName1"),
            Photo.mock(dateTime = localDate.atTime(11, 0), fileName = "fileName2")
        )
        val savedPhotos = listOf(
            Photo.mock(dateTime = localDate.atTime(20, 0), fileName = "fileName3"),
            Photo.mock(dateTime = localDate.atTime(21, 0), fileName = "fileName4")
        )
        val expectedPhotos = listOf(
            Selectable(savedPhotos[1], false),
            Selectable(savedPhotos[0], false)
        )

        photoRepository.emitAllPhotosLocalData(remotePhotos)
        photoRepository.emitAllSavedPhotosData(Result.success(savedPhotos))
        viewModel.onFilterModeClicked()

        viewModel.devicePhotosUiState.test {
            delay(5000L)
            with(expectMostRecentItem()) {
                assertThat(filterMode).isEqualTo(PhotosFilterMode.SAVED_ONLY)
                assertThat(dateGroupedSelectablePhotos[localDate]).isEqualTo(expectedPhotos)
            }
        }
    }

    @Test
    fun `all photos should contain only sorted server photos when filter mode is set to server only`() = runTest {
        val localDate = LocalDate.of(2024, 6, 6)
        val remotePhotos = listOf(
            Photo.mock(dateTime = localDate.atTime(10, 0), fileName = "fileName1"),
            Photo.mock(dateTime = localDate.atTime(11, 0), fileName = "fileName2")
        )
        val savedPhotos = listOf(
            Photo.mock(dateTime = localDate.atTime(20, 0), fileName = "fileName3"),
            Photo.mock(dateTime = localDate.atTime(21, 0), fileName = "fileName4")
        )
        val expectedPhotos = listOf(
            Selectable(remotePhotos[1], false),
            Selectable(remotePhotos[0], false)
        )

        photoRepository.emitAllPhotosLocalData(remotePhotos)
        photoRepository.emitAllSavedPhotosData(Result.success(savedPhotos))

        viewModel.onFilterModeClicked()
        viewModel.onFilterModeClicked()

        viewModel.devicePhotosUiState.test {
            delay(5000L)
            with(expectMostRecentItem()) {
                assertThat(filterMode).isEqualTo(PhotosFilterMode.SERVER_ONLY)
                assertThat(dateGroupedSelectablePhotos[localDate]).isEqualTo(expectedPhotos)
            }
        }
    }
}
