package com.fsck.k9.autodiscovery.srvrecords

import com.fsck.k9.mail.ConnectionSecurity
import org.minidns.dnsname.DnsName
import org.minidns.hla.ResolverApi
import org.minidns.hla.SrvProto

class MiniDnsSrvResolver() : SrvResolver {
    override fun lookup(domain: String, type: SrvType): List<MailService> {
        val result = ResolverApi.INSTANCE.resolveSrv(
            DnsName.from(type.label),
            SrvProto.tcp.dnsName,
            DnsName.from(domain)
        )
        return result.answersOrEmptySet.map {
            MailService(
                type,
                it.target.toString(),
                it.port,
                it.priority,
                if (type.assumeTls)
                    ConnectionSecurity.SSL_TLS_REQUIRED
                else
                    ConnectionSecurity.STARTTLS_REQUIRED
            )
        }
    }
}
