plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

kotlin {
    android {
        namespace = "net.thunderbird.app.composition"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.file)
            implementation(projects.feature.account.core)
            implementation(projects.feature.mail.message.export.api)
            implementation(projects.feature.mail.message.export.implEml)
        }
    }
}
