package com.example.faktory.examples.generated

import com.example.faktory.examples.jooq.tables.Posts.Companion.POSTS
import com.example.faktory.examples.jooq.tables.Users.Companion.USERS
import com.example.faktory.examples.post
import com.example.faktory.examples.user
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
class PostDslTest {
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

        dsl.execute(
            """
            CREATE TABLE IF NOT EXISTS posts (
                id SERIAL PRIMARY KEY,
                user_id INT NOT NULL,
                title VARCHAR(255) NOT NULL,
                content TEXT NOT NULL,
                published BOOLEAN DEFAULT FALSE,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (user_id) REFERENCES users(id)
            )
            """.trimIndent(),
        )
    }

    @AfterEach
    fun teardown() {
        dataSource.close()
    }

    @Test
    fun `post() 必須フィールドのみでPostRecordを構築`() {
        val dsl = DSL.using(dataSource, SQLDialect.POSTGRES)

        val userRecord = user(name = "Test User", email = "test@example.com")
        dsl.executeInsert(userRecord)
        val insertedUser = dsl.selectFrom(USERS).fetchOne()!!

        val postRecord =
            post(
                user = insertedUser,
                title = "My First Post",
                content = "Hello, World!",
            )

        assertThat(postRecord.userId).isEqualTo(insertedUser.id)
        assertThat(postRecord.title).isEqualTo("My First Post")
        assertThat(postRecord.content).isEqualTo("Hello, World!")
        assertThat(postRecord.published).isNull()
        assertThat(postRecord.createdAt).isNull()
    }

    @Test
    fun `post() DSLブロックでオプショナルフィールドを設定`() {
        val dsl = DSL.using(dataSource, SQLDialect.POSTGRES)

        val userRecord = user(name = "Test User 2", email = "test2@example.com")
        dsl.executeInsert(userRecord)
        val insertedUser = dsl.selectFrom(USERS).where(USERS.EMAIL.eq("test2@example.com")).fetchOne()!!

        val postRecord =
            post(
                user = insertedUser,
                title = "Published Post",
                content = "This is published",
            ) {
                published = true
            }

        assertThat(postRecord.userId).isEqualTo(insertedUser.id)
        assertThat(postRecord.title).isEqualTo("Published Post")
        assertThat(postRecord.content).isEqualTo("This is published")
        assertThat(postRecord.published).isTrue()
        assertThat(postRecord.createdAt).isNull()
    }

    @Test
    fun `post() 関連するUserと一緒にDBに永続化`() {
        val dsl = DSL.using(dataSource, SQLDialect.POSTGRES)

        val userRecord = user(name = "Alice", email = "alice@example.com")
        dsl.executeInsert(userRecord)

        val insertedUser = dsl.selectFrom(USERS).fetchOne()
        assertThat(insertedUser).isNotNull

        val postRecord =
            post(
                user = insertedUser!!,
                title = "Alice's Post",
                content = "Content by Alice",
            ) {
                published = true
            }
        dsl.executeInsert(postRecord)

        val insertedPost = dsl.selectFrom(POSTS).fetchOne()

        assertThat(insertedPost).isNotNull
        assertThat(insertedPost?.title).isEqualTo("Alice's Post")
        assertThat(insertedPost?.userId).isEqualTo(insertedUser.id)
        assertThat(insertedPost?.published).isTrue()
    }

    @Test
    fun `post() 複数のPostRecordを生成してDBに永続化`() {
        val dsl = DSL.using(dataSource, SQLDialect.POSTGRES)

        val userRecord = user(name = "Bob", email = "bob@example.com")
        dsl.executeInsert(userRecord)
        val insertedUser = dsl.selectFrom(USERS).fetchOne()!!

        val posts =
            (1..3).map { index ->
                post(
                    user = insertedUser,
                    title = "Post $index",
                    content = "Content of post $index",
                ) {
                    published = index % 2 == 0
                }
            }

        posts.forEach { postRecord ->
            dsl.executeInsert(postRecord)
        }

        val count = dsl.selectCount().from(POSTS).fetchOne(0, Int::class.java)
        assertThat(count).isEqualTo(3)

        val allPosts = dsl.selectFrom(POSTS).orderBy(POSTS.ID).fetch()
        assertThat(allPosts).hasSize(3)
        assertThat(allPosts.map { it.title })
            .containsExactly("Post 1", "Post 2", "Post 3")
        assertThat(allPosts.map { it.published })
            .containsExactly(false, true, false)
    }

    @Test
    fun `post() タイムスタンプを明示的に設定`() {
        val dsl = DSL.using(dataSource, SQLDialect.POSTGRES)

        val userRecord = user(name = "Charlie", email = "charlie@example.com")
        dsl.executeInsert(userRecord)
        val insertedUser = dsl.selectFrom(USERS).where(USERS.EMAIL.eq("charlie@example.com")).fetchOne()!!

        val timestamp = java.time.LocalDateTime.now()
        val postRecord =
            post(
                user = insertedUser,
                title = "Timestamped Post",
                content = "Post with timestamp",
            ) {
                published = true
                createdAt = timestamp
            }

        assertThat(postRecord.userId).isEqualTo(insertedUser.id)
        assertThat(postRecord.title).isEqualTo("Timestamped Post")
        assertThat(postRecord.content).isEqualTo("Post with timestamp")
        assertThat(postRecord.published).isTrue()
        assertThat(postRecord.createdAt).isEqualTo(timestamp)
    }
}
