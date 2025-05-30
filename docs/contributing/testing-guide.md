# 🧪 Testing Guide

This document outlines the testing practices and guidelines for the Thunderbird for Android project.

## 📐 Test Structure

### 🔍 Arrange-Act-Assert Pattern

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

Fakes provide better test reliability and are more maintainable in the long run. They also make tests more readable and less prone to breaking when implementation details change.

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

Use AssertK for assertions in tests:

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

Note: You'll need to import the appropriate AssertK assertions:
- `assertk.assertThat` for the base assertion function
- `assertk.assertions.*` for specific assertion types (isEqualTo, contains, isTrue, etc.)

## 🧮 Test Types

### 🔬 Unit Tests

Unit tests should:
- Test a single unit of functionality
- Be fast and independent
- Not rely on external resources (database, network, etc.)
- Use fakes for dependencies

### 🔌 Integration Tests

Integration tests should:
- Test the interaction between components
- Verify that components work together correctly
- May use real implementations of dependencies when appropriate

## 🏃 Running Tests

Run all tests using:

```bash
./gradlew test
```

Run tests for a specific module:

```bash
./gradlew :module-name:test
```

