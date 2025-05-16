package app.k9mail.autodiscovery.autoconfig

import app.k9mail.autodiscovery.api.AutoDiscoveryResult.NoUsableSettingsFound
import app.k9mail.autodiscovery.autoconfig.MockAutoconfigFetcher.Companion.RESULT_ONE
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.common.mail.toUserEmailAddress
import net.thunderbird.core.common.net.toDomain

class MxLookupAutoconfigDiscoveryTest {
    private val mxResolver = MockMxResolver()
    private val baseDomainExtractor = OkHttpBaseDomainExtractor()
    private val urlProvider = createPostMxLookupAutoconfigUrlProvider(
        AutoconfigUrlConfig(
            httpsOnly = true,
            includeEmailAddress = true,
        ),
    )
    private val autoconfigFetcher = MockAutoconfigFetcher()
    private val discovery = MxLookupAutoconfigDiscovery(
        mxResolver = SuspendableMxResolver(mxResolver),
        baseDomainExtractor = baseDomainExtractor,
        subDomainExtractor = RealSubDomainExtractor(baseDomainExtractor),
        urlProvider = urlProvider,
        autoconfigFetcher = autoconfigFetcher,
    )

    @Test
    fun `result from email provider should be used if available`() = runTest {
        val emailAddress = "user@company.example".toUserEmailAddress()
        mxResolver.addResult("mx.emailprovider.example".toDomain())
        autoconfigFetcher.apply {
            addResult(RESULT_ONE)
        }

        val autoDiscoveryRunnables = discovery.initDiscovery(emailAddress)

        assertThat(autoDiscoveryRunnables).hasSize(1)
        assertThat(mxResolver.callCount).isEqualTo(0)
        assertThat(autoconfigFetcher.callCount).isEqualTo(0)

        val discoveryResult = autoDiscoveryRunnables.first().run()

        assertThat(autoconfigFetcher.urls).containsExactly(
            "https://autoconfig.emailprovider.example/mail/config-v1.1.xml?emailaddress=user%40company.example",
        )
        assertThat(discoveryResult).isEqualTo(RESULT_ONE)
    }

    @Test
    fun `result from ISPDB should be used if config is not available at email provider`() = runTest {
        val emailAddress = "user@company.example".toUserEmailAddress()
        mxResolver.addResult("mx.emailprovider.example".toDomain())
        autoconfigFetcher.apply {
            addResult(NoUsableSettingsFound)
            addResult(RESULT_ONE)
        }

        val autoDiscoveryRunnables = discovery.initDiscovery(emailAddress)

        assertThat(autoDiscoveryRunnables).hasSize(1)
        assertThat(mxResolver.callCount).isEqualTo(0)
        assertThat(autoconfigFetcher.callCount).isEqualTo(0)

        val discoveryResult = autoDiscoveryRunnables.first().run()

        assertThat(autoconfigFetcher.urls).containsExactly(
            "https://autoconfig.emailprovider.example/mail/config-v1.1.xml?emailaddress=user%40company.example",
            "https://autoconfig.thunderbird.net/v1.1/emailprovider.example",
        )
        assertThat(discoveryResult).isEqualTo(RESULT_ONE)
    }

    @Test
    fun `base domain and subdomain should be extracted from MX host if possible`() = runTest {
        val emailAddress = "user@company.example".toUserEmailAddress()
        mxResolver.addResult("mx.something.emailprovider.example".toDomain())
        autoconfigFetcher.apply {
            addResult(NoUsableSettingsFound)
            addResult(NoUsableSettingsFound)
            addResult(NoUsableSettingsFound)
            addResult(NoUsableSettingsFound)
        }

        val autoDiscoveryRunnables = discovery.initDiscovery(emailAddress)
        val discoveryResult = autoDiscoveryRunnables.first().run()

        assertThat(autoconfigFetcher.urls).containsExactly(
            "https://autoconfig.something.emailprovider.example/mail/config-v1.1.xml" +
                "?emailaddress=user%40company.example",
            "https://autoconfig.thunderbird.net/v1.1/something.emailprovider.example",
            "https://autoconfig.emailprovider.example/mail/config-v1.1.xml?emailaddress=user%40company.example",
            "https://autoconfig.thunderbird.net/v1.1/emailprovider.example",
        )
        assertThat(discoveryResult).isEqualTo(NoUsableSettingsFound)
    }

    @Test
    fun `skip Autoconfig lookup when MX lookup does not return a result`() = runTest {
        val emailAddress = "user@company.example".toUserEmailAddress()
        mxResolver.addResult(emptyList())

        val autoDiscoveryRunnables = discovery.initDiscovery(emailAddress)
        val discoveryResult = autoDiscoveryRunnables.first().run()

        assertThat(mxResolver.callCount).isEqualTo(1)
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
        assertThat(autoconfigFetcher.callCount).isEqualTo(0)
        assertThat(discoveryResult).isEqualTo(NoUsableSettingsFound)
    }

    @Test
    fun `isTrusted should be false when MxLookupResult_isTrusted is false`() = runTest {
        val emailAddress = "user@company.example".toUserEmailAddress()
        mxResolver.addResult("mx.emailprovider.example".toDomain(), isTrusted = false)
        autoconfigFetcher.addResult(RESULT_ONE.copy(isTrusted = true))

        val autoDiscoveryRunnables = discovery.initDiscovery(emailAddress)
        val discoveryResult = autoDiscoveryRunnables.first().run()

        assertThat(discoveryResult).isEqualTo(RESULT_ONE.copy(isTrusted = false))
    }

    @Test
    fun `isTrusted should be false when AutoDiscoveryResult_isTrusted from AutoconfigFetcher is false`() = runTest {
        val emailAddress = "user@company.example".toUserEmailAddress()
        mxResolver.addResult("mx.emailprovider.example".toDomain(), isTrusted = true)
        autoconfigFetcher.addResult(RESULT_ONE.copy(isTrusted = false))

        val autoDiscoveryRunnables = discovery.initDiscovery(emailAddress)
        val discoveryResult = autoDiscoveryRunnables.first().run()

        assertThat(discoveryResult).isEqualTo(RESULT_ONE.copy(isTrusted = false))
    }
}
