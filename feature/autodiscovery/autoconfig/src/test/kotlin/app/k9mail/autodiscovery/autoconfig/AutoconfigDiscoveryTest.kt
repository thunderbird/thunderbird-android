package app.k9mail.autodiscovery.autoconfig

import app.k9mail.core.common.mail.toEmailAddress
import app.k9mail.core.common.net.toDomain
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.extracting
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import okhttp3.HttpUrl.Companion.toHttpUrl

private val IRRELEVANT_EMAIL_ADDRESS = "irrelevant@domain.example".toEmailAddress()

class AutoconfigDiscoveryTest {
    private val urlProvider = MockAutoconfigUrlProvider()
    private val fetcher = MockHttpFetcher()
    private val parser = MockAutoconfigParser()
    private val discovery = AutoconfigDiscovery(urlProvider, fetcher, SuspendableAutoconfigParser(parser))

    @Test
    fun `AutoconfigFetcher and AutoconfigParser should only be called when AutoDiscoveryRunnable is run`() = runTest {
        val emailAddress = "user@domain.example".toEmailAddress()
        val autoconfigUrl = "https://autoconfig.domain.invalid/mail/config-v1.1.xml".toHttpUrl()
        urlProvider.addResult(listOf(autoconfigUrl))
        fetcher.addResult("data")
        parser.addResult(MockAutoconfigParser.RESULT_ONE)

        val autoDiscoveryRunnables = discovery.initDiscovery(emailAddress)

        assertThat(autoDiscoveryRunnables).hasSize(1)
        assertThat(urlProvider.callArguments).containsExactly("domain.example".toDomain() to emailAddress)
        assertThat(fetcher.callCount).isEqualTo(0)
        assertThat(parser.callCount).isEqualTo(0)

        val discoveryResult = autoDiscoveryRunnables.first().run()

        assertThat(fetcher.callArguments).containsExactly(autoconfigUrl)
        assertThat(parser.callArguments).containsExactly("data" to emailAddress)
        assertThat(discoveryResult).isEqualTo(MockAutoconfigParser.RESULT_ONE)
    }

    @Test
    fun `Two Autoconfig URLs should return two AutoDiscoveryRunnables`() = runTest {
        urlProvider.addResult(
            listOf(
                "https://autoconfig.domain1.invalid/mail/config-v1.1.xml".toHttpUrl(),
                "https://autoconfig.domain2.invalid/mail/config-v1.1.xml".toHttpUrl(),
            ),
        )
        fetcher.apply {
            addResult("data1")
            addResult("data2")
        }
        parser.apply {
            addResult(MockAutoconfigParser.RESULT_ONE)
            addResult(MockAutoconfigParser.RESULT_TWO)
        }

        val autoDiscoveryRunnables = discovery.initDiscovery(IRRELEVANT_EMAIL_ADDRESS)

        assertThat(autoDiscoveryRunnables).hasSize(2)

        val discoveryResultOne = autoDiscoveryRunnables[0].run()

        assertThat(parser.callArguments).extracting { it.first }.containsExactly("data1")
        assertThat(discoveryResultOne).isEqualTo(MockAutoconfigParser.RESULT_ONE)

        parser.callArguments.clear()

        val discoveryResultTwo = autoDiscoveryRunnables[1].run()

        assertThat(parser.callArguments).extracting { it.first }.containsExactly("data2")
        assertThat(discoveryResultTwo).isEqualTo(MockAutoconfigParser.RESULT_TWO)
    }
}
