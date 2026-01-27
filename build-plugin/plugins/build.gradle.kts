import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
}

group = "net.thunderbird.gradle.plugin"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    compileOnly(plugin(libs.plugins.kover))
}

gradlePlugin {
    plugins {
        register("QualityCodeCoverage") {
            id = "net.thunderbird.gradle.plugin.quality.coverage"
            implementationClass = "net.thunderbird.gradle.plugin.quality.coverage.CodeCoveragePlugin"
        }
    }
}

private fun plugin(provider: Provider<PluginDependency>) = with(provider.get()) {
    "$pluginId:$pluginId.gradle.plugin:$version"
}
