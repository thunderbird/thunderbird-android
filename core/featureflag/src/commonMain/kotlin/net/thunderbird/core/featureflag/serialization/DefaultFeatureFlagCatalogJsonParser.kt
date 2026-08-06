package net.thunderbird.core.featureflag.serialization

import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import net.thunderbird.core.featureflag.model.FeatureFlagCatalog
import net.thunderbird.core.featureflag.model.FlagRegistryOverride

/**
 * [FeatureFlagCatalogJsonParser] backed by kotlinx.serialization.
 *
 * @param registrySerializer Custom serializer for deserializing FlagRegistryOverride objects
 * with application-specific factory methods.
 */
internal class DefaultFeatureFlagCatalogJsonParser(
    private val registrySerializer: FlagRegistryOverrideSerializer,
) : FeatureFlagCatalogJsonParser {
    private val json = Json {
        serializersModule = SerializersModule {
            contextual(FlagRegistryOverride::class, registrySerializer)
        }

        ignoreUnknownKeys = false
    }

    override fun decodeFromString(rawJson: String): FeatureFlagCatalog = json.decodeFromString(rawJson)
}
