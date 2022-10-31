package com.fsck.k9.autodiscovery.thunderbird

import java.io.InputStream
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

class ThunderbirdAutoconfigFetcher(private val okHttpClient: OkHttpClient) {

    fun fetchAutoconfigFile(url: HttpUrl): InputStream? {
        val request = Request.Builder().url(url).build()

        val response = okHttpClient.newCall(request).execute()

        return if (response.isSuccessful) {
            response.body?.byteStream()
        } else {
            null
        }
    }
}
