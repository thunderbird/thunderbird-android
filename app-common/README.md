# App Common

`app-common` contains Android-specific application integration and bridges to `legacy:*`, `mail:*`, and `backend:*`
code shared by K-9 Mail and Thunderbird for Android.

Shared Kotlin Multiplatform bindings belong in [`app-composition`](../app-composition/README.md). As legacy bridges are
replaced, their platform-independent bindings should move there.
