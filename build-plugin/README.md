# Build plugins

The `build-plugin` folder defines Gradle build plugins, used as single source of truth for the project configuration.
This helps to avoid duplicated build script setups and provides a central location for all common build logic.

## Background

We use Gradle's
[sharing build logic in a multi-repo setup](https://docs.gradle.org/current/samples/sample_publishing_convention_plugins.html)
to create common configuration. It allows usage of `xyz.gradle.kts` files, that are then automatically converted to
Gradle Plugins.

The `build-plugin` is used as included build in the root `settings.gradle.kts` and provides all
included `xyz.gradle.kts` as plugins under their `xyz` name to the whole project.

The plugins should try to accomplish single responsibility and leave one-off configuration to the
module's `build.gradle.kts`.

## Convention plugins

- `thunderbird.app.android` - Configures common options for Android apps
- `thunderbird.app.android.compose` - Configures common options for Jetpack Compose, based
  on `thunderbird.app.android`
- `thunderbird.library.android` - Configures common options for Android libraries
- `thunderbird.library.android.compose` - Configures common options for Jetpack Compose, based
  on `thunderbird.library.android`
- `thunderbird.library.jvm` - Configures common options for JVM libraries

## Supportive plugins

- `thunderbird.dependency.check` - [Gradle Versions: Gradle plugin to discover dependency updates](https://github.com/ben-manes/gradle-versions-plugin)
  - Use `./gradlew dependencyUpdates` to generate a dependency update report
- `thunderbird.quality.detekt` - [Detekt - Static code analysis for Kotlin](https://detekt.dev/)
  - Use `./gradlew detekt` to check for any issue and `./gradlew detektBaseline` in case you can't fix the reported
    issue.
- `thunderbird.quality.spotless` - [Spotless - Code formatter](https://github.com/diffplug/spotless)
  with [Ktlint - Kotlin linter and formatter](https://pinterest.github.io/ktlint/)
  - Use `./gradlew spotlessCheck` to check for any issue and `./gradlew spotlessApply` to format your code

## Add new build plugin

Create a `thunderbird.xyz.gradle.kts` file, while `xyz` describes the new plugin.

If you need to access dependencies that are not yet defined in `build-plugin/build.gradle.kts` you have to:

1. Add the dependency to the version catalog `gradle/libs.versions.toml`
2. Then add it to `build-plugin/build.gradle.kts`.
   1. In case of a plugin dependency use `implementation(plugin(libs.plugins.YOUR_PLUGIN_DEPENDENCY))`.
   2. Otherwise `implementation(libs.YOUR_DEPENDENCY))`.

When done, add the plugin to `build-plugin/src/main/kotlin/ThunderbirdPlugins.kt`

Then apply the plugin to any subproject it should be used with:

```
plugins {
    id(ThunderbirdPlugins.xyz)
}
```

If the plugin is meant for the root `build.gradle.kts`, you can't use `ThunderbirdPlugins`, as it's not available to
the `plugins` block. Instead use:

```
plugins {
    id("thunderbird.xyz")
}
```

## Acknowledgments

- [Herding Elephants | Square Corner Blog](https://developer.squareup.com/blog/herding-elephants/)
- [Idiomatic Gradle: How do I idiomatically structure a large build with Gradle](https://github.com/jjohannes/idiomatic-gradle#idiomatic-build-logic-structure)

