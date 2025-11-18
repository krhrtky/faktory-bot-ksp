package com.example.faktory.examples.validation

import com.example.faktory.examples.generated.post
import com.example.faktory.examples.generated.user
import com.example.faktory.examples.jooq.tables.records.PostsRecord
import com.example.faktory.examples.jooq.tables.records.UsersRecord

/**
 * 型安全なassociate機能の使用例
 *
 * このファイルでは、extension関数の型パラメータによって
 * 外部キー関連の型安全性がどのように保証されるかを示します。
 */
object TypeSafeAssociateExamples {
    /**
     * Example 1: 基本的なassociate使用
     *
     * user() extension関数は UsersRecord を返すfactoryのみ受け付けます。
     */
    fun basicAssociate(): PostsRecord =
        post(
            title = "My First Post",
            content = "This is the content",
        ) {
            associate {
                // ✅ 型安全: user()はUsersRecordを返すfactoryを要求
                user {
                    user(name = "Alice", email = "alice@example.com")
                }
            }
        }

    /**
     * Example 2: 複数のフィールドを持つUser
     *
     * user()関数の内部で、任意のUserファクトリコードを書けます。
     * 戻り値の型（UsersRecord）さえ満たせばOK。
     */
    fun associateWithComplexUser(): PostsRecord =
        post(
            title = "Post with Complex User",
            content = "Content here",
        ) {
            associate {
                user {
                    // user()ファクトリでオプショナルフィールドも設定可能
                    user(name = "Bob", email = "bob@example.com") {
                        age = 30
                    }
                }
            }
        }

    /**
     * Example 3: 変数を使った関連エンティティ作成
     *
     * lambdaの中で複雑なロジックも書けます。
     * 最後に UsersRecord を返せばOK。
     */
    fun associateWithVariable(): PostsRecord =
        post(
            title = "Post with Variable",
            content = "Content",
        ) {
            associate {
                user {
                    // 変数に保存してから返すことも可能
                    val userRecord = user(name = "Charlie", email = "charlie@example.com")
                    // 追加の処理（ロギングなど）
                    println("Created user: ${userRecord.name}")
                    userRecord // UsersRecordを返す
                }
            }
        }

    /**
     * Example 4: 条件分岐による関連エンティティ作成
     *
     * 条件によって異なるUserを作成する場合も型安全。
     */
    fun associateWithCondition(isAdmin: Boolean): PostsRecord =
        post(
            title = "Conditional Post",
            content = "Content",
        ) {
            associate {
                user {
                    if (isAdmin) {
                        user(name = "Admin User", email = "admin@example.com") {
                            age = 99
                        }
                    } else {
                        user(name = "Regular User", email = "user@example.com")
                    }
                    // どちらのブランチもUsersRecordを返すため型安全
                }
            }
        }

    /**
     * Example 5: 型制約の説明
     *
     * 以下のコードは、extension関数の型シグネチャによって
     * コンパイル時に型安全性が保証されることを示しています。
     */
    fun demonstrateTypeConstraints() {
        // Extension関数の定義（KSPで自動生成される）：
        // fun AssociationContext.user(block: () -> UsersRecord)
        //
        // この型シグネチャにより、以下が保証されます：
        //
        // 1. blockパラメータは () -> UsersRecord 型
        //    → UsersRecordを返す関数のみ渡せる
        //
        // 2. 間違った型を返すとコンパイルエラー
        //    → 実行時ではなくコンパイル時にエラー検出
        //
        // 3. IDEの補完とエラー表示が機能
        //    → 開発中に即座にフィードバック

        post(title = "Example", content = "Content") {
            associate {
                user {
                    // ✅ OK: UsersRecordを返す
                    user(name = "Dave", email = "dave@example.com")

                    // ❌ コンパイルエラー: PostsRecordを返す
                    // post(title = "Wrong", content = "Type")

                    // ❌ コンパイルエラー: Stringを返す
                    // "not a record"

                    // ❌ コンパイルエラー: nullを返す
                    // null
                }
            }
        }
    }
}

/**
 * 型安全性のまとめ
 *
 * ## コンパイル時に保証されること
 *
 * 1. **正しいRecord型の強制**
 *    - user() は UsersRecord を返すfactoryのみ受け付ける
 *    - 間違った型（PostsRecord, String, null等）はコンパイルエラー
 *
 * 2. **外部キー関連の型チェック**
 *    - posts.user_id → users テーブルへの参照
 *    - AssociationContext.user() → UsersRecord を要求
 *    - 型レベルで関連テーブルとの整合性を保証
 *
 * 3. **IDEサポート**
 *    - 型エラーは開発中に即座に表示される
 *    - 補完機能が正しい型のみを提案
 *    - リファクタリングも型安全に実行可能
 *
 * ## 実行時ではなくコンパイル時
 *
 * 従来のfaktory-botでは実行時にチェックしていた以下の検証が、
 * コンパイル時に移行されました：
 *
 * - 外部キー関連の型チェック
 * - 関連エンティティのfactory呼び出しチェック
 *
 * これにより、より早い段階でバグを発見できます。
 */
