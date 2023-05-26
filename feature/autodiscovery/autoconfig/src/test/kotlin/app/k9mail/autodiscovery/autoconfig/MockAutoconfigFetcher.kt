package app.k9mail.autodiscovery.autoconfig

import java.io.InputStream
import okhttp3.HttpUrl

internal class MockAutoconfigFetcher : AutoconfigFetcher {
    val callArguments = mutableListOf<HttpUrl>()

    val callCount: Int
        get() = callArguments.size

    private val results = mutableListOf<InputStream?>()

    fun addResult(data: String?) {
        results.add(data?.byteInputStream())
    }

    override suspend fun fetchAutoconfigFile(url: HttpUrl): InputStream? {
        callArguments.add(url)

        check(results.isNotEmpty()) { "fetchAutoconfigFile($url) called but no result provided" }
        return results.removeAt(0)
    }
}
