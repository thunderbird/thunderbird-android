package net.thunderbird.core.featureflag.model

import kotlinx.serialization.Serializable

/**
 * Container for application-specific feature flag overrides.
 *
 * @property k9 the current overrides for K9-Mail; [EmptyAppVariantOverride]
 *  if running inside TfA
 * @property thunderbird the current overrides for Thunderbird for Android;
 *  [EmptyAppVariantOverride] if running inside K-9 mail.
 */
@Serializable
data class FlagRegistryOverride(val k9: AppVariantOverrides, val thunderbird: AppVariantOverrides) {
    operator fun get(key: String): AppVariantOverrides? = when (key) {
        ::k9.name -> k9
        ::thunderbird.name -> thunderbird
        else -> null
    }
}
