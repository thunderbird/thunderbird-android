plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

kotlin {
    android {
        namespace = "net.thunderbird.app.composition"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.common)
            implementation(projects.core.configstore.api)
            implementation(projects.core.configstore.implBackend)
            implementation(projects.core.file)
            implementation(projects.core.logging.api)
            implementation(projects.core.logging.internal)
            implementation(libs.tb.mobile.components.core.logging.file)
            implementation(libs.filekit.core)
            implementation(projects.feature.account.core)
            implementation(projects.feature.mail.message.export.api)
            implementation(projects.feature.mail.message.export.implEml)
            implementation(projects.feature.mail.message.reader.impl)
            implementation(projects.feature.notification.impl)
        }
    }
}
