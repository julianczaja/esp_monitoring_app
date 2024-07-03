package com.julianczaja.esp_monitoring_app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.julianczaja.esp_monitoring_app.MainDispatcherRule
import com.julianczaja.esp_monitoring_app.common.Constants
import com.julianczaja.esp_monitoring_app.domain.model.AppSettings
import com.julianczaja.esp_monitoring_app.domain.repository.AppSettingsRepository
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder


class AppSettingsRepositoryImplTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    @get:Rule
    val tmpFolder: TemporaryFolder = TemporaryFolder.builder().assureDeletion().build()

    private val testCoroutineScope = TestScope(dispatcherRule.testDispatcher)

    private lateinit var dataStore: DataStore<Preferences>

    private lateinit var appSettingsRepository: AppSettingsRepository

    @Before
    fun setup() {
        dataStore = PreferenceDataStoreFactory.create(
            scope = testCoroutineScope,
            produceFile = {
                tmpFolder.newFile("datastore.preferences_pb")
            }
        )
        appSettingsRepository = AppSettingsRepositoryImpl(dataStore)
    }

    @Test
    fun `Initial app settings are correct`() = runTest {
        val defaultAppSettings = AppSettings(
            baseUrl = Constants.defaultBaseUrl,
            isFirstTimeUser = true,
            isDynamicColor = Constants.DEFAULT_IS_DYNAMIC_COLOR
        )

        appSettingsRepository.getAppSettings().test {
            assertThat(awaitItem()).isEqualTo(defaultAppSettings)
        }
    }

    @Test
    fun `Update app settings success`() = runTest {
        val newBaseUrl = "new_base_url"
        val newIsFirstTimeUser = false
        val isDynamicColor = true

        with(appSettingsRepository) {
            setIsFirstTimeUser(newIsFirstTimeUser)
            setBaseUrl(newBaseUrl)
            setDynamicColor(isDynamicColor)
        }

        appSettingsRepository.getAppSettings().test {
            assertThat(awaitItem()).isEqualTo(AppSettings(newBaseUrl, newIsFirstTimeUser, isDynamicColor))
        }
    }

    @Test
    fun `Updating base url adds new entries to base url history`() = runTest {
        val baseUrl1 = "base_url_1"
        val baseUrl2 = "base_url_2"

        appSettingsRepository.getBaseUrlHistory().test {
            assertThat(awaitItem()).isEmpty()
            appSettingsRepository.setBaseUrl(baseUrl1)
            assertThat(awaitItem()).isEqualTo(setOf(baseUrl1))
            appSettingsRepository.setBaseUrl(baseUrl2)
            assertThat(awaitItem()).isEqualTo(setOf(baseUrl1, baseUrl2))
        }
    }

    @Test
    fun `Updating base url does not adds new entries to base url history if addToHistory is false`() = runTest {
        val baseUrl1 = "base_url_1"
        val baseUrl2 = "base_url_2"

        appSettingsRepository.getBaseUrlHistory().test {
            assertThat(awaitItem()).isEmpty()
            appSettingsRepository.setBaseUrl(baseUrl1, addToHistory = false)
            assertThat(awaitItem()).isEmpty()
            appSettingsRepository.setBaseUrl(baseUrl2, addToHistory = false)
            assertThat(awaitItem()).isEmpty()
        }
    }

    @Test
    fun `Base url history size is limited`() = runTest {
        val baseUrlFormat = "base_url_%d"
        val baseUrls = mutableSetOf<String>()
        val limit = Constants.BASE_URL_HISTORY_LIMIT

        repeat(limit + 5) {
            val baseUrl = baseUrlFormat.format(it + 1)
            baseUrls.add(baseUrl)
            appSettingsRepository.setBaseUrl(baseUrl, addToHistory = true)
        }

        appSettingsRepository.getBaseUrlHistory().test {
            awaitItem().let { history ->
                assertThat(history).hasSize(limit)
                assertThat(history).isEqualTo(baseUrls.drop(5).toSet())
            }
        }
    }
}
