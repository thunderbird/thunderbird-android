pluginManagement {
    repositories {
        includeBuild("build-plugin")
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "k-9"

include(
    ":app-k9mail",
    ":app-thunderbird",
    ":app-ui-catalog",
)

include(
    ":app-common",
)

include(
    ":feature:launcher",
)

include(
    ":feature:onboarding:main",
    ":feature:onboarding:welcome",
    ":feature:onboarding:permissions",
)

include(
    ":feature:settings:import",
)

include(
    ":feature:account:common",
    ":feature:account:edit",
    ":feature:account:oauth",
    ":feature:account:setup",
    ":feature:account:server:certificate",
    ":feature:account:server:settings",
    ":feature:account:server:validation",
)

include(
    ":feature:autodiscovery:api",
    ":feature:autodiscovery:autoconfig",
    ":feature:autodiscovery:service",
    ":feature:autodiscovery:demo",
)

include(
    ":feature:widget:message-list",
    ":feature:widget:shortcut",
    ":feature:widget:unread",
)

include(
    ":core:common",
    ":core:featureflags",
    ":core:testing",
    ":core:android:common",
    ":core:android:permissions",
    ":core:android:testing",
    ":core:ui:compose:common",
    ":core:ui:compose:designsystem",
    ":core:ui:compose:theme2:common",
    ":core:ui:compose:theme2:k9mail",
    ":core:ui:compose:theme2:thunderbird",
    ":core:ui:compose:testing",
    ":core:ui:legacy:designsystem",
    ":core:ui:legacy:theme2:common",
    ":core:ui:legacy:theme2:k9mail",
    ":core:ui:legacy:theme2:thunderbird",
)

include(
    ":legacy:common",
    ":legacy:ui:base",
    ":legacy:ui:legacy",
    ":legacy:core",
    ":legacy:storage",
    ":legacy:crypto-openpgp",
    ":legacy:testing",
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
    ":backend:ddd",
)

include(":plugins:openpgp-api-lib:openpgp-api")

include(
    ":cli:autodiscovery-cli",
    ":cli:html-cleaner-cli",
    ":cli:resource-mover-cli",
    ":cli:translation-cli",
)

include(
    ":library:html-cleaner",
)
include(":backend:ddd")
include(":feature:dddonboarding")
