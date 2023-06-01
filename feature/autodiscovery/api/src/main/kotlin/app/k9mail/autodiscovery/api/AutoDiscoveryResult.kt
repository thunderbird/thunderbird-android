package app.k9mail.autodiscovery.api

import java.io.IOException

/**
 * Results of a mail server settings lookup.
 */
sealed interface AutoDiscoveryResult {
    /**
     * Mail server settings found during the lookup.
     */
    data class Settings(
        val incomingServerSettings: IncomingServerSettings,
        val outgoingServerSettings: OutgoingServerSettings,
    ) : AutoDiscoveryResult

    /**
     * No usable mail server settings were found.
     */
    object NoUsableSettingsFound : AutoDiscoveryResult

    /**
     * A network error occurred while looking for mail server settings.
     */
    data class NetworkError(val exception: IOException) : AutoDiscoveryResult

    /**
     * Encountered an unexpected exception when looking up mail server settings.
     */
    data class UnexpectedException(val exception: Exception) : AutoDiscoveryResult
}

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
