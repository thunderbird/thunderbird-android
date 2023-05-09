package app.k9mail.autodiscovery.autoconfig

import okhttp3.HttpUrl

class ProviderAutoconfigUrlProvider(private val config: AutoconfigUrlConfig) : AutoconfigUrlProvider {
    override fun getAutoconfigUrls(domain: String, email: String?): List<HttpUrl> {
        return buildList {
            add(createProviderUrl(domain, email, useHttps = true))
            add(createDomainUrl(domain, email, useHttps = true))

            if (!config.httpsOnly) {
                add(createProviderUrl(domain, email, useHttps = false))
                add(createDomainUrl(domain, email, useHttps = false))
            }
        }
    }

    private fun createProviderUrl(domain: String, email: String?, useHttps: Boolean): HttpUrl {
        // https://autoconfig.{domain}/mail/config-v1.1.xml?emailaddress={email}
        // http://autoconfig.{domain}/mail/config-v1.1.xml?emailaddress={email}
        return HttpUrl.Builder()
            .scheme(if (useHttps) "https" else "http")
            .host("autoconfig.$domain")
            .addEncodedPathSegments("mail/config-v1.1.xml")
            .apply {
                if (email != null && config.includeEmailAddress) {
                    addQueryParameter("emailaddress", email)
                }
            }
            .build()
    }

    private fun createDomainUrl(domain: String, email: String?, useHttps: Boolean): HttpUrl {
        // https://{domain}/.well-known/autoconfig/mail/config-v1.1.xml?emailaddress={email}
        // http://{domain}/.well-known/autoconfig/mail/config-v1.1.xml?emailaddress={email}
        return HttpUrl.Builder()
            .scheme(if (useHttps) "https" else "http")
            .host(domain)
            .addEncodedPathSegments(".well-known/autoconfig/mail/config-v1.1.xml")
            .apply {
                if (email != null && config.includeEmailAddress) {
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
