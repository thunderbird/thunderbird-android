package app.k9mail.autodiscovery.autoconfig

import net.thunderbird.core.common.mail.EmailAddress
import net.thunderbird.core.common.net.Domain
import okhttp3.HttpUrl

internal class PostMxLookupAutoconfigUrlProvider(
    private val ispDbUrlProvider: AutoconfigUrlProvider,
    private val config: AutoconfigUrlConfig,
) : AutoconfigUrlProvider {
    override fun getAutoconfigUrls(domain: Domain, email: EmailAddress?): List<HttpUrl> {
        return buildList {
            add(createProviderUrl(domain, email))
            addAll(ispDbUrlProvider.getAutoconfigUrls(domain, email))
        }
    }

    private fun createProviderUrl(domain: Domain, email: EmailAddress?): HttpUrl {
        // After an MX lookup only the following provider URL is checked:
        // https://autoconfig.{domain}/mail/config-v1.1.xml?emailaddress={email}
        return HttpUrl.Builder()
            .scheme("https")
            .host("autoconfig.${domain.value}")
            .addEncodedPathSegments("mail/config-v1.1.xml")
            .apply {
                if (email != null && config.includeEmailAddress) {
                    addQueryParameter("emailaddress", email.address)
                }
            }
            .build()
    }
}

internal fun createPostMxLookupAutoconfigUrlProvider(config: AutoconfigUrlConfig): AutoconfigUrlProvider {
    return PostMxLookupAutoconfigUrlProvider(
        ispDbUrlProvider = IspDbAutoconfigUrlProvider(),
        config = config,
    )
}
