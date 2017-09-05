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

    void setArchiveFolderId(String folderId);
    void setDraftsFolderId(String folderId);
    void setTrashFolderId(String folderId);
    void setSpamFolderId(String folderId);
    void setSentFolderId(String folderId);
    void setAutoExpandFolderId(String folderId);
    void setInboxFolderId(String folderId);

    int getMaximumAutoDownloadMessageSize();

    boolean allowRemoteSearch();
    boolean isRemoteSearchFullText();

    boolean isPushPollOnConnect();

    int getDisplayCount();

    int getIdleRefreshMinutes();
}
