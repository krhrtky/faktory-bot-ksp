# faktory-bot-ksp API Reference

## 目次

1. [Annotations](#annotations)
2. [Generated DSL](#generated-dsl)
3. [Runtime Classes](#runtime-classes)
4. [KSP Processor](#ksp-processor)
5. [Metadata Extractors](#metadata-extractors)
6. [Code Generators](#code-generators)

---

## Annotations

### @Factory

ファクトリコード生成のエントリポイント。

**パッケージ:** `com.example.faktory.ksp`

**定義:**
```kotlin
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Factory(
    val tableName: String
)
```

**パラメータ:**
- `tableName: String` - jOOQテーブル名（必須）

**使用例:**
```kotlin
@Factory(tableName = "users")
class UserFactory

@Factory(tableName = "posts")
class PostFactory
```

**生成されるコード:**
- `{TableName}DslBuilder` クラス
- `{singularTableName}()` トップレベル関数

---

### @FactoryDsl

DSLスコープ制御用のDslMarkerアノテーション。

**パッケージ:** `com.example.faktory.core`

**定義:**
```kotlin
@DslMarker
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class FactoryDsl
```

**目的:**
- DSLブロックの誤った入れ子を防止

**自動適用:**
KSP生成のDslBuilderクラスに自動的に付与されます。

**効果:**
```kotlin
// ❌ コンパイルエラー
user(name = "Alice", email = "alice@example.com") {
    user(name = "Bob", email = "bob@example.com") {
        // ネスト不可
    }
}
```

---

## Generated DSL

KSPプロセッサが自動生成するDSLコード。

### DslBuilder Class

**命名規則:** `{TableName}DslBuilder`

**生成例（Usersテーブル）:**
```kotlin
@FactoryDsl
class UsersDslBuilder(
    var name: String,
    var email: String,
) {
    var age: Int? = null
    var createdAt: LocalDateTime? = null

    internal fun build(): UsersRecord = UsersRecord().apply {
        this.name = this@UsersDslBuilder.name
        this.email = this@UsersDslBuilder.email
        this.age = this@UsersDslBuilder.age
        this.createdAt = this@UsersDslBuilder.createdAt
    }
}
```

**構造:**
- **コンストラクタパラメータ:** 必須フィールド（NOT NULL）
- **varプロパティ:** オプショナルフィールド（NULL許可）
- **build()メソッド:** jOOQ Recordを構築

**フィールド命名:**
- snake_case（DB） → camelCase（Kotlin）
- 例: `created_at` → `createdAt`

### Factory Function

**命名規則:** テーブル名の単数形（例: `users` → `user`）

**シグネチャ:**
```kotlin
fun {singularTableName}(
    {requiredField1}: {Type1},
    {requiredField2}: {Type2},
    // ...
    block: {TableName}DslBuilder.() -> Unit = {}
): {TableName}Record
```

**生成例（Usersテーブル）:**
```kotlin
fun user(
    name: String,
    email: String,
    block: UsersDslBuilder.() -> Unit = {},
): UsersRecord = UsersDslBuilder(name, email).apply(block).build()
```

**使用例:**
```kotlin
// 必須フィールドのみ
val user = user(name = "Alice", email = "alice@example.com")

// オプショナルフィールドも設定
val user = user(name = "Bob", email = "bob@example.com") {
    age = 30
    createdAt = LocalDateTime.now()
}
```

---

## Runtime Classes

### Factory<T, B>

メモリ内オブジェクト構築の基底クラス。

**パッケージ:** `com.example.faktory.core`

**定義:**
```kotlin
abstract class Factory<T : Any, B : FactoryBuilder<T>> {
    abstract fun builder(): B

    fun build(): T = builder().build()

    fun buildList(count: Int): List<T> =
        (1..count).map { build() }
}
```

**型パラメータ:**
- `T: Any` - 構築するエンティティ型
- `B: FactoryBuilder<T>` - Builderインターフェース型

**抽象メソッド:**
- `builder(): B` - Builderインスタンスを返す

**メソッド:**

#### build()
```kotlin
fun build(): T
```
単一オブジェクトをメモリ内で構築。

**戻り値:** 構築されたエンティティ

**例:**
```kotlin
class UserFactory : Factory<User, UserBuilder>() {
    override fun builder() = UserBuilder()
}

val user = UserFactory().build()
```

#### buildList()
```kotlin
fun buildList(count: Int): List<T>
```
複数オブジェクトをメモリ内で構築。

**パラメータ:**
- `count: Int` - 構築するオブジェクト数

**戻り値:** 構築されたエンティティのリスト

**例:**
```kotlin
val users = UserFactory().buildList(10)  // 10件生成
```

---

### PersistableFactory<R, T, B>

jOOQ統合とDB永続化を提供する基底クラス。

**パッケージ:** `com.example.faktory.core`

**定義:**
```kotlin
abstract class PersistableFactory<R : TableRecord<R>, T : Any, B : FactoryBuilder<T>>(
    protected val dsl: DSLContext,
) : Factory<T, B>() {
    abstract fun table(): Table<R>
    abstract fun toRecord(entity: T): R

    fun create(): T {
        val entity = build()
        val record = toRecord(entity)
        dsl.executeInsert(record)
        return entity
    }

    fun createList(count: Int): List<T> =
        (1..count).map { create() }
}
```

**型パラメータ:**
- `R: TableRecord<R>` - jOOQ TableRecord型
- `T: Any` - エンティティ型
- `B: FactoryBuilder<T>` - Builderインターフェース型

**コンストラクタ:**
- `dsl: DSLContext` - jOOQデータベースコンテキスト

**抽象メソッド:**

#### table()
```kotlin
abstract fun table(): Table<R>
```
jOOQ Tableインスタンスを返す。

**例:**
```kotlin
import com.example.jooq.tables.Users.USERS

override fun table() = USERS
```

#### toRecord()
```kotlin
abstract fun toRecord(entity: T): R
```
エンティティをjOOQ Recordに変換。

**パラメータ:**
- `entity: T` - 変換元エンティティ

**戻り値:** jOOQ TableRecord

**例:**
```kotlin
override fun toRecord(entity: User): UsersRecord {
    return UsersRecord().apply {
        name = entity.name
        email = entity.email
        age = entity.age
    }
}
```

**メソッド:**

#### create()
```kotlin
fun create(): T
```
オブジェクトを構築してDBに永続化。

**戻り値:** 構築されたエンティティ

**例:**
```kotlin
val factory = UserPersistableFactory(dsl)
val user = factory.create()  // DBに挿入
```

#### createList()
```kotlin
fun createList(count: Int): List<T>
```
複数オブジェクトをDBに永続化。

**パラメータ:**
- `count: Int` - 永続化するオブジェクト数

**戻り値:** 永続化されたエンティティのリスト

**例:**
```kotlin
val users = factory.createList(10)  // 10件挿入
```

---

### FactoryBuilder<T>

Builder Patternの共通インターフェース。

**パッケージ:** `com.example.faktory.core`

**定義:**
```kotlin
interface FactoryBuilder<T : Any> {
    fun build(): T
}
```

**型パラメータ:**
- `T: Any` - 構築するエンティティ型

**メソッド:**

#### build()
```kotlin
fun build(): T
```
エンティティを構築。

**戻り値:** 構築されたエンティティ

**実装例:**
```kotlin
class UserBuilder : FactoryBuilder<User> {
    private var name: String? = null
    private var email: String? = null

    fun withName(value: String) = apply { this.name = value }
    fun withEmail(value: String) = apply { this.email = value }

    override fun build(): User = User(
        name = requireNotNull(name),
        email = requireNotNull(email)
    )
}
```

---

## KSP Processor

### FactoryProcessor

KSP Symbol Processor実装。

**パッケージ:** `com.example.faktory.ksp`

**定義:**
```kotlin
class FactoryProcessor(
    private val environment: SymbolProcessorEnvironment,
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated>
}
```

**責務:**
1. `@Factory`アノテーションを持つクラスを検出
2. `tableName`パラメータを抽出
3. KspJooqMetadataExtractorでメタデータ取得
4. DslCodeGeneratorでコード生成
5. CodeGeneratorで.ktファイル出力

**処理フロー:**
```
@Factory(tableName = "users")
    ↓
FactoryProcessor.process()
    ↓
KspJooqMetadataExtractor.extract("users", resolver)
    ↓ TableMetadata(requiredFields, optionalFields)
DslCodeGenerator.generate(recordClassName, metadata)
    ↓ Kotlinコード文字列
codeGenerator.createNewFile(...).write(code)
    ↓
UsersDslBuilder.kt
```

---

## Metadata Extractors

### KspJooqMetadataExtractor

コンパイル時jOOQメタデータ抽出。

**パッケージ:** `com.example.faktory.ksp.metadata`

**定義:**
```kotlin
object KspJooqMetadataExtractor {
    fun extract(tableName: String, resolver: Resolver): TableMetadata
}
```

**メソッド:**

#### extract()
```kotlin
fun extract(tableName: String, resolver: Resolver): TableMetadata
```

**パラメータ:**
- `tableName: String` - テーブル名（例: "users"）
- `resolver: Resolver` - KSP Resolver

**戻り値:** TableMetadata（テーブルメタデータ）

**処理内容:**
1. `resolver.getClassDeclarationByName("com.example.jooq.tables.Users")`でjOOQクラス取得
2. `TableField<R, V>`型のプロパティを検出
3. DataTypeからnullable情報を抽出
4. idフィールドを除外（自動採番想定）

**例:**
```kotlin
val metadata = KspJooqMetadataExtractor.extract("users", resolver)
// TableMetadata(
//     tableName = "users",
//     requiredFields = ["name", "email"],
//     optionalFields = ["age", "created_at"]
// )
```

---

### JooqMetadataExtractor

実行時jOOQメタデータ抽出（テスト用）。

**パッケージ:** `com.example.faktory.ksp.metadata`

**定義:**
```kotlin
object JooqMetadataExtractor {
    fun extract(table: Table<*>): TableMetadata
}
```

**メソッド:**

#### extract()
```kotlin
fun extract(table: Table<*>): TableMetadata
```

**パラメータ:**
- `table: Table<*>` - jOOQ Tableインスタンス

**戻り値:** TableMetadata

**例:**
```kotlin
import com.example.jooq.tables.Users.USERS

val metadata = JooqMetadataExtractor.extract(USERS)
// TableMetadata(
//     tableName = "users",
//     requiredFields = ["name", "email"],
//     optionalFields = ["age", "created_at"]
// )
```

---

### ForeignKeyDetector

外部キー制約検出。

**パッケージ:** `com.example.faktory.ksp.metadata`

**定義:**
```kotlin
object ForeignKeyDetector {
    fun detect(table: Table<*>): List<ForeignKeyInfo>
}
```

**メソッド:**

#### detect()
```kotlin
fun detect(table: Table<*>): List<ForeignKeyInfo>
```

**パラメータ:**
- `table: Table<*>` - jOOQ Tableインスタンス

**戻り値:** ForeignKeyInfoのリスト

**例:**
```kotlin
import com.example.jooq.tables.Posts.POSTS

val foreignKeys = ForeignKeyDetector.detect(POSTS)
// [
//     ForeignKeyInfo(
//         fieldName = "user_id",
//         referencedTable = "users",
//         referencedField = "id"
//     )
// ]
```

---

### TableMetadata

テーブルメタデータモデル。

**パッケージ:** `com.example.faktory.ksp.metadata`

**定義:**
```kotlin
data class TableMetadata(
    val tableName: String,
    val requiredFields: List<String>,
    val optionalFields: List<String>
)
```

**プロパティ:**
- `tableName: String` - テーブル名
- `requiredFields: List<String>` - 必須フィールド（NOT NULL）
- `optionalFields: List<String>` - オプショナルフィールド（NULL許可）

---

## Code Generators

### DslCodeGenerator

Kotlin DSLコード生成。

**パッケージ:** `com.example.faktory.ksp.codegen`

**定義:**
```kotlin
object DslCodeGenerator {
    fun generate(
        recordClassName: String,
        metadata: TableMetadata,
    ): String
}
```

**メソッド:**

#### generate()
```kotlin
fun generate(
    recordClassName: String,
    metadata: TableMetadata,
): String
```

**パラメータ:**
- `recordClassName: String` - jOOQ Record型名（例: "UsersRecord"）
- `metadata: TableMetadata` - テーブルメタデータ

**戻り値:** 生成されたKotlinコード（文字列）

**生成内容:**
1. DslBuilderクラス
   - `@FactoryDsl` アノテーション
   - 必須フィールド → コンストラクタパラメータ
   - オプショナルフィールド → varプロパティ
   - `build()` メソッド
2. トップレベルDSL関数
   - 必須パラメータ
   - DSLブロックパラメータ（デフォルト: `{}`）

**例:**
```kotlin
val metadata = TableMetadata(
    tableName = "users",
    requiredFields = listOf("name", "email"),
    optionalFields = listOf("age")
)

val code = DslCodeGenerator.generate("UsersRecord", metadata)
// 生成されるコード:
// @FactoryDsl
// class UsersDslBuilder(var name: String, var email: String) {
//     var age: Int? = null
//     internal fun build(): UsersRecord = ...
// }
// fun user(name: String, email: String, block: UsersDslBuilder.() -> Unit = {}): UsersRecord = ...
```

---

### FactoryCodeGenerator

Factory統合コード生成（現在はgenerateComplete()でDSL生成）。

**パッケージ:** `com.example.faktory.ksp.codegen`

**定義:**
```kotlin
object FactoryCodeGenerator {
    fun generateComplete(
        tableName: String,
        recordClassName: String,
        metadata: TableMetadata,
        foreignKeys: List<ForeignKeyInfo>
    ): String
}
```

**メソッド:**

#### generateComplete()
```kotlin
fun generateComplete(
    tableName: String,
    recordClassName: String,
    metadata: TableMetadata,
    foreignKeys: List<ForeignKeyInfo>
): String
```

**パラメータ:**
- `tableName: String` - テーブル名
- `recordClassName: String` - jOOQ Record型名
- `metadata: TableMetadata` - テーブルメタデータ
- `foreignKeys: List<ForeignKeyInfo>` - 外部キー情報（現在未使用）

**戻り値:** 生成されたKotlinコード

**現在の実装:** 内部でDslCodeGenerator.generate()を呼び出し

---

## 型マッピング

### SQL型 → Kotlin型

KSPプロセッサは以下の型マッピングを使用（将来の実装）：

| SQL型 | Kotlin型 |
|-------|----------|
| VARCHAR, TEXT | String |
| INTEGER, SERIAL | Int |
| BIGINT, BIGSERIAL | Long |
| BOOLEAN | Boolean |
| TIMESTAMP, TIMESTAMPTZ | LocalDateTime |
| DATE | LocalDate |
| TIME | LocalTime |
| DECIMAL, NUMERIC | BigDecimal |
| REAL, FLOAT | Float |
| DOUBLE PRECISION | Double |

**現在の実装:** すべてString型として生成（型情報抽出は未実装）

---

## エラーハンドリング

### KSPコンパイルエラー

#### 必須パラメータ不足

```kotlin
val user = user(name = "Alice")
// エラー: No value passed for parameter 'email'
```

#### DSLブロックのネスト

```kotlin
user(name = "Alice", email = "alice@example.com") {
    user(name = "Bob", email = "bob@example.com") { }
}
// エラー: @DslMarker prevents this nesting
```

### 実行時エラー

#### TableRecord型不一致

```kotlin
class UserFactory(dsl: DSLContext) :
    PersistableFactory<PostsRecord, User, UserBuilder>(dsl)
// 実行時エラー: ClassCastException
```

---

## 拡張性

### カスタムコード生成

将来の拡張ポイント：
- カスタムFieldGenerator
- カスタムValidationGenerator
- Association resolver

### プラグインシステム

将来の実装：
- Before/After生成フック
- カスタムアノテーションサポート

---

## バージョン互換性

| faktory-bot-ksp | Kotlin | KSP | jOOQ |
|----------------|--------|-----|------|
| 1.0.0 | 1.9.21 | 1.9.21-1.0.15 | 3.18.7 |

---

## ライセンス

MIT License
