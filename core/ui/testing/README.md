# Core UI Testing

This module provides shared test utilities for Compose UI tests.

## Compose UI Test Harness

Use `ComposeUiTestHarness` for tests that exercise Compose UI from common test code.

```kotlin
class ExampleTest : ComposeUiTestHarness() {

    @Test
    fun `content is shown`() = runComposeTest {
        setContent {
            ExampleContent()
        }

        onNodeWithText("Example").assertExists()
    }
}
```

The harness wraps the platform-specific `runComposeUiTest` implementation and exposes a common
`ComposeUiTestScope`.

## Dispatcher Handling

`runComposeTest` mirrors Compose's `runComposeUiTest` dispatcher API:

```kotlin
fun runComposeTest(
    effectContext: CoroutineContext = EmptyCoroutineContext,
    runTestContext: CoroutineContext = EmptyCoroutineContext,
    testTimeout: Duration = 60.seconds,
    block: suspend ComposeUiTestScope.() -> Unit,
)
```

The harness passes these values to Compose:

- `effectContext` is used for composition, `LaunchedEffect`, `rememberCoroutineScope`, and the main test clock.
- `runTestContext` is used for the test block.
- `testTimeout` controls the timeout for the Compose test.

Do not pass the same `TestDispatcher` or scheduler to both `effectContext` and `runTestContext`. Compose requires these
contexts to not share a `TestCoroutineScheduler`.

If the code under test uses `Dispatchers.Main`, `viewModelScope`, or lifecycle APIs that access
`Dispatchers.Main.immediate`, configure a main dispatcher once on the test class. When `effectContext` is left as
`EmptyCoroutineContext`, the harness uses that same dispatcher as Compose's effect context.

```kotlin
class ExampleTest : ComposeUiTestHarness(
    mainDispatcher = StandardTestDispatcher(),
) {

    @Test
    fun `event updates state`() = runComposeTest {
        setContent {
            Screen()
        }

        onNodeWithText("Submit").performClick()
        waitForIdle()

        onNodeWithText("Submitted").assertExists()
    }
}
```

## Standard Dispatcher

Use `StandardTestDispatcher` when the test needs explicit scheduler control. Work is queued, so call `waitForIdle()`
before asserting results that depend on queued coroutine or Compose work.

```kotlin
@Test
fun `event updates state`() = runComposeTest(
    effectContext = StandardTestDispatcher(),
) {
    setContent {
        Screen()
    }

    onNodeWithText("Submit").performClick()
    waitForIdle()

    onNodeWithText("Submitted").assertExists()
}
```

If the harness already has a `mainDispatcher`, leave `effectContext` as its default to reuse the class-level dispatcher.

## Unconfined Dispatcher

Use `UnconfinedTestDispatcher` when the test benefits from eager execution and does not require explicit scheduler
control. With this dispatcher, immediate assertions may not need `waitForIdle()`.

```kotlin
@Test
fun `event updates state eagerly`() = runComposeTest(
    effectContext = UnconfinedTestDispatcher(),
) {
    setContent {
        Screen()
    }

    onNodeWithText("Submit").performClick()

    onNodeWithText("Submitted").assertExists()
}
```

## Verification

Run the narrow module checks after changing this module:

```shell
./gradlew :core:ui:testing:jvmTest :core:ui:testing:testAndroidHostTest
```

