package net.thunderbird.core.featureflag.provider

/**
 * Metadata describing a feature flag provider implementation.
 *
 * Provides identifying information about the provider, primarily its name which
 * distinguishes different provider implementations (e.g., "bundled-catalog", "remote").
 */
interface ProviderMetadata {
    val name: String
}

/**
 * Metadata for catalog-based feature flag providers.
 *
 * Identifies providers that load feature flags from a catalog data source,
 * such as bundled or remote catalog implementations.
 *
 * @property name The provider's identifying name (e.g., "bundled-catalog", "remote-catalog").
 */
internal data class CatalogProviderMetadata(
    override val name: String,
) : ProviderMetadata
