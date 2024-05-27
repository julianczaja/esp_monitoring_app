package com.julianczaja.esp_monitoring_app.presentation.addeditdevice

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.julianczaja.esp_monitoring_app.data.repository.FakeDeviceRepositoryImpl
import com.julianczaja.esp_monitoring_app.domain.model.Device
import com.julianczaja.esp_monitoring_app.domain.repository.DeviceRepository
import com.julianczaja.esp_monitoring_app.navigation.DeviceIdArgs
import com.julianczaja.esp_monitoring_app.presentation.addeditdevice.AddEditDeviceScreenViewModel.Event
import com.julianczaja.esp_monitoring_app.MainDispatcherRule
import io.mockk.coVerify
import io.mockk.spyk
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test


class AddEditDeviceScreenViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private fun getViewModel(
        savedStateHandle: SavedStateHandle = SavedStateHandle().apply { set(DeviceIdArgs.KEY, DeviceIdArgs.NO_VALUE) },
        deviceRepository: DeviceRepository = spyk(FakeDeviceRepositoryImpl())
    ) = AddEditDeviceScreenViewModel(
        savedStateHandle = savedStateHandle,
        deviceRepository = deviceRepository,
        ioDispatcher = dispatcherRule.testDispatcher
    ).apply { init() }

    //region Adding new device
    @Test
    fun `add device when duplicate id exists gives idError`() = runTest {
        val deviceRepository = spyk(FakeDeviceRepositoryImpl())
        val viewModel = getViewModel(deviceRepository = deviceRepository)

        deviceRepository.addNew(Device(1L, "name"))

        viewModel.idError.test {
            assertThat(awaitItem()).isNull()
            viewModel.updateId("1")
            viewModel.updateName("different name")
            viewModel.apply()
            assertThat(awaitItem()).isNotNull()
        }
        coVerify(exactly = 0) { deviceRepository.addNew(Device(1L, "different name")) }
    }

    @Test
    fun `add device when duplicate name exists gives nameError`() = runTest {
        val deviceRepository = spyk(FakeDeviceRepositoryImpl())
        val viewModel = getViewModel(deviceRepository = deviceRepository)

        deviceRepository.addNew(Device(1L, "name"))

        viewModel.nameError.test {
            assertThat(awaitItem()).isNull()
            viewModel.updateId("2")
            viewModel.updateName("name")
            viewModel.apply()
            assertThat(awaitItem()).isNotNull()
        }
        coVerify(exactly = 0) { deviceRepository.addNew(Device(2L, "name")) }
    }

    @Test
    fun `add device with unique id and name success`() = runTest {
        val deviceRepository = spyk(FakeDeviceRepositoryImpl())
        val viewModel = getViewModel(deviceRepository = deviceRepository)

        viewModel.nameError.test {
            assertThat(awaitItem()).isNull()
            viewModel.updateId("1")
            viewModel.updateName("name")
            viewModel.apply()
            expectNoEvents()
        }
        coVerify(exactly = 1) { deviceRepository.addNew(Device(1L, "name")) }
    }
    //endregion

    //region Updating device
    @Test
    fun `update device when name is the same as before gives nameError`() = runTest {
        val deviceId = 1L
        val deviceInDatabase = Device(deviceId, "name")
        val savedStateHandle = SavedStateHandle().apply { set(DeviceIdArgs.KEY, deviceId) }
        val deviceRepository = spyk(FakeDeviceRepositoryImpl())
        val viewModel = getViewModel(savedStateHandle, deviceRepository)

        deviceRepository.addNew(deviceInDatabase)

        viewModel.nameError.test {
            assertThat(awaitItem()).isNull()
            viewModel.updateName("name")
            viewModel.apply()
            assertThat(awaitItem()).isNotNull()
        }
        coVerify(exactly = 0) { deviceRepository.update(Device(deviceId, "name")) }
    }

    @Test
    fun `update device when duplicate name exists gives nameError`() = runTest {
        val deviceId = 1L
        val duplicateName = "name"
        val deviceInDatabase = Device(2L, duplicateName)
        val savedStateHandle = SavedStateHandle().apply { set(DeviceIdArgs.KEY, deviceId) }
        val deviceRepository = spyk(FakeDeviceRepositoryImpl())
        val viewModel = getViewModel(savedStateHandle, deviceRepository)

        deviceRepository.addNew(deviceInDatabase)

        viewModel.nameError.test {
            assertThat(awaitItem()).isNull()
            viewModel.updateName(duplicateName)
            viewModel.apply()
            assertThat(awaitItem()).isNotNull()
        }
        coVerify(exactly = 0) { deviceRepository.update(Device(deviceId, duplicateName)) }
    }

    @Test
    fun `update device with unique name success`() = runTest {
        val deviceId = 1L
        val deviceInDatabase = Device(deviceId, "name")
        val savedStateHandle = SavedStateHandle().apply { set(DeviceIdArgs.KEY, deviceId) }
        val deviceRepository = spyk(FakeDeviceRepositoryImpl().apply { addNew(deviceInDatabase) })
        val viewModel = getViewModel(savedStateHandle, deviceRepository)

        viewModel.nameError.test {
            assertThat(awaitItem()).isNull()
            viewModel.updateId("1")
            viewModel.updateName("new name")
            viewModel.apply()
            expectNoEvents()
        }
        coVerify(exactly = 1) { deviceRepository.update(Device(deviceId, "new name")) }
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
    fun `DeviceUpdated event is emitted when device is added`() = runTest {
        val deviceId = 1L
        val deviceInDatabase = Device(deviceId, "name")
        val savedStateHandle = SavedStateHandle().apply { set(DeviceIdArgs.KEY, deviceId) }
        val deviceRepository = FakeDeviceRepositoryImpl().apply { addNew(deviceInDatabase) }
        val viewModel = getViewModel(savedStateHandle, deviceRepository)

        viewModel.eventFlow.test {
            viewModel.updateName("updated name")
            viewModel.apply()
            assertThat(awaitItem()).isInstanceOf(Event.DeviceUpdated::class.java)
            expectNoEvents()
        }
    }

    @Test
    fun `ShowError event is emitted when exception thrown during adding device`() = runTest {
        val deviceRepository = FakeDeviceRepositoryImpl()
        val viewModel = getViewModel(deviceRepository = deviceRepository)

        deviceRepository.addNewDeviceThrowsError = true

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
        val deviceId = 1L
        val deviceInDatabase = Device(deviceId, "name")
        val savedStateHandle = SavedStateHandle().apply { set(DeviceIdArgs.KEY, deviceId) }
        val deviceRepository = FakeDeviceRepositoryImpl().apply {
            addNew(deviceInDatabase)
            updateDeviceThrowsError = true
        }
        val viewModel = getViewModel(savedStateHandle, deviceRepository)

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
    fun `when name is empty nameError contains null`() = runTest {
        val viewModel = getViewModel()

        viewModel.nameError.test {
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
