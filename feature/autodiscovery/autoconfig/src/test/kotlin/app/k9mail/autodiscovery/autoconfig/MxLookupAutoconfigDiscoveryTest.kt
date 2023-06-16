package app.k9mail.autodiscovery.autoconfig

import app.k9mail.autodiscovery.api.AutoDiscoveryResult.NoUsableSettingsFound
import app.k9mail.autodiscovery.autoconfig.MockAutoconfigFetcher.Companion.RESULT_ONE
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

class MxLookupAutoconfigDiscoveryTest {
    private val mxResolver = MockMxResolver()
    private val baseDomainExtractor = OkHttpBaseDomainExtractor()
    private val urlProvider = MockAutoconfigUrlProvider()
    private val autoconfigFetcher = MockAutoconfigFetcher()
    private val discovery = MxLookupAutoconfigDiscovery(
        mxResolver = SuspendableMxResolver(mxResolver),
        baseDomainExtractor = baseDomainExtractor,
        subDomainExtractor = RealSubDomainExtractor(baseDomainExtractor),
        urlProvider = urlProvider,
        autoconfigFetcher = autoconfigFetcher,
    )

    @Test
    fun `AutoconfigUrlProvider should be called with MX base domain`() = runTest {
        val emailAddress = "user@company.example".toUserEmailAddress()
        mxResolver.addResult("mx.emailprovider.example".toDomain())
        urlProvider.addResult(listOf("https://ispdb.invalid/emailprovider.example".toHttpUrl()))
        autoconfigFetcher.addResult(RESULT_ONE)

        val autoDiscoveryRunnables = discovery.initDiscovery(emailAddress)

        assertThat(autoDiscoveryRunnables).hasSize(1)
        assertThat(mxResolver.callCount).isEqualTo(0)
        assertThat(autoconfigFetcher.callCount).isEqualTo(0)

        val discoveryResult = autoDiscoveryRunnables.first().run()

        assertThat(mxResolver.callArguments).containsExactly("company.example".toDomain())
        assertThat(urlProvider.callArguments).extracting { it.first }
            .containsExactly("emailprovider.example".toDomain())
        assertThat(autoconfigFetcher.callCount).isEqualTo(1)
        assertThat(discoveryResult).isEqualTo(RESULT_ONE)
    }

    @Test
    fun `AutoconfigUrlProvider should be called with MX base domain and subdomain`() = runTest {
        val emailAddress = "user@company.example".toUserEmailAddress()
        mxResolver.addResult("mx.something.emailprovider.example".toDomain())
        urlProvider.apply {
            addResult(listOf("https://ispdb.invalid/something.emailprovider.example".toHttpUrl()))
            addResult(listOf("https://ispdb.invalid/emailprovider.example".toHttpUrl()))
        }
        autoconfigFetcher.apply {
            addResult(NoUsableSettingsFound)
            addResult(NoUsableSettingsFound)
        }

        val autoDiscoveryRunnables = discovery.initDiscovery(emailAddress)
        val discoveryResult = autoDiscoveryRunnables.first().run()

        assertThat(urlProvider.callArguments).extracting { it.first }.containsExactly(
            "something.emailprovider.example".toDomain(),
            "emailprovider.example".toDomain(),
        )
        assertThat(autoconfigFetcher.callCount).isEqualTo(2)
        assertThat(discoveryResult).isEqualTo(NoUsableSettingsFound)
    }

    @Test
    fun `skip Autoconfig lookup when MX lookup does not return a result`() = runTest {
        val emailAddress = "user@company.example".toUserEmailAddress()
        mxResolver.addResult(emptyList())

        val autoDiscoveryRunnables = discovery.initDiscovery(emailAddress)
        val discoveryResult = autoDiscoveryRunnables.first().run()

        assertThat(mxResolver.callCount).isEqualTo(1)
        assertThat(urlProvider.callCount).isEqualTo(0)
        assertThat(autoconfigFetcher.callCount).isEqualTo(0)
        assertThat(discoveryResult).isEqualTo(NoUsableSettingsFound)
    }

    @Test
    fun `skip Autoconfig lookup when base domain of MX record is email domain`() = runTest {
        val emailAddress = "user@company.example".toUserEmailAddress()
        mxResolver.addResult("mx.company.example".toDomain())

        val autoDiscoveryRunnables = discovery.initDiscovery(emailAddress)
        val discoveryResult = autoDiscoveryRunnables.first().run()

        assertThat(mxResolver.callCount).isEqualTo(1)
        assertThat(urlProvider.callCount).isEqualTo(0)
        assertThat(autoconfigFetcher.callCount).isEqualTo(0)
        assertThat(discoveryResult).isEqualTo(NoUsableSettingsFound)
    }

    @Test
    fun `isTrusted should be false when MxLookupResult_isTrusted is false`() = runTest {
        val emailAddress = "user@company.example".toUserEmailAddress()
        mxResolver.addResult("mx.emailprovider.example".toDomain(), isTrusted = false)
        urlProvider.addResult(listOf("https://ispdb.invalid/emailprovider.example".toHttpUrl()))
        autoconfigFetcher.addResult(RESULT_ONE.copy(isTrusted = true))

        val autoDiscoveryRunnables = discovery.initDiscovery(emailAddress)
        val discoveryResult = autoDiscoveryRunnables.first().run()

        assertThat(discoveryResult).isEqualTo(RESULT_ONE.copy(isTrusted = false))
    }

    @Test
    fun `isTrusted should be false when AutoDiscoveryResult_isTrusted from AutoconfigFetcher is false`() = runTest {
        val emailAddress = "user@company.example".toUserEmailAddress()
        mxResolver.addResult("mx.emailprovider.example".toDomain(), isTrusted = true)
        urlProvider.addResult(listOf("https://ispdb.invalid/emailprovider.example".toHttpUrl()))
        autoconfigFetcher.addResult(RESULT_ONE.copy(isTrusted = false))

        val autoDiscoveryRunnables = discovery.initDiscovery(emailAddress)
        val discoveryResult = autoDiscoveryRunnables.first().run()

        assertThat(discoveryResult).isEqualTo(RESULT_ONE.copy(isTrusted = false))
    }
}
