import org.jetbrains.kotlin.gradle.internal.config.LanguageFeature

plugins {
    id(ThunderbirdPlugins.Library.kmpCompose)
    alias(libs.plugins.dev.mokkery)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.common)
            implementation(projects.core.outcome)
        }
        commonTest.dependencies {
            implementation(projects.feature.notification.testing)
        }
        androidMain.dependencies {
            implementation(projects.core.ui.compose.designsystem)
        }
    }

    sourceSets.all {
        languageSettings.enableLanguageFeature(LanguageFeature.ExpectActualClasses.name)
    }
}

android {
    namespace = "net.thunderbird.feature.notification.api"
}

compose.resources {
    publicResClass = false
    packageOfResClass = "net.thunderbird.feature.notification.resources.api"
}
