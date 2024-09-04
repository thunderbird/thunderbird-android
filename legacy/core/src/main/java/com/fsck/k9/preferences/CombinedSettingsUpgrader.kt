package com.fsck.k9.preferences

fun interface CombinedSettingsUpgrader {
    fun upgrade(account: ValidatedSettings.Account): ValidatedSettings.Account
}
