plugins {
    id(ThunderbirdPlugins.Library.jvm)
    alias(libs.plugins.buildconfig)
}

description = "PII logging K2 compiler plugin: FIR errors, and toString() override generation for " +
    "data classes annotated with net.thunderbird.piisafe.annotations.Pii."

kotlin {
    explicitApi()
}

dependencies {
    implementation(projects.library.piiSafe.annotations)
    compileOnly(libs.kotlin.compiler)

    testImplementation(libs.kotlin.compiler)
    testImplementation(libs.kotlin.compile.testing)
}

buildConfig {
    packageName("net.thunderbird.piisafe.compiler.plugin.buildconfig")
    val piiSafePluginId = providers.gradleProperty("tfa.piisafe.compiler.plugin.id").get()
    buildConfigField(name = "PII_SAFE_PLUGIN_ID", value = piiSafePluginId)
    val piiSafePluginEnabled = try {
        providers
            .gradleProperty("tfa.piisafe.compiler.plugin.enabled")
            .get()
            .toBooleanStrict()
    } catch (e: IllegalArgumentException) {
        throw GradleException(
            "Invalid value assigned to tfa.piisafe.compiler.plugin.enabled property. " +
                "Check your gradle.properties file. \nReason: ${e.message}",
        )
    }
    buildConfigField(name = "PII_SAFE_PLUGIN_ENABLED", value = piiSafePluginEnabled)
}
