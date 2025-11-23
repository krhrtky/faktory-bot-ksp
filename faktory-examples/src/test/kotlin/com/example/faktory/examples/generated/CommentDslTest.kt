package com.example.faktory.examples.generated

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class CommentDslTest {
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
            (1..3).map { index ->
                comment(
                    postId = 1,
                    userId = index,
                    content = "Comment $index",
                )
            }

        assertThat(comments).hasSize(3)
        assertThat(comments.map { it.content })
            .containsExactly("Comment 1", "Comment 2", "Comment 3")
        assertThat(comments.map { it.userId })
            .containsExactly(1, 2, 3)
    }

    @Test
    fun `異なるPostに対するCommentを生成できる`() {
        val comments =
            listOf(
                comment(postId = 1, userId = 1, content = "Comment on post 1"),
                comment(postId = 2, userId = 1, content = "Comment on post 2"),
                comment(postId = 1, userId = 2, content = "Another comment on post 1"),
            )

        assertThat(comments).hasSize(3)
        assertThat(comments.map { it.postId })
            .containsExactly(1, 2, 1)
    }

    @Test
    fun `comment() DSLで複数外部キーを正しく設定できる`() {
        val commentRecord = comment(postId = 10, userId = 20, content = "Multi-FK test")

        assertThat(commentRecord.postId).isEqualTo(10)
        assertThat(commentRecord.userId).isEqualTo(20)
        assertThat(commentRecord.content).isEqualTo("Multi-FK test")
    }
}
