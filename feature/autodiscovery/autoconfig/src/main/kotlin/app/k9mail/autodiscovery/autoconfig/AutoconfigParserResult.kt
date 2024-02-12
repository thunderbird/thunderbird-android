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
        val incomingServerSettings: List<IncomingServerSettings>,
        val outgoingServerSettings: List<OutgoingServerSettings>,
    ) : AutoconfigParserResult {
        init {
            require(incomingServerSettings.isNotEmpty())
            require(outgoingServerSettings.isNotEmpty())
        }
    }

    /**
     * Server settings couldn't be extracted.
     */
    data class ParserError(val error: AutoconfigParserException) : AutoconfigParserResult
}
