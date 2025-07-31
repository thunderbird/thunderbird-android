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
            implementation(projects.core.ui.compose.theme2.common)
        }
    }

    sourceSets.all {
        languageSettings.apply {
            enableLanguageFeature(LanguageFeature.ExpectActualClasses.name)
            enableLanguageFeature(LanguageFeature.WhenGuards.name)
        }
    }
}

android {
    namespace = "net.thunderbird.feature.notification.api"
}

compose.resources {
    publicResClass = false
    packageOfResClass = "net.thunderbird.feature.notification.resources.api"
}
