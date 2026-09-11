package net.thunderbird.gradle.plugin.piisafe

import javax.inject.Inject
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

/**
 * Gradle subplugin that integrates the PII Safe Kotlin compiler plugin into the build process.
 *
 * This plugin configures the Kotlin compilation to include the PII Safe compiler plugin, which
 * enforces personally identifiable information (PII) safety checks at compile time. It automatically
 * sets up necessary dependencies and resolution strategies for both the compiler plugin and runtime
 * annotations.
 *
 * Suppressed "unused". This plugin is used by the Gradle build system via plugin declaration defined
 * on build.gradle.kts
 */
@Suppress("unused")
abstract class PiiSafeGradleSubplugin @Inject constructor(
    private val providers: ProviderFactory,
) : KotlinCompilerPluginSupportPlugin {
    override fun apply(target: Project) {
        target.configurations.configureEach {
            resolutionStrategy.dependencySubstitution {
                substitute(module("net.thunderbird:piisafe-compiler-plugin"))
                    .using(project(":library:pii-safe:compiler-plugin"))
            }
        }
    }

    override fun getCompilerPluginId(): String = providers.gradleProperty("tfa.piisafe.compiler.plugin.id").get()

    override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
        groupId = "net.thunderbird",
        artifactId = "piisafe-compiler-plugin",
        version = "unspecified",
    )

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        kotlinCompilation.defaultSourceSet.dependencies {
            implementation(project(":library:pii-safe:annotations"))
        }
        return kotlinCompilation.target.project.provider { emptyList() }
    }
}
