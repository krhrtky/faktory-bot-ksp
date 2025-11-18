package com.example.faktory.examples.validation

import com.example.faktory.examples.generated.post
import com.example.faktory.examples.generated.user
import com.example.faktory.examples.jooq.tables.records.PostsRecord
import com.example.faktory.examples.jooq.tables.records.UsersRecord
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * 型レベルでの外部キー関連の型安全性を検証するテスト
 */
class TypeSafetyValidationTest {
    @Test
    fun `associate block enforces correct Record type at compile time`() {
        // この実装はコンパイル可能（正しい型）
        val postRecord = post(
            title = "Type Safe Post",
            content = "Content",
        ) {
            // associateブロック内でuser extension関数を呼ぶ
            associate {
                // user()はUsersRecordを返すfactoryのみ受け付ける
                user {
                    user(name = "Alice", email = "alice@example.com")
                }
            }
        }

        // PostRecordが正しく構築される
        assertThat(postRecord).isInstanceOf(PostsRecord::class.java)
        assertThat(postRecord.title).isEqualTo("Type Safe Post")
        assertThat(postRecord.content).isEqualTo("Content")
    }

    @Test
    fun `user extension function only accepts lambda returning UsersRecord`() {
        val postRecord = post(
            title = "Another Post",
            content = "More content",
        ) {
            associate {
                // user()に渡すlambdaはUsersRecordを返す必要がある
                user {
                    // これはコンパイル可能
                    val userRecord = user(name = "Bob", email = "bob@example.com")
                    userRecord // UsersRecordを返す
                }
            }
        }

        assertThat(postRecord).isNotNull
    }

    @Test
    fun `associate extension provides type-safe factory API`() {
        // Extension関数は型パラメータで制約されている
        // fun AssociationContext.user(block: () -> UsersRecord)

        val postRecord = post(
            title = "Type Constrained",
            content = "Content",
        ) {
            associate {
                // この呼び出しは型安全
                // - user()はUsersRecord型のfactoryのみ受け付ける
                // - 間違った型を返すとコンパイルエラー
                user { user(name = "Charlie", email = "charlie@example.com") }
            }
        }

        assertThat(postRecord.title).isEqualTo("Type Constrained")
    }

    // このテストはコンパイルエラーを示すためのドキュメント用
    // 実際には以下のコードはコンパイルできない
    /*
    @Test
    fun `COMPILE ERROR - wrong type in user extension`() {
        post(title = "Wrong", content = "Type") {
            associate {
                user {
                    // コンパイルエラー：PostsRecordはUsersRecordではない
                    post(title = "Nested", content = "Wrong")
                }
            }
        }
    }
    */
}
