plugins {
    id(ThunderbirdPlugins.App.androidCompose)
}

android {
    namespace = "app.k9mail.feature.preview"

    defaultConfig {
        applicationId = "net.thunderbird.feature.preview"
        versionCode = 1
        versionName = "1.0"
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
