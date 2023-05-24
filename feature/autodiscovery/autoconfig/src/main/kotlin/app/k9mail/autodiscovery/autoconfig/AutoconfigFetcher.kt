package app.k9mail.autodiscovery.autoconfig

import com.fsck.k9.logging.Timber
import java.io.IOException
import java.io.InputStream
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

internal class AutoconfigFetcher(private val okHttpClient: OkHttpClient) {

    suspend fun fetchAutoconfigFile(url: HttpUrl): InputStream? {
        return try {
            performHttpRequest(url)
        } catch (e: IOException) {
            Timber.d(e, "Error fetching URL: %s", url)
            null
        }
    }

    private suspend fun performHttpRequest(url: HttpUrl): InputStream? {
        return suspendCancellableCoroutine { cancellableContinuation ->
            val request = Request.Builder()
                .url(url)
                .build()

            val call = okHttpClient.newCall(request)

            cancellableContinuation.invokeOnCancellation {
                call.cancel()
            }

            val responseCallback = object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    cancellableContinuation.cancel(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        val inputStream = response.body?.byteStream()
                        cancellableContinuation.resume(inputStream)
                    } else {
                        // We don't care about the body of error responses.
                        response.close()
                        cancellableContinuation.resume(null)
                    }
                }
            }

            call.enqueue(responseCallback)
        }
    }
}
