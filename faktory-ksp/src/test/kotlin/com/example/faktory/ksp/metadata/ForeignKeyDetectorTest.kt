package com.example.faktory.ksp.metadata

import com.example.faktory.examples.jooq.tables.Comments.Companion.COMMENTS
import com.example.faktory.examples.jooq.tables.Posts.Companion.POSTS
import com.example.faktory.examples.jooq.tables.Users.Companion.USERS
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ForeignKeyDetectorTest {
    @Test
    fun `detect foreign key constraints from jOOQ Table`() {
        val foreignKeys = ForeignKeyDetector.detect(POSTS)

        assertThat(foreignKeys).hasSize(1)
        assertThat(foreignKeys[0].fieldName).isEqualTo("user_id")
        assertThat(foreignKeys[0].referencedTable).isEqualTo("users")
    }

    @Test
    fun `detect multiple foreign key constraints`() {
        val foreignKeys = ForeignKeyDetector.detect(COMMENTS)

        assertThat(foreignKeys).hasSize(2)
        assertThat(foreignKeys.map { it.fieldName })
            .containsExactlyInAnyOrder("post_id", "user_id")
    }

    @Test
    fun `no foreign keys when table has none`() {
        val foreignKeys = ForeignKeyDetector.detect(USERS)

        assertThat(foreignKeys).isEmpty()
    }
}
