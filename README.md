# faktory-bot-ksp

[![CI](https://github.com/krhrtky/faktory-bot-ksp/actions/workflows/ci.yml/badge.svg)](https://github.com/krhrtky/faktory-bot-ksp/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

Compile-time type-safe factory pattern for jOOQ test data generation using Kotlin Symbol Processing (KSP).

## Overview

faktory-bot-ksp is a KSP version of [faktory-bot](https://github.com/krhrtky/faktory-bot) that moves runtime validation to **compile-time type-level validation**. It automatically generates type-safe DSL code from jOOQ metadata, enforcing NOT NULL constraints at compile time.

### Key Features

- ✅ **Compile-time type safety** - Required fields enforced by Kotlin type system
- ✅ **Kotlin DSL** - Natural, idiomatic Kotlin syntax
- ✅ **Automatic code generation** - Zero-config code generation from jOOQ metadata via KSP
- ✅ **jOOQ integration** - Seamless integration with jOOQ for database operations
- ✅ **@DslMarker** - Scope control prevents DSL misuse

### Comparison with Original faktory-bot

| Feature | faktory-bot (Original) | faktory-bot-ksp |
|---------|------------------------|-----------------|
| NOT NULL validation | Runtime (RequiredAttributeValidator) | **Compile-time (Type System)** |
| jOOQ Table resolution | Runtime (Reflection) | **Compile-time (KSP)** |
| Factory definition | Runtime | **Compile-time (KSP)** |
| API Style | Builder Pattern | **Kotlin DSL** |

## Quick Example

```kotlin
// 1. Define schema
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    age INT
);

CREATE TABLE posts (
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    published BOOLEAN,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

// 2. Define factories
@Factory(tableName = "users")
class UserFactory

@Factory(tableName = "posts")
class PostFactory

// 3. KSP generates type-safe DSL
fun user(
    name: String,      // NOT NULL → required parameter
    email: String,     // NOT NULL → required parameter
    block: UsersDslBuilder.() -> Unit = {}
): UsersRecord

fun post(
    user: UsersRecord, // Foreign key → accepts parent record
    title: String,
    content: String,
    block: PostsDslBuilder.() -> Unit = {}
): PostsRecord

// 4. Use in JUnit tests
@Test
fun `create user and post`() {
    val dsl = DSL.using(dataSource, SQLDialect.POSTGRES)

    // Build and persist user
    val userRecord = user(name = "Alice", email = "alice@example.com") {
        age = 30
    }
    dsl.executeInsert(userRecord)
    val savedUser = dsl.selectFrom(USERS).fetchOne()!!

    // Build and persist post with foreign key
    val postRecord = post(user = savedUser, title = "My Post", content = "Content") {
        published = true
    }
    dsl.executeInsert(postRecord)

    assertThat(postRecord.userId).isEqualTo(savedUser.id)
    assertThat(postRecord.published).isTrue()
}

// Compile error: missing required parameter
val user = user(name = "Alice")  // ❌ email is required
```

## Documentation

📚 **[Complete Documentation](./docs/)** - Comprehensive guides and API reference

### Quick Links

- **[Getting Started](./docs/guides/getting-started.md)** - Installation and setup
- **[Usage Guide](./docs/guides/usage-guide.md)** - Detailed usage patterns
- **[API Reference](./docs/api/api-reference.md)** - Complete API documentation
- **[Troubleshooting](./docs/guides/troubleshooting.md)** - Common issues and solutions

### Module Documentation

- **[faktory-ksp](./docs/modules/faktory-ksp.md)** - KSP processor internals
- **[faktory-runtime](./docs/modules/faktory-runtime.md)** - Runtime library details

## How It Works

```
┌─────────────────┐
│  SQL Schema     │
│  (schema.sql)   │
└────────┬────────┘
         │
         v
┌─────────────────┐
│  jOOQ Codegen   │  ./gradlew generateJooq
└────────┬────────┘
         │
         v
┌─────────────────┐
│  jOOQ Classes   │  UsersRecord, USERS table
└────────┬────────┘
         │
         v
┌─────────────────┐
│  @Factory       │  @Factory(tableName = "users")
│  Annotation     │  class UserFactory
└────────┬────────┘
         │
         v
┌─────────────────┐
│  KSP Processor  │  ./gradlew kspKotlin
│  - Extract NOT  │
│    NULL fields  │
│  - Generate DSL │
└────────┬────────┘
         │
         v
┌─────────────────┐
│  Generated DSL  │  fun user(name: String, email: String, ...)
│  - Type-safe    │
│  - Compile-time │
│    validation   │
└─────────────────┘
```

For detailed architecture, see [faktory-ksp module documentation](./docs/modules/faktory-ksp.md).

## Generated Code Example

KSP automatically generates DSL code from your `@Factory` annotations. Here's what gets generated:

### Input: Factory Definition

```kotlin
@Factory(tableName = "users")
class UserFactory

@Factory(tableName = "posts")
class PostFactory
```

### Output: Generated DSL

**UsersDsl.kt:**
```kotlin
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

**PostsDsl.kt:**
```kotlin
@FactoryDsl
class PostsDslBuilder(
    var user: UsersRecord,  // Foreign key accepts parent record
    var title: String,
    var content: String,
) {
    var published: Boolean? = null
    var createdAt: LocalDateTime? = null

    internal fun build(): PostsRecord = PostsRecord().apply {
        this.userId = this@PostsDslBuilder.user.id  // Extract ID from parent
        this.title = this@PostsDslBuilder.title
        this.content = this@PostsDslBuilder.content
        this.published = this@PostsDslBuilder.published
        this.createdAt = this@PostsDslBuilder.createdAt
    }
}

fun post(
    user: UsersRecord,
    title: String,
    content: String,
    block: PostsDslBuilder.() -> Unit = {},
): PostsRecord = PostsDslBuilder(user, title, content).apply(block).build()
```

### Key Features of Generated Code

1. **Type-safe parameters**: Required fields (NOT NULL) become constructor parameters
2. **Optional fields**: Nullable fields become optional properties in DSL block
3. **Foreign key handling**: Accepts parent record, extracts ID automatically
4. **@DslMarker**: Prevents incorrect DSL nesting
5. **snake_case → camelCase**: Converts database column names to Kotlin conventions

## Installation

### From GitHub Packages

Add GitHub Packages repository to your `build.gradle.kts`:

```kotlin
repositories {
    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/krhrtky/faktory-bot-ksp")
        credentials {
            username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
            password = project.findProperty("gpr.token") as String? ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    implementation("com.example.faktory:faktory-runtime:0.1.0")
    ksp("com.example.faktory:faktory-ksp:0.1.0")
}
```

For detailed installation instructions, see [Getting Started](./docs/guides/getting-started.md).

## Development

```bash
# Build
./gradlew build

# Run tests
./gradlew test

# Code quality
./gradlew ktlintCheck detekt
```

### CI/CD

[![CI](https://github.com/krhrtky/faktory-bot-ksp/actions/workflows/ci.yml/badge.svg)](https://github.com/krhrtky/faktory-bot-ksp/actions/workflows/ci.yml)

GitHub Actions runs tests, linting, and builds on every push.

View workflow status: [GitHub Actions](https://github.com/krhrtky/faktory-bot-ksp/actions)

## Contributing

Contributions are welcome! Please:

1. Read [Getting Started](./docs/guides/getting-started.md) to understand the project
2. Check [existing issues](https://github.com/krhrtky/faktory-bot-ksp/issues)
3. Submit pull requests with tests and documentation

## Support

- 📖 **[Documentation](./docs/)** - Complete guides and API reference
- 🐛 **[Issue Tracker](https://github.com/krhrtky/faktory-bot-ksp/issues)** - Report bugs or request features
- 💬 **[Discussions](https://github.com/krhrtky/faktory-bot-ksp/discussions)** - Ask questions and share ideas

## License

MIT License - See [LICENSE](./LICENSE) for details

---

**Repository:** https://github.com/krhrtky/faktory-bot-ksp
