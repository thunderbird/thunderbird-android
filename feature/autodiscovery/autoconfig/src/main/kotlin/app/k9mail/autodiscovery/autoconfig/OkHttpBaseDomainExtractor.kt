package app.k9mail.autodiscovery.autoconfig

import net.thunderbird.core.common.net.Domain
import net.thunderbird.core.common.net.toDomain
import okhttp3.HttpUrl

internal class OkHttpBaseDomainExtractor : BaseDomainExtractor {
    override fun extractBaseDomain(domain: Domain): Domain {
        return domain.value.toHttpUrlOrNull().topPrivateDomain()?.toDomain() ?: domain
    }

    private fun String.toHttpUrlOrNull(): HttpUrl {
        return HttpUrl.Builder()
            .scheme("https")
            .host(this)
            .build()
    }
}
