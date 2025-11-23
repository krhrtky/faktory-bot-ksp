# faktory-runtime

Runtime library for faktory-bot-ksp: Provides base classes and annotations for factory pattern with jOOQ.

## 概要

faktory-runtimeモジュールは、KSPで生成されたDSLコードが依存するランタイムライブラリです。
Factory基底クラス、PersistableFactory、@FactoryDsl annotationを提供します。

## 主要コンポーネント

### @FactoryDsl Annotation

`@DslMarker`アノテーションを使用したDSLスコープ制御。

```kotlin
@DslMarker
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class FactoryDsl
```

**目的:**
- DSLブロックの誤った入れ子を防止
- コンパイル時にスコープエラーを検出

**使用例:**

```kotlin
@FactoryDsl
class UsersDslBuilder(
    var name: String,
    var email: String,
) {
    var age: Int? = null
    // ...
}

// ❌ これはコンパイルエラー（@DslMarkerにより防止）
user(name = "Alice", email = "alice@example.com") {
    user(name = "Bob", email = "bob@example.com") {
        // ネストされたDSLは許可されない
    }
}
```

### Factory<T, B> (基底クラス)

メモリ内でのオブジェクト構築を提供する抽象クラス。

```kotlin
abstract class Factory<T : Any, B : FactoryBuilder<T>> {
    abstract fun builder(): B

    fun build(): T = builder().build()

    fun buildList(count: Int): List<T> =
        (1..count).map { build() }
}
```

**型パラメータ:**
- `T`: 構築するエンティティ型（例: User, Post）
- `B`: Builderインターフェース型（例: UserBuilder）

**メソッド:**
- `build()`: 単一オブジェクトをメモリ内構築
- `buildList(count)`: 複数オブジェクトをメモリ内構築

**使用例:**

```kotlin
data class User(val name: String, val email: String)

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

class UserFactory : Factory<User, UserBuilder>() {
    override fun builder() = UserBuilder()
}

// 使用
val factory = UserFactory()
val user = factory.build()
val users = factory.buildList(10)
```

### PersistableFactory<R, T, B> (DB永続化)

jOOQ統合とDB永続化を提供する抽象クラス。

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
- `R`: jOOQ TableRecord型（例: UsersRecord）
- `T`: エンティティ型（例: User）
- `B`: Builderインターフェース型（例: UserBuilder）

**コンストラクタ:**
- `dsl: DSLContext` - jOOQのデータベースコンテキスト

**抽象メソッド:**
- `table()`: jOOQ Tableインスタンスを返す
- `toRecord(entity)`: エンティティをjOOQ Recordに変換

**メソッド:**
- `create()`: オブジェクトを構築してDBに永続化
- `createList(count)`: 複数オブジェクトをDBに永続化

**使用例:**

```kotlin
import com.example.jooq.tables.Users.USERS
import com.example.jooq.tables.records.UsersRecord
import org.jooq.DSLContext

class UserPersistableFactory(dsl: DSLContext) :
    PersistableFactory<UsersRecord, User, UserBuilder>(dsl) {

    override fun builder() = UserBuilder()

    override fun table() = USERS

    override fun toRecord(entity: User): UsersRecord {
        return UsersRecord().apply {
            name = entity.name
            email = entity.email
        }
    }
}

// 使用
val dsl = DSL.using(dataSource, SQLDialect.POSTGRES)
val factory = UserPersistableFactory(dsl)

val user = factory.create()        // DBに挿入
val users = factory.createList(10)  // 10件挿入
```

## FactoryBuilder<T> Interface

Builder Patternの共通インターフェース。

```kotlin
interface FactoryBuilder<T : Any> {
    fun build(): T
}
```

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

## 使用パターン

### パターン1: DSL + jOOQ直接永続化（推奨）

KSP生成DSL + jOOQ DSLContextで直接永続化。

```kotlin
// KSPで生成されるDSL
@FactoryDsl
class UsersDslBuilder(var name: String, var email: String) {
    internal fun build(): UsersRecord = UsersRecord().apply {
        this.name = this@UsersDslBuilder.name
        this.email = this@UsersDslBuilder.email
    }
}

fun user(
    name: String,
    email: String,
    block: UsersDslBuilder.() -> Unit = {}
): UsersRecord = UsersDslBuilder(name, email).apply(block).build()

// 使用
val dsl = DSL.using(dataSource, SQLDialect.POSTGRES)

val userRecord = user(name = "Alice", email = "alice@example.com") {
    age = 30
}
dsl.executeInsert(userRecord)
```

**メリット:**
- シンプルで直感的
- jOOQ APIをフル活用
- KSP生成コードのみで完結

### パターン2: Factory基底クラス（メモリ構築のみ）

Factory<T, B>を使用したメモリ内構築。

```kotlin
data class User(val name: String, val email: String)

class UserFactory : Factory<User, UserBuilder>() {
    override fun builder() = UserBuilder()
}

// 使用
val factory = UserFactory()
val user = factory.build()        // メモリ内のみ
val users = factory.buildList(10) // メモリ内のみ
```

**用途:**
- DBを使わないユニットテスト
- メモリ内データ生成

### パターン3: PersistableFactory（カスタムエンティティ）

jOOQ Recordとは異なる独自エンティティを永続化。

```kotlin
data class User(
    val id: Int?,
    val name: String,
    val email: String,
    val createdAt: LocalDateTime
)

class UserPersistableFactory(dsl: DSLContext) :
    PersistableFactory<UsersRecord, User, UserBuilder>(dsl) {

    override fun builder() = UserBuilder()
    override fun table() = USERS

    override fun toRecord(entity: User): UsersRecord {
        return UsersRecord().apply {
            id = entity.id
            name = entity.name
            email = entity.email
            createdAt = entity.createdAt
        }
    }

    override fun build(): User {
        val builder = builder()
        // デフォルト値設定
        builder.withName("Default User")
        builder.withEmail("default@example.com")
        builder.withCreatedAt(LocalDateTime.now())
        return builder.build()
    }
}

// 使用
val factory = UserPersistableFactory(dsl)
val user = factory.create()  // デフォルト値でDB挿入
```

**用途:**
- jOOQ Recordとビジネスロジックモデルを分離
- デフォルト値の一元管理
- 複雑な初期化ロジック

## テスト支援

### テストでの使用例

```kotlin
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer

class UserServiceTest {
    private val postgres = PostgreSQLContainer<Nothing>("postgres:15")
    private val dsl by lazy {
        DSL.using(postgres.jdbcUrl, postgres.username, postgres.password)
    }

    @Test
    fun `ユーザーサービスのテスト`() {
        // テストデータ作成
        val userRecord = user(name = "Alice", email = "alice@example.com")
        dsl.executeInsert(userRecord)

        // サービスのテスト
        val service = UserService(dsl)
        val found = service.findByEmail("alice@example.com")

        assertThat(found?.name).isEqualTo("Alice")
    }
}
```

### モックデータ生成

```kotlin
// 大量のテストデータ生成
val testUsers = (1..100).map { i ->
    user(name = "User $i", email = "user$i@example.com") {
        age = 20 + (i % 50)
    }
}
testUsers.forEach { dsl.executeInsert(it) }
```

## ビルド設定

### build.gradle.kts

```kotlin
dependencies {
    // Runtime（このモジュール）
    implementation("com.example.faktory:faktory-runtime:1.0.0")

    // jOOQ（PersistableFactoryに必要）
    implementation("org.jooq:jooq:3.18.7")
    implementation("org.jooq:jooq-kotlin:3.18.7")
}
```

## トラブルシューティング

### @FactoryDslが認識されない

**原因:** faktory-runtimeが依存関係に含まれていない

**解決策:**
```kotlin
dependencies {
    implementation("com.example.faktory:faktory-runtime:1.0.0")
}
```

### DSLContextが見つからない

**原因:** jOOQ依存関係が不足

**解決策:**
```kotlin
dependencies {
    implementation("org.jooq:jooq:3.18.7")
}
```

### TableRecordのキャストエラー

**原因:** PersistableFactoryの型パラメータが間違っている

**解決策:**
```kotlin
// ❌ 間違い
class UserFactory(dsl: DSLContext) :
    PersistableFactory<PostsRecord, User, UserBuilder>(dsl)  // 型不一致

// ✅ 正しい
class UserFactory(dsl: DSLContext) :
    PersistableFactory<UsersRecord, User, UserBuilder>(dsl)
```

## アーキテクチャ

```
faktory-runtime/
├── src/main/kotlin/
│   └── com/example/faktory/core/
│       ├── Factory.kt              # メモリ内構築
│       ├── PersistableFactory.kt   # DB永続化
│       └── FactoryDsl.kt           # @DslMarker annotation
└── src/test/kotlin/
    └── com/example/faktory/core/
        ├── FactoryTest.kt
        ├── PersistableFactoryTest.kt
        └── FactoryDslTest.kt
```

## 依存関係

```kotlin
dependencies {
    // jOOQ（PersistableFactoryに必要）
    implementation("org.jooq:jooq:3.18.7")
    implementation("org.jooq:jooq-kotlin:3.18.7")

    // テスト
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testImplementation("org.assertj:assertj-core:3.24.2")
    testImplementation("org.testcontainers:postgresql:1.19.3")
}
```

## ライセンス

MIT License
