package net.thunderbird.feature.account.storage.profile

data class AvatarDto(
    val avatarType: AvatarTypeDto,
    val avatarMonogram: String?,
    val avatarImageUri: String?,
    val avatarIconName: String?,
)
