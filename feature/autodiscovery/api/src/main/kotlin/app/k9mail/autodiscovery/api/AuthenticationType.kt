package app.k9mail.autodiscovery.api

/**
 * The authentication types supported when using the [AutoDiscovery] mechanism.
 *
 * Note: Currently we support the same set of values in [ImapServerSettings] and [SmtpServerSettings]. As soon as this
 * changes, this type should be replaced with `ImapAuthenticationType` and `SmtpAuthenticationType`.
 */
enum class AuthenticationType {
    PasswordCleartext,
    PasswordEncrypted,
    OAuth2,
}
