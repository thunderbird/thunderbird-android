package net.thunderbird.core.featureflag.data

import kotlinx.io.IOException

class RemoteCatalogException(
    val code: Code,
    override val message: String? = null,
    override val cause: Throwable? = null,
) : IOException(message, cause) {
    enum class Code {
        RemoteCatalogUserDisabled,
        CantWriteCacheFile,
        CantReadCacheFile,
    }
}
