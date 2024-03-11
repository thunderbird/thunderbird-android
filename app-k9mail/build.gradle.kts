plugins {
    id(ThunderbirdPlugins.App.android)
    alias(libs.plugins.dependency.guard)
    id("thunderbird.quality.badging")
}

val testCoverageEnabled: Boolean by extra
if (testCoverageEnabled) {
    apply(plugin = "jacoco")
}

dependencies {
    implementation(projects.app.common)

    implementation(projects.app.core)
    implementation(projects.app.ui.legacy)
    implementation(projects.app.ui.messageListWidget)

    debugImplementation(projects.backend.demo)

    implementation(libs.androidx.work.runtime)

    testImplementation(libs.robolectric)

    // Required for DependencyInjectionTest to be able to resolve OpenPgpApiManager
    testImplementation(projects.plugins.openpgpApiLib.openpgpApi)
    testImplementation(projects.feature.account.setup)
}

android {
    namespace = "com.fsck.k9"

    defaultConfig {
        applicationId = "com.fsck.k9"
        testApplicationId = "com.fsck.k9.tests"

        versionCode = 38001
        versionName = "6.801"

        // Keep in sync with the resource string array "supported_languages"
        resourceConfigurations.addAll(
            listOf(
                "in", "br", "ca", "cs", "cy", "da", "de", "et", "en", "en_GB", "es", "eo", "eu", "fr", "gd", "gl",
                "hr", "is", "it", "lv", "lt", "hu", "nl", "nb", "pl", "pt_PT", "pt_BR", "ru", "ro", "sq", "sk", "sl",
                "fi", "sv", "tr", "el", "be", "bg", "sr", "uk", "ar", "fa", "ml", "zh_CN", "zh_TW", "ja",
                "fy",
            ),
        )

        buildConfigField("String", "CLIENT_ID_APP_NAME", "\"K-9 Mail\"")
    }

    signingConfigs {
        if (project.hasProperty("k9mail.keyAlias") &&
            project.hasProperty("k9mail.keyPassword") &&
            project.hasProperty("k9mail.storeFile") &&
            project.hasProperty("k9mail.storePassword")
        ) {
            create("release") {
                keyAlias = project.property("k9mail.keyAlias") as String
                keyPassword = project.property("k9mail.keyPassword") as String
                storeFile = file(project.property("k9mail.storeFile") as String)
                storePassword = project.property("k9mail.storePassword") as String
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")

            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android.txt"),
                "proguard-rules.pro",
            )

            buildConfigField(
                "String",
                "OAUTH_GMAIL_CLIENT_ID",
                "\"262622259280-hhmh92rhklkg2k1tjil69epo0o9a12jm.apps.googleusercontent.com\"",
            )
            buildConfigField(
                "String",
                "OAUTH_YAHOO_CLIENT_ID",
                "\"dj0yJmk9aHNUb3d2MW5TQnpRJmQ9WVdrOWVYbHpaRWM0YkdnbWNHbzlNQT09JnM9Y29uc3VtZXJzZWNyZXQmc3Y9MCZ4PWIz\"",
            )
            buildConfigField(
                "String",
                "OAUTH_AOL_CLIENT_ID",
                "\"dj0yJmk9dUNqYXZhYWxOYkdRJmQ9WVdrOU1YQnZVRFZoY1ZrbWNHbzlNQT09JnM9Y29uc3VtZXJzZWNyZXQmc3Y9MCZ4PWIw\"",
            )
            buildConfigField("String", "OAUTH_MICROSOFT_CLIENT_ID", "\"e647013a-ada4-4114-b419-e43d250f99c5\"")
            buildConfigField(
                "String",
                "OAUTH_MICROSOFT_REDIRECT_URI",
                "\"msauth://com.fsck.k9/Dx8yUsuhyU3dYYba1aA16Wxu5eM%3D\"",
            )

            manifestPlaceholders["appAuthRedirectScheme"] = "com.fsck.k9"
        }

        debug {
            applicationIdSuffix = ".debug"
            enableUnitTestCoverage = testCoverageEnabled
            enableAndroidTestCoverage = testCoverageEnabled

            isMinifyEnabled = false

            buildConfigField(
                "String",
                "OAUTH_GMAIL_CLIENT_ID",
                "\"262622259280-5qb3vtj68d5dtudmaif4g9vd3cpar8r3.apps.googleusercontent.com\"",
            )
            buildConfigField(
                "String",
                "OAUTH_YAHOO_CLIENT_ID",
                "\"dj0yJmk9ejRCRU1ybmZjQlVBJmQ9WVdrOVVrZEViak4xYmxZbWNHbzlNQT09JnM9Y29uc3VtZXJzZWNyZXQmc3Y9MCZ4PTZj\"",
            )
            buildConfigField(
                "String",
                "OAUTH_AOL_CLIENT_ID",
                "\"dj0yJmk9cHYydkJkTUxHcXlYJmQ9WVdrOWVHZHhVVXN4VVV3bWNHbzlNQT09JnM9Y29uc3VtZXJzZWNyZXQmc3Y9MCZ4PTdm\"",
            )
            buildConfigField("String", "OAUTH_MICROSOFT_CLIENT_ID", "\"e647013a-ada4-4114-b419-e43d250f99c5\"")
            buildConfigField(
                "String",
                "OAUTH_MICROSOFT_REDIRECT_URI",
                "\"msauth://com.fsck.k9.debug/VZF2DYuLYAu4TurFd6usQB2JPts%3D\"",
            )

            manifestPlaceholders["appAuthRedirectScheme"] = "com.fsck.k9.debug"
        }
    }

    packaging {
        jniLibs {
            excludes += listOf("kotlin/**")
        }

        resources {
            excludes += listOf(
                "META-INF/*.kotlin_module",
                "META-INF/*.version",
                "kotlin/**",
                "DebugProbesKt.bin",
            )
        }
    }
}

dependencyGuard {
    configuration("releaseRuntimeClasspath")
}
