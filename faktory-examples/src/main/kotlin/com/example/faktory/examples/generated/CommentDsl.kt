package com.example.faktory.examples.generated

import com.example.faktory.core.FactoryDsl
import com.example.faktory.examples.jooq.tables.records.CommentsRecord
import java.time.LocalDateTime

@FactoryDsl
class CommentsDslBuilder(
    var postId: Int,
    var userId: Int,
    var content: String,
) {
    var createdAt: LocalDateTime? = null

    internal fun build(): CommentsRecord =
        CommentsRecord().apply {
            this.postId = this@CommentsDslBuilder.postId
            this.userId = this@CommentsDslBuilder.userId
            this.content = this@CommentsDslBuilder.content
            this.createdAt = this@CommentsDslBuilder.createdAt
        }
}

fun comment(
    postId: Int,
    userId: Int,
    content: String,
    block: CommentsDslBuilder.() -> Unit = {},
): CommentsRecord = CommentsDslBuilder(postId, userId, content).apply(block).build()
