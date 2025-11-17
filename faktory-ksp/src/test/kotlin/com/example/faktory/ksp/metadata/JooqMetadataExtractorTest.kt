package com.example.faktory.ksp.metadata

import com.example.faktory.examples.jooq.tables.Users.Companion.USERS
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class JooqMetadataExtractorTest {
    @Test
    fun `extract table name from jOOQ Table class`() {
        val metadata = JooqMetadataExtractor.extract(USERS)

        assertThat(metadata.tableName).isEqualTo("users")
    }

    @Test
    fun `extract NOT NULL fields from jOOQ Table`() {
        val metadata = JooqMetadataExtractor.extract(USERS)

        assertThat(metadata.requiredFields).containsExactlyInAnyOrder("name", "email")
    }
}
