package com.example.faktory.core

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FactoryTest {
    data class User(val name: String, val email: String)

    class UserBuilder : FactoryBuilder<User> {
        private var name: String = "Default Name"
        private var email: String = "default@example.com"

        fun withName(value: String): UserBuilder = apply { name = value }

        fun withEmail(value: String): UserBuilder = apply { email = value }

        override fun build(): User = User(name, email)
    }

    class UserFactory : Factory<User, UserBuilder>() {
        override fun builder(): UserBuilder = UserBuilder()
    }

    @Test
    fun `build creates instance`() {
        val factory = UserFactory()
        val user = factory.build()

        assertThat(user).isNotNull()
        assertThat(user.name).isEqualTo("Default Name")
    }

    @Test
    fun `buildList creates multiple instances`() {
        val factory = UserFactory()
        val users = factory.buildList(3)

        assertThat(users).hasSize(3)
        assertThat(users).allMatch { it.name == "Default Name" }
    }

    @Test
    fun `builder allows customization`() {
        val factory = UserFactory()
        val user =
            factory.builder()
                .withName("Alice")
                .withEmail("alice@example.com")
                .build()

        assertThat(user.name).isEqualTo("Alice")
        assertThat(user.email).isEqualTo("alice@example.com")
    }
}
