package net.thunderbird.feature.account.storage.profile

import net.thunderbird.feature.account.Account
import net.thunderbird.feature.account.AccountId

data class ProfileDto(
    override val id: AccountId,
    val name: String,
    val color: Int,
) : Account
