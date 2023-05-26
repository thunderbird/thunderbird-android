package app.k9mail.autodiscovery.autoconfig

import app.k9mail.core.common.net.Domain

class MockMxResolver : MxResolver {
    val callArguments = mutableListOf<Domain>()

    val callCount: Int
        get() = callArguments.size

    private val results = mutableListOf<List<Domain>>()

    fun addResult(domain: Domain) {
        results.add(listOf(domain))
    }

    fun addResult(domains: List<Domain>) {
        results.add(domains)
    }

    override fun lookup(domain: Domain): List<Domain> {
        callArguments.add(domain)

        check(results.isNotEmpty()) { "lookup($domain) called but no result provided" }
        return results.removeAt(0)
    }
}
