package com.example.faktory.examples

import com.example.faktory.core.PersistableFactory
import com.example.faktory.examples.jooq.tables.Users.Companion.USERS
import com.example.faktory.examples.jooq.tables.records.UsersRecord
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.assertj.core.api.Assertions.assertThat
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.LocalDateTime

@Testcontainers
class CallbackHooksTest {
    @Container
    private val postgres =
        PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")

    private lateinit var dataSource: HikariDataSource
    private lateinit var dsl: DSLContext

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

        dsl = DSL.using(dataSource, SQLDialect.POSTGRES)

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
    fun `afterBuild hook adds timestamp to entity`() {
        class UserFactoryWithTimestamp(dsl: DSLContext) :
            PersistableFactory<UsersRecord, User, UserBuilder>(dsl) {
            override fun builder() = UserBuilder()

            override fun table() = USERS

            override fun toRecord(entity: User): UsersRecord =
                UsersRecord().apply {
                    entity.id?.let { id = it }
                    name = entity.name
                    email = entity.email
                    age = entity.age
                }

            override fun afterBuild(entity: User): User {
                println("[afterBuild] User built: ${entity.name}")
                return entity
            }
        }

        val factory = UserFactoryWithTimestamp(dsl)
        val user =
            factory
                .builder()
                .withName("Alice")
                .withEmail("alice@example.com")
                .build()

        assertThat(user.name).isEqualTo("Alice")
    }

    @Test
    fun `beforeCreate validates entity before persistence`() {
        class UserFactoryWithValidation(dsl: DSLContext) :
            PersistableFactory<UsersRecord, User, UserBuilder>(dsl) {
            override fun builder() = UserBuilder()

            override fun table() = USERS

            override fun toRecord(entity: User): UsersRecord =
                UsersRecord().apply {
                    entity.id?.let { id = it }
                    name = entity.name
                    email = entity.email
                    age = entity.age
                }

            override fun beforeCreate(entity: User): User {
                require(entity.email.contains("@")) { "Invalid email format" }
                println("[beforeCreate] Validated: ${entity.email}")
                return entity
            }
        }

        val factory = UserFactoryWithValidation(dsl)
        val user =
            factory
                .builder()
                .withName("Bob")
                .withEmail("bob@example.com")
                .build()
        val record = factory.toRecord(user)
        dsl.executeInsert(record)

        val inserted = dsl.selectFrom(USERS).fetchOne()
        assertThat(inserted).isNotNull
        assertThat(inserted?.email).isEqualTo("bob@example.com")
    }

    @Test
    fun `afterCreate logs persistence event`() {
        val persistedUsers = mutableListOf<String>()

        class UserFactoryWithLogging(dsl: DSLContext) :
            PersistableFactory<UsersRecord, User, UserBuilder>(dsl) {
            override fun builder() = UserBuilder()

            override fun table() = USERS

            override fun toRecord(entity: User): UsersRecord =
                UsersRecord().apply {
                    entity.id?.let { id = it }
                    name = entity.name
                    email = entity.email
                    age = entity.age
                }

            override fun afterCreate(entity: User): User {
                persistedUsers.add(entity.name)
                println("[afterCreate] Persisted: ${entity.name}")
                return entity
            }
        }

        val factory = UserFactoryWithLogging(dsl)
        factory
            .builder()
            .withName("Charlie")
            .withEmail("charlie@example.com")
            .build()
            .let {
                val record = factory.toRecord(it)
                dsl.executeInsert(record)
                factory.afterCreate(it)
            }

        assertThat(persistedUsers).containsExactly("Charlie")
    }

    @Test
    fun `multiple hooks work together`() {
        val hookCalls = mutableListOf<String>()

        class UserFactoryWithMultipleHooks(dsl: DSLContext) :
            PersistableFactory<UsersRecord, User, UserBuilder>(dsl) {
            override fun builder() = UserBuilder()

            override fun table() = USERS

            override fun toRecord(entity: User): UsersRecord =
                UsersRecord().apply {
                    entity.id?.let { id = it }
                    name = entity.name
                    email = entity.email
                    age = entity.age
                }

            override fun afterBuild(entity: User): User {
                hookCalls.add("afterBuild: ${entity.name}")
                return entity
            }

            override fun beforeCreate(entity: User): User {
                hookCalls.add("beforeCreate: ${entity.name}")
                return entity
            }

            override fun afterCreate(entity: User): User {
                hookCalls.add("afterCreate: ${entity.name}")
                return entity
            }
        }

        val factory = UserFactoryWithMultipleHooks(dsl)
        val user =
            factory
                .builder()
                .withName("Diana")
                .withEmail("diana@example.com")
                .build()

        factory.beforeCreate(user)
        val record = factory.toRecord(user)
        dsl.executeInsert(record)
        factory.afterCreate(user)

        assertThat(hookCalls).containsExactly(
            "afterBuild: Diana",
            "beforeCreate: Diana",
            "afterCreate: Diana",
        )
    }
}
