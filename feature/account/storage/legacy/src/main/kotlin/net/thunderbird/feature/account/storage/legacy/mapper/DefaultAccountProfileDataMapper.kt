package net.thunderbird.feature.account.storage.legacy.mapper

import net.thunderbird.feature.account.profile.AccountProfile
import net.thunderbird.feature.account.storage.mapper.AccountProfileDataMapper
import net.thunderbird.feature.account.storage.mapper.AvatarDataMapper
import net.thunderbird.feature.account.storage.profile.ProfileDto

class DefaultAccountProfileDataMapper(
    private val avatarMapper: AvatarDataMapper,
) : AccountProfileDataMapper {
    override fun toDomain(dto: ProfileDto): AccountProfile {
        return AccountProfile(
            id = dto.id,
            name = dto.name,
            color = dto.color,
            avatar = avatarMapper.toDomain(dto.avatar),
        )
    }

    override fun toDto(domain: AccountProfile): ProfileDto {
        return ProfileDto(
            id = domain.id,
            name = domain.name,
            color = domain.color,
            avatar = avatarMapper.toDto(domain.avatar),
        )
    }
}
