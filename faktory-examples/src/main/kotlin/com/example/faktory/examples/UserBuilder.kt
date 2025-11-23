package com.example.faktory.examples

import com.example.faktory.core.FactoryBuilder
import java.util.concurrent.atomic.AtomicInteger

class UserBuilder(
    private val sequence: AtomicInteger = AtomicInteger(0),
) : FactoryBuilder<User> {
    private var name: String? = null
    private var email: String? = null
    private var age: Int? = null

    fun withName(value: String) = apply { this.name = value }

    fun withEmail(value: String) = apply { this.email = value }

    fun withAge(value: Int) = apply { this.age = value }

    fun withSequence() =
        apply {
            val seq = sequence.incrementAndGet()
            this.name = "User $seq"
            this.email = "user$seq@example.com"
        }

    override fun build(): User {
        val seq = sequence.get()
        return User(
            name = name ?: "User ${sequence.incrementAndGet()}",
            email = email ?: "user${sequence.get()}@example.com",
            age = age,
        )
    }
}
