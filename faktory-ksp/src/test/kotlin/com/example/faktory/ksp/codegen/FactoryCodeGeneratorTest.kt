package com.example.faktory.ksp.codegen

import com.example.faktory.ksp.metadata.TableMetadata
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FactoryCodeGeneratorTest {
    @Test
    fun `generate factory interface with required fields`() {
        val metadata =
            TableMetadata(
                tableName = "users",
                requiredFields = listOf("name", "email"),
            )

        val code =
            FactoryCodeGenerator.generate(
                tableName = "users",
                recordClassName = "UsersRecord",
                metadata = metadata,
                foreignKeys = emptyList(),
            )

        assertThat(code).contains("interface UsersFactoryBuilder")
        assertThat(code).contains("withName")
        assertThat(code).contains("withEmail")
        assertThat(code).contains("String")
    }

    @Test
    fun `generate complete factory with phantom types and builder`() {
        val metadata =
            TableMetadata(
                tableName = "users",
                requiredFields = listOf("name", "email"),
            )

        val code =
            FactoryCodeGenerator.generateComplete(
                tableName = "users",
                recordClassName = "UsersRecord",
                metadata = metadata,
                foreignKeys = emptyList(),
            )

        assertThat(code).contains("sealed interface UsersFieldState")
        assertThat(code).contains("object WithName")
        assertThat(code).contains("object WithEmail")
        assertThat(code).contains("object Complete")
        assertThat(code).contains("interface UsersFactoryBuilder<S : UsersFieldState>")
        assertThat(code).contains("fun withName")
        assertThat(code).contains("fun withEmail")
    }
}
