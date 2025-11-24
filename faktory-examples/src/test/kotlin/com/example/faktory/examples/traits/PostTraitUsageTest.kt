package com.example.faktory.examples.traits

import com.example.faktory.core.DslTrait
import com.example.faktory.core.trait
import com.example.faktory.examples.PostsDslBuilder
import com.example.faktory.examples.post
import com.example.faktory.examples.user
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class PostTraitUsageTest {
    @Test
    fun `published trait sets post as published`() {
        val userRecord = user(name = "Alice", email = "alice@example.com")
        val postRecord =
            post(user = userRecord, title = "My Post", content = "Content") {
                trait(Published)
            }

        assertThat(postRecord.published).isTrue()
    }

    @Test
    fun `draft trait sets post as unpublished`() {
        val userRecord = user(name = "Bob", email = "bob@example.com")
        val postRecord =
            post(user = userRecord, title = "Draft Post", content = "Draft Content") {
                trait(Draft)
            }

        assertThat(postRecord.published).isFalse()
    }

    @Test
    fun `combine traits with manual configuration`() {
        val userRecord = user(name = "Charlie", email = "charlie@example.com")
        val timestamp = LocalDateTime.of(2024, 1, 1, 12, 0)

        val postRecord =
            post(user = userRecord, title = "Scheduled Post", content = "Content") {
                trait(Published)
                createdAt = timestamp
            }

        assertThat(postRecord.published).isTrue()
        assertThat(postRecord.createdAt).isEqualTo(timestamp)
    }

    @Test
    fun `applyTraits for multiple traits`() {
        val userRecord = user(name = "David", email = "david@example.com")
        val postRecord =
            post(user = userRecord, title = "Published with Timestamp", content = "Content") {
                trait(Published, PostWithCurrentTimestamp)
            }

        assertThat(postRecord.published).isTrue()
        assertThat(postRecord.createdAt).isNotNull()
    }

    @Test
    fun `custom trait for specific use case`() {
        val scheduledPost =
            object : DslTrait<PostsDslBuilder> {
                override fun invoke(builder: PostsDslBuilder) {
                    builder.published = false
                    builder.createdAt = LocalDateTime.now().plusDays(1)
                }
            }

        val userRecord = user(name = "Eve", email = "eve@example.com")
        val postRecord =
            post(user = userRecord, title = "Scheduled", content = "Future content") {
                trait(scheduledPost)
            }

        assertThat(postRecord.published).isFalse()
        assertThat(postRecord.createdAt).isAfter(LocalDateTime.now())
    }

    @Test
    fun `trait composition for complex scenarios`() {
        val publishedWithTimestamp =
            object : DslTrait<PostsDslBuilder> {
                override fun invoke(builder: PostsDslBuilder) {
                    builder.trait(Published)
                    builder.trait(PostWithCurrentTimestamp)
                }
            }

        val userRecord = user(name = "Frank", email = "frank@example.com")
        val postRecord =
            post(user = userRecord, title = "Composed", content = "Content") {
                trait(publishedWithTimestamp)
            }

        assertThat(postRecord.published).isTrue()
        assertThat(postRecord.createdAt).isNotNull()
    }
}
