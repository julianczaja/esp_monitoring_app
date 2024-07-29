package com.julianczaja.esp_monitoring_app.presentation.devicephotos

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.testing.invoke
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.julianczaja.esp_monitoring_app.MainDispatcherRule
import com.julianczaja.esp_monitoring_app.data.NetworkManager
import com.julianczaja.esp_monitoring_app.data.repository.FakeDayRepository
import com.julianczaja.esp_monitoring_app.data.repository.FakePhotoRepositoryImpl
import com.julianczaja.esp_monitoring_app.domain.TimelapseCreator
import com.julianczaja.esp_monitoring_app.domain.model.Day
import com.julianczaja.esp_monitoring_app.domain.model.Photo
import com.julianczaja.esp_monitoring_app.domain.model.PhotosFilterMode
import com.julianczaja.esp_monitoring_app.domain.model.Selectable
import com.julianczaja.esp_monitoring_app.domain.usecase.SelectOrDeselectAllPhotosByDateUseCase
import com.julianczaja.esp_monitoring_app.navigation.DeviceScreen
import io.mockk.clearMocks
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
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
    private lateinit var dayRepository: FakeDayRepository
    private lateinit var networkManager: NetworkManager
    private lateinit var timelapseCreator: TimelapseCreator
    private lateinit var viewModel: DevicePhotosScreenViewModel

    private val deviceId = 1L

    @Before
    fun setup() {
        savedStateHandle = SavedStateHandle(route = DeviceScreen(deviceId))
        photoRepository = spyk(FakePhotoRepositoryImpl())
        photoRepository.tryEmitAllSavedPhotosData(Result.success(emptyList()))
        dayRepository = spyk(FakeDayRepository())
        networkManager = mockk()
        timelapseCreator = mockk()
        every { networkManager.isOnline } returns flow { delay(1000); emit(true) }
        viewModel = DevicePhotosScreenViewModel(
            savedStateHandle = savedStateHandle,
            photoRepository = photoRepository,
            dayRepository = dayRepository,
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
        coVerify(exactly = 0) { dayRepository.updateDeviceDaysRemote(deviceId) }
    }

    @Test
    fun `isLoading should be true after init has been called`() = runTest {
        val remotePhotos = listOf(Photo.mock(deviceId))
        val remoteDays = listOf(Day(deviceId, remotePhotos[0].dateTime.toLocalDate()))

        dayRepository.remoteDays = remoteDays
        photoRepository.remotePhotos = remotePhotos

        viewModel.devicePhotosUiState.test {
            assertThat(awaitItem().isLoading).isEqualTo(false)
            viewModel.init()
            assertThat(awaitItem().isLoading).isEqualTo(true)
            assertThat(awaitItem().isLoading).isEqualTo(false)
            cancelAndIgnoreRemainingEvents()
        }
        coVerify(exactly = 1) { dayRepository.updateDeviceDaysRemote(deviceId) }
        coVerify(exactly = 1) { photoRepository.updateAllPhotosByDayRemote(remoteDays[0]) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `on data refresh only days and selected day photos are fetched`() = runTest {
        val day1 = Day(deviceId, LocalDate.of(2024, 7, 29))
        val day2 = Day(deviceId, LocalDate.of(2024, 7, 28))
        val remoteDays = listOf(day1, day2)

        val remotePhotos = listOf(
            Photo.mock(deviceId, dateTime = day1.date.atTime(10, 15)),
            Photo.mock(deviceId, dateTime = day1.date.atTime(10, 16)),
            Photo.mock(deviceId, dateTime = day2.date.atTime(12, 10)),
        )

        dayRepository.remoteDays = remoteDays
        photoRepository.remotePhotos = remotePhotos
        viewModel.init()
        advanceUntilIdle()

        coVerify(exactly = 1) { dayRepository.updateDeviceDaysRemote(deviceId) }
        coVerify(exactly = 1) { photoRepository.updateAllPhotosByDayRemote(day1) }
        coVerify(exactly = 0) { photoRepository.updateAllPhotosByDayRemote(day2) }

        clearMocks(dayRepository, recordedCalls = true)
        clearMocks(photoRepository, recordedCalls = true)

        viewModel.refreshData()
        advanceUntilIdle()

        coVerify(exactly = 1) { dayRepository.updateDeviceDaysRemote(deviceId) }
        coVerify(exactly = 1) { photoRepository.updateAllPhotosByDayRemote(day1) }
        coVerify(exactly = 0) { photoRepository.updateAllPhotosByDayRemote(day2) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `on day change only selected day photos are fetched`() = runTest {
        val day1 = Day(deviceId, LocalDate.of(2024, 7, 29))
        val day2 = Day(deviceId, LocalDate.of(2024, 7, 28))
        val remoteDays = listOf(day1, day2)

        val remotePhotos = listOf(
            Photo.mock(deviceId, dateTime = day1.date.atTime(10, 15)),
            Photo.mock(deviceId, dateTime = day1.date.atTime(10, 16)),
            Photo.mock(deviceId, dateTime = day2.date.atTime(12, 10)),
        )

        dayRepository.remoteDays = remoteDays
        photoRepository.remotePhotos = remotePhotos
        viewModel.init()
        advanceUntilIdle()

        coVerify(exactly = 1) { dayRepository.updateDeviceDaysRemote(deviceId) }
        coVerify(exactly = 1) { photoRepository.updateAllPhotosByDayRemote(day1) }
        coVerify(exactly = 0) { photoRepository.updateAllPhotosByDayRemote(day2) }

        clearMocks(dayRepository, recordedCalls = true)
        clearMocks(photoRepository, recordedCalls = true)

        viewModel.onDayChanged(day2)
        advanceUntilIdle()

        coVerify(exactly = 0) { dayRepository.updateDeviceDaysRemote(deviceId) }
        coVerify(exactly = 0) { photoRepository.updateAllPhotosByDayRemote(day1) }
        coVerify(exactly = 1) { photoRepository.updateAllPhotosByDayRemote(day2) }
    }

    @Test
    fun `local photos should be mapped to dayGroupedSelectablePhotos`() = runTest {
        val day1 = Day(deviceId, LocalDate.of(2024, 7, 28))
        val day2 = Day(deviceId, LocalDate.of(2024, 7, 29))
        val localDays = listOf(day1, day2)

        val localPhotos = listOf(
            Photo.mock(deviceId, dateTime = day1.date.atTime(10, 15)),
            Photo.mock(deviceId, dateTime = day1.date.atTime(10, 16)),
            Photo.mock(deviceId, dateTime = day2.date.atTime(12, 10)),
        )

        val expectedDateGroupedSelectablePhotos = mapOf(
            day1 to listOf(Selectable(localPhotos[1], false), Selectable(localPhotos[0], false)),
            day2 to listOf(Selectable(localPhotos[2], false))
        )

        viewModel.devicePhotosUiState.test {
            with(awaitItem()) { assertThat(dayGroupedSelectablePhotos).isEmpty() }
            dayRepository.emitAllDaysLocalData(localDays)
            photoRepository.emitAllPhotosLocalData(localPhotos)
            with(awaitItem()) { assertThat(dayGroupedSelectablePhotos).isEqualTo(expectedDateGroupedSelectablePhotos) }
        }
        verify(exactly = 1) { dayRepository.getDeviceDaysLocal(deviceId) }
        verify(exactly = 1) { photoRepository.getAllPhotosByDayLocal(day1) }
        verify(exactly = 1) { photoRepository.getAllPhotosByDayLocal(day2) }
    }

    @Test
    fun `event flow emits ShowError when updateDeviceDays thrown exception`() = runTest {
        dayRepository.updateAllDaysReturnsException = true

        viewModel.eventFlow.test {
            viewModel.init()
            assertThat(awaitItem()).isInstanceOf(DevicePhotosScreenViewModel.Event.ShowError::class.java)
        }
        coVerify(exactly = 1) { dayRepository.updateDeviceDaysRemote(deviceId) }
    }

    @Test
    fun `event flow emits ShowError when updateAllPhotosByDay thrown exception`() = runTest {
        val day = Day(deviceId, LocalDate.of(2024, 7, 29))
        dayRepository.remoteDays = listOf(day)
        photoRepository.updateAllPhotosByDayRemote = true

        viewModel.eventFlow.test {
            viewModel.init()
            assertThat(awaitItem()).isInstanceOf(DevicePhotosScreenViewModel.Event.ShowError::class.java)
        }
        coVerify(exactly = 1) { photoRepository.updateAllPhotosByDayRemote(day) }
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
    fun `should select all photos if only one photo with given date is selected`() = runTest {
        val localDate = LocalDate.of(2024, 6, 6)
        val remoteDays = listOf(Day(deviceId, localDate))
        val remotePhotos = listOf(
            Photo.mock(deviceId, dateTime = localDate.atTime(10, 0)),
            Photo.mock(deviceId, dateTime = localDate.atTime(11, 0)),
            Photo.mock(deviceId, dateTime = localDate.atTime(12, 0)),
        )

        dayRepository.remoteDays = remoteDays
        photoRepository.remotePhotos = remotePhotos
        viewModel.init()

        viewModel.devicePhotosUiState.test {
            delay(5000L)

            var photos = expectMostRecentItem().dayGroupedSelectablePhotos[remoteDays[0]].orEmpty()
            println(photos)
            assertThat(photos).hasSize(3)

            viewModel.onPhotoLongClick(photos.first())
            viewModel.selectDeselectAllPhotos()

            photos = expectMostRecentItem().dayGroupedSelectablePhotos[remoteDays[0]].orEmpty()
            println(photos)
            assertThat(photos.all { it.isSelected }).isTrue()
        }
    }

    @Test
    fun `should deselect all photos if all photos with given date are selected`() = runTest {
        val localDate = LocalDate.of(2024, 6, 6)
        val remoteDays = listOf(Day(deviceId, localDate))
        val remotePhotos = listOf(
            Photo.mock(dateTime = localDate.atTime(10, 0)),
            Photo.mock(dateTime = localDate.atTime(11, 0)),
            Photo.mock(dateTime = localDate.atTime(12, 0)),
        )

        dayRepository.remoteDays = remoteDays
        photoRepository.remotePhotos = remotePhotos
        viewModel.init()

        viewModel.devicePhotosUiState.test {
            delay(5000L)

            var photos = expectMostRecentItem().dayGroupedSelectablePhotos[remoteDays[0]].orEmpty()
            assertThat(photos).hasSize(3)

            with(viewModel) {
                onPhotoLongClick(photos[0])
                onPhotoClick(photos[1])
                onPhotoClick(photos[2])
                selectDeselectAllPhotos()
            }

            photos = expectMostRecentItem().dayGroupedSelectablePhotos[remoteDays[0]].orEmpty()
            assertThat(photos.all { !it.isSelected }).isTrue()
        }
    }

    @Test
    fun `all photos should contain sorted local and saved photos`() = runTest {
        val localDate = LocalDate.of(2024, 6, 6)
        val remoteDays = listOf(Day(deviceId, localDate))
        val remotePhotos = listOf(
            Photo.mock(deviceId, dateTime = localDate.atTime(10, 0), fileName = "fileName1"),
            Photo.mock(deviceId, dateTime = localDate.atTime(11, 0), fileName = "fileName2")
        )
        val savedPhotos = listOf(
            Photo.mock(deviceId, dateTime = localDate.atTime(20, 0), fileName = "fileName3"),
            Photo.mock(deviceId, dateTime = localDate.atTime(21, 0), fileName = "fileName4")
        )
        val expectedPhotos = listOf(
            Selectable(savedPhotos[1], false),
            Selectable(savedPhotos[0], false),
            Selectable(remotePhotos[1], false),
            Selectable(remotePhotos[0], false)
        )

        dayRepository.remoteDays = remoteDays
        photoRepository.remotePhotos = remotePhotos
        photoRepository.emitAllSavedPhotosData(Result.success(savedPhotos))
        viewModel.init()

        viewModel.devicePhotosUiState.test {
            delay(5000L)
            expectMostRecentItem().dayGroupedSelectablePhotos[remoteDays[0]].let { photos ->
                println(photos)
                assertThat(photos).isEqualTo(expectedPhotos)
            }
        }
    }

    @Test
    fun `all photos should contain sorted server and saved photos when filter mode is set to all`() = runTest {
        val localDate = LocalDate.of(2024, 6, 6)
        val remoteDays = listOf(Day(deviceId, localDate))
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

        dayRepository.remoteDays = remoteDays
        photoRepository.remotePhotos = remotePhotos
        photoRepository.emitAllSavedPhotosData(Result.success(savedPhotos))
        viewModel.init()

        viewModel.devicePhotosUiState.test {
            delay(5000L)
            with(expectMostRecentItem()) {
                assertThat(filterMode).isEqualTo(PhotosFilterMode.ALL)
                assertThat(dayGroupedSelectablePhotos[remoteDays[0]]).isEqualTo(expectedPhotos)
            }
        }
    }

    @Test
    fun `all photos should contain only sorted saved photos when filter mode is set to saved only`() = runTest {
        val localDate = LocalDate.of(2024, 6, 6)
        val remoteDays = listOf(Day(deviceId, localDate))
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

        dayRepository.remoteDays = remoteDays
        photoRepository.remotePhotos = remotePhotos
        photoRepository.emitAllSavedPhotosData(Result.success(savedPhotos))
        viewModel.init()
        viewModel.onFilterModeClicked()

        viewModel.devicePhotosUiState.test {
            delay(5000L)
            with(expectMostRecentItem()) {
                assertThat(filterMode).isEqualTo(PhotosFilterMode.SAVED_ONLY)
                assertThat(dayGroupedSelectablePhotos[remoteDays[0]]).isEqualTo(expectedPhotos)
            }
        }
    }

    @Test
    fun `all photos should contain only sorted server photos when filter mode is set to server only`() = runTest {
        val localDate = LocalDate.of(2024, 6, 6)
        val remoteDays = listOf(Day(deviceId, localDate))
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

        dayRepository.remoteDays = remoteDays
        photoRepository.remotePhotos = remotePhotos
        photoRepository.emitAllSavedPhotosData(Result.success(savedPhotos))
        viewModel.init()
        viewModel.onFilterModeClicked()
        viewModel.onFilterModeClicked()

        viewModel.devicePhotosUiState.test {
            delay(5000L)
            with(expectMostRecentItem()) {
                assertThat(filterMode).isEqualTo(PhotosFilterMode.SERVER_ONLY)
                assertThat(dayGroupedSelectablePhotos[remoteDays[0]]).isEqualTo(expectedPhotos)
            }
        }
    }
}
