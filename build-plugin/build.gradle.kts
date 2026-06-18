plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))

    implementation(plugin(libs.plugins.kotlin.jvm))
    implementation(plugin(libs.plugins.kotlin.multiplatform))
    implementation(plugin(libs.plugins.kotlin.parcelize))
    implementation(plugin(libs.plugins.kotlin.serialization))

    implementation(plugin(libs.plugins.android.application))
    implementation(plugin(libs.plugins.android.library))

    implementation(plugin(libs.plugins.compose))

    implementation(plugin(libs.plugins.jetbrains.compose))

    implementation(plugin(libs.plugins.dependency.check))
    implementation(plugin(libs.plugins.detekt))
    implementation(plugin(libs.plugins.spotless))
    implementation(plugin(libs.plugins.kover))

    // Make custom plugins in ":plugin" available to precompiled convention plugins by classpath
    implementation(project(":plugin"))

    compileOnly(libs.android.tools.common)
}

kotlin {
    compilerOptions {
        allWarningsAsErrors = true
    }
}

private fun plugin(provider: Provider<PluginDependency>) = with(provider.get()) {
    "$pluginId:$pluginId.gradle.plugin:$version"
}
