package com.fsck.k9.autodiscovery

import com.fsck.k9.helper.EmailHelper
import java.io.InputStream
import java.net.URL
import java.net.URLEncoder

class ThunderbirdAutoconfigFetcher() {

    fun fetchAutoconfigFile(email: String): InputStream? {
        val url = URL(getAutodiscoveryAddress(email))

        return url.openConnection().inputStream
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
