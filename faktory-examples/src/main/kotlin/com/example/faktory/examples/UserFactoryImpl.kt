package com.example.faktory.examples

import com.example.faktory.core.PersistableFactory
import com.example.faktory.examples.jooq.tables.Users.Companion.USERS
import com.example.faktory.examples.jooq.tables.records.UsersRecord
import org.jooq.DSLContext

open class UserFactoryImpl(
    dsl: DSLContext,
) : PersistableFactory<UsersRecord, User, UserBuilder>(dsl) {
    private val sequence = java.util.concurrent.atomic.AtomicInteger(0)

    override fun builder() = UserBuilder(sequence)

    override fun table() = USERS

    override fun toRecord(entity: User): UsersRecord =
        UsersRecord().apply {
            entity.id?.let { id = it }
            name = entity.name
            email = entity.email
            age = entity.age
        }
}
