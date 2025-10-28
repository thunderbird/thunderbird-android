plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.feature.mail.message.reader.api)
        }
    }
}

android {
    namespace = "net.thunderbird.feature.mail.message.reader.impl"
}
