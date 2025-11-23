package com.example.faktory.core

import org.jooq.DSLContext
import org.jooq.Table
import org.jooq.TableRecord

abstract class PersistableFactory<R : TableRecord<R>, T : Any, B : FactoryBuilder<T>>(
    protected val dsl: DSLContext,
) : Factory<T, B>() {
    abstract fun table(): Table<R>

    abstract fun toRecord(entity: T): R

    open fun beforeCreate(entity: T): T = entity

    open fun afterCreate(entity: T): T = entity

    fun create(): T {
        val built = build()
        val beforePersist = beforeCreate(built)
        val record = toRecord(beforePersist)
        dsl.executeInsert(record)
        return afterCreate(beforePersist)
    }

    fun createList(count: Int): List<T> = (1..count).map { create() }
}
