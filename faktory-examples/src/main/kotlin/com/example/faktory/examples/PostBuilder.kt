package com.example.faktory.examples

import com.example.faktory.core.FactoryBuilder
import java.util.concurrent.atomic.AtomicInteger

class PostBuilder(
    private val sequence: AtomicInteger = AtomicInteger(0),
) : FactoryBuilder<Post> {
    private var userId: Int? = null
    private var title: String? = null
    private var content: String? = null
    private var published: Boolean = false

    fun withUserId(value: Int) = apply { this.userId = value }

    fun withUser(user: User) = apply { this.userId = user.id }

    fun withTitle(value: String) = apply { this.title = value }

    fun withContent(value: String) = apply { this.content = value }

    fun withPublished(value: Boolean) = apply { this.published = value }

    fun withSequence() =
        apply {
            val seq = sequence.incrementAndGet()
            this.title = "Post $seq"
            this.content = "Content of post $seq"
        }

    override fun build(): Post {
        val seq = sequence.get()
        return Post(
            userId = requireNotNull(userId) { "userId is required" },
            title = title ?: "Post ${sequence.incrementAndGet()}",
            content = content ?: "Content of post ${sequence.get()}",
            published = published,
        )
    }
}
