package com.example.faktory.ksp.codegen

import com.example.faktory.ksp.metadata.ForeignKeyConstraint
import com.example.faktory.ksp.metadata.TableMetadata
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FactoryCodeGeneratorTest {
    @Test
    fun `generate factory interface with required fields`() {
        val metadata = TableMetadata(
            tableName = "users",
            requiredFields = listOf("name", "email"),
        )

        val code = FactoryCodeGenerator.generate(
            tableName = "users",
            recordClassName = "UsersRecord",
            metadata = metadata,
            foreignKeys = emptyList(),
        )

        println("Generated code:\n$code")

        assertThat(code).contains("interface UsersFactoryBuilder")
        assertThat(code).contains("withName")
        assertThat(code).contains("withEmail")
        assertThat(code).contains("String")
    }
}
