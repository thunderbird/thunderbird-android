package app.k9mail.autodiscovery.autoconfig

import app.k9mail.core.common.mail.EmailAddress
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible

internal class SuspendableAutoconfigParser(private val autoconfigParser: AutoconfigParser) {
    suspend fun parseSettings(inputStream: InputStream, email: EmailAddress): AutoconfigParserResult {
        return runInterruptible(Dispatchers.IO) {
            autoconfigParser.parseSettings(inputStream, email)
        }
    }
}
