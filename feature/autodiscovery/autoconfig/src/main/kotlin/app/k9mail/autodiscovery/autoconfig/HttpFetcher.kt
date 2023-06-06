package app.k9mail.autodiscovery.autoconfig

import okhttp3.HttpUrl

internal interface HttpFetcher {
    suspend fun fetch(url: HttpUrl): HttpFetchResult
}
