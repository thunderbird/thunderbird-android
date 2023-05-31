package app.k9mail.autodiscovery.autoconfig

import app.k9mail.core.common.mail.EmailAddress
import app.k9mail.core.common.net.Domain
import okhttp3.HttpUrl

internal class MockAutoconfigUrlProvider : AutoconfigUrlProvider {
    val callArguments = mutableListOf<Pair<Domain, EmailAddress?>>()

    val callCount: Int
        get() = callArguments.size

    private val results = mutableListOf<List<HttpUrl>>()

    fun addResult(urls: List<HttpUrl>) {
        results.add(urls)
    }

    override fun getAutoconfigUrls(domain: Domain, email: EmailAddress?): List<HttpUrl> {
        callArguments.add(domain to email)

        check(results.isNotEmpty()) { "getAutoconfigUrls($domain, $email) called but no result provided" }
        return results.removeAt(0)
    }
}
