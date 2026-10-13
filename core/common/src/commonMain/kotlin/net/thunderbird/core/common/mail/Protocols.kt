package net.thunderbird.core.common.mail

object Protocols {
    const val IMAP = "imap"
    const val POP3 = "pop3"
    const val SMTP = "smtp"
    const val GRAPH = "msgraphapi"
    const val GRAPH_LEGACY = "graph"
    const val GMAIL_API = "gmailapi"
    const val ZOHO_MAIL_API = "zohomailapi"

    fun isGraphProtocol(type: String): Boolean = type == GRAPH || type == GRAPH_LEGACY

    fun isNativeApiProtocol(type: String): Boolean =
        isGraphProtocol(type) || type == GMAIL_API || type == ZOHO_MAIL_API
}
