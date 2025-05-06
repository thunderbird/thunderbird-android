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

    implementation(projects.core.featureflags)
    implementation(projects.feature.launcher)

    implementation(projects.feature.account.setup)
    implementation(projects.feature.account.edit)
    implementation(projects.feature.navigation.drawer.api)
    implementation(projects.feature.settings.import)

    implementation(projects.feature.widget.unread)
    implementation(projects.feature.widget.messageList)

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.work.runtime)
    implementation(libs.preferencex)
    implementation(libs.timber)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.appauth)

    implementation(libs.glide)
    annotationProcessor(libs.glide.compiler)

    if (project.hasProperty("k9mail.enableLeakCanary") && project.property("k9mail.enableLeakCanary") == "true") {
        debugImplementation(libs.leakcanary.android)
    }

    // Required for DependencyInjectionTest to be able to resolve OpenPgpApiManager
    testImplementation(projects.plugins.openpgpApiLib.openpgpApi)

    testImplementation(libs.robolectric)
}

android {
    namespace = "com.fsck.k9.common"
}
