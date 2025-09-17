package net.thunderbird.app.common.feature.mail

import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.feature.account.storage.legacy.mapper.LegacyAccountDataMapper

internal class FakeLegacyAccountDataMapper : LegacyAccountDataMapper {
    var lastMapped: LegacyAccount? = null
    var toDtoResult: LegacyAccountDto? = null

    override fun toDomain(dto: LegacyAccountDto): LegacyAccount = throw UnsupportedOperationException()
    override fun toDto(domain: LegacyAccount): LegacyAccountDto {
        lastMapped = domain
        return toDtoResult ?: error("toDtoResult must be set")
    }
}
