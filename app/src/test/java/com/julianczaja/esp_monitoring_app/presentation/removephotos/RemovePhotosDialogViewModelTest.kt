package com.julianczaja.esp_monitoring_app.presentation.removephotos

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.testing.invoke
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.julianczaja.esp_monitoring_app.MainDispatcherRule
import com.julianczaja.esp_monitoring_app.data.repository.FakePhotoRepositoryImpl
import com.julianczaja.esp_monitoring_app.domain.model.Photo
import com.julianczaja.esp_monitoring_app.domain.repository.PhotoRepository
import com.julianczaja.esp_monitoring_app.navigation.RemovePhotosDialog
import com.julianczaja.esp_monitoring_app.navigation.parcelableCollectionType
import io.mockk.coVerify
import io.mockk.spyk
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.reflect.typeOf

/**
 * These tests use Robolectric because the subject under test (the ViewModel) uses
 * `SavedStateHandle.toRoute` which has a dependency on `android.os.Bundle`.
 *
 * TODO: Remove Robolectric if/when AndroidX Navigation API is updated to remove Android dependency.
 *  See b/340966212.
 */
@RunWith(RobolectricTestRunner::class)
class RemovePhotosDialogViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private fun getViewModel(
        photos: List<Photo>,
        photoRepository: PhotoRepository? = null
    ) = RemovePhotosDialogViewModel(
        savedStateHandle = SavedStateHandle(
            route = RemovePhotosDialog(photos),
            typeMap = mapOf(typeOf<List<Photo>>() to parcelableCollectionType<Photo>())
        ),
        photoRepository = photoRepository ?: FakePhotoRepositoryImpl(),
        ioDispatcher = dispatcherRule.testDispatcher
    )

    @Test
    fun `UI state is Confirm on start`() = runTest {
        val photos = listOf(Photo.mock(fileName = "fileName1"), Photo.mock(fileName = "fileName2"))
        val viewModel = getViewModel(photos)

        viewModel.uiState.test {
            assertThat(awaitItem()).isInstanceOf(RemovePhotosDialogViewModel.UiState.Confirm::class.java)
        }
    }

    @Test
    fun `shouldShowRemoveSaved and removeSaved are false when there is no saved photo in photos list`() = runTest {
        val photos = listOf(
            Photo.mock(fileName = "fileName1", isSaved = false),
            Photo.mock(fileName = "fileName2", isSaved = false)
        )
        val viewModel = getViewModel(photos)

        viewModel.uiState.test {
            with(awaitItem() as RemovePhotosDialogViewModel.UiState.Confirm) {
                assertThat(this.photos).isEqualTo(photos)
                assertThat(this.shouldShowRemoveSaved).isFalse()
                assertThat(this.removeSaved).isFalse()
            }
        }
    }

    @Test
    fun `shouldShowRemoveSaved is true and removeSaved false when there are saved photo in photos list`() = runTest {
        val photos = listOf(
            Photo.mock(fileName = "fileName1", isSaved = false),
            Photo.mock(fileName = "fileName2", isSaved = true)
        )
        val viewModel = getViewModel(photos)

        viewModel.uiState.test {
            with(awaitItem() as RemovePhotosDialogViewModel.UiState.Confirm) {
                assertThat(this.photos).isEqualTo(photos)
                assertThat(this.shouldShowRemoveSaved).isTrue()
                assertThat(this.removeSaved).isFalse()
            }
        }
    }

    @Test
    fun `shouldShowRemoveSaved is false and removeSaved true when all photos in list are saved`() = runTest {
        val photos = listOf(
            Photo.mock(fileName = "fileName1", isSaved = true),
            Photo.mock(fileName = "fileName2", isSaved = true)
        )
        val viewModel = getViewModel(photos)

        viewModel.uiState.test {
            with(awaitItem() as RemovePhotosDialogViewModel.UiState.Confirm) {
                assertThat(this.photos).isEqualTo(photos)
                assertThat(this.shouldShowRemoveSaved).isFalse()
                assertThat(this.removeSaved).isTrue()
            }
        }
    }

    @Test
    fun `UI state is Removing after remove photos success`() = runTest {
        val photos = listOf(Photo.mock(fileName = "fileName1"), Photo.mock(fileName = "fileName2"))
        val photoRepository = spyk(FakePhotoRepositoryImpl())
        val viewModel = getViewModel(photos, photoRepository)

        viewModel.uiState.test {
            assertThat(awaitItem()).isInstanceOf(RemovePhotosDialogViewModel.UiState.Confirm::class.java)
            viewModel.removePhotos()
            assertThat((awaitItem() as RemovePhotosDialogViewModel.UiState.Removing).progress).isEqualTo(0.0f)
            assertThat((awaitItem() as RemovePhotosDialogViewModel.UiState.Removing).progress).isEqualTo(0.5f)
        }
        coVerify(exactly = 2) { photoRepository.removePhotoByFileNameRemote(any()) }
        coVerify(exactly = 2) { photoRepository.removePhotoByFileNameLocal(any()) }
    }

    @Test
    fun `UI state is Error when exception thrown during photo removal`() = runTest {
        val photos = listOf(Photo.mock(fileName = "fileName1"), Photo.mock(fileName = "fileName2"))
        val photoRepository = spyk(
            FakePhotoRepositoryImpl().apply { removePhotoByFileNameRemoteReturnsException = true }
        )
        val viewModel = getViewModel(photos, photoRepository)

        viewModel.uiState.test {
            assertThat(awaitItem()).isInstanceOf(RemovePhotosDialogViewModel.UiState.Confirm::class.java)
            viewModel.removePhotos()
            assertThat((awaitItem() as RemovePhotosDialogViewModel.UiState.Removing).progress).isEqualTo(0.0f)
            assertThat((awaitItem() as RemovePhotosDialogViewModel.UiState.Removing).progress).isEqualTo(0.5f)
            assertThat((awaitItem() as RemovePhotosDialogViewModel.UiState.Error).results).hasSize(2)
        }
    }

    @Test
    fun `Local photos are not removed when exception thrown during remote photos removal`() = runTest {
        val photos = listOf(Photo.mock(fileName = "fileName1"), Photo.mock(fileName = "fileName2"))
        val photoRepository = spyk(
            FakePhotoRepositoryImpl().apply { removePhotoByFileNameRemoteReturnsException = true }
        )
        val viewModel = getViewModel(photos, photoRepository)

        viewModel.removePhotos()

        coVerify(exactly = 2) { photoRepository.removePhotoByFileNameRemote(any()) }
        coVerify(exactly = 0) { photoRepository.removePhotoByFileNameLocal(any()) }
    }

    @Test
    fun `Saved photos are not removed when checkbox is unchecked`() = runTest {
        val photos = listOf(
            Photo.mock(fileName = "fileName1", isSaved = true),
            Photo.mock(fileName = "fileName2", isSaved = true),
            Photo.mock(fileName = "fileName3", isSaved = false)
        )
        val photoRepository = spyk(FakePhotoRepositoryImpl()        )
        val viewModel = getViewModel(photos, photoRepository)

        viewModel.onRemoveSavedChanged(false)
        viewModel.removePhotos()

        coVerify(exactly = 0) { photoRepository.removeSavedPhotoFromExternalStorage(any()) }
        coVerify(exactly = 1) { photoRepository.removePhotoByFileNameRemote(any()) }
        coVerify(exactly = 1) { photoRepository.removePhotoByFileNameLocal(any()) }
    }

    @Test
    fun `Saved photos are removed when checkbox is unchecked`() = runTest {
        val photos = listOf(
            Photo.mock(fileName = "fileName1", isSaved = true),
            Photo.mock(fileName = "fileName2", isSaved = true),
            Photo.mock(fileName = "fileName3", isSaved = false)
        )
        val photoRepository = spyk(FakePhotoRepositoryImpl()        )
        val viewModel = getViewModel(photos, photoRepository)

        viewModel.onRemoveSavedChanged(true)
        viewModel.removePhotos()

        coVerify(exactly = 2) { photoRepository.removeSavedPhotoFromExternalStorage(any()) }
        coVerify(exactly = 1) { photoRepository.removePhotoByFileNameRemote(any()) }
        coVerify(exactly = 1) { photoRepository.removePhotoByFileNameLocal(any()) }
    }

    @Test
    fun `UI state Error results are mapped correctly`() = runTest {
        val photos = listOf(Photo.mock(fileName = "fileName1"), Photo.mock(fileName = "fileName2"))
        val photoRepository = spyk(
            FakePhotoRepositoryImpl().apply { removePhotoByFileNameRemoteReturnsException = true }
        )
        val viewModel = getViewModel(photos, photoRepository)
        viewModel.removePhotos()

        viewModel.uiState.test {
            with(awaitItem() as RemovePhotosDialogViewModel.UiState.Error) {
                assertThat(results).hasSize(2)
                assertThat(results.keys.toList()).isEqualTo(photos)
                assertThat(results.values.all { it != null }).isTrue()
            }
        }
    }

    @Test
    fun `Event flow emits PHOTOS_REMOVED after successful photo removal`() = runTest {
        val photos = listOf(Photo.mock())
        val viewModel = getViewModel(photos)

        viewModel.eventFlow.test {
            viewModel.removePhotos()
            assertThat(awaitItem()).isInstanceOf(RemovePhotosDialogViewModel.Event.PHOTOS_REMOVED::class.java)
        }
    }
}
