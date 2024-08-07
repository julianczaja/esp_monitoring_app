package com.julianczaja.esp_monitoring_app.presentation.addeditdevice

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.testing.invoke
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.julianczaja.esp_monitoring_app.MainDispatcherRule
import com.julianczaja.esp_monitoring_app.data.repository.FakeDeviceRepositoryImpl
import com.julianczaja.esp_monitoring_app.domain.model.Device
import com.julianczaja.esp_monitoring_app.domain.repository.DeviceRepository
import com.julianczaja.esp_monitoring_app.navigation.AddEditDeviceScreen
import com.julianczaja.esp_monitoring_app.navigation.DeviceIdArgs
import com.julianczaja.esp_monitoring_app.presentation.addeditdevice.AddEditDeviceScreenViewModel.Event
import io.mockk.coVerify
import io.mockk.spyk
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * These tests use Robolectric because the subject under test (the ViewModel) uses
 * `SavedStateHandle.toRoute` which has a dependency on `android.os.Bundle`.
 *
 * TODO: Remove Robolectric if/when AndroidX Navigation API is updated to remove Android dependency.
 *  See b/340966212.
 */
@RunWith(RobolectricTestRunner::class)
class AddEditDeviceScreenViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private fun getViewModel(
        deviceId: Long? = null,
        deviceRepository: DeviceRepository = spyk(FakeDeviceRepositoryImpl())
    ) = AddEditDeviceScreenViewModel(
        savedStateHandle = SavedStateHandle(route = AddEditDeviceScreen(deviceId ?: DeviceIdArgs.NO_VALUE)),
        deviceRepository = deviceRepository,
        ioDispatcher = dispatcherRule.testDispatcher
    ).apply { init() }

    //region Adding new device
    @Test
    fun `add device when duplicate id exists gives idError`() = runTest {
        val deviceRepository = spyk(FakeDeviceRepositoryImpl()).apply {
            addNew(Device(1L, "name"))
        }
        val viewModel = getViewModel(deviceRepository = deviceRepository)

        viewModel.idError.test {
            assertThat(awaitItem()).isNull()
            viewModel.updateId("1")
            assertThat(awaitItem()).isNotNull()
        }
    }

    @Test
    fun `add device when duplicate name exists gives nameError`() = runTest {
        val deviceRepository = spyk(FakeDeviceRepositoryImpl()).apply {
            addNew(Device(1L, "name"))
        }
        val viewModel = getViewModel(deviceRepository = deviceRepository)

        viewModel.nameError.test {
            assertThat(awaitItem()).isNull()
            viewModel.updateName("name")
            assertThat(awaitItem()).isNotNull()
        }
    }

    @Test
    fun `add device with unique id and name success`() = runTest {
        val deviceRepository = spyk(FakeDeviceRepositoryImpl())
        val viewModel = getViewModel(deviceRepository = deviceRepository)

        viewModel.nameError.test {
            viewModel.updateName("name")
            viewModel.updateId("1")
            assertThat(awaitItem()).isNull()
            viewModel.apply()
            expectNoEvents()
        }
        coVerify(exactly = 1) { deviceRepository.addNew(Device(1L, "name")) }
    }
    //endregion

    //region Updating device
    @Test
    fun `update device with unchanged name does not return nameError or update the database`() = runTest {
        val deviceInDatabase = Device(1L, "name")
        val deviceRepository = spyk(FakeDeviceRepositoryImpl()).apply {
            addNew(deviceInDatabase)
        }
        val viewModel = getViewModel(deviceInDatabase.id, deviceRepository)

        viewModel.nameError.test {
            viewModel.updateName("different name")
            assertThat(awaitItem()).isNull()
            viewModel.updateName(deviceInDatabase.name)
            expectNoEvents()
            viewModel.apply()
        }
        coVerify(exactly = 0) { deviceRepository.update(any()) }
    }

    @Test
    fun `update device when duplicate name exists gives nameError`() = runTest {
        val deviceInDatabase1 = Device(1L, "name1")
        val deviceInDatabase2 = Device(2L, "name2")
        val deviceRepository = spyk(FakeDeviceRepositoryImpl()).apply {
            addNew(deviceInDatabase1)
            addNew(deviceInDatabase2)
        }
        val viewModel = getViewModel(deviceInDatabase1.id, deviceRepository)

        viewModel.nameError.test {
            assertThat(awaitItem()).isNull()
            viewModel.updateName(deviceInDatabase2.name)
            assertThat(awaitItem()).isNotNull()
        }
    }

    @Test
    fun `update device with unique name success`() = runTest {
        val deviceId = 1L
        val deviceInDatabase = Device(deviceId, "name")
        val deviceRepository = spyk(FakeDeviceRepositoryImpl()).apply {
            addNew(deviceInDatabase)
        }
        val viewModel = getViewModel(deviceInDatabase.id, deviceRepository)

        viewModel.nameError.test {
            assertThat(awaitItem()).isNull()
            viewModel.updateName("new name")
            expectNoEvents()
            viewModel.apply()
        }
        coVerify(exactly = 1) { deviceRepository.update(Device(deviceId, "new name")) }
    }

    @Test
    fun `id error is null in update mode`() = runTest {
        val deviceInDatabase = Device(1L, "name")
        val deviceRepository = spyk(FakeDeviceRepositoryImpl()).apply { addNew(deviceInDatabase) }
        val viewModel = getViewModel(deviceInDatabase.id, deviceRepository)

        viewModel.idError.test {
            assertThat(awaitItem()).isNull()
        }
    }
    //endregion

    //region Events flow
    @Test
    fun `DeviceAdded event is emitted when device is added`() = runTest {
        val viewModel = getViewModel()

        viewModel.eventFlow.test {
            viewModel.updateId("1")
            viewModel.updateName("name")
            viewModel.apply()
            assertThat(awaitItem()).isInstanceOf(Event.DeviceAdded::class.java)
            expectNoEvents()
        }
    }

    @Test
    fun `DeviceUpdated event is emitted when device is updated`() = runTest {
        val deviceInDatabase = Device(1L, "name")
        val deviceRepository = FakeDeviceRepositoryImpl().apply { addNew(deviceInDatabase) }
        val viewModel = getViewModel(deviceInDatabase.id, deviceRepository)

        viewModel.eventFlow.test {
            viewModel.updateName("updated name")
            viewModel.apply()
            assertThat(awaitItem()).isInstanceOf(Event.DeviceUpdated::class.java)
            expectNoEvents()
        }
    }

    @Test
    fun `ShowError event is emitted when exception thrown during adding device`() = runTest {
        val deviceRepository = FakeDeviceRepositoryImpl().apply { addNewDeviceThrowsError = true }
        val viewModel = getViewModel(deviceRepository = deviceRepository)

        viewModel.eventFlow.test {
            viewModel.updateId("1")
            viewModel.updateName("name")
            viewModel.apply()
            assertThat(awaitItem()).isInstanceOf(Event.ShowError::class.java)
            expectNoEvents()
        }
    }

    @Test
    fun `ShowError event is emitted when exception thrown during updating device`() = runTest {
        val deviceInDatabase = Device(1L, "name")
        val deviceRepository = FakeDeviceRepositoryImpl().apply {
            addNew(deviceInDatabase)
            updateDeviceThrowsError = true
        }
        val viewModel = getViewModel(deviceInDatabase.id, deviceRepository)

        viewModel.eventFlow.test {
            viewModel.updateName("updated name")
            viewModel.apply()
            assertThat(awaitItem()).isInstanceOf(Event.ShowError::class.java)
            expectNoEvents()
        }
    }
    //endregion

    //region Fields validation
    @Test
    fun `when id is above 0 idError is null`() = runTest {
        val viewModel = getViewModel()

        viewModel.idError.test {
            assertThat(awaitItem()).isNull()
            viewModel.updateId("1")
            expectNoEvents()
        }
    }

    @Test
    fun `when id is below 0 idError contains error`() = runTest {
        val viewModel = getViewModel()

        viewModel.idError.test {
            assertThat(awaitItem()).isNull()
            viewModel.updateId("-1")
            assertThat(awaitItem()).isNotNull()
        }
    }

    @Test
    fun `when id is 0 idError is null`() = runTest {
        val viewModel = getViewModel()

        viewModel.idError.test {
            assertThat(awaitItem()).isNull()
            viewModel.updateId("0")
            expectNoEvents()
        }
    }

    @Test
    fun `when id is not number idError contains error`() = runTest {
        val viewModel = getViewModel()

        viewModel.idError.test {
            assertThat(awaitItem()).isNull()
            viewModel.updateId("abc")
            assertThat(awaitItem()).isNotNull()
        }
    }

    @Test
    fun `when name is empty nameError contains error`() = runTest {
        val viewModel = getViewModel()

        viewModel.nameError.test {
            viewModel.updateName("valid name")
            assertThat(awaitItem()).isNull()
            viewModel.updateName("")
            assertThat(awaitItem()).isNotNull()
        }
    }

    @Test
    fun `when name is longer than 20 characters nameError contains error`() = runTest {
        val viewModel = getViewModel()

        viewModel.nameError.test {
            assertThat(awaitItem()).isNull()
            viewModel.updateName("some very very very very very very long name")
            assertThat(awaitItem()).isNotNull()
        }
    }

    @Test
    fun `when name is 20 characters nameError is null`() = runTest {
        val viewModel = getViewModel()

        viewModel.nameError.test {
            assertThat(awaitItem()).isNull()
            viewModel.updateName("a".repeat(20))
            expectNoEvents()
        }
    }
    //endregion
}
