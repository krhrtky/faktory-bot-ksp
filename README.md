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

// 2. Define factory
@Factory(tableName = "users")
class UserFactory

// 3. KSP generates type-safe DSL
fun user(
    name: String,      // NOT NULL → required parameter
    email: String,     // NOT NULL → required parameter
    block: UsersDslBuilder.() -> Unit = {}
): UsersRecord

// 4. Use the generated DSL
val user = user(name = "Alice", email = "alice@example.com") {
    age = 30  // Optional field
}

// Compile error: missing required parameter
val user = user(name = "Alice")  // ❌ email is required

// Database persistence
val dsl = DSL.using(dataSource, SQLDialect.POSTGRES)
dsl.executeInsert(user)
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
