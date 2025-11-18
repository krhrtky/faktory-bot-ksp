# Associate機能設計ドキュメント

## 概要

**Associate機能**は、外部キー制約を持つエンティティを作成する際に、関連エンティティを自動的に生成・管理する機能です。

### 現状の問題

```kotlin
// 現在の実装：userIdを手動で生成する必要がある
val userRecord = user(name = "Alice", email = "alice@example.com")
dsl.executeInsert(userRecord)
val userId = dsl.selectFrom(USERS).fetchOne()!!.id!!

val postRecord = post(
    userId = userId,  // 手動でuserIdを指定
    title = "Alice's Post",
    content = "Content",
)
dsl.executeInsert(postRecord)
```

### 目標

```kotlin
// Associate機能実装後：関連エンティティを自動生成
val postRecord = post(
    title = "My Post",
    content = "Content",
) {
    // userIdを省略 → 自動的にUserを生成
}

// または明示的に指定
val postRecord = post(
    title = "My Post",
    content = "Content",
) {
    associate { user(name = "Bob", email = "bob@example.com") }
}
```

## 要件定義

### 必須要件（Phase 1）

1. **外部キーフィールドの省略**
   - 外部キー制約フィールドをオプショナルパラメータに変更
   - 省略時にデフォルト値で関連エンティティを自動生成

2. **Associate DSL**
   - `associate { }` ブロックで関連エンティティを指定
   - ブロック内でファクトリ関数を呼び出し

3. **自動永続化**
   - 関連エンティティを先にDBに保存
   - 生成されたIDを外部キーフィールドに設定

4. **型安全性**
   - 関連エンティティの型をコンパイル時に検証
   - 間違ったエンティティを指定した場合はコンパイルエラー

### 拡張要件（Phase 2-3）

5. **複数外部キーサポート**
   - 1つのテーブルが複数の外部キーを持つ場合
   - 各外部キーごとにassociateブロックを提供

6. **循環依存検出**
   - User → Post → User のような循環参照を検出
   - コンパイル時または実行時にエラー

7. **既存レコード参照**
   - 既にDBに存在するレコードを参照
   - `associate { existing(userId = 123) }`

8. **トランザクション統合**
   - 関連エンティティの生成をトランザクション内で実行
   - エラー時に自動ロールバック

## 設計

### アーキテクチャ

```
┌─────────────────────────────────────────┐
│  User Code (DSL Usage)                  │
│  post(title = "...") {                  │
│    associate { user(...) }              │
│  }                                      │
└──────────────┬──────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────┐
│  Generated DSL Code (KSP)               │
│  - PostsDslBuilder                      │
│  - AssociationContext                   │
│  - associate() method                   │
└──────────────┬──────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────┐
│  Runtime Library                        │
│  - AssociationResolver                  │
│  - ForeignKeyHandler                    │
│  - TransactionManager                   │
└──────────────┬──────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────┐
│  jOOQ / Database                        │
└─────────────────────────────────────────┘
```

### コンポーネント設計

#### 1. AssociationContext (Runtime)

関連エンティティを管理するコンテキスト。

```kotlin
class AssociationContext(
    private val dsl: DSLContext,
) {
    private val associations = mutableMapOf<String, Any>()

    fun <T : TableRecord<T>> associate(
        fieldName: String,
        factory: () -> T,
    ) {
        val record = factory()
        dsl.executeInsert(record)
        // IDを取得して保存
        associations[fieldName] = record
    }

    fun <T> get(fieldName: String): T? =
        @Suppress("UNCHECKED_CAST")
        associations[fieldName] as? T
}
```

#### 2. DslBuilder拡張 (KSP生成)

外部キーフィールドをオプショナルに変更。

**Before (現状):**
```kotlin
class PostsDslBuilder(
    var userId: Int,  // 必須
    var title: String,
    var content: String,
)
```

**After (Associate実装後):**
```kotlin
class PostsDslBuilder(
    var title: String,
    var content: String,
) {
    var userId: Int? = null  // オプショナル

    private val associationContext = AssociationContext(dsl)

    fun associate(block: AssociationContext.() -> Unit) {
        associationContext.block()
    }

    internal fun build(): PostsRecord {
        // userIdが未設定なら関連エンティティから取得
        val finalUserId = userId ?: run {
            val userRecord = associationContext.get<UsersRecord>("user")
                ?: user(name = "Default User", email = "default@example.com").also {
                    dsl.executeInsert(it)
                }
            userRecord.id!!
        }

        return PostsRecord().apply {
            this.userId = finalUserId
            this.title = this@PostsDslBuilder.title
            this.content = this@PostsDslBuilder.content
        }
    }
}
```

#### 3. ForeignKeyMetadata拡張 (KSP)

ForeignKeyDetectorの情報を活用。

```kotlin
data class ForeignKeyConstraint(
    val fieldName: String,           // "user_id"
    val referencedTable: String,     // "users"
    val referencedRecordType: String, // "UsersRecord"
)
```

#### 4. AssociateCodeGenerator (KSP)

associate対応のDSLコードを生成。

```kotlin
object AssociateCodeGenerator {
    fun generateAssociateMethod(
        foreignKeys: List<ForeignKeyConstraint>,
    ): FunSpec {
        return FunSpec.builder("associate")
            .addParameter("block", LambdaTypeName.get(...))
            .addStatement("associationContext.block()")
            .build()
    }

    fun generateBuildWithAssociation(
        foreignKeys: List<ForeignKeyConstraint>,
    ): FunSpec {
        // userIdが未設定なら自動生成するロジック
    }
}
```

## TDD実装計画

### Phase 1: AssociationContext (Runtime)

**目標:** 関連エンティティ管理の基盤

#### Step 1.1: AssociationContext基本機能

**RED: 失敗するテスト**
```kotlin
class AssociationContextTest {
    @Test
    fun `associate()で関連エンティティを登録`() {
        val context = AssociationContext()
        val userRecord = UsersRecord().apply {
            id = 1
            name = "Alice"
            email = "alice@example.com"
        }

        context.register("user", userRecord)

        val retrieved = context.get<UsersRecord>("user")
        assertThat(retrieved).isEqualTo(userRecord)
    }
}
```

**GREEN: 最小実装**
```kotlin
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

**REFACTOR: 型安全性向上**
```kotlin
class AssociationContext {
    private val associations = mutableMapOf<String, TableRecord<*>>()

    fun <T : TableRecord<T>> register(key: String, record: T) {
        associations[key] = record
    }

    fun <T : TableRecord<T>> get(key: String): T? =
        @Suppress("UNCHECKED_CAST")
        associations[key] as? T
}
```

#### Step 1.2: DB永続化統合

**RED: 失敗するテスト**
```kotlin
@Test
fun `associate()でエンティティをDBに保存しIDを取得`() {
    val dsl = mockDslContext()
    val context = AssociationContext(dsl)

    context.associate("user") {
        user(name = "Bob", email = "bob@example.com")
    }

    val userRecord = context.get<UsersRecord>("user")
    assertThat(userRecord?.id).isNotNull()
    verify(dsl).executeInsert(any<UsersRecord>())
}
```

**GREEN: 実装**
```kotlin
class AssociationContext(
    private val dsl: DSLContext,
) {
    fun <T : TableRecord<T>> associate(
        key: String,
        factory: () -> T,
    ) {
        val record = factory()
        dsl.executeInsert(record)
        register(key, record)
    }
}
```

### Phase 2: DslBuilder拡張

**目標:** 外部キーフィールドをオプショナル化

#### Step 2.1: オプショナル外部キー

**RED: 失敗するテスト**
```kotlin
class PostDslBuilderTest {
    @Test
    fun `post() userIdを省略してPostRecordを構築`() {
        // userIdなしでコンパイル可能
        val postRecord = post(
            title = "My Post",
            content = "Content",
        )

        // この段階ではuserIdはnull
        assertThat(postRecord.userId).isNull()
    }
}
```

**GREEN: DslCodeGenerator拡張**
- 外部キーフィールドをコンストラクタから除外
- オプショナルプロパティとして定義

#### Step 2.2: Associate DSL統合

**RED: 失敗するテスト**
```kotlin
@Test
fun `post() associateブロックでUserを指定`() {
    val dsl = mockDslContext()

    val postRecord = post(
        title = "My Post",
        content = "Content",
    ) {
        associate("user") {
            user(name = "Alice", email = "alice@example.com")
        }
    }

    assertThat(postRecord.userId).isEqualTo(1)
}
```

**GREEN: DslBuilder生成コード**
```kotlin
class PostsDslBuilder(
    var title: String,
    var content: String,
) {
    var userId: Int? = null
    private val associationContext = AssociationContext(dsl)

    fun associate(key: String, block: () -> TableRecord<*>) {
        associationContext.associate(key, block)
    }
}
```

### Phase 3: KSP統合

**目標:** ForeignKey情報からassociateコード自動生成

#### Step 3.1: ForeignKeyMetadata拡張

**RED: 失敗するテスト**
```kotlin
class ForeignKeyDetectorTest {
    @Test
    fun `detect()が参照先のRecord型を返す`() {
        val constraints = ForeignKeyDetector.detect(PostsTable)

        assertThat(constraints).hasSize(1)
        assertThat(constraints[0].fieldName).isEqualTo("user_id")
        assertThat(constraints[0].referencedTable).isEqualTo("users")
        assertThat(constraints[0].referencedRecordType).isEqualTo("UsersRecord")
    }
}
```

**GREEN: ForeignKeyDetector拡張**
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
}
```

#### Step 3.2: AssociateCodeGenerator

**RED: 失敗するテスト**
```kotlin
class AssociateCodeGeneratorTest {
    @Test
    fun `generateAssociateMethod()でassociateメソッドを生成`() {
        val fk = ForeignKeyConstraint(
            fieldName = "user_id",
            referencedTable = "users",
            referencedRecordType = "UsersRecord",
        )

        val method = AssociateCodeGenerator.generateAssociateMethod(fk)
        val code = method.toString()

        assertThat(code).contains("fun user(")
        assertThat(code).contains("block: () -> UsersRecord")
    }
}
```

**GREEN: 実装**
```kotlin
object AssociateCodeGenerator {
    fun generateAssociateMethod(
        fk: ForeignKeyConstraint,
    ): FunSpec {
        val methodName = fk.referencedTable.removeSuffix("s")

        return FunSpec.builder(methodName)
            .addParameter(
                "block",
                LambdaTypeName.get(
                    returnType = ClassName("", fk.referencedRecordType),
                ),
            )
            .addStatement(
                "associationContext.associate(%S, block)",
                fk.fieldName,
            )
            .build()
    }
}
```

### Phase 4: 統合テスト

**目標:** End-to-Endでassociate機能を検証

#### Step 4.1: 統合テスト（Testcontainers）

**RED: 失敗するテスト**
```kotlin
@Testcontainers
class AssociateIntegrationTest {
    @Test
    fun `post()でuserIdを省略すると自動的にUserを生成`() {
        val dsl = DSL.using(dataSource, SQLDialect.POSTGRES)

        val postRecord = post(
            title = "My Post",
            content = "Content",
        ) {
            // userIdを省略
        }
        dsl.executeInsert(postRecord)

        val insertedPost = dsl.selectFrom(POSTS).fetchOne()
        assertThat(insertedPost).isNotNull
        assertThat(insertedPost?.userId).isNotNull()

        val user = dsl.selectFrom(USERS)
            .where(USERS.ID.eq(insertedPost?.userId))
            .fetchOne()
        assertThat(user).isNotNull()
        assertThat(user?.name).isEqualTo("Default User")
    }
}
```

**GREEN: 実装**
- DslCodeGeneratorでデフォルト生成ロジックを追加
- build()メソッド内でuserIdがnullなら自動生成

## 実装順序

1. **Phase 1.1:** AssociationContext基本機能 (TDD)
2. **Phase 1.2:** DB永続化統合 (TDD)
3. **Phase 2.1:** オプショナル外部キー (TDD)
4. **Phase 2.2:** Associate DSL統合 (TDD)
5. **Phase 3.1:** ForeignKeyMetadata拡張 (TDD)
6. **Phase 3.2:** AssociateCodeGenerator (TDD)
7. **Phase 4.1:** 統合テスト (TDD)

## 成功基準

- 全テストケースがGREEN
- コンパイル時型安全性の保証
- 外部キー制約の自動解決
- デフォルト値での関連エンティティ生成
- 明示的な関連エンティティ指定

## 今後の拡張

- 循環依存検出
- 既存レコード参照（`existing(userId = 123)`）
- トランザクション統合
- バッチ生成最適化
- Nested associations（Post → User → Company）
