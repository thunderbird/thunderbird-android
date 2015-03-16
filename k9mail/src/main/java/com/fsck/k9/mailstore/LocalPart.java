package com.fsck.k9.mailstore;


public interface LocalPart {
    String getAccountUuid();
    long getId();
    String getDisplayName();
    long getSize();
    boolean isFirstClassAttachment();
    LocalMessage getMessage();
}
