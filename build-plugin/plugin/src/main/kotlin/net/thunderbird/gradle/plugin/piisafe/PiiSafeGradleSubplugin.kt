package net.thunderbird.gradle.plugin.piisafe

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

/**
 * Applies the PII logging K2 compiler plugin (`:library:piisafe:compiler-plugin`) to a
 * Kotlin compilation.
 *
 * Lives here, rather than in a `:library:piisafe:gradle-plugin` project module, because a plugin
 * id is only resolvable via `plugins { id(...) }` from an included build registered in
 * `pluginManagement` (see `settings.gradle.kts`'s `includeBuild("build-plugin")`) — an ordinary
 * subproject can't be applied that way, no matter how it's wired internally.
 *
 * This module is a separate Gradle build from the one it configures, so it can't hold a project
 * dependency on `:library:piisafe:annotations` — the plugin id below is a literal that
 * must be kept in sync with `net.thunderbird.piisafe.annotation.PII_SAFE_PLUGIN_ID`.
 * The dependency substitution in [apply] still resolves correctly against the *consuming* build's
 * project graph, since it configures [target]'s own configurations rather than anything cross-build.
 */
class PiiSafeGradleSubplugin : KotlinCompilerPluginSupportPlugin {
    private companion object {
        const val PLUGIN_ID = "net.thunderbird.piisafe"
    }

    override fun apply(target: Project) {
        target.configurations.configureEach {
            resolutionStrategy.dependencySubstitution {
                substitute(module("net.thunderbird:piisafe-compiler-plugin"))
                    .using(project(":library:piisafe:compiler-plugin"))
            }
        }
    }

    override fun getCompilerPluginId(): String = PLUGIN_ID

    override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
        groupId = "net.thunderbird",
        artifactId = "piisafe-compiler-plugin",
        version = "unspecified",
    )

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> =
        kotlinCompilation.target.project.provider { emptyList() }
}
