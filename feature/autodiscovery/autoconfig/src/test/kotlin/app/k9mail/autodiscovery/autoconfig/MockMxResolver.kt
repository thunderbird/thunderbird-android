package app.k9mail.autodiscovery.autoconfig

import net.thunderbird.core.common.net.Domain

class MockMxResolver : MxResolver {
    val callArguments = mutableListOf<Domain>()

    val callCount: Int
        get() = callArguments.size

    private val results = mutableListOf<MxLookupResult>()

    fun addResult(domain: Domain, isTrusted: Boolean = true) {
        results.add(MxLookupResult(mxNames = listOf(domain), isTrusted = isTrusted))
    }

    fun addResult(domains: List<Domain>) {
        results.add(MxLookupResult(mxNames = domains, isTrusted = true))
    }

    override fun lookup(domain: Domain): MxLookupResult {
        callArguments.add(domain)

        check(results.isNotEmpty()) { "lookup($domain) called but no result provided" }
        return results.removeAt(0)
    }
}
