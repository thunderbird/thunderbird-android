package app.k9mail.autodiscovery.autoconfig

import app.k9mail.core.common.mail.toEmailAddress
import app.k9mail.core.common.net.toDomain
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.extracting
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import okhttp3.HttpUrl.Companion.toHttpUrl

class MxLookupAutoconfigDiscoveryTest {
    private val mxResolver = MockMxResolver()
    private val baseDomainExtractor = OkHttpBaseDomainExtractor()
    private val urlProvider = MockAutoconfigUrlProvider()
    private val fetcher = MockHttpFetcher()
    private val parser = MockAutoconfigParser()
    private val discovery = MxLookupAutoconfigDiscovery(
        mxResolver = SuspendableMxResolver(mxResolver),
        baseDomainExtractor = baseDomainExtractor,
        subDomainExtractor = RealSubDomainExtractor(baseDomainExtractor),
        urlProvider = urlProvider,
        fetcher = fetcher,
        parser = SuspendableAutoconfigParser(parser),
    )

    @Test
    fun `AutoconfigUrlProvider should be called with MX base domain`() = runTest {
        val emailAddress = "user@company.example".toEmailAddress()
        mxResolver.addResult("mx.emailprovider.example".toDomain())
        urlProvider.addResult(listOf("https://ispdb.invalid/emailprovider.example".toHttpUrl()))
        fetcher.addSuccessResult("data")
        parser.addResult(MockAutoconfigParser.RESULT_ONE)

        val autoDiscoveryRunnables = discovery.initDiscovery(emailAddress)

        assertThat(autoDiscoveryRunnables).hasSize(1)
        assertThat(mxResolver.callCount).isEqualTo(0)
        assertThat(fetcher.callCount).isEqualTo(0)
        assertThat(parser.callCount).isEqualTo(0)

        val discoveryResult = autoDiscoveryRunnables.first().run()

        assertThat(mxResolver.callArguments).containsExactly("company.example".toDomain())
        assertThat(urlProvider.callArguments).extracting { it.first }
            .containsExactly("emailprovider.example".toDomain())
        assertThat(fetcher.callCount).isEqualTo(1)
        assertThat(parser.callCount).isEqualTo(1)
        assertThat(discoveryResult).isEqualTo(MockAutoconfigParser.RESULT_ONE)
    }

    @Test
    fun `AutoconfigUrlProvider should be called with MX base domain and subdomain`() = runTest {
        val emailAddress = "user@company.example".toEmailAddress()
        mxResolver.addResult("mx.something.emailprovider.example".toDomain())
        urlProvider.apply {
            addResult(listOf("https://ispdb.invalid/something.emailprovider.example".toHttpUrl()))
            addResult(listOf("https://ispdb.invalid/emailprovider.example".toHttpUrl()))
        }
        fetcher.apply {
            addErrorResult(404)
            addErrorResult(404)
        }

        val autoDiscoveryRunnables = discovery.initDiscovery(emailAddress)
        val discoveryResult = autoDiscoveryRunnables.first().run()

        assertThat(urlProvider.callArguments).extracting { it.first }.containsExactly(
            "something.emailprovider.example".toDomain(),
            "emailprovider.example".toDomain(),
        )
        assertThat(fetcher.callCount).isEqualTo(2)
        assertThat(parser.callCount).isEqualTo(0)
        assertThat(discoveryResult).isNull()
    }

    @Test
    fun `skip Autoconfig lookup when MX lookup does not return a result`() = runTest {
        val emailAddress = "user@company.example".toEmailAddress()
        mxResolver.addResult(emptyList())

        val autoDiscoveryRunnables = discovery.initDiscovery(emailAddress)
        val discoveryResult = autoDiscoveryRunnables.first().run()

        assertThat(mxResolver.callCount).isEqualTo(1)
        assertThat(urlProvider.callCount).isEqualTo(0)
        assertThat(fetcher.callCount).isEqualTo(0)
        assertThat(parser.callCount).isEqualTo(0)
        assertThat(discoveryResult).isNull()
    }

    @Test
    fun `skip Autoconfig lookup when base domain of MX record is email domain`() = runTest {
        val emailAddress = "user@company.example".toEmailAddress()
        mxResolver.addResult("mx.company.example".toDomain())

        val autoDiscoveryRunnables = discovery.initDiscovery(emailAddress)
        val discoveryResult = autoDiscoveryRunnables.first().run()

        assertThat(mxResolver.callCount).isEqualTo(1)
        assertThat(urlProvider.callCount).isEqualTo(0)
        assertThat(fetcher.callCount).isEqualTo(0)
        assertThat(parser.callCount).isEqualTo(0)
        assertThat(discoveryResult).isNull()
    }
}
