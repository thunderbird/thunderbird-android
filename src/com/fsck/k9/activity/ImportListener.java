package com.fsck.k9.activity;

public interface ImportListener {
    public void importSuccess(int numAccounts);

    public void failure(String message, Exception e);

}
