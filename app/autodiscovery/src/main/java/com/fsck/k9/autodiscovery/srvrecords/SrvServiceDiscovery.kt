package com.fsck.k9.autodiscovery.srvrecords

import com.fsck.k9.autodiscovery.ConnectionSettings
import com.fsck.k9.autodiscovery.ConnectionSettingsDiscovery
import com.fsck.k9.helper.EmailHelper
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import java.util.Locale
import org.minidns.dnsname.DnsName
import org.minidns.hla.ResolverApi
import org.minidns.hla.SrvProto

enum class SrvType(var protocol: String, var assumeTls: Boolean) {
    SUBMISSION("smtp", false),
    IMAPS("imap", true),
    POP3S("pop", true),
    IMAP("imap", false),
    POP3("pop3", false)
}

data class MailService(
    var srvType: SrvType,
    var host: String,
    var port: Int,
    var priority: Int,
    var security: ConnectionSecurity?
)

fun pickMailService(a: MailService?, b: MailService?): MailService? {
    if (a == null || b == null) {
        return a ?: b
    } else {
        return minOf(a, b, compareBy { it.priority })
    }
}

class SrvServiceDiscovery : ConnectionSettingsDiscovery {

    private var domain: String? = null

    override fun discover(email: String): ConnectionSettings? {
        this.domain = EmailHelper.getDomainFromEmailAddress(email)

        val outgoingService = this.srvLookup(SrvType.SUBMISSION) ?: return null
        val incomingService = pickMailService(
            this.srvLookup(SrvType.IMAPS),
            this.srvLookup(SrvType.POP3S)
        ) ?: pickMailService(
            this.srvLookup(SrvType.IMAP),
            this.srvLookup(SrvType.POP3)
        ) ?: return null

        return ConnectionSettings(
            incoming = ServerSettings(
                incomingService.srvType.protocol,
                incomingService.host,
                incomingService.port,
                incomingService.security,
                AuthType.PLAIN,
                email,
                null,
                null
            ),
            outgoing = ServerSettings(
                outgoingService.srvType.protocol,
                outgoingService.host,
                outgoingService.port,
                outgoingService.security,
                AuthType.PLAIN,
                email,
                null,
                null
            )
        )
    }

    private fun srvLookup(type: SrvType): MailService? {
        val result = ResolverApi.INSTANCE.resolveSrv(
            DnsName.from('_' + type.name.toLowerCase(Locale.ROOT)),
            SrvProto.tcp.dnsName,
            DnsName.from(this.domain)
        )
        val answer = result?.answersOrEmptySet?.minBy { it.priority } ?: return null
        return MailService(
                type,
                answer.target.toString(),
                answer.port,
                answer.priority,
                if (type.assumeTls)
                    ConnectionSecurity.SSL_TLS_REQUIRED
                else
                    ConnectionSecurity.STARTTLS_REQUIRED
        )
    }
}
