package net.thunderbird.feature.account.storage.legacy.mapper

import net.thunderbird.feature.account.avatar.Avatar
import net.thunderbird.feature.account.storage.mapper.AccountAvatarDataMapper
import net.thunderbird.feature.account.storage.profile.AvatarDto
import net.thunderbird.feature.account.storage.profile.AvatarTypeDto

class DefaultAccountAvatarDataMapper : AccountAvatarDataMapper {

    override fun toDomain(dto: AvatarDto): Avatar {
        return when (dto.avatarType) {
            AvatarTypeDto.MONOGRAM -> Avatar.Monogram(
                value = dto.avatarMonogram ?: DEFAULT_MONOGRAM,
            )

            AvatarTypeDto.IMAGE -> {
                val uri = dto.avatarImageUri

                if (uri.isNullOrEmpty()) {
                    Avatar.Monogram(
                        value = DEFAULT_MONOGRAM,
                    )
                } else {
                    Avatar.Image(
                        uri = uri,
                    )
                }
            }

            AvatarTypeDto.ICON -> {
                val name = dto.avatarIconName

                if (name.isNullOrEmpty()) {
                    Avatar.Monogram(
                        value = DEFAULT_MONOGRAM,
                    )
                } else {
                    Avatar.Icon(
                        name = name,
                    )
                }
            }
        }
    }

    override fun toDto(domain: Avatar): AvatarDto {
        return AvatarDto(
            avatarType = when (domain) {
                is Avatar.Monogram -> AvatarTypeDto.MONOGRAM
                is Avatar.Image -> AvatarTypeDto.IMAGE
                is Avatar.Icon -> AvatarTypeDto.ICON
            },
            avatarMonogram = if (domain is Avatar.Monogram) domain.value else null,
            avatarImageUri = if (domain is Avatar.Image) domain.uri else null,
            avatarIconName = if (domain is Avatar.Icon) domain.name else null,
        )
    }

    private companion object {
        const val DEFAULT_MONOGRAM = "XX"
    }
}
