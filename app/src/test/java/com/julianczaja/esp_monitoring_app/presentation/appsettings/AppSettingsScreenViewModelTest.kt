package com.julianczaja.esp_monitoring_app.presentation.appsettings

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.julianczaja.esp_monitoring_app.MainDispatcherRule
import com.julianczaja.esp_monitoring_app.domain.repository.AppSettingsRepository
import com.julianczaja.esp_monitoring_app.presentation.appsettings.AppSettingsScreenViewModel.Event
import com.julianczaja.esp_monitoring_app.presentation.appsettings.AppSettingsScreenViewModel.UiState
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Before
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

    private lateinit var appSettingsRepository: AppSettingsRepository

    @Before
    fun setup() {
        appSettingsRepository = mockk(relaxed = true)
        every { appSettingsRepository.getDynamicColor() } returns flow {
            delay(100)
            emit(false)
        }
        every { appSettingsRepository.getBaseUrl() } returns flow {
            delay(100)
            emit(VALID_BASE_URL)
        }
        every { appSettingsRepository.getBaseUrlHistory() } returns flow {
            delay(100)
            emit(setOf(VALID_BASE_URL, VALID_BASE_URL_2))
        }
    }

    private fun getViewModel() = AppSettingsScreenViewModel(
        appSettingsRepository = appSettingsRepository,
        workManager = mockk(relaxed = true),
        ioDispatcher = dispatcherRule.testDispatcher
    )

    @Test
    fun `base url is loaded from datastore on start`() = runTest {
        val initialBaseUrl = VALID_BASE_URL
        every { appSettingsRepository.getBaseUrl() } returns flow {
            delay(1000)
            emit(initialBaseUrl)
        }
        val viewModel = getViewModel()

        viewModel.uiState.test {
            assertThat(awaitItem()).isInstanceOf(UiState.Loading::class.java)
            with(awaitItem() as UiState.Success) {
                assertThat(baseUrlFieldState.error).isNull()
                assertThat(baseUrlFieldState.data).isEmpty()
            }
            with(awaitItem() as UiState.Success) {
                assertThat(baseUrlFieldState.error).isNull()
                assertThat(baseUrlFieldState.data).isEqualTo(initialBaseUrl)
            }
        }
    }

    @Test
    fun `dynamic color is loaded from datastore on start`() = runTest {
        val initialDynamicColor = true
        every { appSettingsRepository.getDynamicColor() } returns flow {
            delay(100)
            emit(initialDynamicColor)
        }
        val viewModel = getViewModel()

        viewModel.uiState.test {
            assertThat(awaitItem()).isInstanceOf(UiState.Loading::class.java)
            delay(1000L)
            assertThat((expectMostRecentItem() as UiState.Success).isDynamicColor).isEqualTo(initialDynamicColor)
            ensureAllEventsConsumed()
        }
    }

    @Test
    fun `when invalid base url is set, error is not null`() = runTest {
        val initialBaseUrl = VALID_BASE_URL
        val invalidBaseUrl = INVALID_BASE_URL
        every { appSettingsRepository.getBaseUrl() } returns flow {
            delay(1000)
            emit(initialBaseUrl)
        }
        val viewModel = getViewModel()

        viewModel.uiState.test {
            skipItems(2)
            with(awaitItem() as UiState.Success) {
                assertThat(baseUrlFieldState.error).isNull()
                assertThat(baseUrlFieldState.data).isEqualTo(initialBaseUrl)
            }

            viewModel.setBaseUrl(invalidBaseUrl)

            with(awaitItem() as UiState.Success) {
                assertThat(baseUrlFieldState.error).isNotNull()
                assertThat(baseUrlFieldState.data).isEqualTo(invalidBaseUrl)
            }
        }
    }

    @Test
    fun `when valid base url is set, error is null`() = runTest {
        val initialBaseUrl = VALID_BASE_URL
        val updatedBaseUrl = VALID_BASE_URL_2
        every { appSettingsRepository.getBaseUrl() } returns flow {
            delay(1000)
            emit(initialBaseUrl)
        }
        val viewModel = getViewModel()

        viewModel.uiState.test {
            skipItems(2)
            with(awaitItem() as UiState.Success) {
                assertThat(baseUrlFieldState.error).isNull()
                assertThat(baseUrlFieldState.data).isEqualTo(initialBaseUrl)
            }

            viewModel.setBaseUrl(updatedBaseUrl)

            with(awaitItem() as UiState.Success) {
                assertThat(baseUrlFieldState.error).isNull()
                assertThat(baseUrlFieldState.data).isEqualTo(updatedBaseUrl)
            }
        }
    }

    @Test
    fun `when invalid base url is applied, datastore is not written`() = runTest {
        val invalidBaseUrl = INVALID_BASE_URL
        val viewModel = getViewModel()

        viewModel.setBaseUrl(invalidBaseUrl)
        viewModel.applyBaseUrl()

        coVerify(exactly = 0) { appSettingsRepository.setBaseUrl(any()) }
    }

    @Test
    fun `when valid base url is applied, datastore is written`() = runTest {
        val updatedBaseUrl = VALID_BASE_URL
        val viewModel = getViewModel()

        viewModel.setBaseUrl(updatedBaseUrl)
        viewModel.applyBaseUrl()

        coVerify(exactly = 1) { appSettingsRepository.setBaseUrl(any()) }
    }

    @Test
    fun `when valid base url is applied, event flow emits BaseUrlSaved`() = runTest {
        val updatedBaseUrl = VALID_BASE_URL
        val viewModel = getViewModel()

        viewModel.setBaseUrl(updatedBaseUrl)

        viewModel.eventFlow.test {
            viewModel.applyBaseUrl()
            assertThat(awaitItem()).isInstanceOf(Event.BaseUrlSaved::class.java)
        }

        coVerify(exactly = 1) { appSettingsRepository.setBaseUrl(any()) }
    }
}
