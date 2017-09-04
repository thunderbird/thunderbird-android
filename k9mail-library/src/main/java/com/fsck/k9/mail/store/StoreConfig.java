package com.fsck.k9.mail.store;


import com.fsck.k9.mail.NetworkType;

public interface StoreConfig {
    String getStoreUri();
    String getTransportUri();

    boolean subscribedFoldersOnly();
    boolean useCompression(NetworkType type);

    String getInboxFolderId();
    String getOutboxFolderId();
    String getDraftsFolderId();

    void setArchiveFolderId(String name);
    void setDraftsFolderId(String name);
    void setTrashFolderId(String name);
    void setSpamFolderId(String name);
    void setSentFolderId(String name);
    void setAutoExpandFolderId(String name);
    void setInboxFolderId(String name);

    int getMaximumAutoDownloadMessageSize();

    boolean allowRemoteSearch();
    boolean isRemoteSearchFullText();

    boolean isPushPollOnConnect();

    int getDisplayCount();

    int getIdleRefreshMinutes();
}
