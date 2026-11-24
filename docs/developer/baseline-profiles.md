# Baseline Profiles

A Baseline Profile is a list of the classes and methods the app uses during startup. It ships inside the app. When
the app is installed, Android compiles this code ahead of time, so the app starts faster.

Each app has its own profile, checked into the repository:

- `app-thunderbird/src/main/generated/baselineProfiles/`
- `app-k9mail/src/main/generated/baselineProfiles/`

Each folder contains `baseline-prof.txt` and `startup-prof.txt`. The release, beta and daily builds include them
automatically. A normal build does not generate a new profile, you have to do that by hand.

Thunderbird is published as three apps: Release, Beta and Daily. They have different feature flags, so they can run
different code at startup. The profile is generated from all three and merged into one file that all three apps use.

## When to generate a new profile

Generate a new profile when the code that runs at startup changes in a big way. For example, after a large refactor,
a new screen on the startup path, or a major library update.

An old profile still works. It just covers less of the code that runs today.

## Requirements

- A device or emulator running Android 13 (API 33) or higher, or a rooted device running Android 9 (API 28) or
  higher. Connect only one device, the tasks run on every connected device.
- A test email account. Release builds can't use the demo account.

Add the account to `local.properties` in the project root:

```properties
tfa.benchmark.account.email=someone@example.com
tfa.benchmark.account.password=password
# Optional
tfa.benchmark.account.display_name=Benchmark
tfa.benchmark.account.incoming_server=imap.example.com
tfa.benchmark.account.outgoing_server=smtp.example.com
```

- The account must use a password. OAuth is not supported.
- Set both servers or none. Without servers, auto discovery has to find them.
- The inbox must contain messages. The startup benchmarks wait for them to show up.

> [!WARNING]
> The password is visible on the device and in Gradle logs. Only use a throwaway account, **never a personal one.**

## Generate the profile

Run the task for the app you want to update:

```shell
./gradlew :app-thunderbird:generateBaselineProfile
./gradlew :app-k9mail:generateBaselineProfile
```

The task builds the app, installs it on the device, and sets up the account through the onboarding screens if needed.
Then it starts the app a few times and records which code runs. The result is written to the folder listed above.

For Thunderbird, this runs for each app (Release, Beta and Daily) in both the `foss` and `full` versions, so it takes
a while.

Check the diff before you commit it. A new profile usually adds and removes a few lines. If most of the file is gone,
the app most likely did not reach the message list.

## Measure startup

Use the startup benchmarks to check that a profile helps, or to compare startup time before and after a change.

```shell
# Thunderbird daily build, uses the demo account if no account is set in local.properties
./gradlew :app-thunderbird:benchmarks:connectedFossBenchmarkDailyAndroidTest

# Thunderbird release build, needs an account in local.properties
./gradlew :app-thunderbird:benchmarks:connectedFullBenchmarkReleaseAndroidTest

# K-9 Mail, needs an account in local.properties
./gradlew :app-k9mail:benchmarks:connectedFullBenchmarkReleaseAndroidTest
```

To see all benchmark tasks, run `./gradlew :app-thunderbird:benchmarks:tasks --all | grep connected`.

The Thunderbird `daily` build doesn't use R8 by default. Add `-Pci=true` to turn it on, so the build is closer to a
release build.

Each test starts the app from a cold state and waits for the message list:

|                     Test                     |                         What it measures                          |
|----------------------------------------------|-------------------------------------------------------------------|
| `startupCompilationNone`                     | Nothing compiled ahead of time. Worst case.                       |
| `startupCompilationBaselineProfilesDisabled` | Profile ignored, app started once before to warm it up.           |
| `startupCompilationBaselineProfiles`         | Profile used. Closest to what users get from the Play Store.      |
| `startupCompilationFull`                     | Everything compiled ahead of time. Best case, users don't get it. |

Each test reports two numbers:

- `timeToInitialDisplayMs`: time until the first frame is drawn.
- `timeToFullDisplayMs`: time until the message list shows its first messages.

Look at the median values. The results are printed in the test output and saved in
`<app>/benchmarks/build/outputs/connected_android_test_additional_output/`.

To compare two changes, run the same test before and after the change, on the same device. Use a physical device,
an emulator doesn't give reliable numbers.

To run a single test, pass its name:

```shell
./gradlew :app-thunderbird:benchmarks:connectedFossBenchmarkDailyAndroidTest \
    -Pandroid.testInstrumentationRunnerArguments.class=net.thunderbird.baselineprofile.StartupBenchmarks#startupCompilationBaselineProfiles
```

## Where the code lives

- `benchmarks/`: shared code for both apps. It has the profile generator, the startup benchmarks, and the steps that
  set up the account.
- `app-thunderbird/benchmarks/` and `app-k9mail/benchmarks/`: one test module per app, pointing to that app.
- `build-plugin/`: the `net.thunderbird.baselineprofile.producer.app` plugin for the test modules and the
  `net.thunderbird.baselineprofile.consumer.app` plugin for the apps.

If you change the onboarding or the message list screens, the account setup can break. The setup steps are in
`benchmarks/src/main/kotlin/net/thunderbird/benchmarks/common/account/setup/step/`.
