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
    SUBMISSIONS("_submissions", "smtp", true),
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

interface SrvResolution {
    fun lookup(domain: String, type: SrvType): List<MailService>
}

class SrvResolver() : SrvResolution {
    override fun lookup(domain: String, type: SrvType): List<MailService> {
        val result = ResolverApi.INSTANCE.resolveSrv(
            DnsName.from(type.label),
            SrvProto.tcp.dnsName,
            DnsName.from(domain)
        )
        return result?.answersOrEmptySet?.map {
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
        } ?: listOf()
    }
}

class SrvServiceDiscovery(
    private val srvResolver: SrvResolver
) : ConnectionSettingsDiscovery {
    override fun discover(email: String): ConnectionSettings? {
        val domain = EmailHelper.getDomainFromEmailAddress(email) ?: return null
        val pickMailService = compareBy<MailService> { it.priority }.thenByDescending { it.security }

        val submissionServices =
            this.srvResolver.lookup(domain, SrvType.SUBMISSIONS).plus(
            this.srvResolver.lookup(domain, SrvType.SUBMISSION))
        val imapServices =
            this.srvResolver.lookup(domain, SrvType.IMAPS).plus(
            this.srvResolver.lookup(domain, SrvType.IMAP))

        val outgoingService = submissionServices.minWith(pickMailService) ?: return null
        val incomingService = imapServices.minWith(pickMailService) ?: return null

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
}
