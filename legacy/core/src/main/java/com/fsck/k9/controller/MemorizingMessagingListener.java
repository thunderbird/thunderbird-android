package com.fsck.k9.controller;


import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import app.k9mail.legacy.message.controller.MessagingListener;
import app.k9mail.legacy.message.controller.SimpleMessagingListener;
import net.thunderbird.feature.account.AccountId;


/**
 * TODO: Move this sync-state tracking into the sync feature and remove this legacy listener.
 */
class MemorizingMessagingListener extends SimpleMessagingListener {
    Map<String, Memory> memories = new HashMap<>(31);

    synchronized void removeAccount(AccountId accountId) {
        Iterator<Entry<String, Memory>> memIt = memories.entrySet().iterator();

        while (memIt.hasNext()) {
            Entry<String, Memory> memoryEntry = memIt.next();

            String uuidForMemory = memoryEntry.getValue().accountId.toString();

            if (uuidForMemory.equals(accountId.toString())) {
                memIt.remove();
            }
        }
    }

    synchronized void refreshOther(MessagingListener other) {
        if (other != null) {

            Memory syncStarted = null;

            for (Memory memory : memories.values()) {

                if (memory.syncingState != null) {
                    switch (memory.syncingState) {
                        case STARTED:
                            syncStarted = memory;
                            break;
                        case FINISHED:
                            other.synchronizeMailboxFinished(memory.accountId, memory.folderId);
                            break;
                        case FAILED:
                            other.synchronizeMailboxFailed(memory.accountId, memory.folderId,
                                    memory.failureMessage);
                            break;
                    }
                }
            }
            Memory somethingStarted = null;
            if (syncStarted != null) {
                other.synchronizeMailboxStarted(syncStarted.accountId, syncStarted.folderId);
                somethingStarted = syncStarted;
            }
            if (somethingStarted != null && somethingStarted.folderTotal > 0) {
                other.synchronizeMailboxProgress(somethingStarted.accountId, somethingStarted.folderId,
                        somethingStarted.folderCompleted, somethingStarted.folderTotal);
            }

        }
    }

    @Override
    public synchronized void synchronizeMailboxStarted(AccountId accountId, long folderId) {
        Memory memory = getMemory(accountId, folderId);
        memory.syncingState = MemorizingState.STARTED;
        memory.folderCompleted = 0;
        memory.folderTotal = 0;
    }

    @Override
    public synchronized void synchronizeMailboxFinished(AccountId accountId, long folderId) {
        Memory memory = getMemory(accountId, folderId);
        memory.syncingState = MemorizingState.FINISHED;
    }

    @Override
    public synchronized void synchronizeMailboxFailed(AccountId accountId, long folderId,
            String message) {

        Memory memory = getMemory(accountId, folderId);
        memory.syncingState = MemorizingState.FAILED;
        memory.failureMessage = message;
    }

    @Override
    public synchronized void synchronizeMailboxProgress(AccountId accountId, long folderId, int completed,
            int total) {
        Memory memory = getMemory(accountId, folderId);
        memory.folderCompleted = completed;
        memory.folderTotal = total;
    }

    private Memory getMemory(AccountId accountId, long folderId) {
        Memory memory = memories.get(getMemoryKey(accountId, folderId));
        if (memory == null) {
            memory = new Memory(accountId, folderId);
            memories.put(getMemoryKey(memory.accountId, memory.folderId), memory);
        }
        return memory;
    }

    private static String getMemoryKey(AccountId accountId, long folderId) {
        return accountId + ":" + folderId;
    }

    private enum MemorizingState { STARTED, FINISHED, FAILED }

    private static class Memory {
        AccountId accountId;
        long folderId;
        MemorizingState syncingState = null;
        String failureMessage = null;

        int folderCompleted = 0;
        int folderTotal = 0;

        Memory(AccountId accountId, long folderId) {
            this.accountId = accountId;
            this.folderId = folderId;
        }
    }
}
