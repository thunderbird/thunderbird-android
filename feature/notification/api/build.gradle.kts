import org.jetbrains.kotlin.gradle.internal.config.LanguageFeature

plugins {
    id(ThunderbirdPlugins.Library.kmpCompose)
    id("kotlin-parcelize")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.common)
            implementation(projects.core.outcome)
        }
        androidMain.dependencies {
            implementation(projects.core.ui.compose.designsystem)
        }
    }

    compilerOptions {
        freeCompilerArgs.addAll(
            "-P",
            "plugin:org.jetbrains.kotlin.parcelize:additionalAnnotation=net.thunderbird.core.common.io.KmpParcelize",
        )
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
    packageOfResClass = "net.thunderbird.feature.notification.resources"
}
