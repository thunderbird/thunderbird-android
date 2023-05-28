package app.k9mail.core.common.mail

class EmailAddressParserException internal constructor(
    message: String,
    val error: EmailAddressParserError,
    val input: String,
    val position: Int,
) : RuntimeException(message)
