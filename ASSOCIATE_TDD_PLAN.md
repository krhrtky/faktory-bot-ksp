# Associate機能 TDD実装計画

## t-wadaの3原則

1. **失敗するテストなしにプロダクトコードを書かない**
2. **失敗を解消する以上のプロダクトコードを書かない**
3. **一度に一つの失敗だけに対処する**

## Red-Green-Refactorサイクル

各ステップで以下を厳守：

1. **RED:** 失敗するテストを書く
2. **GREEN:** 最小限の実装でテストを通す
3. **REFACTOR:** 重複を除去し、設計を改善

## Phase 1: AssociationContext (Runtime基盤)

### 目標
関連エンティティを管理する基盤クラスを実装

### Step 1.1: 基本的なregister/get機能

#### Cycle 1.1.1: AssociationContext初期化

**RED: テストファイル作成**

ファイル: `faktory-runtime/src/test/kotlin/com/example/faktory/core/AssociationContextTest.kt`

```kotlin
package com.example.faktory.core

import org.assertj.core.api.Assertions.assertThat
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
}

data class DummyRecord(val id: Int, val name: String)
```

**実行:** テスト実行 → コンパイルエラー（AssociationContextが存在しない）

**GREEN: 最小実装**

ファイル: `faktory-runtime/src/main/kotlin/com/example/faktory/core/AssociationContext.kt`

```kotlin
package com.example.faktory.core

class AssociationContext {
    private val associations = mutableMapOf<String, Any>()

    fun register(key: String, record: Any) {
        associations[key] = record
    }

    fun <T> get(key: String): T? =
        @Suppress("UNCHECKED_CAST")
        associations[key] as? T
}
```

**実行:** テスト実行 → GREEN ✅

**REFACTOR: なし**（シンプルな実装のためリファクタ不要）

#### Cycle 1.1.2: 複数エンティティの管理

**RED: テスト追加**

```kotlin
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
```

**実行:** テスト実行 → GREEN ✅（既存実装で対応済み）

**REFACTOR: なし**

#### Cycle 1.1.3: 存在しないキーの取得

**RED: テスト追加**

```kotlin
@Test
fun `get()で存在しないキーを指定するとnullを返す`() {
    val context = AssociationContext()

    val result = context.get<DummyRecord>("nonexistent")

    assertThat(result).isNull()
}
```

**実行:** テスト実行 → GREEN ✅（既存実装で対応済み）

### Step 1.2: jOOQ統合（DB永続化）

#### Cycle 1.2.1: DSLContext統合

**RED: テスト追加**

```kotlin
import org.jooq.DSLContext
import org.jooq.impl.TableRecordImpl
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@Test
fun `associateWithPersist()でエンティティをDBに保存`() {
    val dsl = mock<DSLContext>()
    val record = DummyTableRecord().apply {
        id = null
        name = "Charlie"
    }

    // executeInsert後にIDが設定される想定
    whenever(dsl.executeInsert(any<DummyTableRecord>())).thenAnswer {
        (it.arguments[0] as DummyTableRecord).id = 123
        1
    }

    val context = AssociationContext(dsl)

    context.associateWithPersist("user") {
        record
    }

    val retrieved = context.get<DummyTableRecord>("user")
    assertThat(retrieved?.id).isEqualTo(123)
    verify(dsl).executeInsert(record)
}

class DummyTableRecord : TableRecordImpl<DummyTableRecord>(mock()) {
    var id: Int? = null
    var name: String? = null
}
```

**実行:** テスト実行 → コンパイルエラー（associateWithPersist未実装）

**GREEN: 最小実装**

```kotlin
class AssociationContext(
    private val dsl: DSLContext? = null,
) {
    private val associations = mutableMapOf<String, Any>()

    fun register(key: String, record: Any) {
        associations[key] = record
    }

    fun <T : Any> associateWithPersist(key: String, factory: () -> T) {
        val record = factory()
        dsl?.executeInsert(record as org.jooq.TableRecord<*>)
        register(key, record)
    }

    fun <T> get(key: String): T? =
        @Suppress("UNCHECKED_CAST")
        associations[key] as? T
}
```

**実行:** テスト実行 → GREEN ✅

**REFACTOR: 型安全性向上**

```kotlin
import org.jooq.DSLContext
import org.jooq.TableRecord

class AssociationContext(
    private val dsl: DSLContext? = null,
) {
    private val associations = mutableMapOf<String, Any>()

    fun register(key: String, record: Any) {
        associations[key] = record
    }

    fun <T : TableRecord<T>> associateWithPersist(
        key: String,
        factory: () -> T,
    ) {
        val record = factory()
        dsl?.executeInsert(record)
        register(key, record)
    }

    fun <T> get(key: String): T? =
        @Suppress("UNCHECKED_CAST")
        associations[key] as? T
}
```

**実行:** テスト実行 → GREEN ✅

## Phase 2: DslBuilder拡張

### 目標
外部キーフィールドをオプショナル化し、associateブロックをサポート

### Step 2.1: 外部キーをオプショナル化

#### Cycle 2.1.1: DslCodeGenerator拡張（外部キー検出）

**RED: テスト追加**

ファイル: `faktory-ksp/src/test/kotlin/com/example/faktory/ksp/codegen/DslCodeGeneratorTest.kt`

```kotlin
@Test
fun `generate()が外部キーフィールドをオプショナルプロパティとして生成`() {
    val metadata = TableMetadata(
        tableName = "posts",
        recordClassName = "PostsRecord",
        requiredFields = listOf("title", "content"),  // userIdを除外
        optionalFields = listOf("published", "created_at"),
        foreignKeys = listOf(
            ForeignKeyConstraint(
                fieldName = "user_id",
                referencedTable = "users",
            )
        ),
    )

    val code = DslCodeGenerator.generate("PostsRecord", metadata)

    // userIdがコンストラクタパラメータに含まれない
    assertThat(code).doesNotContain("var userId: Int,")
    // userIdがオプショナルプロパティとして定義される
    assertThat(code).contains("var userId: Int? = null")
}
```

**実行:** テスト実行 → FAIL（foreignKeysフィールドが存在しない）

**GREEN: TableMetadata拡張**

ファイル: `faktory-ksp/src/main/kotlin/com/example/faktory/ksp/metadata/TableMetadata.kt`

```kotlin
data class TableMetadata(
    val tableName: String,
    val recordClassName: String,
    val requiredFields: List<String>,
    val optionalFields: List<String> = emptyList(),
    val foreignKeys: List<ForeignKeyConstraint> = emptyList(),  // 追加
)
```

**GREEN: DslCodeGenerator拡張**

```kotlin
private fun generateDslBuilder(...): TypeSpec {
    // 外部キーフィールドを除外
    val fkFieldNames = metadata.foreignKeys.map { it.fieldName }
    val constructorFields = metadata.requiredFields
        .filterNot { it in fkFieldNames }

    // 外部キーをオプショナルプロパティとして追加
    val fkProperties = metadata.foreignKeys.map { fk ->
        PropertySpec.builder(
            fk.fieldName.toCamelCase(),
            Int::class.asClassName().copy(nullable = true),
        ).mutable(true)
            .initializer("null")
            .build()
    }

    // ...
}
```

**実行:** テスト実行 → GREEN ✅

### Step 2.2: associateメソッド生成

#### Cycle 2.2.1: associateメソッドのコード生成

**RED: テスト追加**

```kotlin
@Test
fun `generate()がassociateメソッドを生成`() {
    val metadata = TableMetadata(
        tableName = "posts",
        recordClassName = "PostsRecord",
        requiredFields = listOf("title", "content"),
        optionalFields = listOf(),
        foreignKeys = listOf(
            ForeignKeyConstraint(
                fieldName = "user_id",
                referencedTable = "users",
            )
        ),
    )

    val code = DslCodeGenerator.generate("PostsRecord", metadata)

    assertThat(code).contains("fun associate(")
    assertThat(code).contains("AssociationContext")
}
```

**実行:** テスト実行 → FAIL

**GREEN: DslCodeGenerator拡張**

```kotlin
private fun generateDslBuilder(...): TypeSpec {
    // ...

    val associateMethod = FunSpec.builder("associate")
        .addParameter(
            "block",
            LambdaTypeName.get(
                receiver = ClassName("com.example.faktory.core", "AssociationContext"),
                returnType = UNIT,
            ),
        )
        .addStatement("associationContext.block()")
        .build()

    return TypeSpec.classBuilder(builderClassName)
        // ...
        .addFunction(associateMethod)
        .build()
}
```

**実行:** テスト実行 → GREEN ✅

## Phase 3: KSP統合

### 目標
ForeignKey情報からassociateコードを自動生成

### Step 3.1: ForeignKeyConstraint拡張

#### Cycle 3.1.1: referencedRecordType追加

**RED: テスト追加**

ファイル: `faktory-ksp/src/test/kotlin/com/example/faktory/ksp/metadata/ForeignKeyDetectorTest.kt`

```kotlin
@Test
fun `detect()が参照先のRecord型名を返す`() {
    val postsTable = Posts.POSTS
    val constraints = ForeignKeyDetector.detect(postsTable)

    assertThat(constraints).hasSize(1)
    assertThat(constraints[0].referencedRecordType).isEqualTo("UsersRecord")
}
```

**実行:** テスト実行 → コンパイルエラー（referencedRecordTypeが存在しない）

**GREEN: ForeignKeyConstraint拡張**

ファイル: `faktory-ksp/src/main/kotlin/com/example/faktory/ksp/metadata/ForeignKeyDetector.kt`

```kotlin
data class ForeignKeyConstraint(
    val fieldName: String,
    val referencedTable: String,
    val referencedRecordType: String,  // 追加
)

object ForeignKeyDetector {
    fun detect(table: Table<*>): List<ForeignKeyConstraint> {
        return table.references.flatMap { fk ->
            fk.fields.map { field ->
                ForeignKeyConstraint(
                    fieldName = field.name,
                    referencedTable = fk.key.table.name,
                    referencedRecordType = "${fk.key.table.name.toPascalCase()}Record",
                )
            }
        }
    }

    private fun String.toPascalCase(): String =
        split("_").joinToString("") { it.replaceFirstChar { c -> c.uppercaseChar() } }
}
```

**実行:** テスト実行 → GREEN ✅

### Step 3.2: AssociateCodeGenerator新規作成

#### Cycle 3.2.1: associate専用メソッド生成

**RED: テストファイル作成**

ファイル: `faktory-ksp/src/test/kotlin/com/example/faktory/ksp/codegen/AssociateCodeGeneratorTest.kt`

```kotlin
package com.example.faktory.ksp.codegen

import com.example.faktory.ksp.metadata.ForeignKeyConstraint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AssociateCodeGeneratorTest {
    @Test
    fun `generateAssociateExtension()でuser用のextension関数を生成`() {
        val fk = ForeignKeyConstraint(
            fieldName = "user_id",
            referencedTable = "users",
            referencedRecordType = "UsersRecord",
        )

        val extension = AssociateCodeGenerator.generateAssociateExtension(fk)
        val code = extension.toString()

        assertThat(code).contains("fun AssociationContext.user(")
        assertThat(code).contains("block: () -> UsersRecord")
        assertThat(code).contains("associateWithPersist(\"user_id\", block)")
    }
}
```

**実行:** テスト実行 → コンパイルエラー（AssociateCodeGeneratorが存在しない）

**GREEN: 最小実装**

ファイル: `faktory-ksp/src/main/kotlin/com/example/faktory/ksp/codegen/AssociateCodeGenerator.kt`

```kotlin
package com.example.faktory.ksp.codegen

import com.example.faktory.ksp.metadata.ForeignKeyConstraint
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.LambdaTypeName

object AssociateCodeGenerator {
    fun generateAssociateExtension(fk: ForeignKeyConstraint): FunSpec {
        val methodName = fk.referencedTable.removeSuffix("s")

        return FunSpec.builder(methodName)
            .receiver(ClassName("com.example.faktory.core", "AssociationContext"))
            .addParameter(
                "block",
                LambdaTypeName.get(
                    returnType = ClassName("", fk.referencedRecordType),
                ),
            )
            .addStatement("associateWithPersist(%S, block)", fk.fieldName)
            .build()
    }
}
```

**実行:** テスト実行 → GREEN ✅

## Phase 4: 統合テスト

### 目標
End-to-Endでassociate機能を検証

### Step 4.1: 生成コードの統合テスト

#### Cycle 4.1.1: KSP統合テスト

**RED: テストファイル作成**

ファイル: `faktory-examples/src/test/kotlin/com/example/faktory/examples/integration/AssociateIntegrationTest.kt`

```kotlin
package com.example.faktory.examples.integration

import com.example.faktory.examples.generated.post
import com.example.faktory.examples.generated.user
import com.example.faktory.examples.jooq.tables.Posts.Companion.POSTS
import com.example.faktory.examples.jooq.tables.Users.Companion.USERS
import org.assertj.core.api.Assertions.assertThat
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
class AssociateIntegrationTest {
    @Container
    private val postgres = PostgreSQLContainer("postgres:16-alpine")

    @Test
    fun `post()でassociateブロックを使ってUserを関連付け`() {
        val dsl = setupDatabase()

        val postRecord = post(
            title = "My Post",
            content = "Content",
        ) {
            associate {
                user {
                    user(name = "Alice", email = "alice@example.com")
                }
            }
        }
        dsl.executeInsert(postRecord)

        val insertedPost = dsl.selectFrom(POSTS).fetchOne()
        assertThat(insertedPost).isNotNull

        val user = dsl.selectFrom(USERS)
            .where(USERS.ID.eq(insertedPost?.userId))
            .fetchOne()
        assertThat(user).isNotNull
        assertThat(user?.name).isEqualTo("Alice")
    }

    private fun setupDatabase(): DSLContext {
        // DB setup logic
    }
}
```

**実行:** テスト実行 → FAIL（生成コードがassociateブロックに未対応）

**GREEN: DslCodeGenerator完全実装**

Phase 2.2で実装したassociateメソッドと、Phase 3.2で実装したextension関数を統合。

**実行:** テスト実行 → GREEN ✅

## 実装チェックリスト

### Phase 1: AssociationContext ✅
- [ ] Cycle 1.1.1: register/get基本機能
- [ ] Cycle 1.1.2: 複数エンティティ管理
- [ ] Cycle 1.1.3: 存在しないキーの処理
- [ ] Cycle 1.2.1: DSLContext統合とDB永続化

### Phase 2: DslBuilder拡張 ✅
- [ ] Cycle 2.1.1: 外部キーをオプショナル化
- [ ] Cycle 2.2.1: associateメソッド生成

### Phase 3: KSP統合 ✅
- [ ] Cycle 3.1.1: ForeignKeyConstraint拡張
- [ ] Cycle 3.2.1: AssociateCodeGenerator実装

### Phase 4: 統合テスト ✅
- [ ] Cycle 4.1.1: KSP統合テスト

## 完了条件

- 全テストケースがGREEN ✅
- TDD原則を全ステップで遵守 ✅
- Red-Green-Refactorサイクルを厳守 ✅
- コミットメッセージに各cycleを明記 ✅

## 次のステップ

Phase 1から順番に実装を開始します。各cycleごとにコミットを作成し、確実にGREENを維持しながら進めます。
