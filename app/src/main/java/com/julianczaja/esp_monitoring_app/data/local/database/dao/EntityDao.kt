package com.julianczaja.esp_monitoring_app.data.local.database.dao

import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Transaction
import androidx.room.Update
import com.julianczaja.esp_monitoring_app.data.local.database.entity.BaseEntity

abstract class EntityDao<in E : BaseEntity> {

    @Insert
    abstract fun insert(entity: E): Long

    @Insert
    abstract fun insertAll(vararg entity: E)

    @Insert
    abstract fun insertAll(entities: List<E>)

    @Update
    abstract fun update(entity: E)

    @Delete
    abstract fun deleteEntity(entity: E): Int

    @Transaction
    open fun withTransaction(tx: () -> Unit) = tx()

    fun insertOrUpdate(entity: E): Long {
        return if (entity.id == 0L) {
            insert(entity)
        } else {
            update(entity)
            entity.id
        }
    }

    @Transaction
    open fun insertOrUpdate(entities: List<E>) {
        entities.forEach {
            insertOrUpdate(it)
        }
    }
}
