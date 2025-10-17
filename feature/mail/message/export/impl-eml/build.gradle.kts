plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

kotlin {
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

android {
    namespace = "net.thunderbird.feature.mail.message.export.eml"
}
