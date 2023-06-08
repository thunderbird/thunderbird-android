package app.k9mail.autodiscovery.autoconfig

import app.k9mail.autodiscovery.api.IncomingServerSettings
import app.k9mail.autodiscovery.api.OutgoingServerSettings

/**
 * Result type for [AutoconfigParser].
 */
internal sealed interface AutoconfigParserResult {
    /**
     * Server settings extracted from the Autoconfig XML.
     */
    data class Settings(
        val incomingServerSettings: IncomingServerSettings,
        val outgoingServerSettings: OutgoingServerSettings,
    ) : AutoconfigParserResult

    /**
     * Server settings couldn't be extracted.
     */
    data class ParserError(val error: AutoconfigParserException) : AutoconfigParserResult
}
