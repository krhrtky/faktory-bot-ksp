# DSL Generated Code

このディレクトリには、`DslCodeGenerator`で生成されたDSLコードが含まれています。

## 生成方法

```kotlin
val metadata = TableMetadata(
    tableName = "users",
    requiredFields = listOf("name", "email"),
    optionalFields = listOf("age", "created_at")
)

val code = DslCodeGenerator.generate("UsersRecord", metadata)
```

## 使用例

### User DSL

```kotlin
// 必須フィールドのみ（コンパイル時検証）
val user = user(name = "Alice", email = "alice@example.com")

// オプショナルフィールド追加
val user = user(name = "Bob", email = "bob@example.com") {
    age = 30
}
```

### Post DSL

```kotlin
// 必須フィールド：userId, title, content
val post = post(
    userId = 1,
    title = "My First Post",
    content = "Hello, World!"
)

// オプショナルフィールド：published
val post = post(
    userId = 1,
    title = "Published Post",
    content = "Content"
) {
    published = true
}
```

## 特徴

1. **コンパイル時型安全性**
   - 必須フィールドはコンストラクタパラメータ
   - 不足している場合はコンパイルエラー

2. **Kotlinらしい記述**
   - 名前付き引数で意図が明確
   - DSLブロックでオプショナルフィールド設定

3. **@DslMarkerによるスコープ制御**
   - 入れ子構造の誤用を防止

## テスト例

### UserDslTest

```kotlin
@Test
fun `user() 必須フィールドのみでUserRecordを構築`() {
    val userRecord = user(name = "Alice", email = "alice@example.com")

    assertThat(userRecord.name).isEqualTo("Alice")
    assertThat(userRecord.email).isEqualTo("alice@example.com")
}

@Test
fun `user() DSLブロックでオプショナルフィールドを設定`() {
    val userRecord = user(name = "Bob", email = "bob@example.com") {
        age = 30
    }

    assertThat(userRecord.age).isEqualTo(30)
}
```

### PostDslTest

```kotlin
@Test
fun `post() 必須フィールドのみでPostRecordを構築`() {
    val postRecord = post(
        userId = 1,
        title = "My First Post",
        content = "Hello, World!",
    )

    assertThat(postRecord.title).isEqualTo("My First Post")
}

@Test
fun `post() 関連するUserと一緒にDBに永続化`() {
    val dsl = DSL.using(dataSource, SQLDialect.POSTGRES)

    val userRecord = user(name = "Alice", email = "alice@example.com")
    dsl.executeInsert(userRecord)

    val userId = dsl.selectFrom(USERS).fetchOne()!!.id!!

    val postRecord = post(
        userId = userId,
        title = "Alice's Post",
        content = "Content by Alice",
    ) {
        published = true
    }
    dsl.executeInsert(postRecord)
}
```

## テスト実行について

統合テストを実行するにはDockerが必要です。

### Colima使用時

```bash
# Colimaが起動していることを確認
colima status

# 起動していない場合は起動
colima start

# テスト実行
./gradlew :faktory-examples:test
```

### Docker Desktop使用時

Docker Desktopが起動していることを確認してからテストを実行してください。

```bash
./gradlew :faktory-examples:test
```

## 将来の改善

- 型情報の正確な抽出（Int, Boolean, Timestamp等）
- jOOQ Recordへの値設定を自動生成
- KSP統合で完全自動生成
