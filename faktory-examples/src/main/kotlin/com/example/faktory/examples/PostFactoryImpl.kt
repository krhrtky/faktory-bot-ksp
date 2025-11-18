package com.example.faktory.examples

import com.example.faktory.core.PersistableFactory
import com.example.faktory.examples.jooq.tables.Posts.Companion.POSTS
import com.example.faktory.examples.jooq.tables.records.PostsRecord
import org.jooq.DSLContext

class PostFactoryImpl(
    dsl: DSLContext,
) : PersistableFactory<PostsRecord, Post, PostBuilder>(dsl) {
    override fun builder() = PostBuilder()

    override fun table() = POSTS

    override fun toRecord(entity: Post): PostsRecord =
        PostsRecord().apply {
            id = entity.id
            userId = entity.userId
            title = entity.title
            content = entity.content
            published = entity.published
        }
}
