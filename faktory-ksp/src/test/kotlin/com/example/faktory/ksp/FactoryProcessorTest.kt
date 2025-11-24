package com.example.faktory.ksp

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.kspSourcesDir
import com.tschuchort.compiletesting.symbolProcessorProviders
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class FactoryProcessorTest {
    @TempDir
    lateinit var tempDir: File

    private fun createUsersTableMock(): SourceFile =
        SourceFile.kotlin(
            "Users.kt",
            """
            package com.example.faktory.examples.jooq.tables

            import org.jooq.TableField
            import org.jooq.impl.TableImpl
            import org.jooq.impl.DSL
            import org.jooq.impl.SQLDataType
            import com.example.faktory.examples.jooq.tables.records.UsersRecord

            class Users : TableImpl<UsersRecord>(DSL.name("users")) {
                val ID: TableField<UsersRecord, Int> = createField(DSL.name("id"), SQLDataType.INTEGER.nullable(false), this, "")
                val NAME: TableField<UsersRecord, String> = createField(DSL.name("name"), SQLDataType.VARCHAR(255).nullable(false), this, "")
                val EMAIL: TableField<UsersRecord, String> = createField(DSL.name("email"), SQLDataType.VARCHAR(255).nullable(false), this, "")
                val AGE: TableField<UsersRecord, Int?> = createField(DSL.name("age"), SQLDataType.INTEGER.nullable(true), this, "")
                val CREATED_AT: TableField<UsersRecord, java.time.LocalDateTime?> = createField(DSL.name("created_at"), SQLDataType.LOCALDATETIME.nullable(true), this, "")

                companion object {
                    val USERS = Users()
                }
            }
            """.trimIndent(),
        )

    private fun createUsersRecordMock(): SourceFile =
        SourceFile.kotlin(
            "UsersRecord.kt",
            """
            package com.example.faktory.examples.jooq.tables.records

            import org.jooq.impl.UpdatableRecordImpl
            import com.example.faktory.examples.jooq.tables.Users

            open class UsersRecord : UpdatableRecordImpl<UsersRecord>(Users.USERS) {
                var id: Int? = null
                var name: String? = null
                var email: String? = null
                var age: Int? = null
                var createdAt: java.time.LocalDateTime? = null
            }
            """.trimIndent(),
        )

    private fun createPostsTableMock(): SourceFile =
        SourceFile.kotlin(
            "Posts.kt",
            """
            package com.example.faktory.examples.jooq.tables

            import org.jooq.TableField
            import org.jooq.impl.TableImpl
            import org.jooq.impl.DSL
            import org.jooq.impl.SQLDataType
            import com.example.faktory.examples.jooq.tables.records.PostsRecord
            import com.example.faktory.examples.jooq.tables.records.UsersRecord

            class Posts : TableImpl<PostsRecord>(DSL.name("posts")) {
                val ID: TableField<PostsRecord, Int> = createField(DSL.name("id"), SQLDataType.INTEGER.nullable(false), this, "")
                val USER_ID: TableField<PostsRecord, Int> = createField(DSL.name("user_id"), SQLDataType.INTEGER.nullable(false), this, "")
                val TITLE: TableField<PostsRecord, String> = createField(DSL.name("title"), SQLDataType.VARCHAR(255).nullable(false), this, "")
                val CONTENT: TableField<PostsRecord, String> = createField(DSL.name("content"), SQLDataType.CLOB.nullable(false), this, "")
                val PUBLISHED: TableField<PostsRecord, Boolean?> = createField(DSL.name("published"), SQLDataType.BOOLEAN.nullable(true), this, "")
                val CREATED_AT: TableField<PostsRecord, java.time.LocalDateTime?> = createField(DSL.name("created_at"), SQLDataType.LOCALDATETIME.nullable(true), this, "")

                companion object {
                    val POSTS = Posts()
                }
            }
            """.trimIndent(),
        )

    private fun createPostsRecordMock(): SourceFile =
        SourceFile.kotlin(
            "PostsRecord.kt",
            """
            package com.example.faktory.examples.jooq.tables.records

            import org.jooq.impl.UpdatableRecordImpl
            import com.example.faktory.examples.jooq.tables.Posts

            open class PostsRecord : UpdatableRecordImpl<PostsRecord>(Posts.POSTS) {
                var id: Int? = null
                var userId: Int? = null
                var title: String? = null
                var content: String? = null
                var published: Boolean? = null
                var createdAt: java.time.LocalDateTime? = null
            }
            """.trimIndent(),
        )

    @Test
    fun `generate factory builder for annotated class`() {
        val source =
            SourceFile.kotlin(
                "UserFactory.kt",
                """
                package com.example.test

                import com.example.faktory.ksp.Factory

                @Factory(tableName = "users")
                class UserFactory
                """.trimIndent(),
            )

        val compilation =
            KotlinCompilation().apply {
                sources = listOf(source, createUsersTableMock(), createUsersRecordMock())
                symbolProcessorProviders = listOf(FactoryProcessorProvider())
                workingDir = tempDir
                inheritClassPath = true
                verbose = false
            }

        val result = compilation.compile()

        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)

        val kspGeneratedFiles = compilation.kspSourcesDir.walkTopDown().filter { it.isFile }.toList()
        val generatedFile =
            kspGeneratedFiles
                .firstOrNull { it.name == "UsersDsl.kt" }

        assertThat(generatedFile).isNotNull()
        assertThat(generatedFile!!.readText()).contains("fun user(")
    }

    @Test
    fun `use table name from annotation parameter`() {
        val source =
            SourceFile.kotlin(
                "PostFactory.kt",
                """
                package com.example.test

                import com.example.faktory.ksp.Factory

                @Factory(tableName = "posts")
                class PostFactory
                """.trimIndent(),
            )

        val compilation =
            KotlinCompilation().apply {
                sources = listOf(source, createPostsTableMock(), createPostsRecordMock())
                symbolProcessorProviders = listOf(FactoryProcessorProvider())
                workingDir = tempDir
                inheritClassPath = true
                verbose = false
            }

        val result = compilation.compile()

        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)

        val kspGeneratedFiles = compilation.kspSourcesDir.walkTopDown().filter { it.isFile }.toList()
        val generatedFile =
            kspGeneratedFiles
                .firstOrNull { it.name == "PostsDsl.kt" }

        assertThat(generatedFile).isNotNull()
    }

    @Test
    fun `generate builder methods based on jOOQ table structure`() {
        val source =
            SourceFile.kotlin(
                "PostFactory.kt",
                """
                package com.example.test

                import com.example.faktory.ksp.Factory

                @Factory(tableName = "posts")
                class PostFactory
                """.trimIndent(),
            )

        val compilation =
            KotlinCompilation().apply {
                sources = listOf(source, createPostsTableMock(), createPostsRecordMock())
                symbolProcessorProviders = listOf(FactoryProcessorProvider())
                workingDir = tempDir
                inheritClassPath = true
                verbose = false
            }

        val result = compilation.compile()

        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)

        val kspGeneratedFiles = compilation.kspSourcesDir.walkTopDown().filter { it.isFile }.toList()
        val generatedFile =
            kspGeneratedFiles
                .firstOrNull { it.name == "PostsDsl.kt" }

        assertThat(generatedFile).isNotNull()
        val content = generatedFile!!.readText()
        assertThat(content).contains("fun post(")
        assertThat(content).contains("title: ")
        assertThat(content).contains("content: ")
    }
}
