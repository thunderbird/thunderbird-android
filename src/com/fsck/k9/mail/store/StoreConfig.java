package com.fsck.k9.mail.store;

public interface StoreConfig {
    String getUuid();
    String getStoreUri();
    String getTransportUri();

    boolean subscribedFoldersOnly();
    boolean useCompression(int type);

    String getInboxFolderName();
    String getOutboxFolderName();
    String getDraftsFolderName();

    void setInboxFolderName(String folderName);
    void setDraftsFolderName(String decodedFolderName);
    void setTrashFolderName(String decodedFolderName);
    void setSpamFolderName(String decodedFolderName);
    void setSentFolderName(String decodedFolderName);
    void setAutoExpandFolderName(String folderName);

    int getMaximumAutoDownloadMessageSize();

    boolean allowRemoteSearch();
    boolean isRemoteSearchFullText();

    boolean isPushPollOnConnect();

    int getDisplayCount();

    int getIdleRefreshMinutes();
}
