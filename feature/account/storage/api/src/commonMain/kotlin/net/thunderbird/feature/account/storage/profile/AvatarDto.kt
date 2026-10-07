package net.thunderbird.feature.account.storage.profile

import net.thunderbird.feature.account.Account
import net.thunderbird.feature.account.AccountId

data class AvatarDto(
    override val id: AccountId,
    val avatarType: AvatarTypeDto,
    val avatarMonogram: String?,
    val avatarImageUri: String?,
    val avatarIconName: String?,
) : Account
