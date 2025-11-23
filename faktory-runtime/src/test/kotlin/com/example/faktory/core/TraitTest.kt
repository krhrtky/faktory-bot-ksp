package com.example.faktory.core

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TraitTest {
    data class User(val name: String, val email: String, val isActive: Boolean = false, val age: Int? = null)

    class UserBuilder : FactoryBuilder<User> {
        private var name: String = "Default Name"
        private var email: String = "default@example.com"
        private var isActive: Boolean = false
        private var age: Int? = null

        fun withName(value: String): UserBuilder = apply { name = value }

        fun withEmail(value: String): UserBuilder = apply { email = value }

        fun withActive(value: Boolean): UserBuilder = apply { isActive = value }

        fun withAge(value: Int): UserBuilder = apply { age = value }

        override fun build(): User = User(name, email, isActive, age)
    }

    class ActiveUserTrait : Trait<UserBuilder> {
        override fun apply(builder: UserBuilder): UserBuilder = builder.withActive(true)
    }

    class AdultUserTrait : Trait<UserBuilder> {
        override fun apply(builder: UserBuilder): UserBuilder = builder.withAge(30)
    }

    class UserFactory : Factory<User, UserBuilder>() {
        override fun builder(): UserBuilder = UserBuilder()

        fun withTrait(trait: Trait<UserBuilder>): UserBuilder = trait.apply(builder())

        fun withTraits(vararg traits: Trait<UserBuilder>): UserBuilder {
            var builder = builder()
            traits.forEach { trait ->
                builder = trait.apply(builder)
            }
            return builder
        }
    }

    @Test
    fun `trait applies configuration to builder`() {
        val factory = UserFactory()
        val user = factory.withTrait(ActiveUserTrait()).build()

        assertThat(user.isActive).isTrue()
    }

    @Test
    fun `multiple traits can be applied`() {
        val factory = UserFactory()
        val user = factory.withTraits(ActiveUserTrait(), AdultUserTrait()).build()

        assertThat(user.isActive).isTrue()
        assertThat(user.age).isEqualTo(30)
    }

    @Test
    fun `trait can be combined with manual configuration`() {
        val factory = UserFactory()
        val user =
            factory
                .withTrait(ActiveUserTrait())
                .withName("Alice")
                .withEmail("alice@example.com")
                .build()

        assertThat(user.name).isEqualTo("Alice")
        assertThat(user.isActive).isTrue()
    }

    @Test
    fun `lambda trait for inline configuration`() {
        val emailDomainTrait =
            object : Trait<UserBuilder> {
                override fun apply(builder: UserBuilder): UserBuilder =
                    builder.withEmail("user@company.com")
            }

        val factory = UserFactory()
        val user = factory.withTrait(emailDomainTrait).build()

        assertThat(user.email).isEqualTo("user@company.com")
    }
}
