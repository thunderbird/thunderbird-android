# App Composition

`app-composition` is the Kotlin Multiplatform application assembly layer. It binds shared feature and core contracts to
their internal implementations for reuse by Android and future application targets.

The module contains shared dependency-injection wiring and compile-time application composition. Platform-specific
setup and legacy integration remain in their respective application modules.
