# Tasks: initial-ksp-implementation

## Phase 1: KSP基盤構築（TDD）

### 完了
- [x] 元のfaktory-botリポジトリ調査
- [x] KSP版設計方針策定
- [x] CLAUDE.md作成
- [x] コンテキスト管理設定

### 進行中
- [ ] プロジェクト構造初期化
  - [ ] Gradleマルチプロジェクト設定
  - [ ] faktory-kspモジュール作成
  - [ ] faktory-runtimeモジュール作成
  - [ ] faktory-examplesモジュール作成

### 待機中
- [ ] JooqMetadataExtractor実装（TDD）
- [ ] RequiredFieldDetector実装（TDD）
- [ ] ForeignKeyDetector実装（TDD）
- [ ] FactoryProcessor実装（TDD）

## Phase 2: コード生成エンジン（TDD）
- [ ] FactoryCodeGenerator実装
- [ ] PhantomTypeGenerator実装
- [ ] BuilderCodeGenerator実装

## Phase 3: DSL統合（TDD）
- [ ] FactoryDslAdapter実装
- [ ] TraitSystemAdapter実装
- [ ] CallbackSystemAdapter実装

## Phase 4: トランザクション・永続化（TDD）
- [ ] JooqIntegration実装
- [ ] TransactionManager実装
- [ ] BatchOperations実装
