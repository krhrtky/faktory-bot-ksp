package com.example.faktory.core

import org.jooq.DSLContext
import org.jooq.TableRecord

class AssociationContext(
    private val dsl: DSLContext? = null,
) {
    private val associations = mutableMapOf<String, Any>()

    fun register(key: String, record: Any) {
        associations[key] = record
    }

    fun <T : TableRecord<T>> associateWithPersist(
        key: String,
        factory: () -> T,
    ) {
        val record = factory()
        dsl?.executeInsert(record)
        register(key, record)
    }

    fun <T> get(key: String): T? =
        @Suppress("UNCHECKED_CAST")
        associations[key] as? T
}
