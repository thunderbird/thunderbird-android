# Thunderbird Mobile Components

[`thunderbird-mobile-components`](https://github.com/thunderbird/thunderbird-mobile-components) contains shared
Thunderbird mobile components that can be versioned, built, tested, and released independently from Thunderbird for
Android.

Components are consumed through focused artifacts. Do not depend on a broad "all components" artifact; add only the
component groups a module actually needs.

**Bolt** is the Compose UI component group. It contains the design-system components, theme support, and common UI
support used by Thunderbird for Android.

## Coordinates

Dependency aliases are declared in `gradle/libs.versions.toml`:

- `libs.tb.mobile.components.ui.bolt` -> `net.thunderbird.components.ui.bolt:bolt`

Common UI support, design-system components, and theme support are packaged as part of the Bolt artifact.

Use the aliases when consuming Bolt artifacts:

```kotlin
dependencies {
    implementation(libs.tb.mobile.components.ui.bolt)
}
```

The Bolt component version is managed by `libs.versions.tbMobileComponents`.

## Local Development

The root `settings.gradle.kts` includes the local `components/` build and substitutes the tracked Bolt coordinates by
default:

- `net.thunderbird.components.ui.bolt:bolt` -> `:components:ui:bolt`
- `net.thunderbird.components.ui:testing` -> `:components:ui:testing`

Disable local component substitution when testing released artifacts with:

```shell
./gradlew -Ptb.components.local=false :app-thunderbird:assembleDebug
```

Control Bolt substitution independently with `tb.components.local.bolt`. This is useful when testing released component
artifacts for everything else while still working on Bolt locally:

```shell
./gradlew -Ptb.components.local=false -Ptb.components.local.bolt=true :app-thunderbird:assembleDebug
```

Build Bolt directly from the included build when working on the components. In this command, `:ui:bolt`
is the project path inside the `components` build:

```shell
./gradlew -p components :ui:bolt:assemble
```

Build the Bolt catalog directly from the components build:

```shell
./gradlew -p components :ui:catalog:assembleDebug
```

Do not add a broad dependency on all components. Add new coordinates and substitutions only for component groups that
Thunderbird for Android actually consumes.
