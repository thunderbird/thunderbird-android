# Components

This directory contains independently buildable and releasable parts of the project.

A component should have a clear boundary, be testable on its own, and be consumable by the app through a published
artifact, such as a Maven dependency. Local source development can still be supported through Gradle composite builds.

## What belongs here

Use `components/` for code that can reasonably be versioned, built, tested, and released independently.

Examples:

* shared KMP or Android libraries
* sync, storage, network, crypto, or telemetry libraries
* reusable test support libraries
* developer tooling such as code generators or lint rules

## What does not belong here

Do not use `components/` for app-owned feature code or app-internal shared modules.

Use:

* `apps-xyz` for final applications
* `features/` for product features and screens
* `core/` for app-internal shared modules
* `build-logic/` for Gradle convention plugins and build infrastructure

## Rules

Components must not depend on `apps/`, `features/`, or `core/`.

If a component needs code from `core/`, either move that dependency into the component, promote the shared code into
its own component, or introduce a smaller public API.

The app currently consumes local component sources by default through dependency substitution. Disable local source
substitution with the `tb.components.local` Gradle property when testing released artifacts.

The UI catalog app lives in the components build and can be built directly:

```shell
./gradlew -p components :ui:catalog:assembleDebug
```

Individual component groups can provide their own override property. Bolt substitution can be controlled with
`tb.components.local.bolt`.

Moving code into `components/` is only useful if the app build no longer configures and compiles that component by default.
