package app.k9mail.autodiscovery.autoconfig

import app.k9mail.autodiscovery.api.AutoDiscoveryResult
import app.k9mail.autodiscovery.autoconfig.AutoconfigParserResult.ParserError
import app.k9mail.autodiscovery.autoconfig.AutoconfigParserResult.Settings
import app.k9mail.autodiscovery.autoconfig.HttpFetchResult.ErrorResponse
import app.k9mail.autodiscovery.autoconfig.HttpFetchResult.SuccessResponse
import app.k9mail.core.common.mail.EmailAddress
import com.fsck.k9.logging.Timber
import java.io.IOException
import java.io.InputStream
import okhttp3.HttpUrl

internal class RealAutoconfigFetcher(
    private val fetcher: HttpFetcher,
    private val parser: SuspendableAutoconfigParser,
) : AutoconfigFetcher {
    override suspend fun fetchAutoconfig(autoconfigUrl: HttpUrl, email: EmailAddress): AutoDiscoveryResult? {
        return try {
            when (val fetchResult = fetcher.fetch(autoconfigUrl)) {
                is SuccessResponse -> {
                    fetchResult.inputStream.use { inputStream ->
                        parseSettings(inputStream, email, autoconfigUrl)
                    }
                }
                is ErrorResponse -> null
            }
        } catch (e: IOException) {
            Timber.d(e, "Error fetching Autoconfig from URL: %s", autoconfigUrl)
            null
        }
    }

    private suspend fun parseSettings(
        inputStream: InputStream,
        email: EmailAddress,
        autoconfigUrl: HttpUrl,
    ): AutoDiscoveryResult? {
        return try {
            return when (val parserResult = parser.parseSettings(inputStream, email)) {
                is Settings -> {
                    AutoDiscoveryResult(
                        incomingServerSettings = parserResult.incomingServerSettings,
                        outgoingServerSettings = parserResult.outgoingServerSettings,
                    )
                }
                is ParserError -> null
            }
        } catch (e: AutoconfigParserException) {
            Timber.d(e, "Failed to parse config from URL: %s", autoconfigUrl)
            null
        }
    }
}
