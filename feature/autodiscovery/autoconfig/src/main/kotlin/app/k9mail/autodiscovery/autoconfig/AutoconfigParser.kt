package app.k9mail.autodiscovery.autoconfig

import java.io.InputStream
import net.thunderbird.core.common.mail.EmailAddress

/**
 * Parser for Thunderbird's Autoconfig file format.
 *
 * See [https://github.com/thunderbird/autoconfig](https://github.com/thunderbird/autoconfig)
 */
internal interface AutoconfigParser {
    fun parseSettings(inputStream: InputStream, email: EmailAddress): AutoconfigParserResult
}
