plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
    alias(libs.plugins.kotlin.parcelize)
}

dependencies {
    api(projects.legacy.ui.base)
    api(projects.core.ui.account)
    api(projects.legacy.ui.folder)
    api(projects.core.ui.legacy.designsystem)

    implementation(projects.legacy.core)
    implementation(projects.feature.mail.account.api)
    implementation(projects.mail.common)
    implementation(projects.uiUtils.toolbarBottomSheet)
    implementation(projects.core.android.contact)

    implementation(projects.core.featureflag)
    implementation(projects.core.logging.api)
    implementation(projects.core.ui.theme.api)
    implementation(projects.feature.launcher)
    implementation(projects.core.common)
    implementation(projects.core.ui.compose.designsystem)
    implementation(projects.feature.navigation.drawer.api)
    implementation(projects.feature.navigation.drawer.dropdown)
    implementation(projects.feature.navigation.drawer.siderail)
    implementation(projects.feature.notification.api)
    // TODO: Remove AccountOauth dependency
    implementation(projects.feature.account.oauth)
    implementation(projects.feature.account.avatar.api)
    implementation(projects.feature.account.avatar.impl)
    implementation(projects.feature.funding.api)
    implementation(projects.feature.search.implLegacy)
    implementation(projects.feature.settings.import)
    implementation(projects.feature.telemetry.api)
    implementation(projects.feature.mail.message.list)
    implementation(projects.feature.mail.message.composer)
    implementation(projects.feature.mail.message.export.api)

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
    implementation(projects.library.tokenAutoComplete)
    implementation(libs.safeContentResolver)
    implementation(libs.searchPreference)
    implementation(libs.fastadapter)
    implementation(libs.fastadapter.extensions.drag)
    implementation(libs.fastadapter.extensions.utils)
    implementation(libs.circleImageView)
    implementation(libs.androidx.work.runtime)

    implementation(libs.commons.io)
    implementation(libs.androidx.core.ktx)
    implementation(libs.jcip.annotations)
    implementation(libs.mime4j.core)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.datetime)
    implementation(libs.uri)

    implementation(libs.glide)
    annotationProcessor(libs.glide.compiler)

    testImplementation(projects.core.logging.testing)
    testImplementation(projects.feature.account.fake)

    // This is necessary as RecipientPresenterTest fails to inject
    testImplementation(projects.legacy.common)
    testImplementation(projects.core.testing)
    testImplementation(projects.core.android.testing)
    testImplementation(projects.mail.testing)
    testImplementation(projects.legacy.storage)
    testImplementation(projects.feature.telemetry.noop)
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

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}
