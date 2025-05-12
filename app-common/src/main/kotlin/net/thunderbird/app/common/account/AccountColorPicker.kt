package net.thunderbird.app.common.account

import android.content.res.Resources
import app.k9mail.core.ui.legacy.theme2.common.R
import app.k9mail.legacy.account.AccountManager

internal class AccountColorPicker(
    private val accountManager: AccountManager,
    private val resources: Resources,
) {
    fun pickColor(): Int {
        val accounts = accountManager.getAccounts()
        val usedAccountColors = accounts.map { it.chipColor }.toSet()
        val accountColors = resources.getIntArray(R.array.account_colors).toList()

        val availableColors = accountColors - usedAccountColors
        if (availableColors.isEmpty()) {
            return accountColors.random()
        }

        val defaultAccountColors = resources.getIntArray(R.array.default_account_colors)
        return availableColors.shuffled().minByOrNull { color ->
            val index = defaultAccountColors.indexOf(color)
            if (index != -1) index else defaultAccountColors.size
        } ?: error("availableColors must not be empty")
    }
}
