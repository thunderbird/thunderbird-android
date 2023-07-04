package app.k9mail.autodiscovery.autoconfig

import app.k9mail.core.common.net.Domain
import app.k9mail.core.common.net.toDomain
import org.minidns.hla.ResolverApi
import org.minidns.record.MX

internal class MiniDnsMxResolver : MxResolver {
    override fun lookup(domain: Domain): MxLookupResult {
        val result = ResolverApi.INSTANCE.resolve(domain.value, MX::class.java)

        val mxNames = result.answersOrEmptySet
            .sortedBy { it.priority }
            .map { it.target.toString().toDomain() }

        return MxLookupResult(
            mxNames = mxNames,
            isTrusted = if (result.wasSuccessful()) result.isAuthenticData else false,
        )
    }
}
