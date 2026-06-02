plugins {
    id(ThunderbirdPlugins.Library.kmpCompose)
}

kotlin {
    android {
        namespace = "net.thunderbird.feature.mail.message.reader.impl"
    }
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.common)
            implementation(projects.core.featureflag)
            implementation(projects.core.preference.api)
            implementation(projects.core.ui.contract)
            implementation(projects.core.ui.theme.api)
            implementation(projects.feature.mail.message.reader.api)
        }

        androidMain.dependencies {
            // Ideally temporary; once the message reader is rewritten, we might be able
            // to drop below dependency. Needed because of AttachmentViewInfo.
            implementation(projects.legacy.core)
        }
    }
}

codeCoverage {
    branchCoverage = 0
    lineCoverage = 0
}
