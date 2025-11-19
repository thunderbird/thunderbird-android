plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.common)
            implementation(projects.core.featureflag)
        }
    }
}

android {
    namespace = "net.thunderbird.feature.mail.message.reader.api"
}
