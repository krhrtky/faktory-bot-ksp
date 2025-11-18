package com.example.faktory.examples.generated

import com.example.faktory.core.FactoryDsl
import com.example.faktory.examples.jooq.tables.records.PostsRecord
import java.time.LocalDateTime

@FactoryDsl
class PostsDslBuilder(
    var userId: Int,
    var title: String,
    var content: String,
) {
    var published: Boolean? = null
    var createdAt: LocalDateTime? = null

    internal fun build(): PostsRecord =
        PostsRecord().apply {
            this.userId = this@PostsDslBuilder.userId
            this.title = this@PostsDslBuilder.title
            this.content = this@PostsDslBuilder.content
            this.published = this@PostsDslBuilder.published
            this.createdAt = this@PostsDslBuilder.createdAt
        }
}

fun post(
    userId: Int,
    title: String,
    content: String,
    block: PostsDslBuilder.() -> Unit = {},
): PostsRecord = PostsDslBuilder(userId, title, content).apply(block).build()
