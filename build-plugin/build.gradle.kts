plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))

    implementation(plugin(libs.plugins.kotlin.jvm))

    implementation(plugin(libs.plugins.spotless))

    implementation(plugin(libs.plugins.kotlin.android))

    implementation(plugin(libs.plugins.android.application))
}

fun plugin(provider: Provider<PluginDependency>) = with(provider.get()) {
    "$pluginId:$pluginId.gradle.plugin:$version"
}
