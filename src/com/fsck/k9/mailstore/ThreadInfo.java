package com.fsck.k9.mailstore;

class ThreadInfo {
    public final long threadId;
    public final long msgId;
    public final String messageId;
    public final long rootId;
    public final long parentId;

    public ThreadInfo(long threadId, long msgId, String messageId, long rootId, long parentId) {
        this.threadId = threadId;
        this.msgId = msgId;
        this.messageId = messageId;
        this.rootId = rootId;
        this.parentId = parentId;
    }
}