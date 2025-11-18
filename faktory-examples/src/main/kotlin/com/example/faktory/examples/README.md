# faktory-bot-ksp Examples

このディレクトリには、faktory-bot-kspの実際に動作するサンプルコードが含まれています。

## ファイル構成

### エンティティ

- `User.kt` - Userデータクラス
- `Post.kt` - Postデータクラス

### ビルダー

- `UserBuilder.kt` - Userエンティティのビルダー
- `PostBuilder.kt` - Postエンティティのビルダー

### ファクトリ実装

- `UserFactoryImpl.kt` - `PersistableFactory`を継承したUser用ファクトリ
- `PostFactoryImpl.kt` - `PersistableFactory`を継承したPost用ファクトリ

## 使い方

### 1. jOOQコード生成

```bash
./gradlew :faktory-examples:generateJooq
```

### 2. メモリ内でデータ生成

```kotlin
val userFactory = UserFactoryImpl(dsl)
val user = userFactory
    .builder()
    .withName("Alice")
    .withEmail("alice@example.com")
    .build()
```

### 3. DBに永続化

```kotlin
val userFactory = UserFactoryImpl(dsl)

// 単一エンティティ
val user = userFactory
    .builder()
    .withName("Bob")
    .withEmail("bob@example.com")
    .create()

// 複数エンティティ
val users = (1..10).map { i ->
    userFactory
        .builder()
        .withSequence()
        .create()
}
```

### 4. 外部キー制約のあるデータ

```kotlin
val userFactory = UserFactoryImpl(dsl)
val postFactory = PostFactoryImpl(dsl)

// ユーザーを作成
val user = userFactory
    .builder()
    .withName("Alice")
    .withEmail("alice@example.com")
    .build()

val userRecord = userFactory.toRecord(user)
dsl.executeInsert(userRecord)
val insertedUser = dsl.selectFrom(userFactory.table()).fetchOne()

// 関連するPostを作成
val post = postFactory
    .builder()
    .withUserId(insertedUser!!.id!!)
    .withTitle("My First Post")
    .withContent("Hello, World!")
    .build()
```

## テスト

統合テストは `src/test/kotlin` に含まれています。

```bash
./gradlew :faktory-examples:test
```

### テストケース

- `UserFactoryTest` - UserFactoryの動作確認
- `PostFactoryTest` - PostFactoryと外部キー制約の動作確認

## 注意事項

### KSP自動生成について

このexampleは手動実装です。将来のKSP統合版では、以下のようなコード生成が予定されています：

```kotlin
@Factory(tableName = "users")
class UserFactory

// ↓ KSPが自動生成

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

現在は、BuilderCodeGeneratorの改善が進行中です。
