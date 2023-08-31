package app.k9mail.feature.account.oauth.domain.usecase

import app.k9mail.core.common.mail.Protocols
import app.k9mail.feature.account.oauth.domain.AccountOAuthDomainContract.UseCase

@Deprecated("This is not needed anymore, remove once auth setup flow is updated")
class SuggestServerName : UseCase.SuggestServerName {
    override fun suggest(protocol: String, domain: String): String = when (protocol) {
        Protocols.IMAP -> "imap.$domain"
        Protocols.SMTP -> "smtp.$domain"
        Protocols.POP3 -> "pop3.$domain"
        else -> throw AssertionError("Missed case: $protocol")
    }
}
