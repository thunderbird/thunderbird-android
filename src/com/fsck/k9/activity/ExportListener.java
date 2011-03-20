package com.fsck.k9.activity;

public interface ExportListener {
    public void success(String fileName);
    public void success();

    public void failure(String message, Exception e);

    public void canceled();

    public void started();

}
