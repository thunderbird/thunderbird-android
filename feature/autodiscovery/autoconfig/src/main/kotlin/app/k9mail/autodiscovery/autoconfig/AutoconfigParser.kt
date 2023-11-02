package app.k9mail.autodiscovery.autoconfig

import app.k9mail.core.common.mail.EmailAddress
import java.io.InputStream

/**
 * Parser for Thunderbird's Autoconfig file format.
 *
 * See [https://github.com/thunderbird/autoconfig](https://github.com/thunderbird/autoconfig)
 */
internal interface AutoconfigParser {
    fun parseSettings(inputStream: InputStream, email: EmailAddress): AutoconfigParserResult
}
