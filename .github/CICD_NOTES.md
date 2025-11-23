# CI/CD Configuration Notes

## GitHub Actions Workflows

### CI Workflow (`.github/workflows/ci.yml`)

Runs on every push and pull request to `main`, `develop`, or `claude/**` branches.

**Jobs:**
1. **test** - Run all tests including Testcontainers integration tests
2. **lint** - Run ktlint and detekt for code quality
3. **build** - Build all modules and create artifacts

**Key Configurations:**
- JDK 17 (Temurin distribution)
- Gradle cache enabled
- Test results and reports uploaded as artifacts

### Publish Workflow (`.github/workflows/publish.yml`)

Runs on:
- Tag push (`v*`)
- Branch push to `claude/setup-cicd-pipeline-*`
- Manual trigger (workflow_dispatch)

**Jobs:**
1. **publish** - Run tests, build, publish to GitHub Packages, create release

**Key Configurations:**
- Permissions: `contents: write`, `packages: write`
- Version extraction from git tags
- Automatic GitHub release creation with artifacts

## Testcontainers in CI

The `faktory-examples` module uses Testcontainers for integration tests with PostgreSQL.

**Local Development (Colima):**
```kotlin
// faktory-examples/build.gradle.kts
tasks.test {
    if (System.getenv("CI") == null) {
        val dockerSocket = "/Users/takuya.kurihara/.colima/default/docker.sock"
        if (file(dockerSocket).exists()) {
            environment("DOCKER_HOST", "unix://$dockerSocket")
            environment("TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE", dockerSocket)
            environment("TESTCONTAINERS_RYUK_DISABLED", "true")
        }
    }
}
```

**GitHub Actions:**
- Uses default Docker daemon (pre-installed on Ubuntu runners)
- No special configuration needed
- CI environment variable is automatically set

## Troubleshooting

### Testcontainers Fails in CI

If Testcontainers tests fail in GitHub Actions:

1. Check Docker daemon is running:
   ```yaml
   - name: Check Docker
     run: docker info
   ```

2. Add explicit Testcontainers configuration:
   ```kotlin
   tasks.test {
       systemProperty("testcontainers.reuse.enable", "false")
   }
   ```

3. Skip integration tests in CI:
   ```yaml
   - name: Run tests
     env:
       SKIP_INTEGRATION_TESTS: "true"
     run: ./gradlew test
   ```

### jOOQ Code Generation Issues

The `faktory-examples` module uses DDL-based jOOQ code generation:

```kotlin
database {
    name = "org.jooq.meta.extensions.ddl.DDLDatabase"
    properties.add(
        Property().apply {
            key = "scripts"
            value = "src/test/resources/schema.sql"
        }
    )
}
```

This should work in CI without a database connection.

If jOOQ generation fails:
- Check `schema.sql` exists in `src/test/resources/`
- Verify jOOQ plugin version compatibility
- Run `./gradlew generateJooq` locally to debug

### Build Cache Issues

Gradle cache is enabled in GitHub Actions:
```yaml
- uses: actions/setup-java@v4
  with:
    cache: 'gradle'
```

If cache causes issues:
- Clear cache manually in GitHub UI
- Add `--no-build-cache` to Gradle commands

## Publishing to GitHub Packages

**Prerequisites:**
- `GITHUB_TOKEN` secret (automatically provided)
- Repository permissions for packages

**Artifact Names:**
- `com.example.faktory:faktory-ksp:VERSION`
- `com.example.faktory:faktory-runtime:VERSION`

**Release Process:**
1. Create and push tag: `git tag v0.1.0 && git push origin v0.1.0`
2. Publish workflow runs automatically
3. Packages published to GitHub Packages
4. GitHub release created with artifacts

## CI Environment Variables

**Automatically set by GitHub Actions:**
- `CI=true` - Indicates CI environment
- `GITHUB_TOKEN` - Authentication token
- `GITHUB_ACTOR` - Actor triggering the workflow
- `GITHUB_REF` - Git ref (branch/tag)

**Custom variables (if needed):**
- `SKIP_INTEGRATION_TESTS` - Skip Testcontainers tests
- `TESTCONTAINERS_RYUK_DISABLED` - Disable Ryuk container (local only)

## Module Dependencies

```
faktory-ksp (no external test dependencies)
    ↓
faktory-runtime (no external test dependencies)
    ↓
faktory-examples (uses Testcontainers for integration tests)
```

**Test Execution Order:**
1. faktory-ksp tests (unit tests only)
2. faktory-runtime tests (unit tests only)
3. faktory-examples tests (includes Testcontainers integration tests)

If only faktory-examples tests fail, the core functionality is still validated.
