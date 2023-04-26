package app.k9mail.autodiscovery.srvrecords

import app.k9mail.autodiscovery.api.ConnectionSettingsDiscovery
import app.k9mail.autodiscovery.api.DiscoveredServerSettings
import app.k9mail.autodiscovery.api.DiscoveryResults
import com.fsck.k9.helper.EmailHelper
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity

class SrvServiceDiscovery(
    private val srvResolver: MiniDnsSrvResolver,
) : ConnectionSettingsDiscovery {

    override fun discover(email: String): DiscoveryResults? {
        val domain = EmailHelper.getDomainFromEmailAddress(email) ?: return null
        val mailServicePriority = compareBy<MailService> { it.priority }.thenByDescending { it.security }

        val outgoingSettings = listOf(SrvType.SUBMISSIONS, SrvType.SUBMISSION)
            .flatMap { srvResolver.lookup(domain, it) }
            .sortedWith(mailServicePriority)
            .map { newServerSettings(it, email) }

        val incomingSettings = listOf(SrvType.IMAPS, SrvType.IMAP)
            .flatMap { srvResolver.lookup(domain, it) }
            .sortedWith(mailServicePriority)
            .map { newServerSettings(it, email) }

        return DiscoveryResults(incoming = incomingSettings, outgoing = outgoingSettings)
    }
}

fun newServerSettings(service: MailService, email: String): DiscoveredServerSettings {
    return DiscoveredServerSettings(
        service.srvType.protocol,
        service.host,
        service.port,
        service.security,
        AuthType.PLAIN,
        email,
    )
}

enum class SrvType(val label: String, val protocol: String, val assumeTls: Boolean) {
    SUBMISSIONS("_submissions", "smtp", true),
    SUBMISSION("_submission", "smtp", false),
    IMAPS("_imaps", "imap", true),
    IMAP("_imap", "imap", false),
}

data class MailService(
    val srvType: SrvType,
    val host: String,
    val port: Int,
    val priority: Int,
    val security: ConnectionSecurity,
)
