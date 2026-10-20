package net.thunderbird.feature.account.storage.profile

import net.thunderbird.feature.account.AccountId

data class AvatarDto(
    val accountId: AccountId,
    val avatarType: AvatarTypeDto,
    val avatarMonogram: String?,
    val avatarImageUri: String?,
    val avatarIconName: String?,
) {
    companion object {
        /** placeholder image uri in case avatarType equals image but no actual image was selected yet */
        const val PLACEHOLDER_IMAGE_URI = "avatar_placeholder_uri"
    }
}
