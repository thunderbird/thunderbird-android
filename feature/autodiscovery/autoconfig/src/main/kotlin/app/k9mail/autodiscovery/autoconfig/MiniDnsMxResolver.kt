package app.k9mail.autodiscovery.autoconfig

import org.minidns.hla.ResolverApi
import org.minidns.record.MX

class MiniDnsMxResolver : MxResolver {
    override fun lookup(domain: String): List<String> {
        val result = ResolverApi.INSTANCE.resolve(domain, MX::class.java)
        return result.answersOrEmptySet
            .sortedBy { it.priority }
            .map { it.target.toString() }
    }
}
