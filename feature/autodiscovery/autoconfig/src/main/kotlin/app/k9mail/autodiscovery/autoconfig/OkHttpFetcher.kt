package app.k9mail.autodiscovery.autoconfig

import java.io.IOException
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request.Builder
import okhttp3.Response

internal class OkHttpFetcher(
    private val okHttpClient: OkHttpClient,
) : HttpFetcher {

    override suspend fun fetch(url: HttpUrl): HttpFetchResult {
        return suspendCancellableCoroutine { cancellableContinuation ->
            val request = Builder()
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
                        val result = HttpFetchResult.SuccessResponse(
                            inputStream = response.body.byteStream(),
                            isTrusted = response.isTrusted(),
                        )
                        cancellableContinuation.resume(result)
                    } else {
                        // We don't care about the body of error responses.
                        response.close()

                        val result = HttpFetchResult.ErrorResponse(response.code)
                        cancellableContinuation.resume(result)
                    }
                }
            }

            call.enqueue(responseCallback)
        }
    }

    private tailrec fun Response.isTrusted(): Boolean {
        val priorResponse = priorResponse
        return when {
            !request.isHttps -> false
            priorResponse == null -> true
            else -> priorResponse.isTrusted()
        }
    }
}
