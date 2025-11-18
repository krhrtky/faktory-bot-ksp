package com.example.faktory.examples

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
class UserFactoryTest {
    @Container
    private val postgres =
        PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")

    private lateinit var dataSource: HikariDataSource
    private lateinit var userFactory: UserFactoryImpl

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

        userFactory = UserFactoryImpl(dsl)
    }

    @AfterEach
    fun teardown() {
        dataSource.close()
    }

    @Test
    fun `build() メモリ内でUserオブジェクトを構築`() {
        val user =
            userFactory
                .builder()
                .withName("Alice")
                .withEmail("alice@example.com")
                .build()

        assertThat(user.name).isEqualTo("Alice")
        assertThat(user.email).isEqualTo("alice@example.com")
        assertThat(user.age).isNull()
    }

    @Test
    fun `create() DBに永続化してUserを作成`() {
        val user =
            userFactory
                .builder()
                .withName("Bob")
                .withEmail("bob@example.com")
                .withAge(30)
                .build()

        val dsl = DSL.using(dataSource, SQLDialect.POSTGRES)
        val record = userFactory.toRecord(user)
        dsl.executeInsert(record)

        val inserted = dsl.selectFrom(userFactory.table()).fetchOne()

        assertThat(inserted).isNotNull
        assertThat(inserted?.name).isEqualTo("Bob")
        assertThat(inserted?.email).isEqualTo("bob@example.com")
        assertThat(inserted?.age).isEqualTo(30)
    }

    @Test
    fun `buildList() 複数のUserを生成`() {
        val users = userFactory.buildList(3)

        assertThat(users).hasSize(3)
        assertThat(users.map { it.name })
            .containsExactly("User 1", "User 2", "User 3")
    }

    @Test
    fun `createList() 複数のUserをDBに永続化`() {
        val users =
            userFactory.createList(5).mapIndexed { index, _ ->
                userFactory
                    .builder()
                    .withName("User ${index + 1}")
                    .withEmail("user${index + 1}@example.com")
                    .build()
            }

        val dsl = DSL.using(dataSource, SQLDialect.POSTGRES)
        users.forEach { user ->
            val record = userFactory.toRecord(user)
            dsl.executeInsert(record)
        }

        val count = dsl.selectCount().from(userFactory.table()).fetchOne(0, Int::class.java)
        assertThat(count).isEqualTo(5)
    }

    @Test
    fun `withSequence() 連番付きUserを生成`() {
        val user1 = userFactory.builder().withSequence().build()
        val user2 = userFactory.builder().withSequence().build()

        assertThat(user1.name).isEqualTo("User 1")
        assertThat(user1.email).isEqualTo("user1@example.com")

        assertThat(user2.name).isEqualTo("User 2")
        assertThat(user2.email).isEqualTo("user2@example.com")
    }
}
