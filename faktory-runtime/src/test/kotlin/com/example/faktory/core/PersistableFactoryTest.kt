package com.example.faktory.core

import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.jooq.DSLContext
import org.jooq.Table
import org.jooq.TableRecord
import org.jooq.impl.TableRecordImpl
import org.junit.jupiter.api.Test

class PersistableFactoryTest {
    data class User(val id: Int, val name: String)

    class UserBuilder : FactoryBuilder<User> {
        private var id: Int = 1
        private var name: String = "Test User"

        fun withId(value: Int): UserBuilder = apply { id = value }

        fun withName(value: String): UserBuilder = apply { name = value }

        override fun build(): User = User(id, name)
    }

    @Suppress("UNCHECKED_CAST")
    abstract class TestRecord : TableRecordImpl<TestRecord>(mockk())

    class TestUserFactory(dsl: DSLContext) : PersistableFactory<TestRecord, User, UserBuilder>(dsl) {
        override fun builder(): UserBuilder = UserBuilder()

        override fun table(): Table<TestRecord> = mockk()

        override fun toRecord(entity: User): TestRecord = mockk()
    }

    @Test
    fun `create persists entity to database`() {
        val dsl = mockk<DSLContext>(relaxed = true)
        val factory = TestUserFactory(dsl)

        val user = factory.create()

        assertThat(user).isNotNull()
        verify { dsl.executeInsert(any<TableRecord<*>>()) }
    }

    @Test
    fun `createList persists multiple entities`() {
        val dsl = mockk<DSLContext>(relaxed = true)
        val factory = TestUserFactory(dsl)

        val users = factory.createList(3)

        assertThat(users).hasSize(3)
        verify(exactly = 3) { dsl.executeInsert(any<TableRecord<*>>()) }
    }

    @Test
    fun `build does not persist`() {
        val dsl = mockk<DSLContext>(relaxed = true)
        val factory = TestUserFactory(dsl)

        factory.build()

        verify(exactly = 0) { dsl.executeInsert(any<TableRecord<*>>()) }
    }

    @Test
    fun `beforeCreate hook is called before persistence`() {
        var hookCalled = false
        var entityBeforeCreate: User? = null
        val dsl = mockk<DSLContext>(relaxed = true)

        val factory =
            object : PersistableFactory<TestRecord, User, UserBuilder>(dsl) {
                override fun builder(): UserBuilder = UserBuilder()

                override fun table(): Table<TestRecord> = mockk()

                override fun toRecord(entity: User): TestRecord = mockk()

                override fun beforeCreate(entity: User): User {
                    hookCalled = true
                    entityBeforeCreate = entity
                    return entity
                }
            }

        val user = factory.create()

        assertThat(hookCalled).isTrue()
        assertThat(entityBeforeCreate).isNotNull()
        assertThat(entityBeforeCreate?.name).isEqualTo(user.name)
    }

    @Test
    fun `afterCreate hook is called after persistence`() {
        var hookCalled = false
        var entityAfterCreate: User? = null
        val dsl = mockk<DSLContext>(relaxed = true)

        val factory =
            object : PersistableFactory<TestRecord, User, UserBuilder>(dsl) {
                override fun builder(): UserBuilder = UserBuilder()

                override fun table(): Table<TestRecord> = mockk()

                override fun toRecord(entity: User): TestRecord = mockk()

                override fun afterCreate(entity: User): User {
                    hookCalled = true
                    entityAfterCreate = entity
                    return entity
                }
            }

        val user = factory.create()

        assertThat(hookCalled).isTrue()
        assertThat(entityAfterCreate).isSameAs(user)
    }

    @Test
    fun `beforeCreate can modify entity before persistence`() {
        val dsl = mockk<DSLContext>(relaxed = true)

        val factory =
            object : PersistableFactory<TestRecord, User, UserBuilder>(dsl) {
                override fun builder(): UserBuilder = UserBuilder()

                override fun table(): Table<TestRecord> = mockk()

                override fun toRecord(entity: User): TestRecord = mockk()

                override fun beforeCreate(entity: User): User =
                    entity.copy(name = "${entity.name} (validated)")
            }

        val user = factory.create()

        assertThat(user.name).isEqualTo("Test User (validated)")
    }

    @Test
    fun `afterCreate can modify entity after persistence`() {
        val dsl = mockk<DSLContext>(relaxed = true)

        val factory =
            object : PersistableFactory<TestRecord, User, UserBuilder>(dsl) {
                override fun builder(): UserBuilder = UserBuilder()

                override fun table(): Table<TestRecord> = mockk()

                override fun toRecord(entity: User): TestRecord = mockk()

                override fun afterCreate(entity: User): User =
                    entity.copy(id = 999)
            }

        val user = factory.create()

        assertThat(user.id).isEqualTo(999)
    }

    @Test
    fun `hooks are called in correct order for createList`() {
        val callOrder = mutableListOf<String>()
        val dsl = mockk<DSLContext>(relaxed = true)

        val factory =
            object : PersistableFactory<TestRecord, User, UserBuilder>(dsl) {
                override fun builder(): UserBuilder = UserBuilder()

                override fun table(): Table<TestRecord> = mockk()

                override fun toRecord(entity: User): TestRecord = mockk()

                override fun afterBuild(entity: User): User {
                    callOrder.add("afterBuild")
                    return entity
                }

                override fun beforeCreate(entity: User): User {
                    callOrder.add("beforeCreate")
                    return entity
                }

                override fun afterCreate(entity: User): User {
                    callOrder.add("afterCreate")
                    return entity
                }
            }

        factory.createList(2)

        assertThat(callOrder).containsExactly(
            "afterBuild",
            "beforeCreate",
            "afterCreate",
            "afterBuild",
            "beforeCreate",
            "afterCreate",
        )
    }
}
