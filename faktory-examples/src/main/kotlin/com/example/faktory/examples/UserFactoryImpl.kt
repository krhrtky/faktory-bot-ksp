package com.example.faktory.examples

import com.example.faktory.core.PersistableFactory
import com.example.faktory.examples.jooq.tables.Users.Companion.USERS
import com.example.faktory.examples.jooq.tables.records.UsersRecord
import org.jooq.DSLContext

class UserFactoryImpl(
    dsl: DSLContext,
) : PersistableFactory<UsersRecord, User, UserBuilder>(dsl) {
    override fun builder() = UserBuilder()

    override fun table() = USERS

    override fun toRecord(entity: User): UsersRecord =
        UsersRecord().apply {
            id = entity.id
            name = entity.name
            email = entity.email
            age = entity.age
        }
}
