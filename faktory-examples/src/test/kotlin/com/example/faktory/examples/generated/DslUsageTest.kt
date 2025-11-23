package com.example.faktory.examples.generated

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class DslUsageTest {
    @Test
    fun `user() DSLで必須フィールドのみ指定してUserRecordを構築`() {
        val userRecord = user(name = "Alice", email = "alice@example.com")

        assertThat(userRecord.name).isEqualTo("Alice")
        assertThat(userRecord.email).isEqualTo("alice@example.com")
        assertThat(userRecord.age).isNull()
        assertThat(userRecord.createdAt).isNull()
    }

    @Test
    fun `user() DSLブロックでオプショナルフィールドを設定`() {
        val userRecord =
            user(name = "Bob", email = "bob@example.com") {
                age = 30
            }

        assertThat(userRecord.name).isEqualTo("Bob")
        assertThat(userRecord.email).isEqualTo("bob@example.com")
        assertThat(userRecord.age).isEqualTo(30)
        assertThat(userRecord.createdAt).isNull()
    }

    @Test
    fun `user() DSLで全フィールドを設定`() {
        val timestamp = LocalDateTime.now()
        val userRecord =
            user(name = "Charlie", email = "charlie@example.com") {
                age = 25
                createdAt = timestamp
            }

        assertThat(userRecord.name).isEqualTo("Charlie")
        assertThat(userRecord.email).isEqualTo("charlie@example.com")
        assertThat(userRecord.age).isEqualTo(25)
        assertThat(userRecord.createdAt).isEqualTo(timestamp)
    }

    @Test
    fun `post() DSLで必須フィールドのみ指定してPostRecordを構築`() {
        val postRecord =
            post(
                userId = 1,
                title = "My First Post",
                content = "Hello, World!",
            )

        assertThat(postRecord.userId).isEqualTo(1)
        assertThat(postRecord.title).isEqualTo("My First Post")
        assertThat(postRecord.content).isEqualTo("Hello, World!")
        assertThat(postRecord.published).isNull()
        assertThat(postRecord.createdAt).isNull()
    }

    @Test
    fun `post() DSLブロックでオプショナルフィールドを設定`() {
        val postRecord =
            post(
                userId = 1,
                title = "Published Post",
                content = "This is published",
            ) {
                published = true
            }

        assertThat(postRecord.userId).isEqualTo(1)
        assertThat(postRecord.title).isEqualTo("Published Post")
        assertThat(postRecord.content).isEqualTo("This is published")
        assertThat(postRecord.published).isTrue()
        assertThat(postRecord.createdAt).isNull()
    }

    @Test
    fun `post() DSLで全フィールドを設定`() {
        val timestamp = LocalDateTime.now()
        val postRecord =
            post(
                userId = 1,
                title = "Complete Post",
                content = "Full content",
            ) {
                published = false
                createdAt = timestamp
            }

        assertThat(postRecord.userId).isEqualTo(1)
        assertThat(postRecord.title).isEqualTo("Complete Post")
        assertThat(postRecord.content).isEqualTo("Full content")
        assertThat(postRecord.published).isFalse()
        assertThat(postRecord.createdAt).isEqualTo(timestamp)
    }

    @Test
    fun `複数のUserRecordを生成できる`() {
        val users =
            (1..5).map { index ->
                user(name = "User $index", email = "user$index@example.com") {
                    age = 20 + index
                }
            }

        assertThat(users).hasSize(5)
        assertThat(users.map { it.name })
            .containsExactly("User 1", "User 2", "User 3", "User 4", "User 5")
        assertThat(users.map { it.age })
            .containsExactly(21, 22, 23, 24, 25)
    }

    @Test
    fun `複数のPostRecordを生成できる`() {
        val posts =
            (1..3).map { index ->
                post(
                    userId = index,
                    title = "Post $index",
                    content = "Content of post $index",
                ) {
                    published = index % 2 == 0
                }
            }

        assertThat(posts).hasSize(3)
        assertThat(posts.map { it.title })
            .containsExactly("Post 1", "Post 2", "Post 3")
        assertThat(posts.map { it.published })
            .containsExactly(false, true, false)
    }

    @Test
    fun `comment() DSLで必須フィールドのみ指定してCommentRecordを構築`() {
        val commentRecord = comment(postId = 1, userId = 1, content = "Great post!")

        assertThat(commentRecord.postId).isEqualTo(1)
        assertThat(commentRecord.userId).isEqualTo(1)
        assertThat(commentRecord.content).isEqualTo("Great post!")
        assertThat(commentRecord.createdAt).isNull()
    }

    @Test
    fun `comment() DSLブロックでオプショナルフィールドを設定`() {
        val timestamp = LocalDateTime.now()
        val commentRecord =
            comment(postId = 1, userId = 2, content = "Nice!") {
                createdAt = timestamp
            }

        assertThat(commentRecord.postId).isEqualTo(1)
        assertThat(commentRecord.userId).isEqualTo(2)
        assertThat(commentRecord.content).isEqualTo("Nice!")
        assertThat(commentRecord.createdAt).isEqualTo(timestamp)
    }

    @Test
    fun `複数のCommentRecordを生成できる`() {
        val comments =
            (1..5).map { index ->
                comment(
                    postId = 1,
                    userId = index,
                    content = "Comment $index from user $index",
                )
            }

        assertThat(comments).hasSize(5)
        assertThat(comments.map { it.content })
            .containsExactly(
                "Comment 1 from user 1",
                "Comment 2 from user 2",
                "Comment 3 from user 3",
                "Comment 4 from user 4",
                "Comment 5 from user 5",
            )
    }

    @Test
    fun `comment() DSLで複数外部キーを正しく設定`() {
        val commentRecord = comment(postId = 10, userId = 20, content = "Multi-FK test")

        assertThat(commentRecord.postId).isEqualTo(10)
        assertThat(commentRecord.userId).isEqualTo(20)
        assertThat(commentRecord.content).isEqualTo("Multi-FK test")
    }
}
