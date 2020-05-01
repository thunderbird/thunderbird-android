package com.fsck.k9.mail.store;


import com.fsck.k9.mail.NetworkType;

public interface StoreConfig {
    boolean isSubscribedFoldersOnly();
    boolean useCompression(NetworkType type);

    String getDraftsFolder();

    int getMaximumAutoDownloadMessageSize();

    boolean isRemoteSearchFullText();
}
