# 型安全なAssociate機能の検証

このディレクトリには、faktory-bot-kspの**型レベルでの外部キー関連の型安全性**を示すexampleとテストが含まれています。

## 型安全性の仕組み

### 生成されるExtension関数

KSPによって以下のようなextension関数が自動生成されます：

```kotlin
// posts.user_id → users テーブルへの外部キー
fun AssociationContext.user(block: () -> UsersRecord) {
    associateWithPersist("user_id", block)
}
```

### 型パラメータによる制約

このextension関数の型シグネチャ `block: () -> UsersRecord` により、以下が**コンパイル時**に保証されます：

#### ✅ 正しい型のみ受け付ける

```kotlin
post(title = "Title", content = "Content") {
    associate {
        user {
            // ✅ OK: UsersRecordを返す
            user(name = "Alice", email = "alice@example.com")
        }
    }
}
```

#### ❌ 間違った型はコンパイルエラー

```kotlin
post(title = "Title", content = "Content") {
    associate {
        user {
            // ❌ コンパイルエラー: Type mismatch
            // Required: UsersRecord
            // Found: PostsRecord
            post(title = "Wrong", content = "Type")
        }
    }
}
```

## ファイル構成

### TypeSafeAssociateExamples.kt

実際にコンパイル・実行可能な使用例集：

- `basicAssociate()` - 基本的な使い方
- `associateWithComplexUser()` - 複雑なUserの作成
- `associateWithVariable()` - 変数を使った関連エンティティ作成
- `associateWithCondition()` - 条件分岐
- `demonstrateTypeConstraints()` - 型制約の説明

### CompileErrorExamples.kt

意図的にコンパイルエラーになる例（コメントアウト済み）：

- 間違った型をextensionに渡す
- 間違った戻り値の型
- nullを返す

コメントを外すとコンパイルエラーを確認できます。

### TypeSafetyValidationTest.kt

型安全性を検証するテストケース：

- Extension関数が正しいRecord型のみ受け付けることを確認
- 型パラメータによる制約を検証
- コンパイル可能なコードのテスト

## 従来のfaktory-botとの違い

### faktory-bot（元）

```kotlin
// 実行時に型チェック
val post = PostFactory.build {
    // 実行時エラー: 間違った型のfactoryを指定
    user { CommentFactory.build() }  // RuntimeException
}
```

### faktory-bot-ksp（本プロジェクト）

```kotlin
// コンパイル時に型チェック
val post = post(title = "...", content = "...") {
    associate {
        // コンパイルエラー: 型が合わない
        user { comment(...) }  // Compile Error
    }
}
```

## 型安全性の利点

### 1. 早期のバグ発見

- **実行時** → **コンパイル時** にエラー検出が移行
- テストを実行する前にバグを発見

### 2. IDEサポート

- 型エラーは開発中に即座に表示
- 補完機能が正しい型のみを提案
- リファクタリングも型安全

### 3. ドキュメントとしての型

- 関数シグネチャが明確な契約を示す
- `user(block: () -> UsersRecord)` を見れば、UsersRecordが必要だと分かる

### 4. 実行時オーバーヘッドなし

- コンパイル時にチェックされるため、実行時の検証不要
- パフォーマンス向上

## 検証方法

### 1. テストの実行

```bash
./gradlew :faktory-examples:test --tests "TypeSafetyValidationTest"
```

全てのテストがGREENになることを確認してください。

### 2. コンパイルエラーの確認

`CompileErrorExamples.kt` のコメントを外して、意図的にコンパイルエラーを発生させます：

```bash
./gradlew :faktory-examples:build
```

期待通りのコンパイルエラーが表示されることを確認してください。

### 3. IDEでの確認

IntelliJ IDEAやAndroid Studioで `TypeSafeAssociateExamples.kt` を開き、以下を確認：

- 型エラーが赤線で表示される
- 正しい型の補完が機能する
- ホバーで型情報が表示される

## まとめ

faktory-bot-kspの**型レベルでの外部キー関連の型安全性**により：

- ✅ コンパイル時に関連テーブルの型が検証される
- ✅ 間違った型のfactoryを渡すとコンパイルエラー
- ✅ IDEの強力なサポート（補完、エラー表示、リファクタリング）
- ✅ 実行時オーバーヘッドなし

これにより、元のfaktory-botの実行時検証が、**完全にコンパイル時・型レベル検証に移行**されました。
