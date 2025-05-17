package com.fsck.k9.mailstore;


import net.thunderbird.core.android.account.LegacyAccount;


/**
 * Helper to allow accessing classes and methods that aren't visible or accessible to the 'migrations' package
 */
public interface MigrationsHelper {
    LegacyAccount getAccount();
    void saveAccount();
}
