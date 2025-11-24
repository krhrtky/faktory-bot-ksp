# Trait System - Type-Safe Reusable Configuration Patterns

Trait機能は、**interface実装ベース**の再利用可能な設定パターンを提供します。ライブラリが`DslTrait<B>`インターフェースを提供し、ユーザーはそれを実装することで型安全なtraitを作成できます。

## 基本概念

### DslTrait Interface（ライブラリ提供）

```kotlin
// faktory-runtime で提供
interface DslTrait<B> {
    operator fun invoke(builder: B)
}
```

このinterfaceにより、IDEの補完機能やコンパイル時検証が効きます。`invoke`オペレーターを使用することで、内部実装の詳細を隠蔽しています。

## 使用例

### 1. 定義済みTrait（Object）の利用

```kotlin
import com.example.faktory.examples.traits.ActiveUser
import com.example.faktory.core.applyTrait
import com.example.faktory.examples.user

val userRecord = user(name = "Alice", email = "alice@example.com") {
    applyTrait(ActiveUser)  // age = 25 が設定される
}
```

### 2. 複数Traitの組み合わせ

```kotlin
import com.example.faktory.core.applyTraits

val userRecord = user(name = "Bob", email = "bob@example.com") {
    applyTraits(SeniorUser, WithCurrentTimestamp)
}
```

### 3. パラメータ化されたTrait（Class）

```kotlin
val userRecord = user(name = "Charlie", email = "charlie@example.com") {
    applyTrait(WithAge(35))
}
```

### 4. カスタムInline Trait

```kotlin
import com.example.faktory.core.DslTrait

val customTrait = object : DslTrait<UsersDslBuilder> {
    override fun UsersDslBuilder.apply() {
        age = 50
        createdAt = LocalDateTime.of(2024, 1, 1, 0, 0)
    }
}

val userRecord = user(name = "David", email = "david@example.com") {
    applyTrait(customTrait)
}
```

### 5. Trait合成（コンポジション）

```kotlin
val composedTrait = object : DslTrait<UsersDslBuilder> {
    override fun UsersDslBuilder.apply() {
        applyTrait(ActiveUser)
        applyTrait(WithCurrentTimestamp)
    }
}

val userRecord = user(name = "Eve", email = "eve@example.com") {
    applyTrait(composedTrait)
}
```

## Traitの実装パターン

### 1. Object（Singleton）

状態を持たないtraitはobjectで実装します。

```kotlin
object ActiveUser : DslTrait<UsersDslBuilder> {
    override fun invoke(builder: UsersDslBuilder) {
        builder.age = 25
    }
}
```

**メリット：**
- シングルトンのためメモリ効率が良い
- インポートして即座に使用可能

### 2. Class（パラメータ化）

パラメータが必要なtraitはclassで実装します。

```kotlin
class WithAge(private val value: Int) : DslTrait<UsersDslBuilder> {
    override fun invoke(builder: UsersDslBuilder) {
        builder.age = value
    }
}

// 使用例
user(name = "Frank", email = "frank@example.com") {
    applyTrait(WithAge(35))
}
```

**メリット：**
- 実行時のパラメータを受け取れる
- 再利用可能なファクトリ関数として機能

### 3. Anonymous Object（Inline定義）

一度だけ使うtraitはanonymous objectで定義します。

```kotlin
val scheduledPost = object : DslTrait<PostsDslBuilder> {
    override fun invoke(builder: PostsDslBuilder) {
        builder.published = false
        builder.createdAt = LocalDateTime.now().plusDays(1)
    }
}

post(user = userRecord, title = "Scheduled", content = "Content") {
    applyTrait(scheduledPost)
}
```

**メリット：**
- テストコード内で柔軟に定義できる
- 一時的なパターンに最適

## 実装済みTrait

### UserTraits

| Trait | 実装形式 | 設定内容 |
|-------|---------|---------|
| `ActiveUser` | object | age = 25 |
| `SeniorUser` | object | age = 65 |
| `YoungAdult` | object | age = 20 |
| `WithCurrentTimestamp` | object | createdAt = now |
| `WithAge(Int)` | class | age = 指定値 |
| `WithTimestamp(LocalDateTime)` | class | createdAt = 指定値 |

### PostTraits

| Trait | 実装形式 | 設定内容 |
|-------|---------|---------|
| `Published` | object | published = true |
| `Draft` | object | published = false |
| `PostWithCurrentTimestamp` | object | createdAt = now |
| `PostWithTimestamp(LocalDateTime)` | class | createdAt = 指定値 |

## Traitの作成方法（実装ガイド）

### Step 1: DslTraitを実装

```kotlin
import com.example.faktory.core.DslTrait
import com.example.faktory.examples.UsersDslBuilder

object ActiveUser : DslTrait<UsersDslBuilder> {
    override fun invoke(builder: UsersDslBuilder) {
        builder.age = 25
    }
}
```

### Step 2: テストで検証

```kotlin
@Test
fun `ActiveUser trait sets age to 25`() {
    val userRecord = user(name = "Test", email = "test@example.com") {
        applyTrait(ActiveUser)
    }

    assertThat(userRecord.age).isEqualTo(25)
}
```

### Step 3: ドキュメント化

```kotlin
/**
 * Sets user age to 25, representing an active adult user.
 *
 * Usage:
 * ```
 * user(name = "Alice", email = "alice@example.com") {
 *     applyTrait(ActiveUser)
 * }
 * ```
 */
object ActiveUser : DslTrait<UsersDslBuilder> {
    override fun invoke(builder: UsersDslBuilder) {
        builder.age = 25
    }
}
```

## IDEサポート

### コード補完

`DslTrait<T>`を実装する際、IDEが以下を支援します：

1. **override補完** - `invoke()`オペレーターの自動生成
2. **型推論** - パラメータ型`B`の自動推論
3. **エラー検出** - 存在しないプロパティへのアクセスをコンパイル時に検出

### 使用箇所の検索

interfaceベースなため、以下が簡単に実行できます：

- **Find Usages** - Traitの使用箇所を検索
- **Go to Implementation** - 実装を一覧表示
- **Refactor → Rename** - 型安全にリネーム

## ベストプラクティス

### ✅ DO

```kotlin
// 1. objectで状態を持たないtraitを定義
object ActiveUser : DslTrait<UsersDslBuilder> {
    override fun invoke(builder: UsersDslBuilder) {
        builder.age = 25
    }
}

// 2. classでパラメータ化されたtraitを定義
class WithAge(private val value: Int) : DslTrait<UsersDslBuilder> {
    override fun invoke(builder: UsersDslBuilder) {
        builder.age = value
    }
}

// 3. 命名規則に従う（WithXxx, IsXxx, HasXxxなど）
object WithCurrentTimestamp : DslTrait<UsersDslBuilder> { ... }

// 4. ドキュメントコメントを書く
/**
 * Sets the user as an active adult (age = 25).
 */
object ActiveUser : DslTrait<UsersDslBuilder> { ... }

// 5. Trait合成は新しいTraitとして実装
object ActiveUserWithTimestamp : DslTrait<UsersDslBuilder> {
    override fun invoke(builder: UsersDslBuilder) {
        builder.applyTrait(ActiveUser)
        builder.applyTrait(WithCurrentTimestamp)
    }
}
```

### ❌ DON'T

```kotlin
// 1. 必須フィールドを変更しようとしない
object InvalidTrait : DslTrait<UsersDslBuilder> {
    override fun invoke(builder: UsersDslBuilder) {
        builder.name = "Invalid"  // ❌ コンパイルエラー（varではない）
    }
}

// 2. 複雑すぎるTraitを作らない
object TooComplexTrait : DslTrait<UsersDslBuilder> {
    override fun invoke(builder: UsersDslBuilder) {
        // ❌ 多数のロジックを含むのは避ける
        if (LocalDateTime.now().hour > 12) {
            builder.age = 25
        } else {
            builder.age = 30
        }
    }
}

// 3. mutabilityに依存しない
class StatefulTrait : DslTrait<UsersDslBuilder> {
    var counter = 0  // ❌ 状態を持つのは避ける
    override fun invoke(builder: UsersDslBuilder) {
        builder.age = counter++
    }
}
```

## テスト例

完全なテスト例は以下を参照：

- `TraitUsageTest.kt` - 9テストケース（全成功✅）
- `PostTraitUsageTest.kt` - 6テストケース（全成功✅）

## Interface vs Typealias

### 以前のTypealias実装

```kotlin
typealias UserTrait = UsersDslBuilder.() -> Unit

val activeUser: UserTrait = { age = 25 }
```

### 現在のInterface実装

```kotlin
interface DslTrait<B> {
    operator fun invoke(builder: B)
}

object ActiveUser : DslTrait<UsersDslBuilder> {
    override fun invoke(builder: UsersDslBuilder) {
        builder.age = 25
    }
}
```

### Interface実装の利点

| 項目 | Typealias | Interface |
|------|-----------|-----------|
| IDEサポート | 限定的 | ✅ フル対応 |
| 型推論 | ラムダ型 | ✅ 具体的な型 |
| コード補完 | なし | ✅ override補完 |
| ドキュメント | 関数リテラル | ✅ KDocサポート |
| 階層構造 | なし | ✅ Find Usages可能 |
| リファクタリング | 危険 | ✅ 型安全 |

## まとめ

faktory-bot-kspのTrait機能は、`DslTrait<B>` interfaceにより：

1. **型安全** - コンパイル時検証
2. **IDE統合** - コード補完・リファクタリング対応
3. **実装誘導** - interfaceによる明示的な契約
4. **保守性** - Find Usagesで全使用箇所を追跡可能

ライブラリユーザーは、この`DslTrait<B>` interfaceを実装するだけで、独自のtraitを簡単に作成できます。
