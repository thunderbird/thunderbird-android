package app.k9mail.autodiscovery.autoconfig

import app.k9mail.core.common.net.Domain
import app.k9mail.core.common.net.toDomainOrNull
import org.minidns.hla.DnssecResolverApi
import org.minidns.record.MX

internal class MiniDnsMxResolver : MxResolver {
    override fun lookup(domain: Domain): MxLookupResult {
        val result = DnssecResolverApi.INSTANCE.resolve(domain.value, MX::class.java)

        val mxNames = result.answersOrEmptySet
            .sortedBy { it.priority }
            .mapNotNull { it.target.toString().toDomainOrNull() }

        return MxLookupResult(
            mxNames = mxNames,
            isTrusted = if (result.wasSuccessful()) result.isAuthenticData else false,
        )
    }
}
