package com.fsck.k9.activity;

public interface ExportListener
{
    public void exportSuccess(String fileName);
    
    public void failure(String message, Exception e);

}
