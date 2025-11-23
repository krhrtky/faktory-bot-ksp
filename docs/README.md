# faktory-bot-ksp Documentation

Welcome to the faktory-bot-ksp documentation. This guide will help you get started with type-safe factory patterns for jOOQ using Kotlin Symbol Processing.

## Quick Navigation

### For New Users

1. **[Getting Started](./guides/getting-started.md)** - Installation and first steps
2. **[Usage Guide](./guides/usage-guide.md)** - Detailed usage patterns and examples
3. **[Troubleshooting](./guides/troubleshooting.md)** - Common issues and solutions

### For Developers

- **[API Reference](./api/api-reference.md)** - Complete API documentation
- **[faktory-ksp Module](./modules/faktory-ksp.md)** - KSP processor internals
- **[faktory-runtime Module](./modules/faktory-runtime.md)** - Runtime library details

### Design & Development

- **[DSL Design](./design/dsl-design.md)** - DSL architecture decisions
- **[Documentation Plan](./development/documentation-plan.md)** - Documentation strategy
- **[Documentation Completed](./development/documentation-completed.md)** - Completion report

---

## Documentation Structure

```
docs/
├── README.md                           # This file - Documentation index
├── guides/                             # User guides
│   ├── getting-started.md              # Quick start guide
│   ├── usage-guide.md                  # Detailed usage guide
│   └── troubleshooting.md              # Common issues and solutions
├── api/                                # API documentation
│   └── api-reference.md                # Complete API reference
├── modules/                            # Module documentation
│   ├── faktory-ksp.md                  # KSP processor module
│   └── faktory-runtime.md              # Runtime library module
├── design/                             # Design documents
│   └── dsl-design.md                   # DSL design decisions
└── development/                        # Development documentation
    ├── documentation-plan.md           # Documentation strategy
    └── documentation-completed.md      # Completion report
```

---

## What is faktory-bot-ksp?

faktory-bot-ksp is a Kotlin library that provides **compile-time type-safe factory patterns** for generating test data with jOOQ.

### Key Features

✅ **Compile-time NOT NULL validation** using Kotlin DSL
✅ **Type-safe builder pattern** with constructor parameters
✅ **Automatic code generation** from jOOQ metadata via KSP
✅ **jOOQ integration** for database persistence
✅ **@DslMarker** for scope control

### Quick Example

```kotlin
// Schema: users table with NOT NULL constraints
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    age INT
);

// Define Factory
@Factory(tableName = "users")
class UserFactory

// Generated DSL (automatic)
fun user(
    name: String,      // NOT NULL → required parameter
    email: String,     // NOT NULL → required parameter
    block: UsersDslBuilder.() -> Unit = {}
): UsersRecord

// Usage
val user = user(name = "Alice", email = "alice@example.com") {
    age = 30  // Optional field
}

// Compile error: missing required parameter
val user = user(name = "Alice")  // ❌ email is required
```

---

## Getting Started

### Prerequisites

- Kotlin 1.9.21+
- Gradle 8.0+
- jOOQ 3.18.7+

### Installation

```kotlin
// build.gradle.kts
plugins {
    kotlin("jvm") version "1.9.21"
    id("com.google.devtools.ksp") version "1.9.21-1.0.15"
}

dependencies {
    implementation("com.example:faktory-runtime:1.0.0")
    ksp("com.example:faktory-ksp:1.0.0")
    implementation("org.jooq:jooq:3.18.7")
}
```

### First Steps

1. **Read [Getting Started](./guides/getting-started.md)** for installation and setup
2. **Follow [Usage Guide](./guides/usage-guide.md)** for detailed examples
3. **Refer to [Troubleshooting](./guides/troubleshooting.md)** if you encounter issues

---

## Documentation by Topic

### Guides

#### [Getting Started](./guides/getting-started.md)
- Installation and setup
- Quick start tutorial
- First factory definition
- Basic usage examples

#### [Usage Guide](./guides/usage-guide.md)
- DSL understanding
- Database persistence patterns
- Foreign key relationships
- Testing strategies
- Advanced usage patterns

#### [Troubleshooting](./guides/troubleshooting.md)
- KSP code generation issues
- Compile errors
- jOOQ integration problems
- Runtime errors
- Testing issues

### API Documentation

#### [API Reference](./api/api-reference.md)
- Annotations (@Factory, @FactoryDsl)
- Generated DSL (DslBuilder, Factory Functions)
- Runtime Classes (Factory, PersistableFactory, FactoryBuilder)
- KSP Processor
- Metadata Extractors
- Code Generators
- Type mappings
- Error handling

### Module Documentation

#### [faktory-ksp Module](./modules/faktory-ksp.md)
- KSP processor architecture
- Code generation internals
- Metadata extraction
- Foreign key detection
- Extension points

#### [faktory-runtime Module](./modules/faktory-runtime.md)
- @FactoryDsl annotation
- Factory base classes
- PersistableFactory for DB persistence
- FactoryBuilder interface
- Usage patterns

### Design Documentation

#### [DSL Design](./design/dsl-design.md)
- DSL architecture decisions
- Constructor parameter DSL pattern
- Comparison with alternative approaches
- Type safety guarantees

### Development Documentation

#### [Documentation Plan](./development/documentation-plan.md)
- Documentation strategy
- Organization structure
- Maintenance guidelines

#### [Documentation Completed](./development/documentation-completed.md)
- Completion report
- Documentation coverage
- Quality assurance

---

## Learning Path

### Beginner Path

1. **[Getting Started](./guides/getting-started.md)** - Learn the basics
2. **[Usage Guide](./guides/usage-guide.md)** - Understand DSL patterns
3. **[Troubleshooting](./guides/troubleshooting.md)** - Solve common issues

### Advanced Path

1. **[faktory-ksp Module](./modules/faktory-ksp.md)** - Understand code generation
2. **[faktory-runtime Module](./modules/faktory-runtime.md)** - Master runtime API
3. **[API Reference](./api/api-reference.md)** - Deep dive into APIs

### Contributor Path

1. **[DSL Design](./design/dsl-design.md)** - Understand design decisions
2. **[faktory-ksp Module](./modules/faktory-ksp.md)** - Learn KSP internals
3. **[Documentation Plan](./development/documentation-plan.md)** - Contribute to docs

---

## Examples

### Basic Usage

```kotlin
// In-memory creation
val user = user(name = "Alice", email = "alice@example.com")

// With optional fields
val user = user(name = "Bob", email = "bob@example.com") {
    age = 30
}

// Database persistence
val dsl = DSL.using(dataSource, SQLDialect.POSTGRES)
dsl.executeInsert(user)
```

### Foreign Key Relationships

```kotlin
// Create user
val userRecord = user(name = "Alice", email = "alice@example.com")
dsl.executeInsert(userRecord)

val userId = dsl.selectFrom(USERS)
    .where(USERS.EMAIL.eq("alice@example.com"))
    .fetchOne()!!.id!!

// Create post with foreign key
val postRecord = post(
    userId = userId,
    title = "First Post",
    content = "Hello, World!"
)
dsl.executeInsert(postRecord)
```

### Testing

```kotlin
@Test
fun `create test data`() {
    val users = (1..5).map { i ->
        user(name = "User $i", email = "user$i@example.com")
    }
    users.forEach { dsl.executeInsert(it) }

    val count = dsl.selectCount().from(USERS).fetchOne(0, Int::class.java)
    assertThat(count).isEqualTo(5)
}
```

---

## Contributing to Documentation

### Guidelines

1. Keep examples simple and focused
2. Use real-world scenarios
3. Provide both code and explanations
4. Include troubleshooting tips
5. Update API reference when adding features

### Documentation Updates

When adding new features, update:
- [ ] Getting Started (if it affects setup)
- [ ] Usage Guide (if it affects usage)
- [ ] API Reference (if it adds new APIs)
- [ ] Module docs (if it changes internals)
- [ ] Troubleshooting (if it introduces new issues)

---

## Support

- **GitHub Issues:** [faktory-bot-ksp/issues](https://github.com/krhrtky/faktory-bot-ksp/issues)
- **API Reference:** [api-reference.md](./api/api-reference.md)
- **Troubleshooting:** [troubleshooting.md](./guides/troubleshooting.md)

---

## License

MIT License - See [LICENSE](../LICENSE) for details
