plugins {
    `kotlin-dsl`
    `kotlin-dsl-precompiled-script-plugins`
}

dependencies {
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))

    implementation(plugin(libs.plugins.kotlin.jvm))
    implementation(plugin(libs.plugins.kotlin.android))

    implementation(plugin(libs.plugins.android.application))
    implementation(plugin(libs.plugins.android.library))

    implementation(plugin(libs.plugins.spotless))
    implementation(plugin(libs.plugins.detekt))
    implementation(plugin(libs.plugins.dependency.check))

    implementation(libs.diff.utils)
    compileOnly(libs.android.tools.common)

    // This defines the used Kotlin version for all Plugin dependencies
    // and ensures that transitive dependencies are aligned on one version.
    implementation(platform(libs.kotlin.gradle.bom))
}

fun plugin(provider: Provider<PluginDependency>) = with(provider.get()) {
    "$pluginId:$pluginId.gradle.plugin:$version"
}
