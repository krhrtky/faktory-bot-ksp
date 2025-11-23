package com.example.faktory.examples

import com.example.faktory.core.PersistableFactory
import com.example.faktory.examples.jooq.tables.Comments.Companion.COMMENTS
import com.example.faktory.examples.jooq.tables.records.CommentsRecord
import org.jooq.DSLContext

open class CommentFactoryImpl(
    dsl: DSLContext,
) : PersistableFactory<CommentsRecord, Comment, CommentBuilder>(dsl) {
    private val sequence = java.util.concurrent.atomic.AtomicInteger(0)

    override fun builder() = CommentBuilder(sequence)

    override fun table() = COMMENTS

    override fun toRecord(entity: Comment): CommentsRecord =
        CommentsRecord().apply {
            entity.id?.let { id = it }
            postId = entity.postId
            userId = entity.userId
            content = entity.content
        }
}
