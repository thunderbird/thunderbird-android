package com.fsck.k9.activity;

public interface ImportListener {
    public void success(int numAccounts);

    public void failure(String message, Exception e);
    
    public void canceled();
    
    public void started();

}
