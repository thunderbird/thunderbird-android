plugins {
    id(ThunderbirdPlugins.Library.android)
    alias(libs.plugins.kotlin.parcelize)
}

dependencies {
    api(projects.app.ui.base)
    implementation(projects.app.core)
    implementation(projects.mail.common)
    implementation(projects.uiUtils.toolbarBottomSheet)

    implementation(projects.core.featureflags)
    implementation(projects.feature.launcher)
    // TODO: Remove AccountOauth dependency
    implementation(projects.feature.account.oauth)
    implementation(projects.feature.settings.import)

    compileOnly(projects.mail.protocols.imap)

    implementation(projects.plugins.openpgpApiLib.openpgpApi)

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.preference)
    implementation(libs.preferencex)
    implementation(libs.preferencex.datetimepicker)
    implementation(libs.preferencex.colorpicker)
    implementation(libs.androidx.recyclerview)
    implementation(projects.uiUtils.linearLayoutManager)
    implementation(projects.uiUtils.itemTouchHelper)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.localbroadcastmanager)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.ckchangelog.core)
    implementation(libs.tokenautocomplete)
    implementation(libs.safeContentResolver)
    implementation(libs.materialdrawer)
    implementation(libs.searchPreference)
    implementation(libs.fastadapter)
    implementation(libs.fastadapter.extensions.drag)
    implementation(libs.fastadapter.extensions.utils)
    implementation(libs.circleImageView)
    implementation(libs.androidx.work.runtime)

    implementation(libs.commons.io)
    implementation(libs.androidx.core.ktx)
    implementation(libs.jcip.annotations)
    implementation(libs.timber)
    implementation(libs.mime4j.core)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.glide)
    annotationProcessor(libs.glide.compiler)

    // This is necessary as RecipientPresenterTest fails to inject
    testImplementation(projects.app.common)
    testImplementation(projects.core.testing)
    testImplementation(projects.mail.testing)
    testImplementation(projects.app.storage)
    testImplementation(projects.app.testing)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}

android {
    namespace = "com.fsck.k9.ui"

    buildFeatures {
        buildConfig = true
    }
}
