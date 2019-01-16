package com.fsck.k9.autodiscovery.thunderbird

import com.fsck.k9.helper.EmailHelper
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.InputStream
import java.net.URLEncoder

class ThunderbirdAutoconfigFetcher(private val client: OkHttpClient) {

    fun fetchAutoconfigFile(email: String): InputStream? {
        val url = getAutodiscoveryAddress(email)
        val request = Request.Builder().url(url).build()

        return client.newCall(request).execute().body()?.byteStream()
    }

    companion object {
        // address described at:
        // https://developer.mozilla.org/en-US/docs/Mozilla/Thunderbird/Autoconfiguration#Configuration_server_at_ISP
        internal fun getAutodiscoveryAddress(email: String): String {
            val domain = EmailHelper.getDomainFromEmailAddress(email)
            return "https://${domain}/.well-known/autoconfig/mail/config-v1.1.xml?emailaddress=" + URLEncoder.encode(email, "UTF-8")
        }
    }
}
