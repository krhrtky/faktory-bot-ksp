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
        val source = SourceFile.kotlin(
            "UserFactory.kt",
            """
            package com.example.test

            import com.example.faktory.ksp.Factory

            @Factory(tableName = "users")
            class UserFactory
            """.trimIndent(),
        )

        val compilation = KotlinCompilation().apply {
            sources = listOf(source)
            symbolProcessorProviders = listOf(FactoryProcessorProvider())
            workingDir = tempDir
            inheritClassPath = true
            verbose = false
        }

        val result = compilation.compile()

        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)

        val kspGeneratedFiles = compilation.kspSourcesDir.walkTopDown().filter { it.isFile }.toList()
        val generatedFile = kspGeneratedFiles
            .firstOrNull { it.name == "UserFactoryBuilder.kt" }

        assertThat(generatedFile).isNotNull()
        assertThat(generatedFile!!.readText()).contains("interface UserFactoryBuilder")
    }

    @Test
    fun `use table name from annotation parameter`() {
        val source = SourceFile.kotlin(
            "PostFactory.kt",
            """
            package com.example.test

            import com.example.faktory.ksp.Factory

            @Factory(tableName = "posts")
            class PostFactory
            """.trimIndent(),
        )

        val compilation = KotlinCompilation().apply {
            sources = listOf(source)
            symbolProcessorProviders = listOf(FactoryProcessorProvider())
            workingDir = tempDir
            inheritClassPath = true
            verbose = false
        }

        val result = compilation.compile()

        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)

        val kspGeneratedFiles = compilation.kspSourcesDir.walkTopDown().filter { it.isFile }.toList()
        val generatedFile = kspGeneratedFiles
            .firstOrNull { it.name == "PostFactoryBuilder.kt" }

        assertThat(generatedFile).isNotNull()
    }

    @Test
    fun `generate builder methods based on jOOQ table structure`() {
        val source = SourceFile.kotlin(
            "PostFactory.kt",
            """
            package com.example.test

            import com.example.faktory.ksp.Factory

            @Factory(tableName = "posts")
            class PostFactory
            """.trimIndent(),
        )

        val compilation = KotlinCompilation().apply {
            sources = listOf(source)
            symbolProcessorProviders = listOf(FactoryProcessorProvider())
            workingDir = tempDir
            inheritClassPath = true
            verbose = false
        }

        val result = compilation.compile()

        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)

        val kspGeneratedFiles = compilation.kspSourcesDir.walkTopDown().filter { it.isFile }.toList()
        val generatedFile = kspGeneratedFiles
            .firstOrNull { it.name == "PostFactoryBuilder.kt" }

        assertThat(generatedFile).isNotNull()
        val content = generatedFile!!.readText()
        assertThat(content).contains("withUserId")
        assertThat(content).contains("withTitle")
        assertThat(content).contains("withContent")
    }
}
