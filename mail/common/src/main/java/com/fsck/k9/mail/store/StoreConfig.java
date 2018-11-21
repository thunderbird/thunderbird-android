package com.fsck.k9.mail.store;


import com.fsck.k9.mail.NetworkType;

public interface StoreConfig {
    boolean isSubscribedFoldersOnly();
    boolean useCompression(NetworkType type);

    String getInboxFolder();
    String getOutboxFolder();
    String getDraftsFolder();

    int getMaximumAutoDownloadMessageSize();

    boolean isAllowRemoteSearch();
    boolean isRemoteSearchFullText();

    boolean isPushPollOnConnect();

    int getDisplayCount();

    int getIdleRefreshMinutes();

    boolean shouldHideHostname();
}
