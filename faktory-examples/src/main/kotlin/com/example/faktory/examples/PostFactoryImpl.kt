package com.example.faktory.examples

import com.example.faktory.core.PersistableFactory
import com.example.faktory.examples.jooq.tables.Posts.Companion.POSTS
import com.example.faktory.examples.jooq.tables.records.PostsRecord
import org.jooq.DSLContext

open class PostFactoryImpl(
    dsl: DSLContext,
) : PersistableFactory<PostsRecord, Post, PostBuilder>(dsl) {
    private val sequence = java.util.concurrent.atomic.AtomicInteger(0)

    override fun builder() = PostBuilder(sequence)

    override fun table() = POSTS

    override fun toRecord(entity: Post): PostsRecord =
        PostsRecord().apply {
            entity.id?.let { id = it }
            userId = entity.userId
            title = entity.title
            content = entity.content
            published = entity.published
        }
}
