package com.fsck.k9.autodiscovery.srvrecords

import com.fsck.k9.autodiscovery.ConnectionSettings
import com.fsck.k9.autodiscovery.ConnectionSettingsDiscovery
import com.fsck.k9.helper.EmailHelper
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings

enum class SrvType(val label: String, val protocol: String, val assumeTls: Boolean) {
    SUBMISSIONS("_submissions", "smtp", true),
    SUBMISSION("_submission", "smtp", false),
    IMAPS("_imaps", "imap", true),
    IMAP("_imap", "imap", false)
}

data class MailService(
    val srvType: SrvType,
    val host: String,
    val port: Int,
    val priority: Int,
    val security: ConnectionSecurity?
)

interface SrvResolver {
    fun lookup(domain: String, type: SrvType): List<MailService>
}

class SrvServiceDiscovery(
    private val srvResolver: MiniDnsSrvResolver
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
