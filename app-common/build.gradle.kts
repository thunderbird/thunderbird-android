plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
}

android {
    namespace = "net.thunderbird.app.common"

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    api(projects.legacy.common)
    api(projects.legacy.ui.legacy)

    api(projects.feature.account.core)
    api(projects.feature.launcher)
    api(projects.feature.navigation.drawer.api)

    implementation(projects.legacy.core)
    implementation(projects.core.android.account)

    implementation(projects.core.logging.api)
    implementation(projects.core.logging.implComposite)
    implementation(projects.core.logging.implConsole)
    implementation(projects.core.logging.implLegacy)
    implementation(projects.core.logging.implFile)

    implementation(projects.core.configstore.api)
    implementation(projects.core.configstore.implBackend)

    implementation(projects.core.featureflag)
    implementation(projects.core.file)

    implementation(projects.core.ui.setting.api)
    implementation(projects.core.ui.setting.implDialog)
    implementation(projects.core.ui.legacy.theme2.common)

    implementation(projects.feature.account.avatar.api)
    implementation(projects.feature.account.avatar.impl)
    implementation(projects.feature.account.setup)
    implementation(projects.feature.mail.account.api)
    implementation(projects.feature.mail.message.composer)
    implementation(projects.feature.migration.provider)
    implementation(projects.feature.notification.api)
    implementation(projects.feature.notification.impl)
    implementation(projects.feature.widget.messageList)

    implementation(projects.feature.mail.message.export.api)
    implementation(projects.feature.mail.message.export.implEml)
    implementation(projects.feature.mail.message.reader.api)
    implementation(projects.feature.mail.message.reader.impl)

    implementation(projects.mail.protocols.imap)
    implementation(projects.backend.imap)

    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.kotlinx.collections.immutable)

    testImplementation(projects.feature.account.fake)
    testImplementation(projects.core.testing)
}

codeCoverage {
    branchCoverage.set(9)
    lineCoverage.set(25)
}
