package com.fsck.k9.autodiscovery.srvrecords

import com.fsck.k9.autodiscovery.ConnectionSettings
import com.fsck.k9.autodiscovery.ConnectionSettingsDiscovery
import com.fsck.k9.helper.EmailHelper
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import org.minidns.dnsname.DnsName
import org.minidns.hla.ResolverApi
import org.minidns.hla.SrvProto

enum class SrvType(var label: String, var protocol: String, var assumeTls: Boolean) {
    SUBMISSION("_submission", "smtp", false),
    IMAPS("_imaps", "imap", true),
    IMAP("_imap", "imap", false)
}

data class MailService(
    var srvType: SrvType,
    var host: String,
    var port: Int,
    var priority: Int,
    var security: ConnectionSecurity?
)

fun pickMailService(a: MailService?, b: MailService?): MailService? {
    return if (a == null || b == null) {
        a ?: b
    } else {
        minOf(a, b, compareBy { it.priority })
    }
}

interface SrvResolver {
    fun srvLookup(domain: String, type: SrvType): MailService?
}

class SrvServiceDiscovery : ConnectionSettingsDiscovery, SrvResolver {

    override fun discover(email: String): ConnectionSettings? {
        val domain = EmailHelper.getDomainFromEmailAddress(email) ?: return null
        val outgoingService = this.srvLookup(domain, SrvType.SUBMISSION) ?: return null
        val incomingService = pickMailService(
            this.srvLookup(domain, SrvType.IMAPS),
            this.srvLookup(domain, SrvType.IMAP)
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

    override fun srvLookup(domain: String, type: SrvType): MailService? {
        val result = ResolverApi.INSTANCE.resolveSrv(
            DnsName.from(type.label),
            SrvProto.tcp.dnsName,
            DnsName.from(domain)
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
