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

    @Test
    fun `afterBuild hook is called after build`() {
        var hookCalled = false
        var builtUser: User? = null

        val factory =
            object : Factory<User, UserBuilder>() {
                override fun builder(): UserBuilder = UserBuilder()

                override fun afterBuild(entity: User): User {
                    hookCalled = true
                    builtUser = entity
                    return entity
                }
            }

        val user = factory.build()

        assertThat(hookCalled).isTrue()
        assertThat(builtUser).isSameAs(user)
    }

    @Test
    fun `afterBuild can modify entity`() {
        val factory =
            object : Factory<User, UserBuilder>() {
                override fun builder(): UserBuilder = UserBuilder()

                override fun afterBuild(entity: User): User =
                    entity.copy(name = "${entity.name} (modified)")
            }

        val user = factory.build()

        assertThat(user.name).isEqualTo("Default Name (modified)")
    }

    @Test
    fun `afterBuild is called for each entity in buildList`() {
        var callCount = 0

        val factory =
            object : Factory<User, UserBuilder>() {
                override fun builder(): UserBuilder = UserBuilder()

                override fun afterBuild(entity: User): User {
                    callCount++
                    return entity
                }
            }

        factory.buildList(3)

        assertThat(callCount).isEqualTo(3)
    }
}
