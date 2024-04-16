plugins {
    id(ThunderbirdPlugins.Library.android)
}

dependencies {
    implementation(projects.app.ui.legacy)
    implementation(projects.app.ui.messageListWidget)
    implementation(projects.app.core)
    implementation(projects.app.storage)
    implementation(projects.app.cryptoOpenpgp)
    implementation(projects.backend.imap)
    implementation(projects.backend.pop3)

    implementation(projects.core.featureflags)
    implementation(projects.feature.launcher)

    implementation(projects.feature.account.setup)
    implementation(projects.feature.account.edit)
    implementation(projects.feature.settings.import)

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

    buildTypes {
        debug {
            manifestPlaceholders["appAuthRedirectScheme"] = "FIXME: override this in your app project"
        }
        release {
            manifestPlaceholders["appAuthRedirectScheme"] = "FIXME: override this in your app project"
        }
    }
}
