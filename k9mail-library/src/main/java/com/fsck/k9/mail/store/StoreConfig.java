package com.fsck.k9.mail.store;


import com.fsck.k9.mail.NetworkType;

public interface StoreConfig {
    String getStoreUri();
    String getTransportUri();

    boolean subscribedFoldersOnly();
    boolean useCompression(NetworkType type);

    String getInboxFolder();
    String getOutboxFolder();
    String getDraftsFolder();

    void setArchiveFolder(String name);
    void setDraftsFolder(String name);
    void setTrashFolder(String name);
    void setSpamFolder(String name);
    void setSentFolder(String name);
    void setAutoExpandFolder(String name);
    void setInboxFolder(String name);

    int getMaximumAutoDownloadMessageSize();

    boolean allowRemoteSearch();
    boolean isRemoteSearchFullText();

    boolean isPushPollOnConnect();

    int getDisplayCount();

    int getIdleRefreshMinutes();

    boolean shouldHideHostname();
}
