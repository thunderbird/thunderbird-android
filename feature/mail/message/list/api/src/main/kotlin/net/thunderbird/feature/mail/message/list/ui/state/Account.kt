package net.thunderbird.feature.mail.message.list.ui.state

import androidx.compose.ui.graphics.Color
import net.thunderbird.feature.account.AccountId

/**
 * The minimum representation of an account with its unique identifier and associated display color.
 *
 * @param id The unique identifier for the account.
 * @param color The color assigned to the account for UI differentiation.
 */
data class Account(
    val id: AccountId,
    val color: Color,
)
