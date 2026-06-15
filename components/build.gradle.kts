plugins {
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.multiplatform.library) apply false
    alias(libs.plugins.compose) apply false
    alias(libs.plugins.jetbrains.compose) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kover) apply false
}

val tbMobileComponentsVersion = libs.versions.tbMobileComponents.get()

subprojects {
    group = "net.thunderbird.components.ui"
    version = tbMobileComponentsVersion
}
