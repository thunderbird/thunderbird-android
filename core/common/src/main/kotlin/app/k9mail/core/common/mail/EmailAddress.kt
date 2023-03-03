package app.k9mail.core.common.mail

@JvmInline
value class EmailAddress(val address: String) {
    init {
        require(address.isNotBlank()) { "Email address must not be blank" }
    }
}
