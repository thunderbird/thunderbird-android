package net.thunderbird.core.featureflag.data.configstore

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import net.thunderbird.core.featureflag.model.RemoteCatalogFetchFrequency

@Serializable
@ConsistentCopyVisibility
data class RemoteCatalogConfig internal constructor(
    val enabled: Boolean = true,
    val fetchFrequency: RemoteCatalogFetchFrequency = RemoteCatalogFetchFrequency.Default,
    val lastFetch: LocalDateTime? = null,
    val cacheMetadata: RemoteCatalogCacheMetadata? = null,
)

@Serializable
@ConsistentCopyVisibility
data class RemoteCatalogCacheMetadata internal constructor(
    val eTag: String?,
    val lastModified: String?,
    val contentLength: Long?,
)
