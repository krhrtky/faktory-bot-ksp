package com.example.faktory.ksp.codegen

import com.example.faktory.ksp.metadata.TableMetadata
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BuilderCodeGeneratorTest {
    @Test
    fun `generate builder interface with type parameter`() {
        val metadata = TableMetadata(
            tableName = "users",
            requiredFields = listOf("name", "email"),
        )

        val code = BuilderCodeGenerator.generate(
            recordClassName = "UsersRecord",
            metadata = metadata,
        )

        assertThat(code).contains("interface UsersFactoryBuilder")
        assertThat(code).contains("<S : UsersFieldState>")
    }

    @Test
    fun `generate with methods that change state type`() {
        val metadata = TableMetadata(
            tableName = "users",
            requiredFields = listOf("name"),
        )

        val code = BuilderCodeGenerator.generate(
            recordClassName = "UsersRecord",
            metadata = metadata,
        )

        assertThat(code).contains("fun withName")
        assertThat(code).contains("UsersFactoryBuilder<UsersFieldState.WithName>")
    }

    @Test
    fun `generate build method constrained to Complete state`() {
        val metadata = TableMetadata(
            tableName = "users",
            requiredFields = listOf("name"),
        )

        val code = BuilderCodeGenerator.generate(
            recordClassName = "UsersRecord",
            metadata = metadata,
        )

        assertThat(code).contains("build()")
        assertThat(code).contains("UsersRecord")
        assertThat(code).contains("UsersFieldState.Complete")
    }
}
