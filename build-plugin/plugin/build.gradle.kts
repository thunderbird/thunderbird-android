import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "net.thunderbird.gradle.plugin"

// Configure the build-logic plugins to target JDK 21 similar to the JDK used to build the project.
java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
    }
}

dependencies {
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))

    compileOnly(plugin(libs.plugins.android.application))

    compileOnly(plugin(libs.plugins.kotlin.multiplatform))
    compileOnly(plugin(libs.plugins.kotlin.serialization))

    implementation(plugin(libs.plugins.compose))

    implementation(plugin(libs.plugins.jetbrains.compose))

    implementation(plugin(libs.plugins.dependency.check))
    implementation(plugin(libs.plugins.detekt))
    implementation(plugin(libs.plugins.spotless))

    compileOnly(plugin(libs.plugins.kover))
    implementation(libs.diff.utils)
    compileOnly(libs.kotlinx.datetime)
}

kotlin {
    compilerOptions {
        allWarningsAsErrors = true
    }
}

tasks {
    validatePlugins {
        enableStricterValidation = true
        failOnWarning = true
    }
}

gradlePlugin {
    plugins {
        register("Badging") {
            id = "net.thunderbird.gradle.plugin.app.badging"
            implementationClass = "net.thunderbird.gradle.plugin.app.badging.BadgingPlugin"
        }
        register("Versioning") {
            id = "net.thunderbird.gradle.plugin.app.versioning"
            implementationClass = "net.thunderbird.gradle.plugin.app.versioning.VersioningPlugin"
        }

        register("QualityCodeCoverage") {
            id = "net.thunderbird.gradle.plugin.quality.coverage"
            implementationClass = "net.thunderbird.gradle.plugin.quality.coverage.CodeCoveragePlugin"
        }
    }
}

private fun plugin(provider: Provider<PluginDependency>) = with(provider.get()) {
    "$pluginId:$pluginId.gradle.plugin:$version"
}
