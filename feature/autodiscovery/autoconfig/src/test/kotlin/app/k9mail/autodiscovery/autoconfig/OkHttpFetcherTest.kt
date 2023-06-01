package app.k9mail.autodiscovery.autoconfig

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlin.test.assertNotNull
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

        val inputStream = fetcher.fetch(nonExistentUrl)

        assertThat(inputStream).isNull()
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

        val inputStream = fetcher.fetch(url)

        assertNotNull(inputStream) { inputStream ->
            assertThat(inputStream.available()).isEqualTo(0)
        }
    }
}
