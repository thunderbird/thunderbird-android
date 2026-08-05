package net.thunderbird.core.featureflag.serialization

import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import net.thunderbird.core.featureflag.model.FeatureFlagCatalog
import net.thunderbird.core.featureflag.model.FlagRegistryOverride

/**
 * Parser for deserializing feature flag catalog JSON data.
 *
 * @param registrySerializer Custom serializer for deserializing FlagRegistryOverride objects
 * with application-specific factory methods.
 */
class FeatureFlagCatalogJsonParser(private val registrySerializer: FlagRegistryOverrideSerializer) {
    private val json = Json {
        serializersModule = SerializersModule {
            contextual(FlagRegistryOverride::class, registrySerializer)
        }

        ignoreUnknownKeys = false
    }

    /**
     * Decodes a JSON string into a FeatureFlagCatalog object.
     *
     * @param rawJson JSON string representation of the feature flag catalog.
     * @return Deserialized FeatureFlagCatalog instance containing version, flags, and overrides.
     */
    fun decodeFromString(rawJson: String): FeatureFlagCatalog = json.decodeFromString(rawJson)
}
