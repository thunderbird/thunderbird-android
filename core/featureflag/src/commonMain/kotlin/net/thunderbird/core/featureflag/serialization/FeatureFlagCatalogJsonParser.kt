package net.thunderbird.core.featureflag.serialization

import kotlinx.serialization.SerializationException
import net.thunderbird.core.featureflag.model.FeatureFlagCatalog

/**
 * Parser for deserializing feature flag catalog JSON data.
 */
fun interface FeatureFlagCatalogJsonParser {
    /**
     * Decodes a JSON string into a FeatureFlagCatalog object.
     *
     * @param rawJson JSON string representation of the feature flag catalog.
     * @return Deserialized FeatureFlagCatalog instance containing version, flags, and overrides.
     * @throws SerializationException in case of any decoding-specific error
     * @throws IllegalArgumentException if the decoded input is not a valid instance of [FeatureFlagCatalog]
     */
    fun decodeFromString(rawJson: String): FeatureFlagCatalog
}
