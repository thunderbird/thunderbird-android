package app.k9mail.autodiscovery.autoconfig

import app.k9mail.autodiscovery.autoconfig.MockAutoconfigFetcher.Companion.RESULT_ONE
import app.k9mail.autodiscovery.autoconfig.MockAutoconfigFetcher.Companion.RESULT_TWO
import app.k9mail.core.common.mail.toUserEmailAddress
import app.k9mail.core.common.net.toDomain
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.extracting
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import okhttp3.HttpUrl.Companion.toHttpUrl

private val IRRELEVANT_EMAIL_ADDRESS = "irrelevant@domain.example".toUserEmailAddress()

class AutoconfigDiscoveryTest {
    private val urlProvider = MockAutoconfigUrlProvider()
    private val autoconfigFetcher = MockAutoconfigFetcher()
    private val discovery = AutoconfigDiscovery(urlProvider, autoconfigFetcher)

    @Test
    fun `AutoconfigFetcher and AutoconfigParser should only be called when AutoDiscoveryRunnable is run`() = runTest {
        val emailAddress = "user@domain.example".toUserEmailAddress()
        val autoconfigUrl = "https://autoconfig.domain.invalid/mail/config-v1.1.xml".toHttpUrl()
        urlProvider.addResult(listOf(autoconfigUrl))
        autoconfigFetcher.addResult(RESULT_ONE)

        val autoDiscoveryRunnables = discovery.initDiscovery(emailAddress)

        assertThat(autoDiscoveryRunnables).hasSize(1)
        assertThat(urlProvider.callArguments).containsExactly("domain.example".toDomain() to emailAddress)
        assertThat(autoconfigFetcher.callCount).isEqualTo(0)

        val discoveryResult = autoDiscoveryRunnables.first().run()

        assertThat(autoconfigFetcher.callArguments).containsExactly(autoconfigUrl to emailAddress)
        assertThat(discoveryResult).isEqualTo(RESULT_ONE)
    }

    @Test
    fun `Two Autoconfig URLs should return two AutoDiscoveryRunnables`() = runTest {
        val urlOne = "https://autoconfig.domain1.invalid/mail/config-v1.1.xml".toHttpUrl()
        val urlTwo = "https://autoconfig.domain2.invalid/mail/config-v1.1.xml".toHttpUrl()

        urlProvider.addResult(listOf(urlOne, urlTwo))
        autoconfigFetcher.apply {
            addResult(RESULT_ONE)
            addResult(RESULT_TWO)
        }

        val autoDiscoveryRunnables = discovery.initDiscovery(IRRELEVANT_EMAIL_ADDRESS)

        assertThat(autoDiscoveryRunnables).hasSize(2)

        val discoveryResultOne = autoDiscoveryRunnables[0].run()

        assertThat(autoconfigFetcher.callArguments).extracting { it.first }.containsExactly(urlOne)
        assertThat(discoveryResultOne).isEqualTo(RESULT_ONE)

        autoconfigFetcher.callArguments.clear()

        val discoveryResultTwo = autoDiscoveryRunnables[1].run()

        assertThat(autoconfigFetcher.callArguments).extracting { it.first }.containsExactly(urlTwo)
        assertThat(discoveryResultTwo).isEqualTo(RESULT_TWO)
    }
}
