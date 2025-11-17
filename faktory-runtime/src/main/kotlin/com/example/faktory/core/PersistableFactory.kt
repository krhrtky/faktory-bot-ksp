package com.example.faktory.core

import org.jooq.DSLContext
import org.jooq.Table
import org.jooq.TableRecord

abstract class PersistableFactory<R : TableRecord<R>, T : Any, B : FactoryBuilder<T>>(
    protected val dsl: DSLContext,
) : Factory<T, B>() {
    abstract fun table(): Table<R>
    abstract fun toRecord(entity: T): R

    fun create(): T {
        val entity = build()
        val record = toRecord(entity)
        dsl.executeInsert(record)
        return entity
    }

    fun createList(count: Int): List<T> =
        (1..count).map { create() }
}
