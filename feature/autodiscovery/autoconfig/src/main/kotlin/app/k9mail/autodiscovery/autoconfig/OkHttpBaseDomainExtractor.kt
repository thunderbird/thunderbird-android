package app.k9mail.autodiscovery.autoconfig

import okhttp3.HttpUrl

class OkHttpBaseDomainExtractor : BaseDomainExtractor {
    override fun extractBaseDomain(domain: String): String {
        return domain.toHttpUrlOrNull().topPrivateDomain() ?: domain
    }

    private fun String.toHttpUrlOrNull(): HttpUrl {
        return HttpUrl.Builder()
            .scheme("https")
            .host(this)
            .build()
    }
}
