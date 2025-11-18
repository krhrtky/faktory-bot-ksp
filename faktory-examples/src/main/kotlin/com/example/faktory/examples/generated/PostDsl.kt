package com.example.faktory.examples.generated

import com.example.faktory.core.AssociationContext
import com.example.faktory.core.FactoryDsl
import com.example.faktory.examples.jooq.tables.records.PostsRecord
import com.example.faktory.examples.jooq.tables.records.UsersRecord
import java.time.LocalDateTime

@FactoryDsl
class PostsDslBuilder(
    var title: String,
    var content: String,
) {
    var userId: Int? = null
    var published: Boolean? = null
    var createdAt: LocalDateTime? = null

    private val associationContext = AssociationContext()

    fun associate(block: AssociationContext.() -> Unit) {
        associationContext.block()
    }

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
    title: String,
    content: String,
    block: PostsDslBuilder.() -> Unit = {},
): PostsRecord = PostsDslBuilder(title, content).apply(block).build()

// Associate extension function for user foreign key
fun AssociationContext.user(block: () -> UsersRecord) {
    associateWithPersist("user_id", block)
}
