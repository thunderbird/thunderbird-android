package com.fsck.k9.autodiscovery.srvrecords

import com.fsck.k9.mail.ConnectionSecurity.SSL_TLS_REQUIRED
import com.fsck.k9.mail.ConnectionSecurity.STARTTLS_REQUIRED
import org.minidns.dnslabel.DnsLabel
import org.minidns.dnsname.DnsName
import org.minidns.hla.ResolverApi
import org.minidns.hla.srv.SrvProto

class MiniDnsSrvResolver : SrvResolver {
    override fun lookup(domain: String, type: SrvType): List<MailService> {
        val result = ResolverApi.INSTANCE.resolveSrv(
            DnsLabel.from(type.label),
            SrvProto.tcp.dnsLabel,
            DnsName.from(domain)
        )

        val security = if (type.assumeTls) SSL_TLS_REQUIRED else STARTTLS_REQUIRED
        return result.answersOrEmptySet.map {
            MailService(
                srvType = type,
                host = it.target.toString(),
                port = it.port,
                priority = it.priority,
                security = security
            )
        }
    }
}
