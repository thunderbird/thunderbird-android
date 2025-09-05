# 🚀 Development Environment

This guide will help you set up your development environment and get started with contributing to the Thunderbird for
Android project.

## 📋 Prerequisites

Before you begin, ensure you have the following installed:

- **[Java Development Kit (JDK)](https://adoptium.net/temurin/releases/?version=17)** - Version 17 or higher (Temurin OpenJDK recommended)
- **[Android Studio](https://developer.android.com/studio)** - Latest stable version recommended
- **[Git](https://git-scm.com/downloads)** - For version control
- **Gradle** - Use the Gradle wrapper included in this repo (`./gradlew`); no separate install required
- **Android SDK & command-line tools** – Installed and managed via Android Studio SDK Manager

## 🔧 Setting Up the Development Environment

### 1. Get the Source Code

All contributions happen through a personal fork of the repository.

- If you haven’t forked the project yet, see the [Contribution Workflow](contribution-workflow.md) for step-by-step instructions.
- Once you have a fork, clone it to your machine and open it in Android Studio.

1. Go to the [Thunderbird for Android repository](https://github.com/thunderbird/thunderbird-android)
2. Click the **Fork** button in the top-right corner
3. Create a fork under your GitHub account

### 2. Import the Project into Android Studio

1. Open Android Studio
2. Select **Open an Existing Project**
3. Navigate to the cloned repository and open it
4. Wait for project sync and indexing

### 3. Configure Android Studio

For the best development experience, we recommend the following settings:

- **Recommended plugins:**
  - **Kotlin Multiplatform** (usually bundled; if not, install from the JetBrains Marketplace)

## 🏗️ Building the Project

### Building from Android Studio

1. Select the app module (e.g., `app-thunderbird` or `app-k9mail`) in the **Run/Debug Configuration** dropdown
2. Select build variant you want to work with (Debug/Release) from Build Variants window
3. Click the **Build** button or press `Ctrl+F9` (Windows/Linux) or `Cmd+F9` (macOS)

### Building from Command Line

A Gradle wrapper is included in the project, so you can build the project from the command line without installing
Gradle globally. Run the following commands from the root of the project, where `./gradlew` is the Gradle wrapper script
and the command to `build` runs tests and other checks, while `assemble` only compiles the code and packages the APK.

```bash
# Build all variants
./gradlew assemble
./gradlew build

# Build debug or release variant
./gradlew assembleDebug
./gradlew assembleRelease

# Build a specific app module
./gradlew :app-thunderbird:assembleDebug
./gradlew :app-k9mail:assembleDebug

# Build a specific library/feature module
./gradlew :module-name:build
```

Replace `module-name` with the actual name of the module you want to build.

## 🚀 Running the Application

### Running on an Emulator

1. [Set up an Android Virtual Device (AVD)](https://developer.android.com/studio/run/managing-avds) in Android Studio with a recent API level with Google APIs image.
2. Select the AVD from the device dropdown.
3. Click the **Run** button or press `Shift+F10` (Windows/Linux) or `Ctrl+R` (macOS)

### Running on a Physical Device

1. [Enable Developer Options and USB Debugging](https://developer.android.com/studio/debug/dev-options) on your device
2. Connect your device to your computer via USB and confirm trust dialog if prompted
3. Select your device from the device dropdown and click **Run**

## 🧪 Running Tests

```bash
# Run all tests across modules
./gradlew test

# Run unit tests for a specific module
./gradlew :module-name:test

# Run instrumented tests (device/emulator required)
./gradlew connectedAndroidTest
```

See the [Testing Guide](testing-guide.md) for details.

## 🔍 Checking Code Quality

Maintaining high code quality is essential for the long-term sustainability of the Thunderbird for Android project. The project uses several tools and practices to ensure code quality:

- **Static Analysis Tools**: Android Lint, Detekt, and Spotless
- **Code Style Guidelines**: Kotlin style guide and project-specific conventions
- **Testing**: Unit tests, integration tests, and UI tests
- **Code Reviews**: Peer review process for all code changes
- **Continuous Integration**: Automated checks for build success, tests, and code quality

To run the basic code quality checks:

```bash
# Run lint checks
./gradlew lint

# Run detekt
./gradlew detekt

# Check code formatting
./gradlew spotlessCheck

# Apply code formatting fixes
./gradlew spotlessApply
```

See the [Code Quality Guide](code-quality-guide.md) for more details.

## 🐛 Debugging

### Using the Debugger

1. Set breakpoints in your code by clicking in the gutter next to the line numbers
2. Start debugging by clicking the **Debug** button or pressing `Shift+F9` (Windows/Linux) or `Ctrl+D` (macOS)
3. Use the debugger controls to step through code, inspect variables, and evaluate expressions
4. 

See the [Android Studio Debugger Guide](https://developer.android.com/studio/debug) for a detailed description.

### Logging

Use the project's core logging API `net.thunderbird.core.logging.Logger`, which is provided via dependency injection
(Koin). Avoid logging **personally identifiable information (PII)**.

Example with DI (Koin):

```kotlin
private const val TAG = "ExampleActivity"

class ExampleActivity : ComponentActivity() {
    private val logger: Logger by inject()

    fun doSomething() {
        logger.debug(tag = TAG) { "Debug message" }
        
        try {
            // Some code that might throw an exception
        } catch (exception: Exception) {
            logger.error(tag = TAG, throwable = exception) { "An error occurred" }
        }
    }
}
```

### Profiling

Use Android Studio's built-in [Android Profiler](https://developer.android.com/studio/profile/android-profiler) to monitor:
- CPU usage
- Memory allocation
- Network activity

For performance-sensitive code, also consider Baseline Profiles or Macrobenchmark tests.
