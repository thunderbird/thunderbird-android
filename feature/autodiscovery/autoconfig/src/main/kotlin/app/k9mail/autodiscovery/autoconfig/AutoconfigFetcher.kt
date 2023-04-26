package app.k9mail.autodiscovery.autoconfig

import com.fsck.k9.logging.Timber
import java.io.IOException
import java.io.InputStream
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

class AutoconfigFetcher(private val okHttpClient: OkHttpClient) {

    fun fetchAutoconfigFile(url: HttpUrl): InputStream? {
        return try {
            val request = Request.Builder().url(url).build()

            val response = okHttpClient.newCall(request).execute()

            if (response.isSuccessful) {
                response.body?.byteStream()
            } else {
                null
            }
        } catch (e: IOException) {
            Timber.d(e, "Error fetching URL: %s", url)
            null
        }
    }
}
