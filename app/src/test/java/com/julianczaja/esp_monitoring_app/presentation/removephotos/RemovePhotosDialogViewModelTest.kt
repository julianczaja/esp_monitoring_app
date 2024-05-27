package com.julianczaja.esp_monitoring_app.presentation.removephotos

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.julianczaja.esp_monitoring_app.data.repository.FakePhotoRepositoryImpl
import com.julianczaja.esp_monitoring_app.domain.repository.PhotoRepository
import com.julianczaja.esp_monitoring_app.navigation.RemovePhotosDialogParameters
import com.julianczaja.esp_monitoring_app.MainDispatcherRule
import io.mockk.coVerify
import io.mockk.spyk
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class RemovePhotosDialogViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()


    private fun getViewModel(
        parameters: RemovePhotosDialogParameters,
        photoRepository: PhotoRepository? = null
    ) = RemovePhotosDialogViewModel(
        savedStateHandle = SavedStateHandle(mapOf("params" to parameters)),
        photoRepository = photoRepository ?: FakePhotoRepositoryImpl(),
        ioDispatcher = dispatcherRule.testDispatcher
    )


    @Test
    fun `UI state is success when params are not empty`() = runTest {
        val params = RemovePhotosDialogParameters(listOf("fileName1", "fileName1"))
        val viewModel = getViewModel(params)

        viewModel.uiState.test {
            assertThat(awaitItem()).isInstanceOf(RemovePhotosDialogViewModel.UiState.Success::class.java)
        }
    }

    @Test
    fun `UI state is error when params are empty`() = runTest {
        val emptyParams = RemovePhotosDialogParameters(emptyList())
        val viewModel = getViewModel(emptyParams)

        viewModel.uiState.test {
            assertThat(awaitItem()).isInstanceOf(RemovePhotosDialogViewModel.UiState.Error::class.java)
        }
    }

    @Test
    fun `UI state is loading after remove photos success`() = runTest {
        val params = RemovePhotosDialogParameters(listOf("fileName1", "fileName1"))
        val photoRepository = spyk(FakePhotoRepositoryImpl())
        val viewModel = getViewModel(params, photoRepository)

        viewModel.uiState.test {
            assertThat(awaitItem()).isInstanceOf(RemovePhotosDialogViewModel.UiState.Success::class.java)
            viewModel.removePhotos()
            assertThat(awaitItem()).isInstanceOf(RemovePhotosDialogViewModel.UiState.Loading::class.java)
        }
        coVerify(exactly = 2) { photoRepository.removePhotoByFileNameRemote(any()) }
        coVerify(exactly = 2) { photoRepository.removePhotoByFileNameLocal(any()) }
    }

    @Test
    fun `UI state is error when exception thrown during photo removal`() = runTest {
        val params = RemovePhotosDialogParameters(listOf("fileName1", "fileName1"))
        val photoRepository = spyk(
            FakePhotoRepositoryImpl().apply { removePhotoByFileNameRemoteReturnsException = true }
        )
        val viewModel = getViewModel(params, photoRepository)

        viewModel.uiState.test {
            assertThat(awaitItem()).isInstanceOf(RemovePhotosDialogViewModel.UiState.Success::class.java)
            viewModel.removePhotos()
            assertThat(awaitItem()).isInstanceOf(RemovePhotosDialogViewModel.UiState.Error::class.java)
        }
    }

    @Test
    fun `Local photos not removed when exception thrown during remote photos removal`() = runTest {
        val params = RemovePhotosDialogParameters(listOf("fileName1", "fileName1"))
        val photoRepository = spyk(
            FakePhotoRepositoryImpl().apply { removePhotoByFileNameRemoteReturnsException = true }
        )
        val viewModel = getViewModel(params, photoRepository)

        viewModel.removePhotos()

        coVerify(exactly = 2) { photoRepository.removePhotoByFileNameRemote(any()) }
        coVerify(exactly = 0) { photoRepository.removePhotoByFileNameLocal(any()) }
    }

    @Test
    fun `Event flow emits PHOTOS_REMOVED after successful photo removal`() = runTest {
        val params = RemovePhotosDialogParameters(listOf("fileName1"))
        val viewModel = getViewModel(params)

        viewModel.eventFlow.test {
            viewModel.removePhotos()
            assertThat(awaitItem()).isInstanceOf(RemovePhotosDialogViewModel.Event.PHOTOS_REMOVED::class.java)
        }
    }
}
