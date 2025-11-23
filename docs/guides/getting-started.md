# Getting Started with faktory-bot-ksp

## Overview

faktory-bot-kspは、jOOQと連携してKotlinでテストデータを生成するためのライブラリです。
最大の特徴は、**コンパイル時に型安全性を保証するDSL**です。

### Key Benefits

**従来の問題点:**
```kotlin
val user = createUser()  // 実行時エラー: name フィールドが必須です
```

**faktory-bot-kspの解決策:**
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

## Prerequisites

- Kotlin 1.9.21+
- Gradle 8.0+
- jOOQ 3.18.7+
- PostgreSQL (または他のRDBMS)

## Installation

### 1. Add Gradle Plugins

```kotlin
// build.gradle.kts
plugins {
    kotlin("jvm") version "1.9.21"
    id("com.google.devtools.ksp") version "1.9.21-1.0.15"
    id("nu.studer.jooq") version "8.2"
}
```

### 2. Add Dependencies

```kotlin
dependencies {
    // faktory-bot-ksp
    implementation("com.example.faktory:faktory-runtime:1.0.0")
    ksp("com.example.faktory:faktory-ksp:1.0.0")

    // jOOQ
    implementation("org.jooq:jooq:3.18.7")
    implementation("org.jooq:jooq-kotlin:3.18.7")

    // Database driver
    implementation("org.postgresql:postgresql:42.6.0")

    // Testing (optional)
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testImplementation("org.testcontainers:postgresql:1.19.3")
}
```

### 3. Configure jOOQ Code Generation

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

## Quick Start

### Step 1: Define Database Schema

Create `src/test/resources/schema.sql`:

```sql
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    age INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE posts (
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    published BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (user_id) REFERENCES users(id)
);
```

### Step 2: Generate jOOQ Code

```bash
./gradlew generateJooq
```

This generates:
- `build/generated-jooq/com/example/jooq/tables/Users.kt`
- `build/generated-jooq/com/example/jooq/tables/Posts.kt`
- `build/generated-jooq/com/example/jooq/tables/records/UsersRecord.kt`
- `build/generated-jooq/com/example/jooq/tables/records/PostsRecord.kt`

### Step 3: Define Factories

Create factory definitions:

```kotlin
// src/main/kotlin/com/example/factories/UserFactory.kt
package com.example.factories

import com.example.faktory.ksp.Factory

@Factory(tableName = "users")
class UserFactory

@Factory(tableName = "posts")
class PostFactory
```

### Step 4: Generate Factory Code

```bash
./gradlew kspKotlin
```

This generates DSL functions in `build/generated/ksp/main/kotlin/`:

**UserDsl.kt:**
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

### Step 5: Use the Generated DSL

#### In-Memory Object Creation

```kotlin
import com.example.jooq.tables.records.UsersRecord

// Minimal required fields
val user = user(name = "Alice", email = "alice@example.com")

// With optional fields
val user = user(name = "Bob", email = "bob@example.com") {
    age = 30
    createdAt = LocalDateTime.now()
}

println(user.name)  // "Bob"
println(user.age)   // 30
```

#### Database Persistence

```kotlin
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.jooq.SQLDialect

// Setup DSLContext
val dsl: DSLContext = DSL.using(dataSource, SQLDialect.POSTGRES)

// Create and persist
val userRecord = user(name = "Alice", email = "alice@example.com") {
    age = 30
}
dsl.executeInsert(userRecord)

// Retrieve inserted record
val inserted = dsl.selectFrom(USERS)
    .where(USERS.EMAIL.eq("alice@example.com"))
    .fetchOne()

println(inserted?.id)  // Auto-generated ID
```

#### Related Entities (Foreign Keys)

```kotlin
// Create user first
val userRecord = user(name = "Alice", email = "alice@example.com")
dsl.executeInsert(userRecord)

val userId = dsl.selectFrom(USERS)
    .where(USERS.EMAIL.eq("alice@example.com"))
    .fetchOne()!!
    .id!!

// Create post with foreign key
val postRecord = post(
    userId = userId,
    title = "Alice's First Post",
    content = "Hello, World!"
) {
    published = true
}
dsl.executeInsert(postRecord)
```

## Testing Example

```kotlin
import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat
import org.testcontainers.containers.PostgreSQLContainer

class UserServiceTest {
    private val postgres = PostgreSQLContainer<Nothing>("postgres:15")

    private val dsl by lazy {
        postgres.start()
        DSL.using(postgres.jdbcUrl, postgres.username, postgres.password)
    }

    @Test
    fun `create and retrieve user`() {
        // Create test data
        val userRecord = user(name = "Alice", email = "alice@example.com") {
            age = 30
        }
        dsl.executeInsert(userRecord)

        // Test
        val found = dsl.selectFrom(USERS)
            .where(USERS.EMAIL.eq("alice@example.com"))
            .fetchOne()

        assertThat(found?.name).isEqualTo("Alice")
        assertThat(found?.age).isEqualTo(30)
    }

    @Test
    fun `create multiple users`() {
        val users = (1..5).map { i ->
            user(name = "User $i", email = "user$i@example.com")
        }
        users.forEach { dsl.executeInsert(it) }

        val count = dsl.selectCount().from(USERS).fetchOne(0, Int::class.java)
        assertThat(count).isEqualTo(5)
    }
}
```

## Next Steps

- [Usage Guide](./usage-guide.md) - Detailed usage patterns and best practices
- [API Reference](../api/api-reference.md) - Complete API documentation
- [Troubleshooting](./troubleshooting.md) - Common issues and solutions

## Common Issues

### KSP doesn't generate code

**Solution:**
```bash
./gradlew clean kspKotlin
```

### Generated DSL functions not found

**Solution:**
```kotlin
// build.gradle.kts
kotlin {
    sourceSets.main {
        kotlin.srcDir("build/generated/ksp/main/kotlin")
    }
}
```

### Compile error: missing required parameter

**Cause:** NOT NULL field not specified

**Solution:**
```kotlin
// ❌ Missing email
val user = user(name = "Alice")

// ✅ All required fields
val user = user(name = "Alice", email = "alice@example.com")
```

For more troubleshooting, see the [Troubleshooting Guide](./troubleshooting.md).
