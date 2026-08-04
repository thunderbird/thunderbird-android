package net.thunderbird.gradle.plugin.featureflag.task

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property

/**
 * Extension configuration for generating feature flag key enums.
 *
 * Configures the output location, package name, and base name for the generated
 * enum classes that contain feature flag keys from a catalog file.
 */
abstract class FeatureFlagKeyEnumsExtension {
    /**
     * The directory where generated feature flag key enum files will be written.
     *
     * Defaults to `build/generated/featureflags/src/commonMain/kotlin` if not explicitly configured.
     */
    abstract val outputDirectory: DirectoryProperty

    /**
     * The package name for generated feature flag key enum classes.
     *
     * Defaults to `net.thunderbird.core.featureflag.keys` if not explicitly configured.
     */
    abstract val featureFlagKeysPackageName: Property<String>

    /**
     * The base name used for generated feature flag key enum classes.
     *
     * Defaults to `GeneratedFeatureFlagKey` if not explicitly configured.
     */
    abstract val featureFlagKeyEnumBaseName: Property<String>

    init {
        featureFlagKeyEnumBaseName.convention("GeneratedFeatureFlagKey")
    }
}
