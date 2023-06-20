pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "k-9"

includeBuild("build-plugin")

include(
    ":app-feature-preview",
    ":app-ui-catalog",
)

include(
    ":app:k9mail",
    ":app:ui:base",
    ":app:ui:legacy",
    ":app:ui:message-list-widget",
    ":app:core",
    ":app:storage",
    ":app:crypto-openpgp",
    ":app:testing",
    ":app:html-cleaner",
)

include(
    ":feature:account:setup",
    ":feature:onboarding",
    ":feature:autodiscovery:api",
    ":feature:autodiscovery:providersxml",
    ":feature:autodiscovery:srvrecords",
    ":feature:autodiscovery:autoconfig",
    ":feature:autodiscovery:service",
)

include(
    ":core:common",
    ":core:testing",
    ":core:android:common",
    ":core:android:testing",
    ":core:ui:compose:common",
    ":core:ui:compose:designsystem",
    ":core:ui:compose:theme",
    ":core:ui:compose:testing",
)

include(
    ":ui-utils:LinearLayoutManager",
    ":ui-utils:ItemTouchHelper",
    ":ui-utils:ToolbarBottomSheet",
)

include(
    ":mail:common",
    ":mail:testing",
    ":mail:protocols:imap",
    ":mail:protocols:pop3",
    ":mail:protocols:smtp",
)

include(
    ":backend:api",
    ":backend:testing",
    ":backend:imap",
    ":backend:pop3",
    ":backend:jmap",
    ":backend:demo",
)

include(":plugins:openpgp-api-lib:openpgp-api")

include(
    ":cli:autodiscovery-cli",
    ":cli:html-cleaner-cli",
)
