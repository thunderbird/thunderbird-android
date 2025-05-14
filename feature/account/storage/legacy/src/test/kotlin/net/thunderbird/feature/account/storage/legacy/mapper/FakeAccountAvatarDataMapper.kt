package net.thunderbird.feature.account.storage.legacy.mapper

import net.thunderbird.feature.account.profile.AccountAvatar
import net.thunderbird.feature.account.storage.mapper.AccountAvatarDataMapper
import net.thunderbird.feature.account.storage.profile.AvatarDto

class FakeAccountAvatarDataMapper(
    private val dto: AvatarDto,
    private val domain: AccountAvatar,
) : AccountAvatarDataMapper {
    override fun toDomain(dto: AvatarDto): AccountAvatar = domain

    override fun toDto(domain: AccountAvatar): AvatarDto = dto
}
