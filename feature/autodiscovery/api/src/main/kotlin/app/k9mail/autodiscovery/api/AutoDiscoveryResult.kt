package app.k9mail.autodiscovery.api

/**
 * Results of a mail server settings lookup.
 */
data class AutoDiscoveryResult(
    val incomingServerSettings: IncomingServerSettings,
    val outgoingServerSettings: OutgoingServerSettings,
)

/**
 * Incoming mail server settings.
 *
 * Implementations contain protocol-specific properties.
 */
interface IncomingServerSettings

/**
 * Outgoing mail server settings.
 *
 * Implementations contain protocol-specific properties.
 */
interface OutgoingServerSettings
