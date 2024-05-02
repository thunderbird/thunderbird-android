# Central Management of Android Project Dependencies and Gradle Configurations via Build-Plugin Module

- Pull Request: [#7803](https://github.com/thunderbird/thunderbird-android/pull/7803)

## Status

- **Accepted**

## Context

In our Android project, managing dependencies and configurations directly within each module's `build.gradle.kts` file has historically led to inconsistencies, duplication, and difficulty in updates. This challenge was particularly noticeable when maintaining the project configuration. By centralizing this setup in a `build-plugin` module, we can encapsulate and reuse Gradle logic, streamline the build process, and ensure consistency across all project modules and ease maintainability of our codebase.

## Decision

To address these challenges, we have decided to establish a `build-plugin` module within our project. This module will serve as the foundation for all common Gradle configurations, dependency management, and custom plugins, allowing for simplified configuration across various project modules and plugins. Key components of this module include:

- **Custom Plugins:** A suite of custom plugins that configure Gradle for different project aspects, ensuring each project type has tailored and streamlined build processes. These plugins should cover Android application, Android library, Jetpack Compose and Java modules.
- **Dependency Management:** Utilizing the [Gradle Version Catalog](https://docs.gradle.org/current/userguide/platforms.html) to centrally manage and update all dependencies and plugins, ensuring that every module uses the same versions and reduces the risk of conflicts.
- **Common Configuration Settings:** Establishing common configurations for Java, Kotlin, and Android to reduce the complexity and variability in setup across different modules.

## Consequences

### Positive Consequences

1. **Consistency Across Modules:** All project modules will use the same versions of dependencies and plugins, reducing the risk of conflicts and enhancing uniformity. They will also share common configurations, ensuring consistency in the build process.
2. **Ease of Maintenance:** Centralizing dependency versions in the Gradle Version Catalog allows for simple and quick updates to libraries and tools across all project modules from a single source.
3. **Simplified Configuration Process:** The custom plugins within the `build-plugin` module provides a streamlined way to apply settings and dependencies uniformly, enhancing productivity and reducing setup complexity.

### Negative Consequences

1. **Initial Overhead:** The setup of the build-plugin module with a Gradle Version Catalog and the migration of existing configurations required an initial investment of time and resources, but this has been completed.
2. **Complexity for New Developers:** The centralized build architecture, particularly with the use of a Gradle Version Catalog, may initially seem daunting to new team members who are unfamiliar with this level of abstraction.
3. **Dependency on the Build-Plugin Module:** The entire project becomes reliant on the stability and accuracy of the `build-plugin` module. Errors within this module or the catalog could impact the build process across all modules.
