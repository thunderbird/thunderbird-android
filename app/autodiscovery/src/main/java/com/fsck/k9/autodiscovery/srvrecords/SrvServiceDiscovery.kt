package com.fsck.k9.autodiscovery.srvrecords

import com.fsck.k9.autodiscovery.ConnectionSettings
import com.fsck.k9.autodiscovery.ConnectionSettingsDiscovery
import com.fsck.k9.helper.EmailHelper
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings

class SrvServiceDiscovery(
    private val srvResolver: MiniDnsSrvResolver
) : ConnectionSettingsDiscovery {
    override fun discover(email: String): ConnectionSettings? {
        val domain = EmailHelper.getDomainFromEmailAddress(email) ?: return null
        val pickMailService = compareBy<MailService> { it.priority }.thenByDescending { it.security }

        val outgoingService = listOf(SrvType.SUBMISSIONS, SrvType.SUBMISSION).flatMap { srvResolver.lookup(domain, it) }
            .minWith(pickMailService) ?: return null
        val incomingService = listOf(SrvType.IMAPS, SrvType.IMAP).flatMap { srvResolver.lookup(domain, it) }
            .minWith(pickMailService) ?: return null

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
