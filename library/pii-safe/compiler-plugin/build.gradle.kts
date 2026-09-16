import java.util.Properties
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id(ThunderbirdPlugins.Library.jvm)
    alias(libs.plugins.buildconfig)
}

description = "Pii-safe K2 compiler plugin: FIR errors, and toString() override generation for " +
    "data classes annotated with net.thunderbird.piisafe.annotations.Pii."

kotlin {
    explicitApi()

    sourceSets {
        // Registered as a Kotlin source dir purely so the IDE treats these kotlin-compile-testing
        // fixtures as real, classpath-aware Kotlin (autocomplete, syntax highlighting, navigation)
        // instead of plain text.
        test {
            kotlin.srcDir("src/test/resources/fixtures")
        }
    }
}

tasks.named<KotlinCompile>("compileTestKotlin") {
    // Remove fixture source as they are not part of the production code and to avoid class
    // redeclaration at build.
    exclude("*.fixture.kt")
}

dependencies {
    implementation(projects.library.piiSafe.annotations)
    compileOnly(libs.kotlin.compiler)

    testImplementation(libs.kotlin.compiler)
    testImplementation(libs.kotlin.compile.testing)
    // For fixtures
    testImplementation(projects.core.logging.api)
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
    @Suppress("UnstableApiUsage")
    val localPropertiesFile = isolated.rootProject.projectDirectory.file("local.properties").asFile
    val localProperties = Properties().apply {
        if (localPropertiesFile.exists()) {
            localPropertiesFile.reader().use {
                load(it)
            }
        }
    }

    val dumpIrOnTests = localProperties.getProperty("logger.k2_compiler_plugin.tests.dump_ir.enabled")
    val irKotlinLike = localProperties.getProperty("logger.k2_compiler_plugin.tests.dump_ir.kotlin_like")
    buildConfigField(name = "DUMP_IR_ON_TESTS_ENABLED", value = dumpIrOnTests?.toBoolean() ?: false)
    buildConfigField(name = "DUMP_IR_ON_TESTS_KOTLIN_LIKE", value = irKotlinLike?.toBoolean() ?: false)
}
