package com.fsck.k9.remotecontrol;
/**
 *
 * @author Daniel I. Applebaum
 * The interface to implement in order to accept the arrays containing the UUIDs and descriptions of
 * the accounts configured in K-9 Mail.  Should be passed to fetchAccounts(Context, K9AccountReceptor)
 */
public interface K9AccountReceptor {
    public void accounts(String[] uuids, String[] descriptions);
}