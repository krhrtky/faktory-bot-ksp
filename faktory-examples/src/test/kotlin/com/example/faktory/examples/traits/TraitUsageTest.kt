package com.example.faktory.examples.traits

import com.example.faktory.core.DslTrait
import com.example.faktory.core.trait
import com.example.faktory.examples.UsersDslBuilder
import com.example.faktory.examples.user
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class TraitUsageTest {
    @Test
    fun `single trait can be applied inline`() {
        val userRecord =
            user(name = "Alice", email = "alice@example.com") {
                trait(ActiveUser)
            }

        assertThat(userRecord.name).isEqualTo("Alice")
        assertThat(userRecord.email).isEqualTo("alice@example.com")
        assertThat(userRecord.age).isEqualTo(25)
    }

    @Test
    fun `multiple traits can be combined`() {
        val userRecord =
            user(name = "Bob", email = "bob@example.com") {
                trait(SeniorUser)
                trait(WithCurrentTimestamp)
            }

        assertThat(userRecord.age).isEqualTo(65)
        assertThat(userRecord.createdAt).isNotNull()
    }

    @Test
    fun `traits can be combined with manual configuration`() {
        val userRecord =
            user(name = "Charlie", email = "charlie@example.com") {
                trait(YoungAdult)
                age = 21
            }

        assertThat(userRecord.age).isEqualTo(21)
    }

    @Test
    fun `applyTrait helper function`() {
        val userRecord =
            user(name = "David", email = "david@example.com") {
                trait(ActiveUser)
            }

        assertThat(userRecord.age).isEqualTo(25)
    }

    @Test
    fun `applyTraits for multiple traits`() {
        val userRecord =
            user(name = "Eve", email = "eve@example.com") {
                trait(SeniorUser, WithCurrentTimestamp)
            }

        assertThat(userRecord.age).isEqualTo(65)
        assertThat(userRecord.createdAt).isNotNull()
    }

    @Test
    fun `parameterized trait class`() {
        val userRecord =
            user(name = "Frank", email = "frank@example.com") {
                trait(WithAge(35))
            }

        assertThat(userRecord.age).isEqualTo(35)
    }

    @Test
    fun `custom inline trait implementation`() {
        val customTrait =
            object : DslTrait<UsersDslBuilder> {
                override fun invoke(builder: UsersDslBuilder) {
                    builder.age = 50
                    builder.createdAt = LocalDateTime.of(2024, 1, 1, 0, 0)
                }
            }

        val userRecord =
            user(name = "Grace", email = "grace@example.com") {
                trait(customTrait)
            }

        assertThat(userRecord.age).isEqualTo(50)
        assertThat(userRecord.createdAt).isEqualTo(LocalDateTime.of(2024, 1, 1, 0, 0))
    }

    @Test
    fun `trait composition with custom implementation`() {
        val composedTrait =
            object : DslTrait<UsersDslBuilder> {
                override fun invoke(builder: UsersDslBuilder) {
                    builder.trait(ActiveUser)
                    builder.trait(WithCurrentTimestamp)
                }
            }

        val userRecord =
            user(name = "Henry", email = "henry@example.com") {
                trait(composedTrait)
            }

        assertThat(userRecord.age).isEqualTo(25)
        assertThat(userRecord.createdAt).isNotNull()
    }

    @Test
    fun `reusable trait pattern library`() {
        val defaultUser =
            object : DslTrait<UsersDslBuilder> {
                override fun invoke(builder: UsersDslBuilder) {
                    builder.age = 30
                }
            }

        val testUser =
            object : DslTrait<UsersDslBuilder> {
                override fun invoke(builder: UsersDslBuilder) {
                    builder.age = 25
                    builder.createdAt = LocalDateTime.of(2024, 1, 1, 0, 0)
                }
            }

        val user1 =
            user(name = "User1", email = "user1@example.com") {
                trait(defaultUser)
            }

        val user2 =
            user(name = "User2", email = "user2@example.com") {
                trait(testUser)
            }

        assertThat(user1.age).isEqualTo(30)
        assertThat(user2.age).isEqualTo(25)
        assertThat(user2.createdAt).isEqualTo(LocalDateTime.of(2024, 1, 1, 0, 0))
    }
}
