package app.k9mail.autodiscovery.autoconfig

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlin.test.assertNotNull
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Test

class AutoconfigFetcherTest {
    private val fetcher = AutoconfigFetcher(OkHttpClient.Builder().build())

    @Test
    fun shouldHandleNonexistentUrl() {
        val nonExistentUrl =
            "https://autoconfig.domain.invalid/mail/config-v1.1.xml?emailaddress=test%40domain.example".toHttpUrl()

        val inputStream = fetcher.fetchAutoconfigFile(nonExistentUrl)

        assertThat(inputStream).isNull()
    }

    @Test
    fun shouldHandleEmptyResponse() {
        val server = MockWebServer().apply {
            this.enqueue(
                MockResponse()
                    .setBody("")
                    .setResponseCode(204),
            )
            start()
        }
        val url = server.url("/empty/")

        val inputStream = fetcher.fetchAutoconfigFile(url)

        assertNotNull(inputStream) { inputStream ->
            assertThat(inputStream.available()).isEqualTo(0)
        }
    }
}
