plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

kotlin {
    androidLibrary {
        namespace = "net.thunderbird.feature.mail.message.reader.impl"
    }
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.common)
            implementation(projects.core.featureflag)
            implementation(projects.core.preference.api)
            implementation(projects.core.ui.theme.api)
            implementation(projects.feature.mail.message.reader.api)
        }
    }
}
