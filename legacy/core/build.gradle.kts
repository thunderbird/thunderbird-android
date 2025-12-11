plugins {
    id(ThunderbirdPlugins.Library.android)
    alias(libs.plugins.kotlin.parcelize)
}

dependencies {
    api(projects.mail.common)
    api(projects.backend.api)
    api(projects.library.htmlCleaner)
    api(projects.core.mail.mailserver)
    api(projects.core.android.common)
    api(projects.core.android.account)
    api(projects.core.preference.impl)
    api(projects.core.android.logging)
    api(projects.core.logging.implFile)
    api(projects.core.logging.implComposite)
    api(projects.core.logging.config)
    api(projects.core.android.network)
    api(projects.core.outcome)
    api(projects.feature.mail.folder.api)
    api(projects.feature.account.storage.legacy)

    api(projects.feature.search.implLegacy)
    api(projects.feature.mail.account.api)
    api(projects.legacy.di)
    api(projects.legacy.mailstore)
    api(projects.legacy.message)
    implementation(projects.feature.notification.api)

    implementation(projects.plugins.openpgpApiLib.openpgpApi)
    implementation(projects.feature.telemetry.api)
    implementation(projects.core.featureflag)
    implementation(projects.core.logging.implComposite)

    api(libs.androidx.annotation)

    implementation(libs.okio)
    implementation(libs.commons.io)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.localbroadcastmanager)
    implementation(libs.jsoup)
    implementation(libs.moshi)
    implementation(libs.timber)
    implementation(libs.mime4j.core)
    implementation(libs.mime4j.dom)
    implementation(libs.uri)
    implementation(projects.feature.navigation.drawer.api)
    implementation(projects.feature.mail.message.list.api)
    implementation(projects.feature.mail.message.reader.api)

    testApi(projects.core.testing)
    testApi(projects.core.android.testing)
    testImplementation(projects.core.logging.testing)
    testImplementation(projects.feature.telemetry.noop)
    testImplementation(projects.mail.testing)
    testImplementation(projects.backend.imap)
    testImplementation(projects.mail.protocols.smtp)
    testImplementation(projects.legacy.storage)
    testImplementation(projects.core.android.common)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlin.reflect)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.jdom2)

    // test fakes
    testImplementation(projects.feature.account.fake)
    testImplementation(projects.feature.notification.testing)
}

android {
    namespace = "com.fsck.k9.core"

    buildFeatures {
        buildConfig = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

codeCoverage {
    branchCoverage.set(41)
    lineCoverage.set(46)
}
