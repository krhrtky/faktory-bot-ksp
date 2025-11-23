package com.example.faktory.examples

import com.example.faktory.core.Trait
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TraitSystemTest {
    class PublishedPostTrait : Trait<PostBuilder> {
        override fun apply(builder: PostBuilder): PostBuilder = builder.withPublished(true)
    }

    class AdultUserTrait : Trait<UserBuilder> {
        override fun apply(builder: UserBuilder): UserBuilder = builder.withAge(30)
    }

    class EmailDomainTrait(private val domain: String) : Trait<UserBuilder> {
        override fun apply(builder: UserBuilder): UserBuilder {
            val currentEmail = "user@$domain"
            return builder.withEmail(currentEmail)
        }
    }

    class UserFactoryWithTraits {
        fun withTrait(trait: Trait<UserBuilder>): UserBuilder = trait.apply(UserBuilder())

        fun withTraits(vararg traits: Trait<UserBuilder>): UserBuilder {
            var builder = UserBuilder()
            traits.forEach { trait ->
                builder = trait.apply(builder)
            }
            return builder
        }
    }

    class PostFactoryWithTraits {
        fun withTrait(trait: Trait<PostBuilder>): PostBuilder = trait.apply(PostBuilder())

        fun withTraits(vararg traits: Trait<PostBuilder>): PostBuilder {
            var builder = PostBuilder()
            traits.forEach { trait ->
                builder = trait.apply(builder)
            }
            return builder
        }
    }

    @Test
    fun `PublishedPostTrait sets published to true`() {
        val factory = PostFactoryWithTraits()
        val post =
            factory
                .withTrait(PublishedPostTrait())
                .withUserId(1)
                .withTitle("Published Post")
                .withContent("Content")
                .build()

        assertThat(post.published).isTrue()
    }

    @Test
    fun `AdultUserTrait sets age to 30`() {
        val factory = UserFactoryWithTraits()
        val user =
            factory
                .withTrait(AdultUserTrait())
                .withName("Bob")
                .withEmail("bob@example.com")
                .build()

        assertThat(user.age).isEqualTo(30)
    }

    @Test
    fun `EmailDomainTrait sets email domain`() {
        val factory = UserFactoryWithTraits()
        val user =
            factory
                .withTrait(EmailDomainTrait("company.com"))
                .withName("Alice")
                .build()

        assertThat(user.email).isEqualTo("user@company.com")
    }

    @Test
    fun `multiple traits can be combined`() {
        val factory = UserFactoryWithTraits()
        val user =
            factory
                .withTraits(AdultUserTrait(), EmailDomainTrait("enterprise.com"))
                .withName("Charlie")
                .build()

        assertThat(user.age).isEqualTo(30)
        assertThat(user.email).isEqualTo("user@enterprise.com")
        assertThat(user.name).isEqualTo("Charlie")
    }

    @Test
    fun `inline trait with object expression`() {
        val factory = PostFactoryWithTraits()
        val draftTrait =
            object : Trait<PostBuilder> {
                override fun apply(builder: PostBuilder): PostBuilder = builder.withPublished(false)
            }

        val post =
            factory
                .withTrait(draftTrait)
                .withUserId(1)
                .withTitle("Draft Post")
                .withContent("Draft content")
                .build()

        assertThat(post.published).isFalse()
    }

    @Test
    fun `reusable traits across multiple entities`() {
        val adultTrait = AdultUserTrait()
        val factory = UserFactoryWithTraits()

        val user1 =
            factory
                .withTrait(adultTrait)
                .withName("User 1")
                .withEmail("user1@example.com")
                .build()

        val user2 =
            factory
                .withTrait(adultTrait)
                .withName("User 2")
                .withEmail("user2@example.com")
                .build()

        assertThat(user1.age).isEqualTo(30)
        assertThat(user2.age).isEqualTo(30)
    }

    @Test
    fun `trait for standard test data setup`() {
        val testUserTrait =
            object : Trait<UserBuilder> {
                override fun apply(builder: UserBuilder): UserBuilder =
                    builder
                        .withName("Test User")
                        .withEmail("test@example.com")
                        .withAge(25)
            }

        val factory = UserFactoryWithTraits()
        val user = factory.withTrait(testUserTrait).build()

        assertThat(user.name).isEqualTo("Test User")
        assertThat(user.email).isEqualTo("test@example.com")
        assertThat(user.age).isEqualTo(25)
    }
}
