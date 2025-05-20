plugins {
    id(ThunderbirdPlugins.Library.android)
}

dependencies {
    implementation(projects.legacy.ui.legacy)
    implementation(projects.legacy.core)
    implementation(projects.legacy.storage)
    implementation(projects.legacy.cryptoOpenpgp)
    implementation(projects.backend.imap)
    implementation(projects.backend.pop3)

    implementation(projects.core.featureflag)
    implementation(projects.feature.launcher)

    implementation(projects.feature.account.setup)
    implementation(projects.feature.account.edit)
    implementation(projects.feature.navigation.drawer.api)
    implementation(projects.feature.settings.import)

    implementation(projects.feature.widget.unread)
    implementation(projects.feature.widget.messageList)

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.preferencex)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.appauth)

    implementation(libs.glide)
    annotationProcessor(libs.glide.compiler)

    if (project.hasProperty("k9mail.enableLeakCanary") && project.property("k9mail.enableLeakCanary") == "true") {
        debugImplementation(libs.leakcanary.android)
    }

    testImplementation(projects.core.logging.testing)
    testImplementation(libs.robolectric)
    testImplementation(projects.feature.account.fake)
}

android {
    namespace = "com.fsck.k9.common"
}
