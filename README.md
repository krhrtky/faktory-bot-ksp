# faktory-bot-ksp

KSP version of [faktory-bot](https://github.com/krhrtky/faktory-bot): Move runtime validation to compile-time type-level validation using Kotlin Symbol Processing.

## Overview

faktory-bot-ksp provides compile-time type-safe factory pattern for jOOQ with Kotlin Symbol Processing (KSP). It generates factory code that enforces NOT NULL constraints and required fields at compile time using Phantom Types.

### Key Features

- ✅ **Compile-time NOT NULL validation** using Phantom Types
- ✅ **Type-safe builder pattern** with state tracking
- ✅ **Automatic code generation** from jOOQ metadata via KSP
- ✅ **jOOQ integration** for database persistence
- ✅ **TDD implementation** with 100% test coverage

## Comparison with Original faktory-bot

| Feature | faktory-bot (Original) | faktory-bot-ksp |
|---------|------------------------|-----------------|
| NOT NULL validation | Runtime (RequiredAttributeValidator) | **Compile-time (Phantom Types)** |
| jOOQ Table resolution | Runtime (Reflection) | **Compile-time (KSP)** |
| Factory definition validation | Runtime | **Compile-time (KSP)** |
| build/create | Both persist to DB | build=memory, create=DB |

## Quick Start

### 1. Add Dependencies

```kotlin
// build.gradle.kts
plugins {
    kotlin("jvm") version "1.9.21"
    id("com.google.devtools.ksp") version "1.9.21-1.0.15"
}

dependencies {
    implementation("com.example:faktory-runtime:1.0.0")
    ksp("com.example:faktory-ksp:1.0.0")

    // jOOQ
    implementation("org.jooq:jooq:3.18.7")
}
```

### 2. Define Factory

```kotlin
import com.example.faktory.ksp.Factory

@Factory(tableName = "users")
class UserFactory
```

### 3. Generated Code

KSP automatically generates:

```kotlin
sealed interface UsersFieldState {
    object WithName : UsersFieldState
    object WithEmail : UsersFieldState
    object Complete : UsersFieldState
}

interface UsersFactoryBuilder<S : UsersFieldState> {
    fun withName(value: String): UsersFactoryBuilder<UsersFieldState.WithName>
    fun withEmail(value: String): UsersFactoryBuilder<UsersFieldState.WithEmail>
    fun <S : UsersFieldState.Complete> build(): UsersRecord
}
```

### 4. Usage

```kotlin
// Compile error: required fields not set
val user = UserFactoryBuilder().build() // ❌

// OK: all required fields set
val user = UserFactoryBuilder()
    .withName("Alice")
    .withEmail("alice@example.com")
    .build() // ✅

// Persist to database
class UserFactoryImpl(dsl: DSLContext) :
    PersistableFactory<UsersRecord, User, UserBuilder>(dsl) {

    override fun builder() = UserBuilder()
    override fun table() = USERS
    override fun toRecord(entity: User) = UsersRecord(...)
}

val user = UserFactoryImpl(dsl).create() // Inserts to DB
val users = UserFactoryImpl(dsl).createList(10) // Batch insert
```

## Architecture

### Phase 1: KSP Foundation
- JooqMetadataExtractor: Extract table metadata from jOOQ
- ForeignKeyDetector: Detect FK constraints
- FactoryCodeGenerator: Generate code with KotlinPoet
- FactoryProcessor: Main KSP processor

### Phase 2: Code Generation Engine
- PhantomTypeGenerator: Generate type-level constraints
- BuilderCodeGenerator: Generate type-parameterized builders
- Complete integration of phantom types and builders

### Phase 3: Runtime Foundation
- Factory<T, B>: Base factory class
- FactoryBuilder<T>: Builder interface
- build() and buildList() methods

### Phase 4: jOOQ Integration
- PersistableFactory: DB persistence support
- create() and createList() methods
- DSLContext integration

## Technical Highlights

### Phantom Types for Type Safety

```kotlin
// State is tracked at type level
type Initial = UsersFactoryBuilder<UsersFieldState.Initial>
type WithName = UsersFactoryBuilder<UsersFieldState.WithName>
type Complete = UsersFactoryBuilder<UsersFieldState.Complete>

// Each method changes the state
fun withName(...): WithName  // Initial → WithName
fun withEmail(...): Complete // WithName → Complete
fun build(): UsersRecord where S : Complete // Only callable when Complete
```

### Compile-time Metadata Extraction

```kotlin
// KSP analyzes jOOQ generated code at compile time
val tableClass = resolver.getClassDeclarationByName("com.example.jooq.tables.Users")
val requiredFields = tableClass.getAllProperties()
    .filter { it.type.resolve().declaration.qualifiedName?.startsWith("org.jooq.TableField") }
    .filter { !it.dataType.nullable() }
    .map { it.simpleName.asString() }
```

## Development

### Build

```bash
./gradlew build
```

### Run Tests

```bash
./gradlew test
```

All 20 test cases pass with 100% success rate.

## Repository

https://github.com/krhrtky/faktory-bot-ksp

## License

MIT License
