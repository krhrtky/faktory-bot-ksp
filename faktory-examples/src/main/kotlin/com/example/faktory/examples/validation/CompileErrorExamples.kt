package com.example.faktory.examples.validation

/**
 * 型安全性を示すコンパイルエラーの例
 *
 * これらの例は意図的にコメントアウトされています。
 * コメントを外すとコンパイルエラーになることを確認できます。
 */
object CompileErrorExamples {
    /*
    // ❌ コンパイルエラー例1: 間違った型をuser extensionに渡す
    fun wrongTypeInUserExtension() {
        post(title = "Title", content = "Content") {
            associate {
                user {
                    // エラー: Type mismatch
                    // Required: UsersRecord
                    // Found: PostsRecord
                    post(title = "Nested", content = "Wrong")
                }
            }
        }
    }

    // ❌ コンパイルエラー例2: 間違った戻り値の型
    fun wrongReturnTypeInFactory() {
        post(title = "Title", content = "Content") {
            associate {
                user {
                    // エラー: Type mismatch
                    // Required: UsersRecord
                    // Found: String
                    "This is not a UsersRecord"
                }
            }
        }
    }

    // ❌ コンパイルエラー例3: nullを返す
    fun nullReturnInFactory() {
        post(title = "Title", content = "Content") {
            associate {
                user {
                    // エラー: Type mismatch
                    // Required: UsersRecord
                    // Found: Nothing?
                    null
                }
            }
        }
    }
    */

    // ✅ 正しい使用例
    fun correctUsage() {
        // これはコンパイル可能
        /*
        post(title = "Title", content = "Content") {
            associate {
                // user()はUsersRecordを返すfactoryのみ受け付ける
                user {
                    user(name = "Alice", email = "alice@example.com")
                }
            }
        }
        */
    }
}

/**
 * 型制約の説明
 *
 * 生成されるextension関数：
 * ```kotlin
 * fun AssociationContext.user(block: () -> UsersRecord) {
 *     associateWithPersist("user_id", block)
 * }
 * ```
 *
 * この定義により、以下が保証されます：
 *
 * 1. block パラメータは UsersRecord を返す必要がある
 *    - 戻り値の型が UsersRecord でない場合はコンパイルエラー
 *
 * 2. 関数シグネチャが型チェックを強制
 *    - block: () -> UsersRecord
 *    - PostsRecord や String など、他の型は受け付けない
 *
 * 3. コンパイル時に型安全性が保証される
 *    - 実行時エラーではなく、コンパイル時エラー
 *    - IDEでも即座に型エラーが表示される
 */
