package app.k9mail.autodiscovery.autoconfig

import java.io.InputStream
import okhttp3.HttpUrl

internal interface AutoconfigFetcher {
    suspend fun fetchAutoconfigFile(url: HttpUrl): InputStream?
}
