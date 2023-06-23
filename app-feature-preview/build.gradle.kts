plugins {
    id(ThunderbirdPlugins.App.androidCompose)
}

android {
    namespace = "app.k9mail.feature.preview"

    defaultConfig {
        applicationId = "net.thunderbird.feature.preview"
        versionCode = 1
        versionName = "1.0"

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
        buildConfigField(
            "String",
            "OAUTH_MICROSOFT_CLIENT_ID",
            "\"e647013a-ada4-4114-b419-e43d250f99c5\"",
        )
        buildConfigField(
            "String",
            "OAUTH_MICROSOFT_REDIRECT_URI_ID",
            "\"VZF2DYuLYAu4TurFd6usQB2JPts%3D\"",
        )

        manifestPlaceholders["appAuthRedirectScheme"] = "com.fsck.k9.debug"
    }

    packaging {
        resources {
            excludes += listOf(
                "META-INF/DEPENDENCIES",
            )
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    lint {
        baseline = file("lint-baseline.xml")
    }
}

dependencies {
    implementation(projects.core.ui.compose.designsystem)
    implementation(projects.core.common)

    implementation(projects.feature.onboarding)
    implementation(projects.feature.account.setup)
    implementation(libs.okhttp)
}
