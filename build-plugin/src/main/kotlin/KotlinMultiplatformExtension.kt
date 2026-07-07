import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.named
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

val NamedDomainObjectContainer<KotlinSourceSet>.androidHostTest: NamedDomainObjectProvider<KotlinSourceSet>
    get() = this.named<KotlinSourceSet>("androidHostTest")

/**
 * Creates a dependency with the given configuration.
 *
 * This is a workaround for kotlin-multiplatform's implementation() function not supporting excludes when
 * declaring dependencies from a version catalog.
 *
 * Example:
 *
 * ```kotlin
 * implementationWithExcludes(libs.foo.bar) {
 *     exclude(group = "org.foo.bar", module = "dependency")
 * }
 * ```
 *
 * @param dependency the dependency to create
 * @param configure the configuration to apply to the dependency
 */
fun KotlinDependencyHandler.implementationWithExcludes(
    dependency: Provider<MinimalExternalModuleDependency>,
    configure: ExternalModuleDependency.() -> Unit,
) {
    val copy = dependency.get().copy()
    copy.configure()
    implementation(copy)
}
