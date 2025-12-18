package net.thunderbird.feature.account.storage.profile

import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.AccountIdentifiable

data class ProfileDto(
    override val id: AccountId,
    val name: String,
    val color: Int,
    val avatar: AvatarDto,
) : AccountIdentifiable
