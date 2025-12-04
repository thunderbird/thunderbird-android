package net.thunderbird.feature.account.storage.legacy.mapper

import net.thunderbird.feature.account.avatar.Avatar
import net.thunderbird.feature.account.storage.mapper.AvatarDataMapper
import net.thunderbird.feature.account.storage.profile.AvatarDto

class FakeAvatarDataMapper(
    private val dto: AvatarDto,
    private val domain: Avatar,
) : AvatarDataMapper {
    override fun toDomain(dto: AvatarDto): Avatar = domain

    override fun toDto(domain: Avatar): AvatarDto = dto
}
