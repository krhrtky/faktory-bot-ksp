# faktory-bot-ksp 利用ガイド

## 目次

1. [概要](#概要)
2. [セットアップ](#セットアップ)
3. [基本的な使い方](#基本的な使い方)
4. [型レベル制約の理解](#型レベル制約の理解)
5. [データベース永続化](#データベース永続化)
6. [実践例](#実践例)
7. [トラブルシューティング](#トラブルシューティング)

## 概要

faktory-bot-kspは、jOOQと連携してKotlinでテストデータを生成するためのライブラリです。
最大の特徴は、**コンパイル時に型安全性を保証**することです。

### 従来の問題点

```kotlin
val user = createUser()  // 実行時エラー: name フィールドが必須です
```

### faktory-bot-kspの解決策

```kotlin
val user = UserFactory()
    .build()  // コンパイルエラー: name フィールドが設定されていません

val user = UserFactory()
    .withName("Alice")
    .withEmail("alice@example.com")
    .build()  // ✅ コンパイル成功
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
sealed interface UsersFieldState {
    object WithName : UsersFieldState
    object WithEmail : UsersFieldState
    object Complete : UsersFieldState
}

interface UsersFactoryBuilder<S : UsersFieldState> {
    fun withName(value: String): UsersFactoryBuilder<UsersFieldState.WithName>
    fun withEmail(value: String): UsersFactoryBuilder<UsersFieldState.WithEmail>
    fun <S : UsersFieldState.Complete> build(): UsersRecord
}
```

### データ生成

```kotlin
import com.example.jooq.tables.records.UsersRecord

val user: UsersRecord = UserFactory()
    .withName("Alice")
    .withEmail("alice@example.com")
    .build()

println(user.name)   // "Alice"
println(user.email)  // "alice@example.com"
```

## 型レベル制約の理解

### Phantom Typesによる状態追跡

faktory-bot-kspは、**Phantom Types**パターンを使用して、ビルダーの状態をコンパイル時に追跡します。

```kotlin
// 状態1: 初期状態（何も設定されていない）
val builder1: UsersFactoryBuilder<UsersFieldState.Initial> = UserFactory()

// 状態2: nameが設定された
val builder2: UsersFactoryBuilder<UsersFieldState.WithName> = builder1.withName("Alice")

// 状態3: emailが設定された（Complete状態）
val builder3: UsersFactoryBuilder<UsersFieldState.Complete> = builder2.withEmail("alice@example.com")

// build()はComplete状態でのみ呼び出し可能
val user = builder3.build()  // ✅ OK
```

### コンパイル時検証

```kotlin
val user = UserFactory().build()
// コンパイルエラー:
// Type mismatch: inferred type is UsersFieldState.Initial
// but UsersFieldState.Complete was expected
```

### jOOQメタデータからの自動検出

KSPプロセッサは、jOOQが生成したコードを解析して、NOT NULL制約を自動検出します。

```kotlin
// schema.sql
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,     -- 必須フィールド
    email VARCHAR(255) NOT NULL,    -- 必須フィールド
    age INT                         -- オプショナル
);
```

KSPプロセッサが検出：
- `name`: NOT NULL → withName() メソッド必須
- `email`: NOT NULL → withEmail() メソッド必須
- `age`: NULL許可 → withAge() メソッドはオプショナル

## データベース永続化

### build() vs create()

- **build()**: メモリ内でオブジェクトを構築（DBアクセスなし）
- **create()**: オブジェクトを構築してDBに永続化

### PersistableFactoryの実装

```kotlin
import com.example.faktory.core.PersistableFactory
import com.example.jooq.tables.Users.USERS
import com.example.jooq.tables.records.UsersRecord
import org.jooq.DSLContext

class UserFactoryImpl(dsl: DSLContext) :
    PersistableFactory<UsersRecord, User, UserBuilder>(dsl) {

    override fun builder() = UserBuilder()

    override fun table() = USERS

    override fun toRecord(entity: User): UsersRecord {
        return UsersRecord().apply {
            name = entity.name
            email = entity.email
            age = entity.age
        }
    }
}

data class User(
    val name: String,
    val email: String,
    val age: Int? = null
)

class UserBuilder : FactoryBuilder<User> {
    private var name: String? = null
    private var email: String? = null
    private var age: Int? = null

    fun withName(value: String) = apply { this.name = value }
    fun withEmail(value: String) = apply { this.email = value }
    fun withAge(value: Int) = apply { this.age = value }

    override fun build(): User {
        return User(
            name = requireNotNull(name),
            email = requireNotNull(email),
            age = age
        )
    }
}
```

### 使用例

```kotlin
import org.jooq.DSLContext
import org.jooq.impl.DSL

val dsl: DSLContext = DSL.using(connection, SQLDialect.POSTGRES)
val factory = UserFactoryImpl(dsl)

// メモリ内構築のみ
val user1 = factory.build()

// DB永続化
val user2 = factory.create()

// 複数レコード一括永続化
val users = factory.createList(10)
```

## 実践例

### 基本的なテストデータ生成

```kotlin
import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat

class UserServiceTest {
    private val dsl: DSLContext = setupTestDatabase()
    private val userFactory = UserFactoryImpl(dsl)

    @Test
    fun `ユーザーを作成できる`() {
        val user = userFactory
            .withName("Alice")
            .withEmail("alice@example.com")
            .create()

        assertThat(user.name).isEqualTo("Alice")
        assertThat(user.email).isEqualTo("alice@example.com")
    }

    @Test
    fun `複数ユーザーを一括作成できる`() {
        val users = userFactory.createList(5)

        assertThat(users).hasSize(5)
        assertThat(users.map { it.name }).containsExactly(
            "User 1", "User 2", "User 3", "User 4", "User 5"
        )
    }
}
```

### 外部キー制約を持つテーブル

```sql
CREATE TABLE posts (
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id)
);
```

```kotlin
@Factory(tableName = "posts")
class PostFactory

class PostFactoryImpl(
    private val dsl: DSLContext,
    private val userFactory: UserFactoryImpl
) : PersistableFactory<PostsRecord, Post, PostBuilder>(dsl) {

    override fun builder() = PostBuilder()

    override fun table() = POSTS

    override fun toRecord(entity: Post): PostsRecord {
        return PostsRecord().apply {
            userId = entity.userId
            title = entity.title
            content = entity.content
        }
    }

    fun withUser(user: User): PostBuilder {
        return builder().withUserId(user.id)
    }
}

// 使用例
val user = userFactory.create()
val post = postFactory
    .withUser(user)
    .withTitle("My First Post")
    .withContent("Hello, World!")
    .create()
```

### シーケンス生成

```kotlin
class UserBuilderWithSequence : FactoryBuilder<User> {
    private val sequence = AtomicInteger(0)

    fun withSequentialName() = apply {
        this.name = "User ${sequence.incrementAndGet()}"
    }

    override fun build(): User {
        return User(
            name = name ?: "User ${sequence.incrementAndGet()}",
            email = email ?: "user${sequence.get()}@example.com"
        )
    }
}

// 使用例
val users = (1..10).map {
    userFactory.withSequentialName().create()
}
// User 1, User 2, ..., User 10
```

## トラブルシューティング

### KSPが生成コードを作成しない

**原因**: @Factoryアノテーションが認識されていない

**解決策**:
1. `build.gradle.kts`に`ksp(...)`依存関係を追加
2. `./gradlew clean kspKotlin`を実行

### コンパイルエラー: Type mismatch

**原因**: 必須フィールドが設定されていない

**解決策**:
jOOQが生成したスキーマを確認し、NOT NULL制約のあるフィールドをすべて設定

```kotlin
// 必須フィールドを確認
val tableFields = USERS.fields()
    .filter { !it.dataType.nullable() }
    .map { it.name }
// [id, name, email]

// すべての必須フィールドを設定
val user = UserFactory()
    .withName("Alice")      // 必須
    .withEmail("alice@example.com")  // 必須
    .build()
```

### jOOQコード生成が失敗する

**原因**: schema.sqlのパスが間違っている

**解決策**:
```kotlin
jooq {
    configurations {
        create("main") {
            jooqConfiguration.apply {
                generator.apply {
                    database.apply {
                        properties.add(
                            org.jooq.meta.jaxb.Property().apply {
                                key = "scripts"
                                value = "src/test/resources/schema.sql"  // ここを確認
                            }
                        )
                    }
                }
            }
        }
    }
}
```

### テストでDBコネクションエラー

**原因**: DSLContextが正しく設定されていない

**解決策**:
```kotlin
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jooq.impl.DSL

val config = HikariConfig().apply {
    jdbcUrl = "jdbc:postgresql://localhost:5432/testdb"
    username = "postgres"
    password = "password"
}
val dataSource = HikariDataSource(config)
val dsl = DSL.using(dataSource, SQLDialect.POSTGRES)
```

## 関連リンク

- [README.md](./README.md) - プロジェクト概要
- [CLAUDE.md](./CLAUDE.md) - 実装詳細
- [faktory-examples](./faktory-examples) - サンプルコード

## ライセンス

MIT License
