# White Label Architecture

- Issue: [#7807](https://github.com/thunderbird/thunderbird-android/issues/7807)
- Pull Request: [#7805](https://github.com/thunderbird/thunderbird-android/pull/7805)

## Status

- **Accepted**

## Context

Our project hosts two separate applications, K-9 Mail and Thunderbird for Android, which share a significant amount of functionality. Despite their common features, each app requires distinct branding elements such as app names, themes, and specific strings.

## Decision

We have decided to adopt a modular white-label architecture, where each application is developed as a separate module that relies on a shared codebase. This structure allows us to streamline configuration details specific to each brand either during build or at runtime. This is how we structure the modules:

### Application Modules

There will be 2 separate modules for each of the two applications: **Thunderbird for Android** will be located in `app-thunderbird` and **K-9 Mail** in `app-k9mail`. These modules will contain app-specific implementations, configurations, resources, and startup logic. They should solely depend on the `app-common` module for shared functionalities and may selectively integrate other modules when needed to configure app-specific functionality.

### App Common Module

A central module named `app-common` acts as the central integration point for shared code among the applications. This module contains the core functionality, shared resources, and configurations that are common to both apps. It should be kept as lean as possible to avoid unnecessary dependencies and ensure that it remains focused on shared functionality.

## Consequences

### Positive Consequences

- Enhanced maintainability due to a shared codebase for common functionalities, reducing code duplication.
- Increased agility in developing and deploying new features across both applications, as common enhancements need to be implemented only once.

## Negative Consequences

- Potential for configuration complexities as differentiations increase between the two applications.
- Higher initial setup time and learning curve for new developers due to the modular and decoupled architecture.
