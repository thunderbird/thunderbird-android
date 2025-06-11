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
    }

    compilerOptions {
        freeCompilerArgs.addAll(
            "-P",
            "plugin:org.jetbrains.kotlin.parcelize:additionalAnnotation=net.thunderbird.core.common.io.KmpParcelize",
        )
    }
}

android {
    namespace = "net.thunderbird.feature.notification.api"
}

compose.resources {
    publicResClass = false
    packageOfResClass = "net.thunderbird.feature.notification.resources"
}
