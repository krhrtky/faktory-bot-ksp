# faktory-ksp

KSP (Kotlin Symbol Processing) implementation for faktory-bot-ksp code generation.

## 概要

faktory-kspモジュールは、`@Factory`アノテーションを持つクラスを検出し、jOOQメタデータを解析して、型安全なDSLコードを自動生成するKSPプロセッサです。

## 主要コンポーネント

### FactoryProcessor

KSP entry pointとなるSymbolProcessor実装。

```kotlin
class FactoryProcessor(
    private val environment: SymbolProcessorEnvironment,
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation(Factory::class.qualifiedName!!)
        symbols.filterIsInstance<KSClassDeclaration>().forEach { classDeclaration ->
            processFactory(classDeclaration, resolver)
        }
        return emptyList()
    }
}
```

**責務:**
- `@Factory`アノテーションを持つクラスを検出
- `tableName`パラメータを抽出
- KspJooqMetadataExtractorを使用してメタデータ取得
- DslCodeGeneratorを使用してコード生成
- CodeGeneratorで.ktファイル出力

### KspJooqMetadataExtractor

jOOQ生成コードを解析し、テーブルメタデータを抽出。

```kotlin
object KspJooqMetadataExtractor {
    fun extract(tableName: String, resolver: Resolver): TableMetadata {
        val tableClass = findTableClass(tableName, resolver)
        val requiredFields = extractRequiredFields(tableClass)
        val optionalFields = extractOptionalFields(tableClass)

        return TableMetadata(
            tableName = tableName,
            requiredFields = requiredFields,
            optionalFields = optionalFields
        )
    }
}
```

**抽出する情報:**
- テーブル名
- 必須フィールド（NOT NULL制約）
- オプショナルフィールド（NULL許可）
- フィールド型情報（String, Int, LocalDateTime等）

**動作原理:**
1. `resolver.getClassDeclarationByName("com.example.jooq.tables.Users")`でjOOQ生成クラスを取得
2. `TableField<R, V>`型のプロパティを検出
3. DataTypeからnullable情報を抽出
4. idフィールドを除外（自動採番想定）

### DslCodeGenerator

KotlinPoetを使用して、型安全なDSLコードを生成。

```kotlin
object DslCodeGenerator {
    fun generate(
        recordClassName: String,
        metadata: TableMetadata,
    ): String {
        val builderClassName = "${baseName}DslBuilder"
        val factoryFunctionName = metadata.tableName.removeSuffix("s")

        // 1. DslBuilderクラス生成
        val builder = generateDslBuilder(builderClassName, recordClassName, metadata)

        // 2. トップレベルDSL関数生成
        val function = generateFactoryFunction(factoryFunctionName, builderClassName, recordClassName, metadata)

        return FileSpec.builder("", "Generated")
            .addType(builder)
            .addFunction(function)
            .build()
            .toString()
    }
}
```

**生成するコード:**

1. **DslBuilderクラス**
   ```kotlin
   @FactoryDsl
   class UsersDslBuilder(
       var name: String,      // 必須フィールド → コンストラクタパラメータ
       var email: String,
   ) {
       var age: Int? = null   // オプショナル → varプロパティ

       internal fun build(): UsersRecord = UsersRecord().apply {
           this.name = this@UsersDslBuilder.name
           this.email = this@UsersDslBuilder.email
           this.age = this@UsersDslBuilder.age
       }
   }
   ```

2. **トップレベルDSL関数**
   ```kotlin
   fun user(
       name: String,
       email: String,
       block: UsersDslBuilder.() -> Unit = {},
   ): UsersRecord = UsersDslBuilder(name, email).apply(block).build()
   ```

**命名規則:**
- snake_case（DB） → camelCase（Kotlin）
- テーブル名（users） → 単数形の関数名（user）

### JooqMetadataExtractor

実行時にjOOQ Tableインスタンスからメタデータを抽出（テスト用）。

```kotlin
object JooqMetadataExtractor {
    fun extract(table: Table<*>): TableMetadata {
        val requiredFields = table.fields()
            .filter { !it.dataType.nullable() }
            .filter { it.name != "id" }  // id除外
            .map { it.name }

        val optionalFields = table.fields()
            .filter { it.dataType.nullable() }
            .map { it.name }

        return TableMetadata(
            tableName = table.name,
            requiredFields = requiredFields,
            optionalFields = optionalFields
        )
    }
}
```

**用途:**
- 単体テストでの検証
- KSP非依存の実行時メタデータ抽出

### ForeignKeyDetector

jOOQ TableからForeignKey制約を検出。

```kotlin
object ForeignKeyDetector {
    fun detect(table: Table<*>): List<ForeignKeyInfo> {
        return table.references.map { fk ->
            ForeignKeyInfo(
                fieldName = fk.fields[0].name,
                referencedTable = fk.key.table.name,
                referencedField = fk.key.fields[0].name
            )
        }
    }
}
```

**検出情報:**
- 外部キーフィールド名（user_id）
- 参照先テーブル名（users）
- 参照先フィールド名（id）

**将来の拡張:**
- 関連エンティティの自動生成
- Cascading delete対応

## 使用例

### 1. Factoryクラス定義

```kotlin
import com.example.faktory.ksp.Factory

@Factory(tableName = "users")
class UserFactory

@Factory(tableName = "posts")
class PostFactory
```

### 2. KSP実行

```bash
./gradlew kspKotlin
```

### 3. 生成されるコード

`build/generated/ksp/main/kotlin/`配下に以下が生成されます：

- `UsersDslBuilder.kt` - UserテーブルのDSL
- `PostsDslBuilder.kt` - PostテーブルのDSL

### 4. 生成コードの使用

```kotlin
// UserDsl（生成済み）
val user = user(name = "Alice", email = "alice@example.com") {
    age = 30
}

// PostDsl（生成済み）
val post = post(userId = 1, title = "Title", content = "Content") {
    published = true
}
```

## ビルド設定

### build.gradle.kts

```kotlin
plugins {
    kotlin("jvm") version "1.9.21"
    id("com.google.devtools.ksp") version "1.9.21-1.0.15"
}

dependencies {
    // KSP processor
    ksp("com.example.faktory:faktory-ksp:1.0.0")

    // Runtime（生成コードが依存）
    implementation("com.example.faktory:faktory-runtime:1.0.0")

    // jOOQ（メタデータ解析に必要）
    implementation("org.jooq:jooq:3.18.7")
}
```

### KSP設定

```kotlin
ksp {
    arg("faktory.verbose", "true")  // デバッグログ有効化（オプション）
}
```

## テスト

### 単体テスト

```bash
./gradlew :faktory-ksp:test
```

**テストカバレッジ:**
- JooqMetadataExtractorTest: メタデータ抽出の検証
- ForeignKeyDetectorTest: 外部キー検出の検証
- DslCodeGeneratorTest: コード生成の検証
- FactoryProcessorTest: KSP統合の検証

### KSP統合テスト

```kotlin
@KspTest
class FactoryProcessorIntegrationTest {
    @Test
    fun `generate DSL code from Factory annotation`() {
        val compilation = KotlinCompilation().apply {
            sources = listOf(userFactorySource)
            symbolProcessorProviders = listOf(FactoryProcessorProvider())
        }

        val result = compilation.compile()

        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        assertThat(result.generatedFiles)
            .anyMatch { it.name.contains("UsersDslBuilder") }
    }
}
```

## トラブルシューティング

### KSPが実行されない

**原因:** `ksp(...)`依存関係が設定されていない

**解決策:**
```kotlin
dependencies {
    ksp("com.example.faktory:faktory-ksp:1.0.0")  // これが必要
}
```

### 生成コードが見つからない

**原因:** KSP出力ディレクトリがsourceSetに含まれていない

**解決策:**
```kotlin
kotlin {
    sourceSets.main {
        kotlin.srcDir("build/generated/ksp/main/kotlin")
    }
}
```

### jOOQ Tableクラスが見つからない

**原因:** jOOQコード生成が先に実行されていない

**解決策:**
```bash
# 正しい実行順序
./gradlew generateJooq  # 1. jOOQコード生成
./gradlew kspKotlin     # 2. KSPコード生成
./gradlew build         # 3. ビルド
```

### メタデータ抽出エラー

**原因:** jOOQ生成コードのパッケージ構造が想定と異なる

**解決策:**
jOOQのpackageName設定を確認：
```kotlin
jooq {
    configurations {
        create("main") {
            jooqConfiguration.apply {
                generator.apply {
                    target.apply {
                        packageName = "com.example.jooq"  // KSPがこれを参照
                    }
                }
            }
        }
    }
}
```

## アーキテクチャ

```
faktory-ksp/
├── src/main/kotlin/
│   └── com/example/faktory/ksp/
│       ├── Factory.kt                    # @Factory annotation
│       ├── FactoryProcessor.kt           # KSP entry point
│       ├── FactoryProcessorProvider.kt   # KSP provider
│       ├── metadata/
│       │   ├── JooqMetadataExtractor.kt        # 実行時メタデータ抽出
│       │   ├── KspJooqMetadataExtractor.kt     # コンパイル時メタデータ抽出
│       │   ├── ForeignKeyDetector.kt           # FK制約検出
│       │   └── TableMetadata.kt                # メタデータモデル
│       └── codegen/
│           ├── DslCodeGenerator.kt             # DSLコード生成
│           ├── FactoryCodeGenerator.kt         # Factory統合コード生成
│           ├── PhantomTypeGenerator.kt         # 型レベル制約生成（未使用）
│           └── BuilderCodeGenerator.kt         # Builder生成（未使用）
└── src/test/kotlin/
    └── com/example/faktory/ksp/
        ├── metadata/
        │   ├── JooqMetadataExtractorTest.kt
        │   └── ForeignKeyDetectorTest.kt
        ├── codegen/
        │   ├── DslCodeGeneratorTest.kt
        │   └── FactoryCodeGeneratorTest.kt
        └── FactoryProcessorTest.kt
```

## 依存関係

```kotlin
dependencies {
    // KSP API
    implementation("com.google.devtools.ksp:symbol-processing-api:1.9.21-1.0.15")

    // コード生成
    implementation("com.squareup:kotlinpoet:1.15.3")

    // jOOQメタデータ解析
    implementation("org.jooq:jooq:3.18.7")

    // テスト
    testImplementation("com.github.tschuchortdev:kotlin-compile-testing-ksp:1.5.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testImplementation("org.assertj:assertj-core:3.24.2")
}
```

## ライセンス

MIT License
