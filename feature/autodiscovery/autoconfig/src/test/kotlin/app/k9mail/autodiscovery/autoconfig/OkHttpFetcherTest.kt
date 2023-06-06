package app.k9mail.autodiscovery.autoconfig

import assertk.all
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import java.net.UnknownHostException
import kotlinx.coroutines.test.runTest
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Test

class OkHttpFetcherTest {
    private val fetcher = OkHttpFetcher(OkHttpClient.Builder().build())

    @Test
    fun shouldHandleNonexistentUrl() = runTest {
        val nonExistentUrl =
            "https://autoconfig.domain.invalid/mail/config-v1.1.xml?emailaddress=test%40domain.example".toHttpUrl()

        assertFailure {
            fetcher.fetch(nonExistentUrl)
        }.isInstanceOf<UnknownHostException>()
    }

    @Test
    fun shouldHandleEmptyResponse() = runTest {
        val server = MockWebServer().apply {
            this.enqueue(
                MockResponse()
                    .setBody("")
                    .setResponseCode(204),
            )
            start()
        }
        val url = server.url("/empty/")

        val result = fetcher.fetch(url)

        assertThat(result).isInstanceOf<HttpFetchResult.SuccessResponse>().all {
            prop(HttpFetchResult.SuccessResponse::inputStream).transform { it.available() }.isEqualTo(0)
        }
    }
}
