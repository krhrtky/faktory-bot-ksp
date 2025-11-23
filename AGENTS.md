# Repository Guidelines

## Project Structure & Module Organization
The repository is a multi-module Gradle build. `faktory-runtime` holds the runtime factory APIs, `faktory-ksp` contains the KSP processor and generators, and `faktory-examples` keeps sample factories plus fixtures for integration-style tests. Shared docs (`README.md`, `USAGE.md`, `DSL_DESIGN.md`) live at the root, along with Gradle wrappers and static analysis configs such as `detekt.yml`. Source code follows the conventional `src/main/kotlin` and `src/test/kotlin` layout inside each module; generated jOOQ artifacts are ignored via the lint configs but should stay under each module’s `build/` directory.

## Build, Test, and Development Commands
- `./gradlew build`: compiles every module, runs unit tests, and assembles artifacts; CI runs this command, so keep it green.
- `./gradlew :faktory-ksp:test`: executes the processor-focused suite (KSP compile-testing, metadata extraction, DSL generators).
- `./gradlew ktlintCheck detekt`: enforces Kotlin formatting and static analysis across modules, using the shared configuration.
- `./gradlew :faktory-examples:test`: verifies the example factories against regression fixtures before shipping behavioral changes.

## Coding Style & Naming Conventions
Code is Kotlin/JVM 1.9 targeting JDK 17; rely on ktlint defaults (4-space indentation, trailing commas where helpful, camelCase members, PascalCase types). Generated types should mirror the jOOQ table names (`UsersFactoryBuilder`, `PostsFieldState`). Keep processor packages in `com.example.faktory.ksp.*` and runtime types in `com.example.faktory.runtime.*`. Run ktlint and detekt locally before opening a PR, especially when touching files excluded from automatic formatting (e.g., generated-jooq stubs).

## Testing Guidelines
JUnit 5 with AssertJ and MockK power all suites. Name files `*Test.kt` and test functions with descriptive sentences (`fun shouldGenerateBuilderForNullableColumns()`). KSP behavior is covered via `kotlin-compile-testing-ksp`; when adding new processor branches, supply fixture inputs under `faktory-examples` and assert on generated sources. Maintain the repository’s 100% coverage trajectory by pairing new features with regression tests (builder states, phantom types, DSL integration). Run `./gradlew test` before pushing.

## Commit & Pull Request Guidelines
Commits follow a Conventional Commit style (`feat: Add DSL integration tests`, `docs: Update usage guide`). Keep each commit focused on a single concern and include updated tests or docs when the behavior changes. Pull requests must describe the feature or fix, reference related issues, show relevant `./gradlew` output when touching build/test logic, and provide screenshots or log excerpts if the change affects generated code. Ensure linters pass and mention any follow-up work so reviewers can evaluate scope clearly.
