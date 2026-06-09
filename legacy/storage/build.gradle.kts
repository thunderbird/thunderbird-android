plugins {
    id(ThunderbirdPlugins.Library.android)
}

dependencies {
    api(libs.koin.core)

    implementation(projects.core.logging.api)
    implementation(projects.feature.mail.message.list.api)

    implementation(projects.legacy.core)
    // Required for MigrationTo107
    implementation(projects.mail.common)
    implementation(projects.mail.protocols.imap)

    implementation(libs.androidx.core.ktx)
    implementation(libs.mime4j.core)
    implementation(libs.commons.io)
    implementation(libs.moshi)

    testImplementation(projects.core.logging.testing)
    testImplementation(projects.mail.testing)
    testImplementation(projects.feature.telemetry.noop)
    testImplementation(projects.core.featureflag)

    testImplementation(libs.robolectric)
    testImplementation(libs.commons.io)
    testImplementation(libs.mockito.kotlin)
}

android {
    namespace = "com.fsck.k9.storage"

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

codeCoverage {
    branchCoverage = 45
    lineCoverage = 67
}
