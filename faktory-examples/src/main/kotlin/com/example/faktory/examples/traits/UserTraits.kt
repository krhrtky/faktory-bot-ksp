package com.example.faktory.examples.traits

import com.example.faktory.core.DslTrait
import com.example.faktory.examples.UsersDslBuilder
import java.time.LocalDateTime

object ActiveUser : DslTrait<UsersDslBuilder> {
    override fun invoke(builder: UsersDslBuilder) {
        builder.age = 25
    }
}

object SeniorUser : DslTrait<UsersDslBuilder> {
    override fun invoke(builder: UsersDslBuilder) {
        builder.age = 65
    }
}

object YoungAdult : DslTrait<UsersDslBuilder> {
    override fun invoke(builder: UsersDslBuilder) {
        builder.age = 20
    }
}

object WithCurrentTimestamp : DslTrait<UsersDslBuilder> {
    override fun invoke(builder: UsersDslBuilder) {
        builder.createdAt = LocalDateTime.now()
    }
}

class WithAge(private val value: Int) : DslTrait<UsersDslBuilder> {
    override fun invoke(builder: UsersDslBuilder) {
        builder.age = value
    }
}

class WithTimestamp(private val timestamp: LocalDateTime) : DslTrait<UsersDslBuilder> {
    override fun invoke(builder: UsersDslBuilder) {
        builder.createdAt = timestamp
    }
}
