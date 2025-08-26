# 🔍 Code Quality Guide

This document provides comprehensive guidelines for maintaining high code quality in the Thunderbird for Android
project. Following these guidelines ensures that the codebase remains:

- **Maintainable**: Easy to understand, modify, and extend
- **Reliable**: Functions correctly and consistently
- **Efficient**: Uses resources effectively
- **Secure**: Protects user data and privacy
- **Testable**: Is verified through automated tests

## 🛠️ Static Analysis Tools

We use static analysis tools to automatically detect code quality issues and enforce standards.

### Android Lint

Android Lint checks for potential bugs, optimization opportunities, and Android-specific issues:

```bash
# Run lint checks for all modules
./gradlew lint

# Run lint checks for a specific module
./gradlew :module-name:lint
```

Common issues detected:
- Unused resources
- Accessibility issues
- Performance optimizations
- Internationalization problems
- Security vulnerabilities

#### Configuration

The project's lint configuration is in `config/lint/lint.xml`. You can customize lint rules for specific modules by
adding a `lint` block to the module's `build.gradle.kts` file:

```kotlin
android {
    lint {
        abortOnError = false
        warningsAsErrors = false
        
        // Ignore specific issues
        disable += listOf("InvalidPackage", "MissingTranslation")
        // Enable specific issues
        enable += listOf("RtlHardcoded", "RtlCompat", "RtlEnabled")
    }
}
```

### Detekt

[Detekt](https://detekt.dev/) analyzes Kotlin code for code smells, complexity issues, and potential bugs:

```bash
# Run detekt for all modules
./gradlew detekt

# Run detekt for a specific module
./gradlew :module-name:detekt
```

Detekt checks for:
- Code complexity (cyclomatic complexity, long methods, etc.)
- Potential bugs (empty blocks, unreachable code, etc.)
- Code style issues (naming conventions, formatting, etc.)
- Performance issues (inefficient collection operations, etc.)

#### Configuration

The project's Detekt configuration is defined in the `config/detekt/detekt.yml` file. This file specifies which rules
to apply and their severity levels. The detekt plugin is configured in the
`build-plugin/src/main/kotlin/thunderbird.quality.detekt.gradle.kts` file.

### Spotless

[Spotless](https://github.com/diffplug/spotless) ensures consistent code formatting across the codebase:

```bash
# Check if code formatting meets standards
./gradlew spotlessCheck

# Apply automatic formatting fixes
./gradlew spotlessApply
```

Spotless enforces:
- Consistent indentation
- Line endings
- Import ordering
- Whitespace usage

#### Configuration

The project's Spotless plugin is configured in the
`build-plugin/src/main/kotlin/thunderbird.quality.spotless.gradle.kts` file. We use ktlint for Kotlin formatting.
The rules are defined in the `.editorconfig` file and as editorconfig overrides in the Spotless configuration.

```kotlin
configure<SpotlessExtension> {
    kotlin {
        target(
            "src/*/java/*.kt",
            "src/*/kotlin/*.kt",
            "src/*/java/**/*.kt",
            "src/*/kotlin/**/*.kt",
        )

        ktlint(libs.versions.ktlint.get())
            .setEditorConfigPath("${project.rootProject.projectDir}/.editorconfig")
            .editorConfigOverride(
                mapOf(
                    "ktlint_code_style" to "intellij_idea",
                    "ktlint_standard_function-signature" to "disabled",
                ),
            )
    }
}
```

For Markdown we use Flexmark and no further configuration is needed.

## 📝 Code Style Guidelines

### Kotlin Style Guide

The project follows the [Kotlin style guide](https://developer.android.com/kotlin/style-guide) with some project-specific adaptations:

1. **Naming Conventions**:
   - Use `camelCase` for variables, functions, and methods
   - Use `PascalCase` for classes, interfaces, enums and type parameters
   - Use `UPPER_SNAKE_CASE` for constants and enum constants
   - Prefix interface implementations with `Default` or a specific name, e.g.:
   - `DefaultEmailRepository` implements `EmailRepository`
   - `InMemoryCacge` implements `Cache`
2. **Formatting**:
   - Use 4 spaces for indentation
   - Limit line length to 120 characters
   - Use `./gradlew spotlessApply` to enforce formatting automatically
3. **Comments**:
   - Use KDoc comments for public APIs
   - Include a summary, parameter descriptions, and return value description
   - Document exceptions that might be thrown
4. **File Organization**:
   - One class per file (with exceptions for related small classes)
   - Package structure should reflect the module structure
   - Imports should be organized alphabetically

### Kotlin Best Practices

- Prefer `val` (immutable) over `var` when possible
- Use null-safety (`?.`, `?:`, `requireNotNull`)
- Use extension functions to enhance existing classes
- Leverage Kotlin's functional programming features (`map`, `filter`, etc.) for cleaner code
- Use `data classes` for model objects
- Implement 'sealed classes' for representing finite sets of options
- Use `coroutines` for asynchronous operations
- Use `flow` for reactive programming

### Android Best Practices

- Follow the [Android app architecture guidelines](https://developer.android.com/topic/architecture)
- Use Jetpack libraries when appropriate
- Manage lifecycle properly (`ViewModel`, `LifecycleOwner`)
- Handle configuration changes (rotation, locale, dark mode)
- Optimize for different screen sizes and orientations
- Follow Material 3 design guidelines

## 🔒 Security Practices

Security is critical. Always:

- Validate all input
- Avoid logging sensitive data
- Use HTTPS/TLS for all network traffic
- Store secrets securely (e.g., Android Keystore, EncryptedSharedPreferences)
- Apply least privilege to permissions
- Follow Android’s [security best practices](https://developer.android.com/topic/security/best-practices)

## 🧪 Testing

Comprehensive testing is a critical aspect of code quality. The project uses a multi-layered testing approach:

- **Unit Tests**: Test individual components in isolation
- **Integration Tests**: Test interactions between components
- **UI Tests**: Test the UI behavior and user flows

See [Testing Guide](testing-guide.md) for more details.

### Testing Best Practices

- Write tests for all new code
- Follow the **Arrange-Act-Assert** pattern in tests
- Use descriptive test names that clearly indicate what is being tested
- Prefer fake implementations over mocks for better test reliability
- Keep tests independent (no global state)
- Cover edge cases and error paths
- Maintain a high test coverage

## 🔄 Continuous Integration

The project uses GitHub Actions for continuous integration. Each pull request triggers automated checks for:

- Build success
- Test execution
- Lint issues
- Detekt issues
- Spotless formatting

The CI configuration is defined in the `.github/workflows` directory. The main workflow file is `android.yml`, which
defines the CI pipeline for Android builds.
