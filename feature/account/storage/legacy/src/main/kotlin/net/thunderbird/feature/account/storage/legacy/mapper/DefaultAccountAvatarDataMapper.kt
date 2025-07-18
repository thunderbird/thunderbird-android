package net.thunderbird.feature.account.storage.legacy.mapper

import net.thunderbird.feature.account.profile.AccountAvatar
import net.thunderbird.feature.account.storage.mapper.AccountAvatarDataMapper
import net.thunderbird.feature.account.storage.profile.AvatarDto
import net.thunderbird.feature.account.storage.profile.AvatarTypeDto

class DefaultAccountAvatarDataMapper : AccountAvatarDataMapper {

    override fun toDomain(dto: AvatarDto): AccountAvatar {
        return when (dto.avatarType) {
            AvatarTypeDto.MONOGRAM -> AccountAvatar.Monogram(
                value = dto.avatarMonogram ?: DEFAULT_MONOGRAM,
            )

            AvatarTypeDto.IMAGE -> {
                val uri = dto.avatarImageUri

                if (uri.isNullOrEmpty()) {
                    AccountAvatar.Monogram(
                        value = DEFAULT_MONOGRAM,
                    )
                } else {
                    AccountAvatar.Image(
                        uri = uri,
                    )
                }
            }

            AvatarTypeDto.ICON -> {
                val name = dto.avatarIconName

                if (name.isNullOrEmpty()) {
                    AccountAvatar.Monogram(
                        value = DEFAULT_MONOGRAM,
                    )
                } else {
                    AccountAvatar.Icon(
                        name = name,
                    )
                }
            }
        }
    }

    override fun toDto(domain: AccountAvatar): AvatarDto {
        return AvatarDto(
            avatarType = when (domain) {
                is AccountAvatar.Monogram -> AvatarTypeDto.MONOGRAM
                is AccountAvatar.Image -> AvatarTypeDto.IMAGE
                is AccountAvatar.Icon -> AvatarTypeDto.ICON
            },
            avatarMonogram = if (domain is AccountAvatar.Monogram) domain.value else null,
            avatarImageUri = if (domain is AccountAvatar.Image) domain.uri else null,
            avatarIconName = if (domain is AccountAvatar.Icon) domain.name else null,
        )
    }

    private companion object {
        const val DEFAULT_MONOGRAM = "XX"
    }
}
