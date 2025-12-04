plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

kotlin {
    androidLibrary {
        namespace = "net.thunderbird.feature.mail.message.export.eml"
        withHostTest {}
    }
    sourceSets {
        commonMain.dependencies {
            api(projects.feature.mail.message.export.api)
            implementation(projects.core.outcome)
            implementation(projects.core.file)

            implementation(libs.kotlinx.io.core)
            implementation(libs.uri)
        }
    }
}
