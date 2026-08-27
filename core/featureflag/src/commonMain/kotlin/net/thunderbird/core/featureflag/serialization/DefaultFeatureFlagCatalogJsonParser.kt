package net.thunderbird.core.featureflag.serialization

import kotlinx.serialization.json.Json
import net.thunderbird.core.featureflag.model.FeatureFlagCatalog

/**
 * [FeatureFlagCatalogJsonParser] backed by kotlinx.serialization.
 */
internal class DefaultFeatureFlagCatalogJsonParser(
    private val json: Json,
) : FeatureFlagCatalogJsonParser {
    override fun decodeFromString(rawJson: String): FeatureFlagCatalog = json.decodeFromString(rawJson)
}
