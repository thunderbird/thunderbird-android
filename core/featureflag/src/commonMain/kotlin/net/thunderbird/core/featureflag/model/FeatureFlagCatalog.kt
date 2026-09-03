package net.thunderbird.core.featureflag.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys

/**
 * Represents a complete feature flag configuration catalog.
 *
 * @property version Version identifier of the catalog schema.
 * @property flags The feature flag registrar.
 * @property overrides Application and build-variant specific flag overrides.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonIgnoreUnknownKeys
data class FeatureFlagCatalog(
    val version: String,
    val flags: List<FlagRegistry>,
    @Contextual
    val overrides: FlagRegistryOverride,
)
