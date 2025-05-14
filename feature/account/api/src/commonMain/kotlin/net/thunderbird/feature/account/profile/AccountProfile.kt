package net.thunderbird.feature.account.profile

import net.thunderbird.feature.account.Account
import net.thunderbird.feature.account.AccountId

/**
 * Data class representing an account profile.
 *
 * @property id The unique identifier of the account profile.
 * @property name The name of the account.
 * @property color The color associated with the account.
 * @property avatar The [AccountAvatar] representing the avatar of the account.
 */
data class AccountProfile(
    override val id: AccountId,
    val name: String,
    val color: Int,
    val avatar: AccountAvatar,
) : Account
