@file:JvmName("FeatureFlagProviderCompat")

package net.thunderbird.core.featureflag.compat

import androidx.annotation.Discouraged
import net.thunderbird.core.featureflag.FeatureFlagKey
import net.thunderbird.core.featureflag.FeatureFlagProvider
import net.thunderbird.core.featureflag.FeatureFlagResult
import net.thunderbird.core.featureflag.toFeatureFlagKey

/**
 * Provides a feature flag result based on a string key, primarily for Java compatibility.
 *
 * This function acts as a bridge for Java code to access the Kotlin-idiomatic `provide`
 * function that expects a [FeatureFlagKey], as value classes are not compatible with Java
 * code.
 *
 * **Note:** This function is discouraged for use in Kotlin code. Prefer using the
 * [FeatureFlagProvider.provide(key: FeatureFlagKey)][FeatureFlagProvider.provide]
 * function directly in Kotlin.
 *
 * @receiver The [FeatureFlagProvider] instance to query.
 * @param key The string representation of the feature flag key.
 * @return The [FeatureFlagResult] corresponding to the given key.
 */
@Discouraged(message = "This function should be only used within Java files.")
fun FeatureFlagProvider.provide(key: String): FeatureFlagResult {
    return provide(key.toFeatureFlagKey())
}
