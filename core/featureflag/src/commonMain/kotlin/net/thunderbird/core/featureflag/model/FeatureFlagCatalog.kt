package net.thunderbird.core.featureflag.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

/**
 * Represents a complete feature flag configuration catalog.
 *
 * @property version Version identifier of the catalog schema.
 * @property flags The feature flag registrar.
 * @property overrides Application and build-variant specific flag overrides.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class FeatureFlagCatalog(
    val version: String,
    val flags: List<FlagRegistry>,
    val overrides: FlagRegistryOverride,
)
