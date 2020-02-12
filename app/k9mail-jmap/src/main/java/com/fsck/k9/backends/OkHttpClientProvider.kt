package com.fsck.k9.backends

import okhttp3.OkHttpClient

class OkHttpClientProvider {
    private var okHttpClient: OkHttpClient? = null

    @Synchronized
    fun getOkHttpClient(): OkHttpClient {
        return okHttpClient ?: createOkHttpClient().also { okHttpClient = it }
    }

    private fun createOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder().build()
    }
}
