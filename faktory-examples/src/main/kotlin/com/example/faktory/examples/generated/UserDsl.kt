package com.example.faktory.examples.generated

import com.example.faktory.core.FactoryDsl
import com.example.faktory.examples.jooq.tables.records.UsersRecord
import java.time.LocalDateTime

@FactoryDsl
class UsersDslBuilder(
    var name: String,
    var email: String,
) {
    var age: Int? = null
    var createdAt: LocalDateTime? = null

    internal fun build(): UsersRecord =
        UsersRecord().apply {
            this.name = this@UsersDslBuilder.name
            this.email = this@UsersDslBuilder.email
            this.age = this@UsersDslBuilder.age
            this.createdAt = this@UsersDslBuilder.createdAt
        }
}

fun user(
    name: String,
    email: String,
    block: UsersDslBuilder.() -> Unit = {},
): UsersRecord = UsersDslBuilder(name, email).apply(block).build()
