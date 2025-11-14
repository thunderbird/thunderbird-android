// Thunderbird for Android
rootProject.name = "tfa"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("build-plugin")
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        maven(url = "https://maven.mozilla.org/maven2") {
            mavenContent {
                includeGroup("org.mozilla.components")
                includeGroup("org.mozilla.telemetry")
            }
        }
        maven(url = "https://jitpack.io") {
            mavenContent {
                includeGroup("com.github.ByteHamster")
                includeGroup("com.github.cketti")
            }
        }
        mavenCentral()
    }
}

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
    ":feature:account:api",
    ":feature:account:avatar:api",
    ":feature:account:avatar:impl",
    ":feature:account:core",
    ":feature:account:common",
    ":feature:account:edit",
    ":feature:account:fake",
    ":feature:account:oauth",
    ":feature:account:settings:api",
    ":feature:account:settings:impl",
    ":feature:account:server:certificate",
    ":feature:account:server:settings",
    ":feature:account:server:validation",
    ":feature:account:setup",
    ":feature:account:storage:api",
    ":feature:account:storage:legacy",
)

include(
    ":feature:autodiscovery:api",
    ":feature:autodiscovery:autoconfig",
    ":feature:autodiscovery:service",
    ":feature:autodiscovery:demo",
)

include(
    ":feature:funding:api",
    ":feature:funding:googleplay",
    ":feature:funding:link",
    ":feature:funding:noop",
)

include(
    ":feature:mail:account:api",
    ":feature:mail:folder:api",
    ":feature:mail:message:composer",
    ":feature:mail:message:list",
    ":feature:mail:message:export:api",
    ":feature:mail:message:export:impl-eml",
)

include(
    ":feature:migration:provider",
    ":feature:migration:qrcode",
    ":feature:migration:launcher:api",
    ":feature:migration:launcher:noop",
    ":feature:migration:launcher:thunderbird",
)

include(
    ":feature:navigation:drawer:api",
    ":feature:navigation:drawer:dropdown",
    ":feature:navigation:drawer:siderail",
)

include(
    ":feature:notification:api",
    ":feature:notification:impl",
    ":feature:notification:testing",
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
    ":feature:search:impl-legacy",
)

include(
    ":feature:settings:import",
)

include(
    ":feature:telemetry:api",
    ":feature:telemetry:noop",
    ":feature:telemetry:glean",
)

include(
    ":feature:widget:message-list",
    ":feature:widget:message-list-glance",
    ":feature:widget:shortcut",
    ":feature:widget:unread",
)

include(
    ":core:architecture:api",
    ":core:common",
    ":core:configstore:api",
    ":core:configstore:impl-backend",
    ":core:featureflag",
    ":core:logging:api",
    ":core:logging:config",
    ":core:logging:impl-composite",
    ":core:logging:impl-console",
    ":core:logging:impl-legacy",
    ":core:logging:impl-file",
    ":core:logging:testing",
    ":core:file",
    ":core:mail:mailserver",
    ":core:preference:api",
    ":core:preference:impl",
    ":core:outcome",
    ":core:testing",
    ":core:validation",
)

include(
    ":core:android:account",
    ":core:android:common",
    ":core:android:contact",
    ":core:android:logging",
    ":core:android:network",
    ":core:android:permissions",
    ":core:android:testing",
)

include(
    ":core:ui:setting:api",
    ":core:ui:setting:impl-dialog",
)

include(
    ":core:ui:account",
    ":core:ui:compose:common",
    ":core:ui:compose:designsystem",
    ":core:ui:compose:navigation",
    ":core:ui:compose:testing",
    ":core:ui:compose:theme2:common",
    ":core:ui:compose:theme2:k9mail",
    ":core:ui:compose:theme2:thunderbird",
    ":core:ui:legacy:designsystem",
    ":core:ui:legacy:theme2:common",
    ":core:ui:legacy:theme2:k9mail",
    ":core:ui:legacy:theme2:thunderbird",
    ":core:ui:theme:api",
    ":core:ui:theme:manager",
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

include(
    ":legacy:common",
    ":legacy:core",
    ":legacy:crypto-openpgp",
    ":legacy:di",
    ":legacy:mailstore",
    ":legacy:message",
    ":legacy:storage",
    ":legacy:ui:base",
    ":legacy:ui:folder",
    ":legacy:ui:legacy",
)

include(
    ":ui-utils:LinearLayoutManager",
    ":ui-utils:ItemTouchHelper",
    ":ui-utils:ToolbarBottomSheet",
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

include(
    ":quality:konsist",
)

include(
    ":feature:debug-settings",
)

check(JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_21)) {
    """
        Java 21+ is required to build Thunderbird for Android.
        But it found an incompatible Java version ${{ JavaVersion.current() }}.

        Java Home: [${System.getProperty("java.home")}]

        Please install Java 21+ and set JAVA_HOME to the directory containing the Java 21+ installation.
        https://developer.android.com/build/jdks#jdk-config-in-studio
    """.trimIndent()
}
