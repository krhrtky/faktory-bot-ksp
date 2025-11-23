# DSL設計ドキュメント

## 現状分析

### 現在のBuilderパターン

```kotlin
val user = UserBuilder()
    .withName("Alice")
    .withEmail("alice@example.com")
    .withAge(30)
    .build()
```

**問題点:**
- メソッドチェーンが冗長
- `withXxx()` の繰り返し
- Kotlinらしくない

## 目標DSL設計

### アプローチ1: シンプルDSL（実行時検証）

```kotlin
val user = user {
    name = "Alice"
    email = "alice@example.com"
    age = 30
}
```

**実装:**
```kotlin
@DslMarker
annotation class FactoryDsl

@FactoryDsl
class UserDslBuilder {
    var name: String? = null
    var email: String? = null
    var age: Int? = null

    internal fun build(): User {
        require(name != null) { "name is required" }
        require(email != null) { "email is required" }
        return User(name!!, email!!, age)
    }
}

fun user(block: UserDslBuilder.() -> Unit): User {
    return UserDslBuilder().apply(block).build()
}
```

**長所:**
- Kotlinらしい記述
- スコープ関数活用
- 簡潔

**短所:**
- 必須フィールドが実行時チェック
- コンパイル時型安全性が低い

### アプローチ2: コンストラクタパラメータDSL（推奨）

```kotlin
val user = user(name = "Alice", email = "alice@example.com") {
    age = 30
}
```

**実装:**
```kotlin
@DslMarker
annotation class FactoryDsl

@FactoryDsl
class UserDslBuilder(
    var name: String,
    var email: String,
) {
    var age: Int? = null

    internal fun build(): User = User(name, email, age)
}

fun user(
    name: String,
    email: String,
    block: UserDslBuilder.() -> Unit = {}
): User {
    return UserDslBuilder(name, email).apply(block).build()
}
```

**長所:**
- 必須フィールドがコンパイル時検証
- 型安全性が高い
- 名前付き引数で意図が明確
- オプショナルフィールドはDSLブロック内

**短所:**
- 必須フィールドが多い場合、引数リストが長くなる

### アプローチ3: 段階的DSL（型安全Builder）

```kotlin
val user = user {
    name("Alice")
        .email("alice@example.com")
        .age(30)
        .build()
}
```

**実装:**
```kotlin
class UserDslBuilder0 {
    fun name(value: String) = UserDslBuilder1(value)
}

class UserDslBuilder1(private val name: String) {
    fun email(value: String) = UserDslBuilder2(name, value)
}

@DslMarker
annotation class FactoryDsl

@FactoryDsl
class UserDslBuilder2(
    private val name: String,
    private val email: String,
) {
    private var age: Int? = null

    fun age(value: Int) = apply { this.age = value }
    fun build() = User(name, email, age)
}

fun user(block: UserDslBuilder0.() -> User): User {
    return UserDslBuilder0().block()
}
```

**長所:**
- 完全なコンパイル時型安全性
- 必須フィールドの順序を強制可能

**短所:**
- 実装が複雑
- Builderクラスが増える（N個の必須フィールドでN+1個のクラス）

## 推奨設計: アプローチ2

**理由:**
1. コンパイル時型安全性を保持
2. Kotlinの慣習に沿っている
3. 実装がシンプル
4. 可読性が高い

### 生成コード例

```kotlin
// KSPが生成
@DslMarker
annotation class FactoryDsl

@FactoryDsl
class UserDslBuilder(
    var name: String,
    var email: String,
) {
    var age: Int? = null
    var createdAt: java.time.LocalDateTime? = null

    internal fun build(): UsersRecord {
        return UsersRecord().apply {
            this.name = this@UserDslBuilder.name
            this.email = this@UserDslBuilder.email
            this.age = this@UserDslBuilder.age
            this.createdAt = this@UserDslBuilder.createdAt
        }
    }
}

fun user(
    name: String,
    email: String,
    block: UserDslBuilder.() -> Unit = {}
): UsersRecord {
    return UserDslBuilder(name, email).apply(block).build()
}
```

### 使用例

```kotlin
// 必須フィールドのみ
val user1 = user(name = "Alice", email = "alice@example.com")

// オプショナルフィールドも設定
val user2 = user(name = "Bob", email = "bob@example.com") {
    age = 30
}

// エラー: 必須フィールド未設定
val user3 = user(name = "Charlie") // コンパイルエラー
```

## TDD実装計画

### Phase 1: DSL基盤実装

**Step 1: @FactoryDsl アノテーション（RED → GREEN → REFACTOR）**

```kotlin
// RED: テスト
@Test
fun `@FactoryDsl annotation exists`() {
    val annotation = FactoryDsl::class
    assertThat(annotation.annotations)
        .anyMatch { it is DslMarker }
}

// GREEN: 最小実装
@DslMarker
annotation class FactoryDsl

// REFACTOR: 不要
```

**Step 2: DslBuilder クラス生成（RED → GREEN → REFACTOR）**

```kotlin
// RED: テスト
@Test
fun `generate DslBuilder with required fields as constructor params`() {
    val metadata = TableMetadata(
        tableName = "users",
        requiredFields = listOf("name", "email")
    )

    val code = DslCodeGenerator.generate("UsersRecord", metadata)

    assertThat(code).contains("class UserDslBuilder(")
    assertThat(code).contains("var name: String,")
    assertThat(code).contains("var email: String,")
}

// GREEN: 実装
// REFACTOR: コード整理
```

### Phase 2: コード生成エンジン

**DslCodeGenerator 実装:**
1. 必須フィールド → コンストラクタパラメータ
2. オプショナルフィールド → var プロパティ
3. build() メソッド生成
4. トップレベルDSL関数生成

### Phase 3: Factory統合

**Factory クラス拡張:**
```kotlin
abstract class Factory<T : Any> {
    abstract fun dsl(block: Any.() -> Unit): T

    fun build(): T = dsl {}
    fun buildList(count: Int): List<T> = (1..count).map { build() }
}
```

## メリット・デメリット比較

| 項目 | Builderパターン | DSL (アプローチ2) |
|------|----------------|-------------------|
| コンパイル時型安全性 | ○（Phantom Types） | ○（コンストラクタ） |
| 可読性 | △（冗長） | ○（簡潔） |
| Kotlinらしさ | △ | ○ |
| 実装複雑度 | ○（シンプル） | ○（シンプル） |
| 必須フィールド検証 | コンパイル時 | コンパイル時 |
| IDE補完 | ○ | ○ |

## 実装優先度

1. **Phase 1:** DslCodeGenerator の基盤（必須）
2. **Phase 2:** Factory統合（必須）
3. **Phase 3:** 既存Builderとの互換性維持（オプション）

## 移行戦略

### 段階的移行

```kotlin
// 1. DSL版を追加（Builderは維持）
val user1 = user(name = "Alice", email = "alice@example.com")
val user2 = UserBuilder().withName("Alice").withEmail("alice@example.com").build()

// 2. DSL版を推奨に
// 3. Builder版を非推奨に
@Deprecated("Use DSL: user(name, email) { }")
class UserBuilder

// 4. Builder版を削除
```
