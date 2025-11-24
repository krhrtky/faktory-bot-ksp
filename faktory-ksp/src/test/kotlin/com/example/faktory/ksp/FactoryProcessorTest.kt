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

    @Test
    fun `generate factory builder for annotated class`() {
        val usersTableStub =
            SourceFile.kotlin(
                "Users.kt",
                """
                package com.example.test.jooq.tables

                import org.jooq.TableField
                import org.jooq.impl.TableImpl
                import com.example.test.jooq.tables.records.UsersRecord

                class Users : TableImpl<UsersRecord>("users") {
                    val NAME: TableField<UsersRecord, String> = createField("name", org.jooq.impl.SQLDataType.VARCHAR, this)
                    val EMAIL: TableField<UsersRecord, String> = createField("email", org.jooq.impl.SQLDataType.VARCHAR, this)
                }
                """.trimIndent(),
            )

        val jooqStub =
            SourceFile.kotlin(
                "UsersRecord.kt",
                """
                package com.example.test.jooq.tables.records

                import org.jooq.impl.TableRecordImpl

                class UsersRecord : TableRecordImpl<UsersRecord>(null)
                """.trimIndent(),
            )

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
                sources = listOf(usersTableStub, jooqStub, source)
                symbolProcessorProviders = listOf(FactoryProcessorProvider())
                workingDir = tempDir
                inheritClassPath = true
                verbose = false
            }

        val result = compilation.compile()

        if (result.exitCode != KotlinCompilation.ExitCode.OK) {
            println("Compilation failed with messages:")
            println(result.messages)
        }
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)

        val kspGeneratedFiles = compilation.kspSourcesDir.walkTopDown().filter { it.isFile }.toList()
        val generatedFile =
            kspGeneratedFiles
                .firstOrNull { it.name == "UsersDsl.kt" }

        assertThat(generatedFile).isNotNull()
        assertThat(generatedFile!!.readText()).contains("class UsersDslBuilder")
        assertThat(generatedFile.readText()).contains("fun user(")
    }

    @Test
    fun `use table name from annotation parameter`() {
        val postsTableStub =
            SourceFile.kotlin(
                "Posts.kt",
                """
                package com.example.test.jooq.tables

                import org.jooq.TableField
                import org.jooq.impl.TableImpl
                import com.example.test.jooq.tables.records.PostsRecord

                class Posts : TableImpl<PostsRecord>("posts") {
                    val USER_ID: TableField<PostsRecord, Int> = createField("user_id", org.jooq.impl.SQLDataType.INTEGER, this)
                    val TITLE: TableField<PostsRecord, String> = createField("title", org.jooq.impl.SQLDataType.VARCHAR, this)
                    val CONTENT: TableField<PostsRecord, String> = createField("content", org.jooq.impl.SQLDataType.VARCHAR, this)
                }
                """.trimIndent(),
            )

        val jooqStub =
            SourceFile.kotlin(
                "PostsRecord.kt",
                """
                package com.example.test.jooq.tables.records

                import org.jooq.impl.TableRecordImpl

                class PostsRecord : TableRecordImpl<PostsRecord>(null)
                """.trimIndent(),
            )

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
                sources = listOf(postsTableStub, jooqStub, source)
                symbolProcessorProviders = listOf(FactoryProcessorProvider())
                workingDir = tempDir
                inheritClassPath = true
                verbose = false
            }

        val result = compilation.compile()

        if (result.exitCode != KotlinCompilation.ExitCode.OK) {
            println("Compilation failed with messages:")
            println(result.messages)
        }
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)

        val kspGeneratedFiles = compilation.kspSourcesDir.walkTopDown().filter { it.isFile }.toList()
        val generatedFile =
            kspGeneratedFiles
                .firstOrNull { it.name == "PostsDsl.kt" }

        assertThat(generatedFile).isNotNull()
        assertThat(generatedFile!!.readText()).contains("class PostsDslBuilder")
        assertThat(generatedFile.readText()).contains("fun post(")
    }

    @Test
    fun `generate builder methods based on jOOQ table structure`() {
        val postsTableStub =
            SourceFile.kotlin(
                "Posts.kt",
                """
                package com.example.test.jooq.tables

                import org.jooq.TableField
                import org.jooq.impl.TableImpl
                import com.example.test.jooq.tables.records.PostsRecord

                class Posts : TableImpl<PostsRecord>("posts") {
                    val USER_ID: TableField<PostsRecord, Int> = createField("user_id", org.jooq.impl.SQLDataType.INTEGER, this)
                    val TITLE: TableField<PostsRecord, String> = createField("title", org.jooq.impl.SQLDataType.VARCHAR, this)
                    val CONTENT: TableField<PostsRecord, String> = createField("content", org.jooq.impl.SQLDataType.VARCHAR, this)
                }
                """.trimIndent(),
            )

        val jooqStub =
            SourceFile.kotlin(
                "PostsRecord.kt",
                """
                package com.example.test.jooq.tables.records

                import org.jooq.impl.TableRecordImpl

                class PostsRecord : TableRecordImpl<PostsRecord>(null)
                """.trimIndent(),
            )

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
                sources = listOf(postsTableStub, jooqStub, source)
                symbolProcessorProviders = listOf(FactoryProcessorProvider())
                workingDir = tempDir
                inheritClassPath = true
                verbose = false
            }

        val result = compilation.compile()

        if (result.exitCode != KotlinCompilation.ExitCode.OK) {
            println("Compilation failed with messages:")
            println(result.messages)
        }
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)

        val kspGeneratedFiles = compilation.kspSourcesDir.walkTopDown().filter { it.isFile }.toList()
        val generatedFile =
            kspGeneratedFiles
                .firstOrNull { it.name == "PostsDsl.kt" }

        assertThat(generatedFile).isNotNull()
        val content = generatedFile!!.readText()
        assertThat(content).contains("class PostsDslBuilder")
        assertThat(content).contains("fun post(")
    }
}
