package com.fsck.k9.mailstore;


import app.k9mail.legacy.account.Account;


/**
 * Helper to allow accessing classes and methods that aren't visible or accessible to the 'migrations' package
 */
public interface MigrationsHelper {
    Account getAccount();
    void saveAccount();
}
