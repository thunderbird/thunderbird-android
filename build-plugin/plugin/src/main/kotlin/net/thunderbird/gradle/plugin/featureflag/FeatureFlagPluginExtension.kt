package net.thunderbird.gradle.plugin.featureflag

import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property

/**
 * Extension for configuring the Feature Flag plugin.
 *
 * This extension allows configuration of feature flag catalog and schema files,
 * along with validation options. It is registered under the name "featureFlag"
 * and should be configured in the root project's build script.
 *
 * @property catalog The feature flag catalog JSON file to be validated.
 * @property schema The JSON schema file used to validate the catalog.
 * @property validateFormats Whether to validate format constraints in the schema. Defaults to true.
 */
abstract class FeatureFlagPluginExtension {
    abstract val catalog: RegularFileProperty
    abstract val schema: RegularFileProperty
    abstract val validateFormats: Property<Boolean>

    /**
     * Validates that required configuration properties are present.
     *
     * @throws GradleException if catalog or schema file properties are not configured
     */
    @Throws(GradleException::class)
    internal fun validate() {
        when {
            !catalog.isPresent -> throw GradleException("Missing Feature flag catalog file")
            !schema.isPresent -> throw GradleException("Missing Feature flag schema file")
        }
    }

    internal companion object {
        /**
         * The extension name used to register the Feature Flag plugin configuration in Gradle.
         */
        const val NAME = "featureFlag"
    }
}
