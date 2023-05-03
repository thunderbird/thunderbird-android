package app.k9mail.autodiscovery.autoconfig

import com.fsck.k9.helper.EmailHelper
import okhttp3.HttpUrl

class ProviderAutoconfigUrlProvider(private val config: AutoconfigUrlConfig) : AutoconfigUrlProvider {
    override fun getAutoconfigUrls(email: String): List<HttpUrl> {
        val domain = EmailHelper.getDomainFromEmailAddress(email)
        requireNotNull(domain) { "Couldn't extract domain from email address: $email" }

        return buildList {
            add(createProviderUrl(domain, email, useHttps = true))
            add(createDomainUrl(domain, email, useHttps = true))

            if (!config.httpsOnly) {
                add(createProviderUrl(domain, email, useHttps = false))
                add(createDomainUrl(domain, email, useHttps = false))
            }
        }
    }

    private fun createProviderUrl(domain: String, email: String, useHttps: Boolean): HttpUrl {
        // https://autoconfig.{domain}/mail/config-v1.1.xml?emailaddress={email}
        // http://autoconfig.{domain}/mail/config-v1.1.xml?emailaddress={email}
        return HttpUrl.Builder()
            .scheme(if (useHttps) "https" else "http")
            .host("autoconfig.$domain")
            .addEncodedPathSegments("mail/config-v1.1.xml")
            .apply {
                if (config.includeEmailAddress) {
                    addQueryParameter("emailaddress", email)
                }
            }
            .build()
    }

    private fun createDomainUrl(domain: String, email: String, useHttps: Boolean): HttpUrl {
        // https://{domain}/.well-known/autoconfig/mail/config-v1.1.xml?emailaddress={email}
        // http://{domain}/.well-known/autoconfig/mail/config-v1.1.xml?emailaddress={email}
        return HttpUrl.Builder()
            .scheme(if (useHttps) "https" else "http")
            .host(domain)
            .addEncodedPathSegments(".well-known/autoconfig/mail/config-v1.1.xml")
            .apply {
                if (config.includeEmailAddress) {
                    addQueryParameter("emailaddress", email)
                }
            }
            .build()
    }
}

data class AutoconfigUrlConfig(
    val httpsOnly: Boolean,
    val includeEmailAddress: Boolean,
)
