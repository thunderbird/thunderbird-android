plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}

fun plugin(provider: Provider<PluginDependency>) = with(provider.get()) {
    "$pluginId:$pluginId.gradle.plugin:$version"
}
