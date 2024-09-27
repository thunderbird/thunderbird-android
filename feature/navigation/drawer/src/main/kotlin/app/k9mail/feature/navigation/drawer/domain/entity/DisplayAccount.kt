package app.k9mail.feature.navigation.drawer.domain.entity

import app.k9mail.legacy.account.Account

internal data class DisplayAccount(
    val account: Account,
    val unreadMessageCount: Int,
    val starredMessageCount: Int,
) {
    val uuid: String = account.uuid
}
