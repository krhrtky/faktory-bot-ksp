package com.example.faktory.examples.generated

import com.example.faktory.examples.jooq.tables.Users.Companion.USERS
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.assertj.core.api.Assertions.assertThat
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
class UserDslTest {
    @Container
    private val postgres =
        PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")

    private lateinit var dataSource: HikariDataSource

    @BeforeEach
    fun setup() {
        val config =
            HikariConfig().apply {
                jdbcUrl = postgres.jdbcUrl
                username = postgres.username
                password = postgres.password
                maximumPoolSize = 5
            }
        dataSource = HikariDataSource(config)

        val dsl = DSL.using(dataSource, SQLDialect.POSTGRES)

        dsl.execute(
            """
            CREATE TABLE IF NOT EXISTS users (
                id SERIAL PRIMARY KEY,
                name VARCHAR(255) NOT NULL,
                email VARCHAR(255) NOT NULL UNIQUE,
                age INT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """.trimIndent(),
        )
    }

    @AfterEach
    fun teardown() {
        dataSource.close()
    }

    @Test
    fun `user() 必須フィールドのみでUserRecordを構築`() {
        val userRecord = user(name = "Alice", email = "alice@example.com")

        assertThat(userRecord.name).isEqualTo("Alice")
        assertThat(userRecord.email).isEqualTo("alice@example.com")
        assertThat(userRecord.age).isNull()
        assertThat(userRecord.createdAt).isNull()
    }

    @Test
    fun `user() DSLブロックでオプショナルフィールドを設定`() {
        val userRecord =
            user(name = "Bob", email = "bob@example.com") {
                age = 30
            }

        assertThat(userRecord.name).isEqualTo("Bob")
        assertThat(userRecord.email).isEqualTo("bob@example.com")
        assertThat(userRecord.age).isEqualTo(30)
        assertThat(userRecord.createdAt).isNull()
    }

    @Test
    fun `user() DBに永続化してUserを作成`() {
        val userRecord =
            user(name = "Charlie", email = "charlie@example.com") {
                age = 25
            }

        val dsl = DSL.using(dataSource, SQLDialect.POSTGRES)
        dsl.executeInsert(userRecord)

        val inserted = dsl.selectFrom(USERS).fetchOne()

        assertThat(inserted).isNotNull
        assertThat(inserted?.name).isEqualTo("Charlie")
        assertThat(inserted?.email).isEqualTo("charlie@example.com")
        assertThat(inserted?.age).isEqualTo(25)
    }

    @Test
    fun `user() 複数のUserRecordを生成してDBに永続化`() {
        val dsl = DSL.using(dataSource, SQLDialect.POSTGRES)

        val users =
            (1..3).map { index ->
                user(name = "User $index", email = "user$index@example.com") {
                    age = 20 + index
                }
            }

        users.forEach { userRecord ->
            dsl.executeInsert(userRecord)
        }

        val count = dsl.selectCount().from(USERS).fetchOne(0, Int::class.java)
        assertThat(count).isEqualTo(3)

        val allUsers = dsl.selectFrom(USERS).orderBy(USERS.ID).fetch()
        assertThat(allUsers).hasSize(3)
        assertThat(allUsers.map { it.name })
            .containsExactly("User 1", "User 2", "User 3")
    }

    @Test
    fun `user() タイムスタンプを明示的に設定`() {
        val timestamp = java.time.LocalDateTime.now()
        val userRecord =
            user(name = "David", email = "david@example.com") {
                age = 35
                createdAt = timestamp
            }

        assertThat(userRecord.name).isEqualTo("David")
        assertThat(userRecord.email).isEqualTo("david@example.com")
        assertThat(userRecord.age).isEqualTo(35)
        assertThat(userRecord.createdAt).isEqualTo(timestamp)
    }
}
