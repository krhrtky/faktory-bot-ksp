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
class CommentFactoryTest {
    @Container
    private val postgres =
        PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")

    private lateinit var dataSource: HikariDataSource
    private lateinit var userFactory: UserFactoryImpl
    private lateinit var postFactory: PostFactoryImpl
    private lateinit var commentFactory: CommentFactoryImpl

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

        dsl.execute(
            """
            CREATE TABLE IF NOT EXISTS comments (
                id SERIAL PRIMARY KEY,
                post_id INT NOT NULL,
                user_id INT NOT NULL,
                content TEXT NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (post_id) REFERENCES posts(id),
                FOREIGN KEY (user_id) REFERENCES users(id)
            )
            """.trimIndent(),
        )

        userFactory = UserFactoryImpl(dsl)
        postFactory = PostFactoryImpl(dsl)
        commentFactory = CommentFactoryImpl(dsl)
    }

    @AfterEach
    fun teardown() {
        dataSource.close()
    }

    @Test
    fun `build() メモリ内でCommentオブジェクトを構築`() {
        val comment =
            commentFactory
                .builder()
                .withPostId(1)
                .withUserId(1)
                .withContent("Great post!")
                .build()

        assertThat(comment.postId).isEqualTo(1)
        assertThat(comment.userId).isEqualTo(1)
        assertThat(comment.content).isEqualTo("Great post!")
    }

    @Test
    fun `withPost() Postエンティティから外部キーを設定`() {
        val post = Post(id = 1, userId = 1, title = "Test", content = "Content", published = false)

        val comment =
            commentFactory
                .builder()
                .withPost(post)
                .withUserId(1)
                .withContent("Nice!")
                .build()

        assertThat(comment.postId).isEqualTo(1)
    }

    @Test
    fun `withUser() Userエンティティから外部キーを設定`() {
        val user = User(id = 1, name = "Alice", email = "alice@example.com")

        val comment =
            commentFactory
                .builder()
                .withPostId(1)
                .withUser(user)
                .withContent("Thanks!")
                .build()

        assertThat(comment.userId).isEqualTo(1)
    }

    @Test
    fun `create() 関連するUserとPostと一緒にCommentを永続化`() {
        val dsl = DSL.using(dataSource, SQLDialect.POSTGRES)

        val user =
            userFactory
                .builder()
                .withName("Alice")
                .withEmail("alice@example.com")
                .build()
        val userRecord = userFactory.toRecord(user)
        dsl.executeInsert(userRecord)

        val insertedUser = dsl.selectFrom(userFactory.table()).fetchOne()
        assertThat(insertedUser).isNotNull

        val post =
            postFactory
                .builder()
                .withUserId(insertedUser!!.id!!)
                .withTitle("Alice's Post")
                .withContent("Content")
                .build()
        val postRecord = postFactory.toRecord(post)
        dsl.executeInsert(postRecord)

        val insertedPost = dsl.selectFrom(postFactory.table()).fetchOne()
        assertThat(insertedPost).isNotNull

        val comment =
            commentFactory
                .builder()
                .withPostId(insertedPost!!.id!!)
                .withUserId(insertedUser.id!!)
                .withContent("Great post, Alice!")
                .build()
        val commentRecord = commentFactory.toRecord(comment)
        dsl.executeInsert(commentRecord)

        val insertedComment = dsl.selectFrom(commentFactory.table()).fetchOne()

        assertThat(insertedComment).isNotNull
        assertThat(insertedComment?.content).isEqualTo("Great post, Alice!")
        assertThat(insertedComment?.postId).isEqualTo(insertedPost.id)
        assertThat(insertedComment?.userId).isEqualTo(insertedUser.id)
    }

    @Test
    fun `withSequence() 連番付きCommentを生成`() {
        val comment1 =
            commentFactory
                .builder()
                .withPostId(1)
                .withUserId(1)
                .withSequence()
                .build()
        val comment2 =
            commentFactory
                .builder()
                .withPostId(1)
                .withUserId(1)
                .withSequence()
                .build()

        assertThat(comment1.content).isEqualTo("Comment 1")
        assertThat(comment2.content).isEqualTo("Comment 2")
    }
}
