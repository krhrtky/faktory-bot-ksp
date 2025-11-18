package com.example.faktory.ksp.codegen

import com.example.faktory.ksp.metadata.ForeignKeyConstraint
import com.example.faktory.ksp.metadata.TableMetadata
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DslCodeGeneratorTest {
    @Test
    fun `generate DSL builder with required fields as constructor parameters`() {
        val metadata =
            TableMetadata(
                tableName = "users",
                requiredFields = listOf("name", "email"),
                optionalFields = listOf("age"),
            )

        val code = DslCodeGenerator.generate("UsersRecord", metadata)

        assertThat(code).contains("class UsersDslBuilder(")
        assertThat(code).contains("var name: String,")
        assertThat(code).contains("var email: String,")
    }

    @Test
    fun `generate DSL builder with optional fields as var properties`() {
        val metadata =
            TableMetadata(
                tableName = "users",
                requiredFields = listOf("name", "email"),
                optionalFields = listOf("age", "created_at"),
            )

        val code = DslCodeGenerator.generate("UsersRecord", metadata)

        assertThat(code).contains("var age:")
        assertThat(code).contains("var createdAt:")
    }

    @Test
    fun `generate DSL builder with @FactoryDsl annotation`() {
        val metadata =
            TableMetadata(
                tableName = "users",
                requiredFields = listOf("name"),
            )

        val code = DslCodeGenerator.generate("UsersRecord", metadata)

        assertThat(code).contains("@FactoryDsl")
        assertThat(code).contains("class UsersDslBuilder")
    }

    @Test
    fun `generate build method that creates jOOQ Record`() {
        val metadata =
            TableMetadata(
                tableName = "users",
                requiredFields = listOf("name", "email"),
            )

        val code = DslCodeGenerator.generate("UsersRecord", metadata)

        assertThat(code).contains("fun build(): UsersRecord")
        assertThat(code).contains("UsersRecord()")
    }

    @Test
    fun `generate top-level DSL function`() {
        val metadata =
            TableMetadata(
                tableName = "users",
                requiredFields = listOf("name", "email"),
                optionalFields = listOf("age"),
            )

        val code = DslCodeGenerator.generate("UsersRecord", metadata)

        assertThat(code).contains("fun user(")
        assertThat(code).contains("name: String,")
        assertThat(code).contains("email: String,")
        assertThat(code).contains("block: UsersDslBuilder.() -> Unit = {}")
        assertThat(code).contains("): UsersRecord")
    }

    @Test
    fun `convert snake_case to camelCase`() {
        val metadata =
            TableMetadata(
                tableName = "users",
                requiredFields = listOf("first_name", "last_name"),
            )

        val code = DslCodeGenerator.generate("UsersRecord", metadata)

        assertThat(code).contains("var firstName: String,")
        assertThat(code).contains("var lastName: String,")
    }

    @Test
    fun `generate() excludes foreign key fields from constructor parameters`() {
        val metadata =
            TableMetadata(
                tableName = "posts",
                requiredFields = listOf("user_id", "title", "content"),
                optionalFields = listOf("published", "created_at"),
                foreignKeys = listOf(
                    ForeignKeyConstraint(
                        fieldName = "user_id",
                        referencedTable = "users",
                    ),
                ),
            )

        val code = DslCodeGenerator.generate("PostsRecord", metadata)

        // user_idはコンストラクタパラメータに含まれない
        assertThat(code).doesNotContain("var userId: String,")
        assertThat(code).doesNotContain("var userId: Int,")
        // titleとcontentは必須パラメータ
        assertThat(code).contains("var title: String,")
        assertThat(code).contains("var content: String,")
        // user_idはオプショナルプロパティとして定義される
        assertThat(code).contains("var userId: Int? = null")
    }
}
