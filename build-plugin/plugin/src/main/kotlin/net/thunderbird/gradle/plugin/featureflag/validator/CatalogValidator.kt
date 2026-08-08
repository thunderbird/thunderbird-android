package net.thunderbird.gradle.plugin.featureflag.validator

import net.thunderbird.gradle.plugin.featureflag.FeatureFlagCatalog
import net.thunderbird.gradle.plugin.featureflag.FlagOverrides
import net.thunderbird.gradle.plugin.featureflag.K9Overrides
import net.thunderbird.gradle.plugin.featureflag.ThunderbirdOverrides

/**
 * Maps every key declared on `$.flags` to its default value.
 */
private typealias KeyRegistry = Map<String, Boolean>

/**
 * Validates feature flag catalog configurations by checking override consistency and correctness.
 * @see FeatureFlagCatalog
 */
internal class CatalogValidator {

    /**
     * Validates a feature flag catalog for consistency and correctness.
     *
     * Checks that all flag overrides for both Thunderbird and K-9 Mail reference existing flag keys
     * and do not duplicate default values. Returns aggregated validation errors for both applications
     * if any issues are found.
     *
     * @param catalog The feature flag catalog to validate
     * @return [Result.Success] if all overrides are valid, or [Result.Error.ValidationError] with details
     *  of all validation failures found in the catalog's override configurations
     */
    fun validate(catalog: FeatureFlagCatalog): Result {
        val keyRegistry = catalog.flags.associate { it.key to it.default }
        val tbOverrides = catalog.overrides.thunderbird
        val tbChecks = tbOverrides.checkOverrides(keyRegistry)
        val k9Overrides = catalog.overrides.k9
        val k9Checks = k9Overrides.checkOverrides(keyRegistry)
        val tbPlainErrors = tbChecks.toPlainError("thunderbird")
        val k9PlainErrors = k9Checks.toPlainError("k9")

        return if (tbPlainErrors.isEmpty() && k9PlainErrors.isEmpty()) {
            Result.Success
        } else {
            Result.Error.ValidationError(
                message = listOf(k9PlainErrors, tbPlainErrors)
                    .filter { it.isNotEmpty() }
                    .joinToString(separator = BLOCK_SEPARATOR),
            )
        }
    }

    private fun FlagOverrides.filterOverridesMissingKeyDefinition(keyRegistry: KeyRegistry): List<String> {
        return keys.filterNot { it in keyRegistry }
    }

    private fun FlagOverrides.filterOverridesWithSameValueAsDefault(keyRegistry: KeyRegistry): List<String> {
        return filter { (key, value) -> keyRegistry[key] == value }.keys.toList()
    }

    private fun ThunderbirdOverrides.checkOverrides(keyRegistry: KeyRegistry): Map<String, OverrideErrors> =
        buildMap {
            debug.putIfInvalid(map = this, overrideName = "debug", keyRegistry = keyRegistry)
            daily.putIfInvalid(map = this, overrideName = "daily", keyRegistry = keyRegistry)
            beta.putIfInvalid(map = this, overrideName = "beta", keyRegistry = keyRegistry)
            release.putIfInvalid(map = this, overrideName = "release", keyRegistry = keyRegistry)
        }

    private fun K9Overrides.checkOverrides(keyRegistry: KeyRegistry): Map<String, OverrideErrors> =
        buildMap {
            debug.putIfInvalid(map = this, overrideName = "debug", keyRegistry = keyRegistry)
            release.putIfInvalid(map = this, overrideName = "release", keyRegistry = keyRegistry)
        }

    private fun FlagOverrides.putIfInvalid(
        map: MutableMap<String, OverrideErrors>,
        overrideName: String,
        keyRegistry: KeyRegistry,
    ) {
        val missingKeys = this.filterOverridesMissingKeyDefinition(keyRegistry = keyRegistry)
        val sameValueAsDefaultKeys = this.filterOverridesWithSameValueAsDefault(keyRegistry = keyRegistry)
        if (missingKeys.isNotEmpty() || sameValueAsDefaultKeys.isNotEmpty()) {
            map[overrideName] = OverrideErrors(
                missingKeyDefinition = MissingKeyDefinition(keys = missingKeys),
                overrideWithSameValueAsDefault = OverrideWithSameValueAsDefault(keys = sameValueAsDefaultKeys),
            )
        }
    }

    private fun Map<String, OverrideErrors>.toPlainError(appName: String): String =
        entries.joinToString(separator = BLOCK_SEPARATOR) { (overrideName, errors) ->
            buildString {
                appendLine("overrides.$appName.$overrideName:")
                errors.missingKeyDefinition.keys.forEach { key ->
                    appendLine(
                        "- Key '$key' missing definition at '$.flags'".prependIndent(INDENT),
                    )
                }
                errors.overrideWithSameValueAsDefault.keys.forEach { key ->
                    appendLine(
                        "- Key '$key' has the same value as the default defined at '$.flags'".prependIndent(INDENT),
                    )
                }
            }.trimEnd()
        }

    /**
     * Accumulates the override keys of a single override that have no matching definition on `$.flags`.
     */
    private data class MissingKeyDefinition(val keys: List<String>)

    /**
     * Accumulates the override keys of a single override that have the same value of their definition on `$.flags`.
     */
    private data class OverrideWithSameValueAsDefault(val keys: List<String>)

    /**
     * Accumulates every rule violation of a single override.
     */
    private data class OverrideErrors(
        val missingKeyDefinition: MissingKeyDefinition,
        val overrideWithSameValueAsDefault: OverrideWithSameValueAsDefault,
    )

    sealed interface Result {
        data object Success : Result
        sealed interface Error : Result {
            data class ValidationError(val message: String) : Error
        }
    }

    companion object {
        const val INDENT = "  "
        const val BLOCK_SEPARATOR = "\n\n"
    }
}
