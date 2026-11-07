package com.fsck.k9.mailstore

import net.thunderbird.core.android.account.LegacyAccount

/**
 * Helper to allow accessing classes and methods that aren't visible or accessible to the 'migrations' package
 */
interface MigrationsHelper {
    val account: LegacyAccount

    /**
     * Atomically applies [transform] to the latest version of the account and saves the result.
     */
    fun updateAccount(transform: (LegacyAccount) -> LegacyAccount)
}
