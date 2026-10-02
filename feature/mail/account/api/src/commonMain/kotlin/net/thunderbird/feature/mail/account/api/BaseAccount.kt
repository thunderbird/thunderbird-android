package net.thunderbird.feature.mail.account.api

import net.thunderbird.feature.account.AccountId

interface BaseAccount {
    val id: AccountId
    val name: String?
    val email: String
}
