package net.thunderbird.feature.account.storage.legacy.mapper

import net.thunderbird.feature.account.avatar.Avatar
import net.thunderbird.feature.account.storage.mapper.AccountAvatarDataMapper
import net.thunderbird.feature.account.storage.profile.AvatarDto

class FakeAccountAvatarDataMapper(
    private val dto: AvatarDto,
    private val domain: Avatar,
) : AccountAvatarDataMapper {
    override fun toDomain(dto: AvatarDto): Avatar = domain

    override fun toDto(domain: Avatar): AvatarDto = dto
}
