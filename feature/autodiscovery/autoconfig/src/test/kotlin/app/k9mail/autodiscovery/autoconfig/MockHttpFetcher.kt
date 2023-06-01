package app.k9mail.autodiscovery.autoconfig

import okhttp3.HttpUrl

internal class MockHttpFetcher : HttpFetcher {
    val callArguments = mutableListOf<HttpUrl>()

    val callCount: Int
        get() = callArguments.size

    private val results = mutableListOf<HttpFetchResult>()

    fun addSuccessResult(data: String) {
        val result = HttpFetchResult.SuccessResponse(
            inputStream = data.byteInputStream(),
        )

        results.add(result)
    }

    fun addErrorResult(code: Int) {
        results.add(HttpFetchResult.ErrorResponse(code))
    }

    override suspend fun fetch(url: HttpUrl): HttpFetchResult {
        callArguments.add(url)

        check(results.isNotEmpty()) { "MockHttpFetcher.fetch($url) called but no result provided" }
        return results.removeAt(0)
    }
}
