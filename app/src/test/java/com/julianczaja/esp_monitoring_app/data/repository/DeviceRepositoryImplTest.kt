package com.julianczaja.esp_monitoring_app.data.repository

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.julianczaja.esp_monitoring_app.data.local.database.RoomDeleteResult
import com.julianczaja.esp_monitoring_app.data.local.database.dao.DeviceDao
import com.julianczaja.esp_monitoring_app.data.local.database.entity.DeviceEntity
import com.julianczaja.esp_monitoring_app.domain.model.Device
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.IOException

class DeviceRepositoryImplTest {

    @Test
    fun `Get all devices returns list of DeviceEntity mapped to list of Device`() = runTest {
        val dao: DeviceDao = mockk()
        every { dao.getAll() } returns flowOf(
            listOf(
                DeviceEntity(1L, "name 1"),
                DeviceEntity(2L, "name 2")
            )
        )
        val repository = DeviceRepositoryImpl(dao)

        repository.getAllDevices().test {
            assertThat(awaitItem()).isEqualTo(
                listOf(
                    Device(1L, "name 1"),
                    Device(2L, "name 2"),
                )
            )
            awaitComplete()
        }
    }

    @Test
    fun `Get device by id returns DeviceEntity mapped to Device`() = runTest {
        val dao: DeviceDao = mockk()
        val deviceId = 1L
        every { dao.getById(deviceId) } returns flowOf(
            DeviceEntity(deviceId, "name")
        )
        val repository = DeviceRepositoryImpl(dao)

        repository.getDeviceById(deviceId).test {
            assertThat(awaitItem()).isEqualTo(
                Device(deviceId, "name")
            )
            awaitComplete()
        }
    }

    @Test
    fun `Add device returns success Result when success`() = runTest {
        val dao: DeviceDao = mockk()
        coEvery { dao.insert(any()) } returns 0L
        val repository = DeviceRepositoryImpl(dao)

        val result = repository.addNew(Device(1L, "name"))

        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `Add device returns failure Result when exception thrown`() = runTest {
        val dao: DeviceDao = mockk()
        coEvery { dao.insert(any()) } throws IOException()
        val repository = DeviceRepositoryImpl(dao)

        val result = repository.addNew(Device(1L, "name"))

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `Update device returns success Result when success`() = runTest {
        val dao: DeviceDao = mockk()
        coEvery { dao.update(any()) } returns Unit
        val repository = DeviceRepositoryImpl(dao)

        val result = repository.update(Device(1L, "name"))

        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `Update device returns failure Result when exception thrown`() = runTest {
        val dao: DeviceDao = mockk()
        coEvery { dao.update(any()) } throws IOException()
        val repository = DeviceRepositoryImpl(dao)

        val result = repository.update(Device(1L, "name"))

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `Remove device returns success Result when success`() = runTest {
        val dao: DeviceDao = mockk()
        coEvery { dao.deleteEntity(any()) } returns RoomDeleteResult.SUCCESS
        val repository = DeviceRepositoryImpl(dao)

        val result = repository.remove(Device(1L, "name"))

        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `Remove device returns failure Result on error`() = runTest {
        val dao: DeviceDao = mockk()
        coEvery { dao.deleteEntity(any()) } returns RoomDeleteResult.ERROR
        val repository = DeviceRepositoryImpl(dao)

        val result = repository.remove(Device(1L, "name"))

        assertThat(result.isFailure).isTrue()
    }
}
