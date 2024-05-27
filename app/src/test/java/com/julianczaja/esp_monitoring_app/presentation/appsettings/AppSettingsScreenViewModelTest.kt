package com.julianczaja.esp_monitoring_app.presentation.appsettings

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.julianczaja.esp_monitoring_app.domain.model.AppSettings
import com.julianczaja.esp_monitoring_app.domain.repository.AppSettingsRepository
import com.julianczaja.esp_monitoring_app.presentation.appsettings.AppSettingsScreenViewModel.Event
import com.julianczaja.esp_monitoring_app.presentation.appsettings.AppSettingsScreenViewModel.UiState
import com.julianczaja.esp_monitoring_app.MainDispatcherRule
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test


class AppSettingsScreenViewModelTest {

    private companion object {
        const val VALID_BASE_URL = "http://192.168.1.1:8000/"
        const val VALID_BASE_URL_2 = "https://192.168.0.51:8900/"
        const val INVALID_BASE_URL = "invalid_base_url"
    }

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private fun getViewModel(
        appSettingsRepository: AppSettingsRepository = mockk(relaxed = true)
    ) = AppSettingsScreenViewModel(
        appSettingsRepository = appSettingsRepository,
        ioDispatcher = dispatcherRule.testDispatcher
    )

    @Test
    fun `base url is loaded from datastore on start`() = runTest {
        val initialBaseUrl = VALID_BASE_URL
        val appSettingsRepository: AppSettingsRepository = mockk()
        every { appSettingsRepository.getAppSettings() } returns flow {
            delay(1000)
            emit(AppSettings(initialBaseUrl, false))
        }
        val viewModel = getViewModel(appSettingsRepository)

        viewModel.uiState.test {
            assertThat(awaitItem()).isInstanceOf(UiState.Loading::class.java)
            assertThat((awaitItem() as UiState.Success).appSettings.baseUrl).isEqualTo(initialBaseUrl)
        }
    }

    @Test
    fun `when invalid base url is set, error is not null`() = runTest {
        val initialBaseUrl = VALID_BASE_URL
        val updatedBaseUrl = INVALID_BASE_URL
        val appSettingsRepository: AppSettingsRepository = mockk()
        every { appSettingsRepository.getAppSettings() } returns flow {
            delay(1000)
            emit(AppSettings(initialBaseUrl, false))
        }
        val viewModel = getViewModel(appSettingsRepository)

        viewModel.uiState.test {
            assertThat(awaitItem()).isInstanceOf(UiState.Loading::class.java)
            assertThat(awaitItem()).isInstanceOf(UiState.Success::class.java)

            viewModel.setBaseUrl(updatedBaseUrl)
            with(awaitItem() as UiState.Success) {
                assertThat(baseUrlFieldError).isNotNull()
                assertThat(baseUrlFieldValue).isEqualTo(updatedBaseUrl)
            }
        }
    }

    @Test
    fun `when valid base url is set, error is null`() = runTest {
        val initialBaseUrl = VALID_BASE_URL
        val updatedBaseUrl = VALID_BASE_URL_2
        val appSettingsRepository: AppSettingsRepository = mockk()
        every { appSettingsRepository.getAppSettings() } returns flow {
            delay(1000)
            emit(AppSettings(initialBaseUrl, false))
        }
        val viewModel = getViewModel(appSettingsRepository)

        viewModel.uiState.test {
            assertThat(awaitItem()).isInstanceOf(UiState.Loading::class.java)
            assertThat(awaitItem()).isInstanceOf(UiState.Success::class.java)

            viewModel.setBaseUrl(updatedBaseUrl)
            with(awaitItem() as UiState.Success) {
                assertThat(baseUrlFieldError).isNull()
                assertThat(baseUrlFieldValue).isEqualTo(updatedBaseUrl)
            }
        }
    }

    @Test
    fun `when invalid base url is applied, datastore is not written`() = runTest {
        val updatedBaseUrl = INVALID_BASE_URL
        val appSettingsRepository = spyk<AppSettingsRepository>()
        val viewModel = getViewModel(appSettingsRepository)

        viewModel.setBaseUrl(updatedBaseUrl)
        viewModel.applyBaseUrl()

        coVerify(exactly = 0) { appSettingsRepository.setBaseUrl(any()) }
    }

    @Test
    fun `when valid base url is applied, datastore is written`() = runTest {
        val updatedBaseUrl = VALID_BASE_URL
        val appSettingsRepository = spyk<AppSettingsRepository>()
        val viewModel = getViewModel(appSettingsRepository)

        viewModel.setBaseUrl(updatedBaseUrl)
        viewModel.applyBaseUrl()

        coVerify(exactly = 1) { appSettingsRepository.setBaseUrl(any()) }
    }

    @Test
    fun `when valid base url is applied, event flow emits BaseUrlSaved`() = runTest {
        val updatedBaseUrl = VALID_BASE_URL
        val appSettingsRepository = spyk<AppSettingsRepository>()
        val viewModel = getViewModel(appSettingsRepository)

        viewModel.setBaseUrl(updatedBaseUrl)

        viewModel.eventFlow.test {
            viewModel.applyBaseUrl()
            assertThat(awaitItem()).isInstanceOf(Event.BaseUrlSaved::class.java)
        }

        coVerify(exactly = 1) { appSettingsRepository.setBaseUrl(any()) }
    }
}
