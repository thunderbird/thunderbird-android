pluginManagement {
    includeBuild("build-plugin")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        maven(url = "https://maven.mozilla.org/maven2") {
            content {
                includeGroup("org.mozilla.components")
                includeGroup("org.mozilla.telemetry")
            }
        }
        maven(url = "https://jitpack.io") {
            content {
                includeGroup("com.github.ByteHamster")
                includeGroup("com.github.cketti")
            }
        }
        mavenCentral()
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

// Thunderbird for Android
rootProject.name = "tfa"

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
    ":feature:onboarding:migration:api",
    ":feature:onboarding:migration:thunderbird",
    ":feature:onboarding:migration:noop",
)

include(
    ":feature:settings:import",
)

include(
    ":feature:account:avatar",
    ":feature:account:common",
    ":feature:account:edit",
    ":feature:account:oauth",
    ":feature:account:settings:api",
    ":feature:account:settings:impl",
    ":feature:account:server:certificate",
    ":feature:account:server:settings",
    ":feature:account:server:validation",
    ":feature:account:setup",
)

include(
    ":feature:autodiscovery:api",
    ":feature:autodiscovery:autoconfig",
    ":feature:autodiscovery:service",
    ":feature:autodiscovery:demo",
)

include(
    ":feature:navigation:drawer",
)

include(
    ":feature:widget:message-list",
    ":feature:widget:message-list-glance",
    ":feature:widget:shortcut",
    ":feature:widget:unread",
)

include(
    ":feature:migration:provider",
    ":feature:migration:qrcode",
    ":feature:migration:launcher:api",
    ":feature:migration:launcher:noop",
    ":feature:migration:launcher:thunderbird",
)

include(
    ":feature:telemetry:api",
    ":feature:telemetry:noop",
    ":feature:telemetry:glean",
)

include(
    ":feature:funding:api",
    ":feature:folder:api",
    ":feature:funding:googleplay",
    ":feature:funding:link",
    ":feature:funding:noop",
)

include(
    ":core:common",
    ":core:featureflags",
    ":core:testing",
    ":core:android:common",
    ":core:android:network",
    ":core:android:permissions",
    ":core:android:testing",
    ":core:ui:compose:common",
    ":core:ui:compose:designsystem",
    ":core:ui:compose:navigation",
    ":core:ui:compose:preference",
    ":core:ui:compose:theme2:common",
    ":core:ui:compose:theme2:k9mail",
    ":core:ui:compose:theme2:thunderbird",
    ":core:ui:compose:testing",
    ":core:ui:legacy:designsystem",
    ":core:ui:legacy:theme2:common",
    ":core:ui:legacy:theme2:k9mail",
    ":core:ui:legacy:theme2:thunderbird",
    ":core:ui:theme:api",
)

include(
    ":core:mail:folder:api",
)

include(
    ":legacy:account",
    ":legacy:common",
    ":legacy:core",
    ":legacy:crypto-openpgp",
    ":legacy:di",
    ":legacy:folder",
    ":legacy:mailstore",
    ":legacy:message",
    ":legacy:notification",
    ":legacy:preferences",
    ":legacy:search",
    ":legacy:storage",
    ":legacy:testing",
    ":legacy:ui:base",
    ":legacy:ui:account",
    ":legacy:ui:folder",
    ":legacy:ui:legacy",
    ":legacy:ui:theme",
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
    ":cli:resource-mover-cli",
    ":cli:translation-cli",
)

include(
    ":library:html-cleaner",
    ":library:TokenAutoComplete",
)

check(JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_17)) {
    """
        Java 17+ is required to build Thunderbird for Android.
        But it found an incompatible Java version ${{JavaVersion.current()}}.

        Java Home: [${System.getProperty("java.home")}]

        Please install Java 17+ and set JAVA_HOME to the directory containing the Java 17+ installation.
        https://developer.android.com/build/jdks#jdk-config-in-studio
    """.trimIndent()
}
include(":core:android:logging")
