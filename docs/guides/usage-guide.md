# faktory-bot-ksp 利用ガイド

## 目次

1. [概要](#概要)
2. [セットアップ](#セットアップ)
3. [基本的な使い方](#基本的な使い方)
4. [DSLの理解](#dslの理解)
5. [データベース永続化](#データベース永続化)
6. [実践例](#実践例)
7. [トラブルシューティング](#トラブルシューティング)

## 概要

faktory-bot-kspは、jOOQと連携してKotlinでテストデータを生成するためのライブラリです。
最大の特徴は、**コンパイル時に型安全性を保証するDSL**です。

### 従来の問題点

```kotlin
val user = createUser()  // 実行時エラー: name フィールドが必須です
```

### faktory-bot-kspの解決策

```kotlin
// コンパイルエラー: email フィールドが必須です
val user = user(name = "Alice") // ❌

// ✅ すべての必須フィールドを指定
val user = user(name = "Alice", email = "alice@example.com")

// オプショナルフィールドはDSLブロックで設定
val user = user(name = "Bob", email = "bob@example.com") {
    age = 30
}
```

## セットアップ

### 1. Gradleプロジェクト設定

#### ルートbuild.gradle.kts

```kotlin
plugins {
    kotlin("jvm") version "1.9.21"
    id("com.google.devtools.ksp") version "1.9.21-1.0.15"
    id("nu.studer.jooq") version "8.2"
}
```

#### build.gradle.kts

```kotlin
dependencies {
    implementation("com.example.faktory:faktory-runtime:0.1.0-SNAPSHOT")
    ksp("com.example.faktory:faktory-ksp:0.1.0-SNAPSHOT")

    implementation("org.jooq:jooq:3.18.7")
    implementation("org.jooq:jooq-kotlin:3.18.7")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
}
```

### 2. jOOQ設定

```kotlin
jooq {
    version.set("3.18.7")
    configurations {
        create("main") {
            jooqConfiguration.apply {
                generator.apply {
                    name = "org.jooq.codegen.KotlinGenerator"
                    database.apply {
                        name = "org.jooq.meta.extensions.ddl.DDLDatabase"
                        properties.add(
                            org.jooq.meta.jaxb.Property().apply {
                                key = "scripts"
                                value = "src/test/resources/schema.sql"
                            }
                        )
                    }
                    target.apply {
                        packageName = "com.example.jooq"
                        directory = "build/generated-jooq"
                    }
                }
            }
        }
    }
}
```

### 3. スキーマ定義

`src/test/resources/schema.sql`:

```sql
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    age INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### 4. jOOQコード生成

```bash
./gradlew generateJooq
```

## 基本的な使い方

### Factoryクラスの定義

```kotlin
package com.example.factories

import com.example.faktory.ksp.Factory

@Factory(tableName = "users")
class UserFactory
```

### KSPコード生成

```bash
./gradlew kspKotlin
```

KSPが以下のコードを自動生成します：

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

fun user(
    name: String,
    email: String,
    block: UsersDslBuilder.() -> Unit = {},
): UsersRecord = UsersDslBuilder(name, email).apply(block).build()
```

### データ生成

```kotlin
import com.example.jooq.tables.records.UsersRecord

// 必須フィールドのみ
val user: UsersRecord = user(name = "Alice", email = "alice@example.com")

println(user.name)   // "Alice"
println(user.email)  // "alice@example.com"

// オプショナルフィールドも設定
val user2 = user(name = "Bob", email = "bob@example.com") {
    age = 30
    createdAt = LocalDateTime.now()
}

println(user2.age)   // 30
```

## DSLの理解

### コンストラクタパラメータDSL

faktory-bot-kspは、**コンストラクタパラメータDSL**パターンを採用しています。

```kotlin
// 必須フィールド: コンストラクタパラメータ
fun user(
    name: String,        // NOT NULL in schema
    email: String,       // NOT NULL in schema
    block: UsersDslBuilder.() -> Unit = {}
): UsersRecord

// オプショナルフィールド: DSLブロック内のプロパティ
user(name = "Alice", email = "alice@example.com") {
    age = 30            // NULL allowed in schema
    createdAt = LocalDateTime.now()
}
```

### コンパイル時型安全性

必須フィールドを省略するとコンパイルエラー：

```kotlin
// ❌ コンパイルエラー: email パラメータがありません
val user = user(name = "Alice")

// ✅ OK: すべての必須フィールドを指定
val user = user(name = "Alice", email = "alice@example.com")
```

### jOOQメタデータからの自動検出

KSPプロセッサは、jOOQが生成したコードを解析して、NOT NULL制約を自動検出します。

```kotlin
// schema.sql
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,     -- 必須 → コンストラクタパラメータ
    email VARCHAR(255) NOT NULL,    -- 必須 → コンストラクタパラメータ
    age INT,                        -- オプショナル → DSLブロック内プロパティ
    created_at TIMESTAMP            -- オプショナル → DSLブロック内プロパティ
);
```

生成されるDSL：
```kotlin
fun user(
    name: String,      // 必須
    email: String,     // 必須
    block: UsersDslBuilder.() -> Unit = {}
): UsersRecord

// DSLブロック内で設定可能
class UsersDslBuilder(...) {
    var age: Int? = null              // オプショナル
    var createdAt: LocalDateTime? = null  // オプショナル
}
```

### @DslMarkerによるスコープ制御

`@FactoryDsl`アノテーションは、DSLブロックの誤った入れ子を防ぎます：

```kotlin
@DslMarker
annotation class FactoryDsl

@FactoryDsl
class UsersDslBuilder(...) { ... }

// ❌ これはコンパイルエラー
user(name = "Alice", email = "alice@example.com") {
    user(name = "Bob", email = "bob@example.com") {
        // @DslMarkerによりネストが禁止される
    }
}
```

## データベース永続化

### jOOQ DSLContextを使用した永続化

生成されたDSL関数は`UsersRecord`（jOOQ Record）を返すため、`DSLContext.executeInsert()`で永続化できます。

```kotlin
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.jooq.SQLDialect

// DSLContextのセットアップ
val dsl: DSLContext = DSL.using(dataSource, SQLDialect.POSTGRES)

// レコードを構築
val userRecord = user(name = "Alice", email = "alice@example.com") {
    age = 30
}

// DBに永続化
dsl.executeInsert(userRecord)

// 挿入されたIDを取得
val insertedUser = dsl.selectFrom(USERS)
    .where(USERS.EMAIL.eq("alice@example.com"))
    .fetchOne()

println(insertedUser?.id)  // 自動採番されたID
```

### 複数レコードの一括永続化

```kotlin
// 複数レコードを生成
val users = (1..10).map { i ->
    user(name = "User $i", email = "user$i@example.com") {
        age = 20 + i
    }
}

// バッチ挿入
users.forEach { dsl.executeInsert(it) }

// または、jOOQのバッチ処理を使用
dsl.batchInsert(users).execute()
```

### 外部キー制約を持つレコード

```kotlin
// まずユーザーを作成
val userRecord = user(name = "Alice", email = "alice@example.com")
dsl.executeInsert(userRecord)

// 挿入されたユーザーを取得
val savedUser = dsl.selectFrom(USERS)
    .where(USERS.EMAIL.eq("alice@example.com"))
    .fetchOne()!!

// ユーザーレコードを使用してポストを作成（外部キーは自動抽出）
val postRecord = post(
    user = savedUser,  // UsersRecordを渡す
    title = "Alice's First Post",
    content = "Hello, World!"
) {
    published = true
}
dsl.executeInsert(postRecord)
```

## 実践例

### パターン1: メモリ内テスト（DB不要）

ビジネスロジックのテストでDBが不要な場合、DSL関数だけでテストデータを構築できます。

```kotlin
import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat

class UserValidatorTest {
    @Test
    fun `メモリ内でUserRecordを構築してバリデーション`() {
        // DBなしでレコード構築
        val userRecord = user(name = "Alice", email = "alice@example.com") {
            age = 30
        }

        // ビジネスロジックのテスト
        val validator = UserValidator()
        val result = validator.validate(userRecord)

        assertThat(result.isValid).isTrue()
        assertThat(userRecord.name).isEqualTo("Alice")
        assertThat(userRecord.age).isEqualTo(30)
    }

    @Test
    fun `複数レコードを一括生成`() {
        val users = (1..5).map { i ->
            user(name = "User $i", email = "user$i@example.com") {
                age = 20 + i
            }
        }

        assertThat(users).hasSize(5)
        assertThat(users.map { it.name })
            .containsExactly("User 1", "User 2", "User 3", "User 4", "User 5")
        assertThat(users.map { it.age })
            .containsExactly(21, 22, 23, 24, 25)
    }
}
```

### パターン2: DB統合テスト（Testcontainers）

実際のDBを使った統合テストの完全な例。

```kotlin
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
class UserServiceIntegrationTest {
    @Container
    private val postgres = PostgreSQLContainer("postgres:16-alpine")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test")

    private lateinit var dataSource: HikariDataSource

    @BeforeEach
    fun setup() {
        val config = HikariConfig().apply {
            jdbcUrl = postgres.jdbcUrl
            username = postgres.username
            password = postgres.password
            maximumPoolSize = 5
        }
        dataSource = HikariDataSource(config)

        val dsl = DSL.using(dataSource, SQLDialect.POSTGRES)

        // テーブル作成
        dsl.execute("""
            CREATE TABLE users (
                id SERIAL PRIMARY KEY,
                name VARCHAR(255) NOT NULL,
                email VARCHAR(255) NOT NULL UNIQUE,
                age INT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """.trimIndent())
    }

    @AfterEach
    fun teardown() {
        dataSource.close()
    }

    @Test
    fun `ユーザーを作成してDBから取得できる`() {
        val dsl = DSL.using(dataSource, SQLDialect.POSTGRES)

        // Build and persist
        val userRecord = user(name = "Alice", email = "alice@example.com") {
            age = 30
        }
        dsl.executeInsert(userRecord)

        // Verify
        val inserted = dsl.selectFrom(USERS)
            .where(USERS.EMAIL.eq("alice@example.com"))
            .fetchOne()

        assertThat(inserted?.name).isEqualTo("Alice")
        assertThat(inserted?.age).isEqualTo(30)
    }

    @Test
    fun `複数ユーザーを一括挿入できる`() {
        val dsl = DSL.using(dataSource, SQLDialect.POSTGRES)

        val users = (1..5).map { i ->
            user(name = "User $i", email = "user$i@example.com")
        }
        users.forEach { dsl.executeInsert(it) }

        val count = dsl.selectCount().from(USERS).fetchOne(0, Int::class.java)
        assertThat(count).isEqualTo(5)
    }
}
```

### パターン3: 外部キー制約を持つ統合テスト

関連エンティティを含む実践的な統合テストの例。

```sql
CREATE TABLE posts (
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    published BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);
```

```kotlin
@Factory(tableName = "posts")
class PostFactory

// 生成されるDSL
fun post(
    user: UsersRecord,  // Foreign key accepts parent record
    title: String,
    content: String,
    block: PostsDslBuilder.() -> Unit = {}
): PostsRecord

// 完全な統合テスト例
@Testcontainers
class PostServiceIntegrationTest {
    @Container
    private val postgres = PostgreSQLContainer("postgres:16-alpine")

    private lateinit var dataSource: HikariDataSource

    @BeforeEach
    fun setup() {
        dataSource = HikariDataSource(HikariConfig().apply {
            jdbcUrl = postgres.jdbcUrl
            username = postgres.username
            password = postgres.password
        })

        val dsl = DSL.using(dataSource, SQLDialect.POSTGRES)

        // テーブル作成
        dsl.execute("""
            CREATE TABLE users (
                id SERIAL PRIMARY KEY,
                name VARCHAR(255) NOT NULL,
                email VARCHAR(255) NOT NULL UNIQUE,
                age INT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """)

        dsl.execute("""
            CREATE TABLE posts (
                id SERIAL PRIMARY KEY,
                user_id INT NOT NULL,
                title VARCHAR(255) NOT NULL,
                content TEXT NOT NULL,
                published BOOLEAN DEFAULT FALSE,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (user_id) REFERENCES users(id)
            )
        """)
    }

    @Test
    fun `ユーザーと関連ポストを作成できる`() {
        val dsl = DSL.using(dataSource, SQLDialect.POSTGRES)

        // 1. ユーザーを作成
        val userRecord = user(name = "Alice", email = "alice@example.com")
        dsl.executeInsert(userRecord)

        val savedUser = dsl.selectFrom(USERS).fetchOne()!!

        // 2. ポストを作成（外部キーは自動抽出）
        val postRecord = post(
            user = savedUser,  // UsersRecordを渡すだけ
            title = "Alice's First Post",
            content = "Hello, World!"
        ) {
            published = true
        }
        dsl.executeInsert(postRecord)

        // 3. 検証
        val insertedPost = dsl.selectFrom(POSTS).fetchOne()

        assertThat(insertedPost?.userId).isEqualTo(savedUser.id)
        assertThat(insertedPost?.title).isEqualTo("Alice's First Post")
        assertThat(insertedPost?.published).isTrue()
    }

    @Test
    fun `1人のユーザーに複数のポストを作成できる`() {
        val dsl = DSL.using(dataSource, SQLDialect.POSTGRES)

        // ユーザーを作成
        val userRecord = user(name = "Bob", email = "bob@example.com")
        dsl.executeInsert(userRecord)
        val savedUser = dsl.selectFrom(USERS).fetchOne()!!

        // 複数ポストを生成
        val posts = (1..3).map { i ->
            post(
                user = savedUser,
                title = "Post $i",
                content = "Content of post $i"
            ) {
                published = i % 2 == 0  // 偶数番号のみpublish
            }
        }

        posts.forEach { dsl.executeInsert(it) }

        // 検証
        val insertedPosts = dsl.selectFrom(POSTS)
            .orderBy(POSTS.ID)
            .fetch()

        assertThat(insertedPosts).hasSize(3)
        assertThat(insertedPosts.map { it.title })
            .containsExactly("Post 1", "Post 2", "Post 3")
        assertThat(insertedPosts.map { it.published })
            .containsExactly(false, true, false)
        assertThat(insertedPosts.all { it.userId == savedUser.id }).isTrue()
    }
}
```

### シーケンス生成パターン

```kotlin
// シーケンス生成ヘルパー
object UserSequence {
    private val counter = AtomicInteger(0)

    fun next(): Int = counter.incrementAndGet()

    fun nextUser(): UsersRecord =
        user(
            name = "User ${next()}",
            email = "user${next()}@example.com"
        )
}

// 使用例
@Test
fun `シーケンシャルなユーザーを作成できる`() {
    val users = (1..10).map { UserSequence.nextUser() }
    users.forEach { dsl.executeInsert(it) }

    val count = dsl.selectCount().from(USERS).fetchOne(0, Int::class.java)
    assertThat(count).isEqualTo(10)
}
```

## トラブルシューティング

### KSPが生成コードを作成しない

**原因**: @Factoryアノテーションが認識されていない、またはKSPが実行されていない

**解決策**:
1. `build.gradle.kts`に`ksp(...)`依存関係を追加
   ```kotlin
   dependencies {
       ksp("com.example:faktory-ksp:1.0.0")
   }
   ```
2. KSPを明示的に実行
   ```bash
   ./gradlew clean kspKotlin
   ```
3. 生成されたコードを確認
   ```bash
   ls build/generated/ksp/main/kotlin/
   ```

### コンパイルエラー: Missing required parameter

**原因**: 必須フィールド（NOT NULL制約）がコンストラクタパラメータとして省略されている

**解決策**:
jOOQが生成したスキーマを確認し、NOT NULL制約のあるフィールドをすべて指定

```kotlin
// スキーマ確認
val requiredFields = USERS.fields()
    .filter { !it.dataType.nullable() }
    .map { it.name }
// [id, name, email]

// ❌ コンパイルエラー: email が必須
val user = user(name = "Alice")

// ✅ OK: すべての必須フィールドを指定
val user = user(name = "Alice", email = "alice@example.com")
```

### 生成されたDSL関数が見つからない

**原因**: KSPで生成されたコードがコンパイルパスに含まれていない

**解決策**:
```kotlin
// build.gradle.kts
kotlin {
    sourceSets.main {
        kotlin.srcDir("build/generated/ksp/main/kotlin")
    }
}

// または、IDEの再読み込み
./gradlew clean build --refresh-dependencies
```

### jOOQコード生成が失敗する

**原因**: schema.sqlのパスが間違っている、またはSQL構文エラー

**解決策**:
```kotlin
jooq {
    configurations {
        create("main") {
            jooqConfiguration.apply {
                generator.apply {
                    database.apply {
                        name = "org.jooq.meta.extensions.ddl.DDLDatabase"
                        properties.add(
                            org.jooq.meta.jaxb.Property().apply {
                                key = "scripts"
                                value = "src/test/resources/schema.sql"  // パスを確認
                            }
                        )
                    }
                }
            }
        }
    }
}
```

SQL構文を確認：
```bash
# PostgreSQLの場合
psql -f src/test/resources/schema.sql
```

### テストでDBコネクションエラー（Testcontainers使用時）

**原因**: Docker/Colimaが起動していない

**解決策**:
```bash
# Colimaの状態確認
colima status

# 起動していない場合は起動
colima start

# または、Docker Desktopを起動
```

テストコードで確認：
```kotlin
@Test
fun `DB接続テスト`() {
    val postgres = PostgreSQLContainer<Nothing>("postgres:15")
    postgres.start()

    val dsl = DSL.using(
        postgres.jdbcUrl,
        postgres.username,
        postgres.password
    )

    val result = dsl.selectOne().fetchOne()
    assertThat(result).isNotNull()
}
```

## 関連リンク

- [README.md](./README.md) - プロジェクト概要
- [CLAUDE.md](./CLAUDE.md) - 実装詳細
- [faktory-examples](./faktory-examples) - サンプルコード

## ライセンス

MIT License
