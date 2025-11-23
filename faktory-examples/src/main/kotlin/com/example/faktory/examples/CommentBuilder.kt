package com.example.faktory.examples

import com.example.faktory.core.FactoryBuilder
import java.util.concurrent.atomic.AtomicInteger

class CommentBuilder(
    private val sequence: AtomicInteger = AtomicInteger(0),
) : FactoryBuilder<Comment> {
    private var postId: Int? = null
    private var userId: Int? = null
    private var content: String? = null

    fun withPostId(value: Int) = apply { this.postId = value }

    fun withUserId(value: Int) = apply { this.userId = value }

    fun withContent(value: String) = apply { this.content = value }

    fun withPost(post: Post) = apply { this.postId = post.id }

    fun withUser(user: User) = apply { this.userId = user.id }

    fun withSequence() =
        apply {
            val seq = sequence.incrementAndGet()
            this.content = "Comment $seq"
        }

    override fun build(): Comment =
        Comment(
            postId = postId ?: throw IllegalStateException("postId is required"),
            userId = userId ?: throw IllegalStateException("userId is required"),
            content = content ?: throw IllegalStateException("content is required"),
        )
}
