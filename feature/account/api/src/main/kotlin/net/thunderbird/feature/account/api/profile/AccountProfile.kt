package net.thunderbird.feature.account.api.profile

import net.thunderbird.feature.account.api.Account
import net.thunderbird.feature.account.api.AccountId

data class AccountProfile(
    override val accountId: AccountId,
    val name: String,
    val color: Int,
) : Account
