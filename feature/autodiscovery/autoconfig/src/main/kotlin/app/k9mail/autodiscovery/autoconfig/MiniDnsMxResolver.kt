package app.k9mail.autodiscovery.autoconfig

import app.k9mail.core.common.net.Domain
import app.k9mail.core.common.net.toDomain
import org.minidns.hla.ResolverApi
import org.minidns.record.MX

class MiniDnsMxResolver : MxResolver {
    override fun lookup(domain: Domain): List<Domain> {
        val result = ResolverApi.INSTANCE.resolve(domain.value, MX::class.java)
        return result.answersOrEmptySet
            .sortedBy { it.priority }
            .map { it.target.toString().toDomain() }
    }
}
