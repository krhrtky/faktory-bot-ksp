# faktory-bot-ksp ドキュメント整備計画

## 実装とドキュメントの乖離分析

### 現在の実装状況（2025-11-23時点）

#### 完成した実装
1. **Phase 1-4:** KSP基盤、コード生成、Runtime、jOOQ統合 ✅
2. **Phase 5:** DSL実装 ✅
   - DslCodeGenerator（faktory-ksp）
   - FactoryDsl annotation（faktory-runtime）
   - user(), post()などのトップレベル関数生成
   - 必須フィールド：コンストラクタパラメータ
   - オプショナルフィールド：DSLブロック

#### 実装の現状
```kotlin
// 実際に生成されるコード（UserDsl.kt）
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

#### 使用例
```kotlin
// 必須フィールドのみ
val user = user(name = "Alice", email = "alice@example.com")

// オプショナルフィールドも設定
val user = user(name = "Bob", email = "bob@example.com") {
    age = 30
}

// DB永続化
val dsl = DSL.using(dataSource, SQLDialect.POSTGRES)
dsl.executeInsert(user)
```

### 既存ドキュメントの問題点

#### 1. README.md
**問題:**
- 古いPhantom Types + Builderパターンの例を記載
- `UserFactoryBuilder().withName().withEmail().build()`の記述
- 実際のDSL（user()関数）について記載なし

**必要な更新:**
- Quick Startセクションを現在のDSL例に更新
- Phase 5（DSL実装）の完成を反映
- 生成コード例をDSL版に更新

#### 2. USAGE.md
**問題:**
- Builder Patternの詳細説明が中心
- PersistableFactory実装例が複雑すぎる
- 実際に生成される`user()`, `post()`関数の説明がない

**必要な更新:**
- DSLアプローチを中心に全面改訂
- シンプルな使用例（user()関数）を最初に
- DB永続化は`dsl.executeInsert()`を使った例に簡略化

#### 3. DSL_DESIGN.md
**問題:**
- 設計検討段階のドキュメント（アーカイブ化すべき）
- 3つのアプローチ比較は実装完了後は不要

**必要な更新:**
- docs/archive/に移動
- README/USAGEからは削除

#### 4. モジュール別ドキュメント不足

**不足しているドキュメント:**
- faktory-ksp/README.md（KSPプロセッサの説明）
- faktory-runtime/README.md（Runtime APIの説明）
- API_REFERENCE.md（詳細なAPI仕様）

## 整備計画

### 優先度1: メインドキュメント更新

#### 1. README.md更新
**目的:** ユーザーが最初に見るドキュメントを現在の実装に合わせる

**更新内容:**
- Quick Startを現在のDSL例に変更
- 生成コード例をDSL版に更新（Phantom Types例は削除）
- Phase 5完成の記載追加
- CI/CDセクションは維持

**成果物:**
- README.md（更新版）

#### 2. USAGE.md更新
**目的:** 利用ガイドを現在のDSL中心に全面改訂

**更新内容:**
- 基本的な使い方をDSLから説明
- user(), post()関数の使用例
- DB永続化を`dsl.executeInsert()`で簡略化
- PersistableFactory実装例を削除（またはAdvanced Usageに移動）
- トラブルシューティングを現在の実装に合わせて更新

**成果物:**
- USAGE.md（更新版）

### 優先度2: モジュール別ドキュメント

#### 3. faktory-ksp/README.md作成
**目的:** KSPプロセッサの仕組みと拡張方法を説明

**内容:**
- KSPプロセッサの概要
- コード生成の仕組み（JooqMetadataExtractor, DslCodeGenerator）
- @Factoryアノテーションの使い方
- カスタムコード生成の拡張方法

**成果物:**
- faktory-ksp/README.md（新規）

#### 4. faktory-runtime/README.md作成
**目的:** Runtimeライブラリの使い方を説明

**内容:**
- Factory, PersistableFactoryの概要
- FactoryDsl annotationの役割
- build() vs create()の違い
- カスタムFactoryの実装方法

**成果物:**
- faktory-runtime/README.md（新規）

### 優先度3: API仕様ドキュメント

#### 5. API_REFERENCE.md作成
**目的:** 詳細なAPI仕様を提供

**内容:**
- @Factory annotation
- FactoryDsl annotation
- DslBuilder classes（生成されるクラス）
- Factory functions（user(), post()など）
- Factory<T, B> class
- PersistableFactory<R, T, B> class

**成果物:**
- API_REFERENCE.md（新規）

### 優先度4: アーカイブと整理

#### 6. DSL_DESIGN.mdのアーカイブ化
**目的:** 設計検討ドキュメントをアーカイブ

**内容:**
- docs/archive/DSL_DESIGN.mdに移動
- README/USAGEからの参照削除

**成果物:**
- docs/archive/DSL_DESIGN.md（移動）

## 実施順序

1. **DOCUMENTATION_PLAN.md作成**（このファイル）
2. **README.md更新** - DSL例に変更
3. **USAGE.md更新** - DSL中心に全面改訂
4. **faktory-ksp/README.md作成**
5. **faktory-runtime/README.md作成**
6. **API_REFERENCE.md作成**
7. **DSL_DESIGN.mdアーカイブ化**

## 検証項目

各ドキュメント更新後、以下を確認：

1. **コード例の動作確認**
   - ドキュメント内のコード例が実際に動作するか
   - faktory-examplesのテストと一致しているか

2. **実装との整合性**
   - 生成されるコードと説明が一致しているか
   - API仕様が実装と一致しているか

3. **ユーザー視点の確認**
   - 初めてのユーザーが理解できるか
   - セットアップから使用までの流れが明確か

## 成果物一覧

- [ ] README.md（更新）
- [ ] USAGE.md（更新）
- [ ] faktory-ksp/README.md（新規）
- [ ] faktory-runtime/README.md（新規）
- [ ] API_REFERENCE.md（新規）
- [ ] docs/archive/DSL_DESIGN.md（移動）

## 完成基準

- 全ドキュメントが現在の実装と整合している
- DSL中心のドキュメント構成
- 初めてのユーザーがセットアップから使用まで迷わない
- モジュール別の詳細ドキュメントが揃っている
- API仕様が明確に文書化されている
