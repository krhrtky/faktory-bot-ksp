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
class PostFactoryTest {
    @Container
    private val postgres =
        PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")

    private lateinit var dataSource: HikariDataSource
    private lateinit var userFactory: UserFactoryImpl
    private lateinit var postFactory: PostFactoryImpl

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

        userFactory = UserFactoryImpl(dsl)
        postFactory = PostFactoryImpl(dsl)
    }

    @AfterEach
    fun teardown() {
        dataSource.close()
    }

    @Test
    fun `build() メモリ内でPostオブジェクトを構築`() {
        val post =
            postFactory
                .builder()
                .withUserId(1)
                .withTitle("My First Post")
                .withContent("Hello, World!")
                .build()

        assertThat(post.userId).isEqualTo(1)
        assertThat(post.title).isEqualTo("My First Post")
        assertThat(post.content).isEqualTo("Hello, World!")
        assertThat(post.published).isFalse()
    }

    @Test
    fun `withUser() Userエンティティから外部キーを設定`() {
        val user =
            userFactory
                .builder()
                .withName("Alice")
                .withEmail("alice@example.com")
                .build()

        val userWithId = user.copy(id = 1)

        val post =
            postFactory
                .builder()
                .withUser(userWithId)
                .withTitle("Alice's Post")
                .withContent("Content by Alice")
                .build()

        assertThat(post.userId).isEqualTo(1)
    }

    @Test
    fun `create() 関連するUserと一緒にPostを永続化`() {
        val dsl = DSL.using(dataSource, SQLDialect.POSTGRES)

        val user =
            userFactory
                .builder()
                .withName("Bob")
                .withEmail("bob@example.com")
                .build()
        val userRecord = userFactory.toRecord(user)
        dsl.executeInsert(userRecord)

        val insertedUser = dsl.selectFrom(userFactory.table()).fetchOne()
        assertThat(insertedUser).isNotNull

        val post =
            postFactory
                .builder()
                .withUserId(insertedUser!!.id!!)
                .withTitle("Bob's Post")
                .withContent("Content by Bob")
                .withPublished(true)
                .build()
        val postRecord = postFactory.toRecord(post)
        dsl.executeInsert(postRecord)

        val insertedPost = dsl.selectFrom(postFactory.table()).fetchOne()

        assertThat(insertedPost).isNotNull
        assertThat(insertedPost?.title).isEqualTo("Bob's Post")
        assertThat(insertedPost?.userId).isEqualTo(insertedUser.id)
        assertThat(insertedPost?.published).isTrue()
    }

    @Test
    fun `buildList() 複数のPostを生成`() {
        val posts = postFactory.buildList(3)

        assertThat(posts).hasSize(3)
    }

    @Test
    fun `withSequence() 連番付きPostを生成`() {
        val post1 =
            postFactory
                .builder()
                .withUserId(1)
                .withSequence()
                .build()
        val post2 =
            postFactory
                .builder()
                .withUserId(1)
                .withSequence()
                .build()

        assertThat(post1.title).isEqualTo("Post 1")
        assertThat(post1.content).isEqualTo("Content of post 1")

        assertThat(post2.title).isEqualTo("Post 2")
        assertThat(post2.content).isEqualTo("Content of post 2")
    }
}
