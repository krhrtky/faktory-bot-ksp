package com.example.faktory.examples.traits

import com.example.faktory.core.DslTrait
import com.example.faktory.examples.PostsDslBuilder
import java.time.LocalDateTime

object Published : DslTrait<PostsDslBuilder> {
    override fun invoke(builder: PostsDslBuilder) {
        builder.published = true
    }
}

object Draft : DslTrait<PostsDslBuilder> {
    override fun invoke(builder: PostsDslBuilder) {
        builder.published = false
    }
}

object PostWithCurrentTimestamp : DslTrait<PostsDslBuilder> {
    override fun invoke(builder: PostsDslBuilder) {
        builder.createdAt = LocalDateTime.now()
    }
}

class PostWithTimestamp(private val timestamp: LocalDateTime) : DslTrait<PostsDslBuilder> {
    override fun invoke(builder: PostsDslBuilder) {
        builder.createdAt = timestamp
    }
}
