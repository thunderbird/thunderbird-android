# 🧪 Testing Guide

This document outlines the testing practices and guidelines for the Thunderbird for Android project.

**Key Testing Principles:**
- Follow the Arrange-Act-Assert (AAA) pattern
- Use descriptive test names
- Prefer fake implementations over mocks
- Name the object under test as `testSubject`
- Use [AssertK](https://github.com/willowtreeapps/assertk) for assertions

## 🏗️ Test Structure

### 🔄 Arrange-Act-Assert Pattern

Tests in this project should follow the Arrange-Act-Assert (AAA) pattern:

1. **Arrange**: Set up the test conditions and inputs
2. **Act**: Perform the action being tested
3. **Assert**: Verify the expected outcomes

Example:

```kotlin
@Test
fun `example test using AAA pattern`() {
    // Arrange
    val input = "test input"
    val expectedOutput = "expected result"
    val testSubject = SystemUnderTest()

    // Act
    val result = testSubject.processInput(input)

    // Assert
    assertThat(result).isEqualTo(expectedOutput)
}
```

Use comments to clearly separate these sections in your tests:

```kotlin
// Arrange
// Act
// Assert
```

### 📝 Test Naming

Use descriptive test names that clearly indicate what is being tested. For JVM tests, use backticks:

```kotlin
@Test
fun `method should return expected result when given valid input`() {
    // Test implementation
}
```

Note: Android instrumentation tests do not support backticks in test names. For these tests, use camelCase instead:

```kotlin
@Test
fun methodShouldReturnExpectedResultWhenGivenValidInput() {
    // Test implementation
}
```

## 💻 Test Implementation

### 🎭 Fakes over Mocks

In this project, we prefer using fake implementations over mocks:

- ✅ **Preferred**: Create fake/test implementations of interfaces or classes
- ❌ **Avoid**: Using mocking libraries to create mock objects

Fakes provide better test reliability and are more maintainable in the long run. They also make tests more readable
and less prone to breaking when implementation details change.

Mocks can lead to brittle tests that are tightly coupled to the implementation details, making them harder to maintain.
They also negatively impact test performance, particularly during test initialization. Which can quickly become overwhelming
when an excessive number of tests includes mock implementations.

Example of a fake implementation:

```kotlin
// Interface
interface DataRepository {
    fun getData(): List<String>
}

// Fake implementation for testing
class FakeDataRepository(
    // Allow passing initial data during construction
    initialData: List<String> = emptyList()
) : DataRepository {
    // Mutable property to allow changing data between tests
    var dataToReturn = initialData

    override fun getData(): List<String> {
        return dataToReturn
    }
}

// In test
@Test
fun `processor should transform data correctly`() {
    // Arrange
    val fakeRepo = FakeDataRepository(listOf("item1", "item2"))
    val testSubject = DataProcessor(fakeRepo)

    // Act
    val result = testSubject.process()

    // Assert
    assertThat(result).containsExactly("ITEM1", "ITEM2")
}
```

### 📋 Naming Conventions

When writing tests, use the following naming conventions:

- Name the object under test as `testSubject` (not "sut" or other abbreviations)
- Name fake implementations with a "Fake" prefix (e.g., `FakeDataRepository`)
- Use descriptive variable names that clearly indicate their purpose

### ✅ Assertions

Use [AssertK](https://github.com/willowtreeapps/assertk) for assertions in tests:

```kotlin
@Test
fun `example test`() {
    // Arrange
    val list = listOf("apple", "banana")

    // Act
    val result = list.contains("apple")

    // Assert
    assertThat(result).isTrue()
    assertThat(list).contains("banana")
}
```

Note: You'll need to import the appropriate [AssertK](https://github.com/willowtreeapps/assertk) assertions:
- `assertk.assertThat` for the base assertion function
- Functions from the `assertk.assertions` namespace for specific assertion types (e.g., `import assertk.assertions.isEqualTo`, `import assertk.assertions.contains`, `import assertk.assertions.isTrue`, etc.)

## 🧩 Test Types

This section describes the different types of tests we use in the project. Each type serves a specific purpose in our testing strategy, and together they help ensure the quality and reliability of our codebase.

### 🔬 Unit Tests

> **Unit tests verify that individual components work correctly in isolation.**

**What to Test:**
- Single units of functionality
- Individual methods or functions
- Classes in isolation
- Business logic
- Edge cases and error handling

**Key Characteristics:**
- Independent (no external dependencies)
- No reliance on external resources
- Uses fake implementations for dependencies

**Frameworks:**
- JUnit 4
- [AssertK](https://github.com/willowtreeapps/assertk) for assertions
- [Robolectric](https://robolectric.org/) (for Android framework classes)

**Location:**
- Tests should be in the same module as the code being tested
- Should be in the `src/test` directory or `src/{platformTarget}Test` for Kotlin Multiplatform
- Tests should be in the same package as the code being tested

**Contributor Expectations:**
- ✅ All new code should be covered by unit tests
- ✅ Add tests that reproduce bugs when fixing issues
- ✅ Follow the AAA pattern (Arrange-Act-Assert)
- ✅ Use descriptive test names
- ✅ Prefer fake implementations over mocks

### 🔌 Integration Tests

> **Integration tests verify that components work correctly together.**

**What to Test:**
- Interactions between components
- Communication between layers
- Data flow across multiple units
- Component integration points

**Key Characteristics:**
- Tests multiple components together
- May use real implementations when appropriate
- Focuses on component boundaries

**Frameworks:**
- JUnit 4 (for tests in `src/test`)
- [AssertK](https://github.com/willowtreeapps/assertk) for assertions
- [Robolectric](https://robolectric.org/) (for Android framework classes in `src/test`)
- Espresso (for UI testing in `src/androidTest`)

**Location:**
- Preferably in the `src/test` or `src/commonTest`, `src/{platformTarget}Test` for Kotlin Multiplatform
- Only use `src/androidTest`, when there's a specific need for Android dependencies

**Why prefer `test` over `androidTest`:**
- JUnit tests run faster (on JVM instead of emulator/device)
- Easier to set up and maintain
- Better integration with CI/CD pipelines
- Lower resource requirements
- Faster feedback during development

**When to use androidTest:**
- When testing functionality that depends on Android-specific APIs that are not available with Robolectric
- When tests need to interact with the Android framework directly

**Contributor Expectations:**
- ✅ Add tests for features involving multiple components
- ✅ Focus on critical paths and user flows
- ✅ Be mindful of test execution time
- ✅ Follow the AAA pattern
- ✅ Use descriptive test names

### 📱 UI Tests

> **UI tests verify the application from a user's perspective.**

**What to Test:**
- User interface behavior
- UI component interactions
- Complete user flows
- Screen transitions
- Input handling and validation

**Key Characteristics:**
- Tests from user perspective
- Verifies visual elements and interactions
- Covers end-to-end scenarios

**Frameworks:**
- Espresso for Android UI testing
- Compose UI testing for Jetpack Compose
- JUnit 4 as the test runner

**Location:**
- In the `src/test` directory for Compose UI tests
- In the `src/androidTest` directory for Espresso tests

**Contributor Expectations:**
- ✅ Add tests for new UI components and screens
- ✅ Focus on critical user flows
- ✅ Consider different device configurations
- ✅ Test both positive and negative scenarios
- ✅ Follow the AAA pattern
- ✅ Use descriptive test names

### 📸 Screenshot Tests

**⚠️ Work in Progress ⚠️**

> **Screenshot tests verify the visual appearance of UI components.**

**What to Test:**
- Visual appearance of UI components
- Layout correctness
- Visual regressions
- Theme and style application

**Key Characteristics:**
- Captures visual snapshots
- Compares against reference images (`golden` images)
- Detects unintended visual changes

**Frameworks:**
- JUnit 4 as the test runner
- Compose UI testing
- Screenshot comparison tools (TBD)

**Location:**
- Same module as the code being tested
- In the `src/test` directory

**Contributor Expectations:**
- ✅ Add tests for new Composable UI components
- ✅ Verify correct rendering in different states
- ✅ Update reference screenshots for intentional changes

## 🚫 Test Types We Don't Currently Have

> **This section helps contributors understand our testing strategy and future plans.**

**End-to-End Tests** ✨
- Full system tests verifying complete user journeys
- Tests across multiple screens and features
- Validates entire application workflows

**Performance Tests** ⚡
- Measures startup time, memory usage, responsiveness
- Validates app performance under various conditions
- Identifies performance bottlenecks

**Accessibility Tests** ♿
- Verifies proper content descriptions
- Checks contrast ratios and keyboard navigation
- Ensures app is usable by people with disabilities

**Localization Tests** 🌐
- Verifies correct translation display
- Tests right-to-left language support
- Validates date, time, and number formatting

**Manual Test Scripts** 📝
- Manual testing by QA team for exploratory testing
- Ensures repeatable test execution
- Documents expected behavior for manual tests

## 🏃 Running Tests

> **Quick commands to run tests in the project.**

Run all tests:

```bash
./gradlew test
```

Run tests for a specific module:

```bash
./gradlew :module-name:test
```

Run Android instrumentation tests:

```bash
./gradlew connectedAndroidTest
```

Run tests with coverage:

```bash
./gradlew testDebugUnitTestCoverage
```

## 📊 Code Coverage

> **⚠️ Work in Progress ⚠️**
>
> This section is currently under development and will be updated with specific code coverage rules and guidelines.

Code coverage helps us understand how much of our codebase is being tested. While we don't currently have strict requirements, we aim for high coverage in critical components.

**Current Approach:**
- Focus on critical business logic
- Prioritize user-facing features
- No strict percentage requirements
- Quality of tests over quantity

**Future Guidelines (Coming Soon):**
- Code coverage targets by component type
- Coverage report generation instructions
- Interpretation guidelines
- Exemptions for generated/simple code
- CI/CD integration details

**Remember:** High code coverage doesn't guarantee high-quality tests. Focus on writing meaningful tests that verify correct behavior, not just increasing coverage numbers.
