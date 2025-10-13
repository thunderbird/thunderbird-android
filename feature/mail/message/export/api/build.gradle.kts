plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
    }
}

android {
    namespace = "net.thunderbird.feature.mail.message.export"
}
