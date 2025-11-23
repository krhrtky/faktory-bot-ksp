# Troubleshooting Guide

## Table of Contents

1. [KSP Code Generation Issues](#ksp-code-generation-issues)
2. [Compile Errors](#compile-errors)
3. [jOOQ Code Generation Issues](#jooq-code-generation-issues)
4. [Runtime Errors](#runtime-errors)
5. [Testing Issues](#testing-issues)

---

## KSP Code Generation Issues

### KSPが生成コードを作成しない

**症状:**
`build/generated/ksp/main/kotlin/` にファイルが生成されない

**原因:**
- `@Factory`アノテーションが認識されていない
- KSPが実行されていない
- `ksp(...)`依存関係が設定されていない

**解決策:**

1. **依存関係を確認:**
   ```kotlin
   // build.gradle.kts
   dependencies {
       ksp("com.example:faktory-ksp:1.0.0")  // これが必要
   }
   ```

2. **KSPを明示的に実行:**
   ```bash
   ./gradlew clean kspKotlin
   ```

3. **生成されたコードを確認:**
   ```bash
   ls build/generated/ksp/main/kotlin/
   ```

4. **KSPログを確認:**
   ```bash
   ./gradlew kspKotlin --info
   ```

---

### 生成されたDSL関数が見つからない

**症状:**
```kotlin
val user = user(name = "Alice", email = "alice@example.com")
// Unresolved reference: user
```

**原因:**
KSPで生成されたコードがコンパイルパスに含まれていない

**解決策:**

1. **sourceSetを設定:**
   ```kotlin
   // build.gradle.kts
   kotlin {
       sourceSets.main {
           kotlin.srcDir("build/generated/ksp/main/kotlin")
       }
   }
   ```

2. **IDEを再読み込み:**
   ```bash
   ./gradlew clean build --refresh-dependencies
   ```

3. **IntelliJ IDEA:**
   - File → Invalidate Caches / Restart
   - Gradle sync: View → Tool Windows → Gradle → Reload

---

### @Factoryアノテーションがインポートできない

**症状:**
```kotlin
import com.example.faktory.ksp.Factory  // Unresolved reference
```

**原因:**
faktory-ksp依存関係が不足

**解決策:**
```kotlin
dependencies {
    ksp("com.example:faktory-ksp:1.0.0")
    implementation("com.example:faktory-runtime:1.0.0")  // これも必要
}
```

---

## Compile Errors

### Missing required parameter

**症状:**
```kotlin
val user = user(name = "Alice")
// No value passed for parameter 'email'
```

**原因:**
必須フィールド（NOT NULL制約）がコンストラクタパラメータとして省略されている

**解決策:**

1. **スキーマを確認:**
   ```sql
   -- schema.sql
   CREATE TABLE users (
       name VARCHAR(255) NOT NULL,     -- 必須
       email VARCHAR(255) NOT NULL,    -- 必須
       age INT                         -- オプショナル
   );
   ```

2. **jOOQメタデータで確認:**
   ```kotlin
   val requiredFields = USERS.fields()
       .filter { !it.dataType.nullable() }
       .map { it.name }
   // [id, name, email]
   ```

3. **すべての必須フィールドを指定:**
   ```kotlin
   // ❌ コンパイルエラー
   val user = user(name = "Alice")

   // ✅ OK
   val user = user(name = "Alice", email = "alice@example.com")
   ```

---

### Type mismatch errors

**症状:**
```kotlin
val age: Int = 30
user(name = "Alice", email = "alice@example.com") {
    age = age  // Type mismatch: inferred type is Int but Int? was expected
}
```

**原因:**
オプショナルフィールドは`Int?`（nullable）型

**解決策:**
```kotlin
// 型を明示的に指定
val age: Int? = 30

user(name = "Alice", email = "alice@example.com") {
    this.age = age  // OK
}

// または直接値を設定
user(name = "Alice", email = "alice@example.com") {
    age = 30  // OK
}
```

---

### @DslMarker prevents nesting

**症状:**
```kotlin
user(name = "Alice", email = "alice@example.com") {
    user(name = "Bob", email = "bob@example.com") {  // Error
        // ...
    }
}
```

**原因:**
`@FactoryDsl`アノテーション（`@DslMarker`）がDSLブロックのネストを防止

**解決策:**
ネストせずに個別に作成:
```kotlin
val alice = user(name = "Alice", email = "alice@example.com")
val bob = user(name = "Bob", email = "bob@example.com")
```

---

## jOOQ Code Generation Issues

### jOOQコード生成が失敗する

**症状:**
```
Task :generateJooq FAILED
Could not resolve schema.sql
```

**原因:**
schema.sqlのパスが間違っている、またはSQL構文エラー

**解決策:**

1. **パスを確認:**
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

2. **ファイルの存在を確認:**
   ```bash
   ls -la src/test/resources/schema.sql
   ```

3. **SQL構文を確認:**
   ```bash
   # PostgreSQLの場合
   psql -f src/test/resources/schema.sql

   # または、オンライン検証ツールを使用
   ```

---

### jOOQ Tableクラスが見つからない

**症状:**
```kotlin
import com.example.jooq.tables.Users.USERS  // Unresolved reference
```

**原因:**
jOOQコード生成が先に実行されていない

**解決策:**

正しい実行順序:
```bash
# 1. jOOQコード生成
./gradlew generateJooq

# 2. KSPコード生成
./gradlew kspKotlin

# 3. ビルド
./gradlew build
```

または、一括実行:
```bash
./gradlew clean build
```

---

### Package name mismatch

**症状:**
KSPがjOOQ Tableクラスを見つけられない

**原因:**
jOOQのpackageName設定とKSPの想定が異なる

**解決策:**
```kotlin
jooq {
    configurations {
        create("main") {
            jooqConfiguration.apply {
                generator.apply {
                    target.apply {
                        packageName = "com.example.jooq"  // KSPがこれを参照
                        directory = "build/generated-jooq"
                    }
                }
            }
        }
    }
}
```

---

## Runtime Errors

### DSLContextが見つからない

**症状:**
```kotlin
val dsl = DSL.using(...)  // Unresolved reference: DSL
```

**原因:**
jOOQ依存関係が不足

**解決策:**
```kotlin
dependencies {
    implementation("org.jooq:jooq:3.18.7")
}
```

---

### TableRecord型のキャストエラー

**症状:**
```
ClassCastException: UsersRecord cannot be cast to PostsRecord
```

**原因:**
PersistableFactoryの型パラメータが間違っている

**解決策:**
```kotlin
// ❌ 間違い
class UserFactory(dsl: DSLContext) :
    PersistableFactory<PostsRecord, User, UserBuilder>(dsl)  // 型不一致

// ✅ 正しい
class UserFactory(dsl: DSLContext) :
    PersistableFactory<UsersRecord, User, UserBuilder>(dsl)
```

---

### Foreign key constraint violation

**症状:**
```
ERROR: insert or update on table "posts" violates foreign key constraint
```

**原因:**
参照先のレコード（user）が存在しない

**解決策:**
```kotlin
// ❌ 間違い: userを先に作成していない
val post = post(userId = 999, title = "Post", content = "Content")
dsl.executeInsert(post)  // Error: user_id=999 が存在しない

// ✅ 正しい: userを先に作成
val userRecord = user(name = "Alice", email = "alice@example.com")
dsl.executeInsert(userRecord)

val userId = dsl.selectFrom(USERS)
    .where(USERS.EMAIL.eq("alice@example.com"))
    .fetchOne()!!
    .id!!

val post = post(userId = userId, title = "Post", content = "Content")
dsl.executeInsert(post)  // OK
```

---

## Testing Issues

### Testcontainersが起動しない

**症状:**
```
Could not find a valid Docker environment
```

**原因:**
Docker/Colimaが起動していない

**解決策:**

1. **Colimaの状態確認:**
   ```bash
   colima status
   ```

2. **起動していない場合は起動:**
   ```bash
   colima start
   ```

3. **または、Docker Desktopを起動**

4. **Dockerが動作しているか確認:**
   ```bash
   docker ps
   ```

---

### Testcontainersのタイムアウト

**症状:**
```
Container startup failed: Timed out waiting for container port to open
```

**原因:**
- Dockerリソース不足
- ネットワーク問題

**解決策:**

1. **Dockerリソースを増やす:**
   ```bash
   # Colima
   colima stop
   colima start --cpu 4 --memory 8
   ```

2. **イメージを事前にpull:**
   ```bash
   docker pull postgres:15
   ```

3. **タイムアウトを延長:**
   ```kotlin
   val postgres = PostgreSQLContainer<Nothing>("postgres:15")
       .withStartupTimeout(Duration.ofMinutes(5))
   ```

---

### DB接続エラー（Testcontainers使用時）

**症状:**
```
PSQLException: FATAL: password authentication failed
```

**解決策:**

Testcontainersのデフォルト認証情報を使用:
```kotlin
@Test
fun `DB接続テスト`() {
    val postgres = PostgreSQLContainer<Nothing>("postgres:15")
    postgres.start()

    val dsl = DSL.using(
        postgres.jdbcUrl,      // 自動生成されたURL
        postgres.username,     // デフォルト: "test"
        postgres.password      // デフォルト: "test"
    )

    val result = dsl.selectOne().fetchOne()
    assertThat(result).isNotNull()
}
```

---

### トランザクション分離の問題

**症状:**
テスト間でデータが残る

**解決策:**

各テスト後にテーブルをクリア:
```kotlin
@AfterEach
fun cleanup() {
    dsl.deleteFrom(POSTS).execute()
    dsl.deleteFrom(USERS).execute()
}
```

または、トランザクションをロールバック:
```kotlin
@Test
fun `test with rollback`() {
    dsl.transaction { config ->
        val txDsl = DSL.using(config)

        val user = user(name = "Alice", email = "alice@example.com")
        txDsl.executeInsert(user)

        // テスト終了後に自動ロールバック
    }
}
```

---

## Performance Issues

### 大量データ挿入が遅い

**症状:**
1000件挿入に数分かかる

**解決策:**

バッチ挿入を使用:
```kotlin
// ❌ 遅い: 個別挿入
(1..1000).forEach { i ->
    val user = user(name = "User $i", email = "user$i@example.com")
    dsl.executeInsert(user)  // 1000回のクエリ
}

// ✅ 速い: バッチ挿入
val users = (1..1000).map { i ->
    user(name = "User $i", email = "user$i@example.com")
}
dsl.batchInsert(users).execute()  // 1回のバッチ
```

---

## Getting Help

If you encounter issues not covered in this guide:

1. **Check existing issues:** [GitHub Issues](https://github.com/krhrtky/faktory-bot-ksp/issues)
2. **Enable debug logging:**
   ```bash
   ./gradlew kspKotlin --debug > ksp-debug.log 2>&1
   ```
3. **Create a minimal reproduction:**
   - Schema definition
   - Factory definition
   - Error message
4. **Submit an issue:** [New Issue](https://github.com/krhrtky/faktory-bot-ksp/issues/new)

---

## Related Documentation

- [Getting Started](./getting-started.md)
- [Usage Guide](./usage-guide.md)
- [API Reference](../api/api-reference.md)
