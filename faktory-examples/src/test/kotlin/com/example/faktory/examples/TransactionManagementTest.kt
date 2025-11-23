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
class TransactionManagementTest {
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
    fun `transaction commits when successful`() {
        val dsl = DSL.using(dataSource, SQLDialect.POSTGRES)

        dsl.transaction { configuration ->
            val ctx = DSL.using(configuration)
            val userFactory = UserFactoryImpl(ctx)
            val postFactory = PostFactoryImpl(ctx)

            val user =
                userFactory
                    .builder()
                    .withName("Alice")
                    .withEmail("alice@example.com")
                    .build()
            val userRecord = userFactory.toRecord(user)
            ctx.executeInsert(userRecord)

            val insertedUser = ctx.selectFrom(userFactory.table()).fetchOne()
            assertThat(insertedUser).isNotNull

            val post =
                postFactory
                    .builder()
                    .withUserId(insertedUser!!.id!!)
                    .withTitle("Alice's Post")
                    .withContent("Content")
                    .build()
            val postRecord = postFactory.toRecord(post)
            ctx.executeInsert(postRecord)
        }

        val userCount = dsl.selectCount().from(UserFactoryImpl(dsl).table()).fetchOne(0, Int::class.java)
        val postCount = dsl.selectCount().from(PostFactoryImpl(dsl).table()).fetchOne(0, Int::class.java)

        assertThat(userCount).isEqualTo(1)
        assertThat(postCount).isEqualTo(1)
    }

    @Test
    fun `transaction rolls back on exception`() {
        val dsl = DSL.using(dataSource, SQLDialect.POSTGRES)

        try {
            dsl.transaction { configuration ->
                val ctx = DSL.using(configuration)
                val userFactory = UserFactoryImpl(ctx)

                val user =
                    userFactory
                        .builder()
                        .withName("Bob")
                        .withEmail("bob@example.com")
                        .build()
                val userRecord = userFactory.toRecord(user)
                ctx.executeInsert(userRecord)

                throw RuntimeException("Simulated error")
            }
        } catch (_: Exception) {
        }

        val count = dsl.selectCount().from(UserFactoryImpl(dsl).table()).fetchOne(0, Int::class.java)
        assertThat(count).isEqualTo(0)
    }

    @Test
    fun `nested factories in transaction`() {
        val dsl = DSL.using(dataSource, SQLDialect.POSTGRES)

        dsl.transaction { configuration ->
            val ctx = DSL.using(configuration)
            val userFactory = UserFactoryImpl(ctx)
            val postFactory = PostFactoryImpl(ctx)

            val users =
                (1..3).map { index ->
                    userFactory
                        .builder()
                        .withName("User $index")
                        .withEmail("user$index@example.com")
                        .build()
                }

            users.forEach { user ->
                val userRecord = userFactory.toRecord(user)
                ctx.executeInsert(userRecord)
            }

            val insertedUsers = ctx.selectFrom(userFactory.table()).fetch()

            insertedUsers.forEach { userRecord ->
                val post =
                    postFactory
                        .builder()
                        .withUserId(userRecord.id!!)
                        .withTitle("Post by ${userRecord.name}")
                        .withContent("Content")
                        .build()
                val postRecord = postFactory.toRecord(post)
                ctx.executeInsert(postRecord)
            }
        }

        val userCount = dsl.selectCount().from(UserFactoryImpl(dsl).table()).fetchOne(0, Int::class.java)
        val postCount = dsl.selectCount().from(PostFactoryImpl(dsl).table()).fetchOne(0, Int::class.java)

        assertThat(userCount).isEqualTo(3)
        assertThat(postCount).isEqualTo(3)
    }

    @Test
    fun `transaction with callback hooks`() {
        val createdUsers = mutableListOf<String>()
        val dsl = DSL.using(dataSource, SQLDialect.POSTGRES)

        dsl.transaction { configuration ->
            val ctx = DSL.using(configuration)

            val userFactory =
                object : UserFactoryImpl(ctx) {
                    override fun afterCreate(entity: User): User {
                        createdUsers.add(entity.name)
                        return entity
                    }
                }

            (1..2).forEach { index ->
                val user =
                    userFactory
                        .builder()
                        .withName("User $index")
                        .withEmail("user$index@example.com")
                        .build()
                val record = userFactory.toRecord(user)
                ctx.executeInsert(record)
                userFactory.afterCreate(user)
            }
        }

        assertThat(createdUsers).containsExactly("User 1", "User 2")
    }
}
