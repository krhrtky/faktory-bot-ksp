package com.example.faktory.ksp.codegen

import com.example.faktory.ksp.metadata.TableMetadata
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PhantomTypeGeneratorTest {
    @Test
    fun `generate field state sealed interface`() {
        val metadata =
            TableMetadata(
                tableName = "users",
                requiredFields = listOf("name", "email"),
            )

        val code =
            PhantomTypeGenerator.generate(
                recordClassName = "UsersRecord",
                metadata = metadata,
            )

        assertThat(code).contains("sealed interface UsersFieldState")
    }

    @Test
    fun `generate state objects for each required field`() {
        val metadata =
            TableMetadata(
                tableName = "users",
                requiredFields = listOf("name", "email"),
            )

        val code =
            PhantomTypeGenerator.generate(
                recordClassName = "UsersRecord",
                metadata = metadata,
            )

        assertThat(code).contains("object WithName")
        assertThat(code).contains("object WithEmail")
        assertThat(code).contains("object Complete")
    }
}
