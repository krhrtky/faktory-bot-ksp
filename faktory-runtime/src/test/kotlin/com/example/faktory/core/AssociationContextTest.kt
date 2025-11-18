package com.example.faktory.core

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.jooq.DSLContext
import org.jooq.TableRecord
import org.jooq.impl.TableImpl
import org.junit.jupiter.api.Test

class AssociationContextTest {
    @Test
    fun `register()とget()で関連エンティティを保存・取得`() {
        val context = AssociationContext()

        context.register("user", DummyRecord(id = 1, name = "Alice"))

        val retrieved = context.get<DummyRecord>("user")
        assertThat(retrieved).isNotNull
        assertThat(retrieved?.id).isEqualTo(1)
        assertThat(retrieved?.name).isEqualTo("Alice")
    }

    @Test
    fun `複数の関連エンティティを独立して管理`() {
        val context = AssociationContext()

        context.register("user", DummyRecord(id = 1, name = "Alice"))
        context.register("author", DummyRecord(id = 2, name = "Bob"))

        val user = context.get<DummyRecord>("user")
        val author = context.get<DummyRecord>("author")

        assertThat(user?.id).isEqualTo(1)
        assertThat(author?.id).isEqualTo(2)
    }

    @Test
    fun `get()で存在しないキーを指定するとnullを返す`() {
        val context = AssociationContext()

        val result = context.get<DummyRecord>("nonexistent")

        assertThat(result).isNull()
    }

    @Test
    fun `associateWithPersist()でエンティティをDBに保存`() {
        val dsl = mockk<DSLContext>()
        val record = mockk<DummyTableRecord>(relaxed = true)

        every { record.id } returns null andThen 123
        every { record.name } returns "Charlie"
        every { dsl.executeInsert(record) } answers {
            record.id = 123
            1
        }

        val context = AssociationContext(dsl)

        context.associateWithPersist("user") {
            record
        }

        val retrieved = context.get<DummyTableRecord>("user")
        assertThat(retrieved?.id).isEqualTo(123)
        verify { dsl.executeInsert(record) }
    }
}

data class DummyRecord(val id: Int, val name: String)

@Suppress("UNCHECKED_CAST")
abstract class DummyTableRecord : TableRecord<DummyTableRecord>(
    mockk<TableImpl<DummyTableRecord>>(),
) {
    abstract var id: Int?
    abstract var name: String?
}
