package com.fsck.k9.controller;


import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.fsck.k9.Account;


class MemorizingMessagingListener extends SimpleMessagingListener {
    Map<String, Memory> memories = new HashMap<>(31);

    synchronized void removeAccount(Account account) {
        Iterator<Entry<String, Memory>> memIt = memories.entrySet().iterator();

        while (memIt.hasNext()) {
            Entry<String, Memory> memoryEntry = memIt.next();

            String uuidForMemory = memoryEntry.getValue().account.getUuid();

            if (uuidForMemory.equals(account.getUuid())) {
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
                            other.synchronizeMailboxFinished(memory.account, memory.folderId);
                            break;
                        case FAILED:
                            other.synchronizeMailboxFailed(memory.account, memory.folderId,
                                    memory.failureMessage);
                            break;
                    }
                }
            }
            Memory somethingStarted = null;
            if (syncStarted != null) {
                other.synchronizeMailboxStarted(syncStarted.account, syncStarted.folderId);
                somethingStarted = syncStarted;
            }
            if (somethingStarted != null && somethingStarted.folderTotal > 0) {
                other.synchronizeMailboxProgress(somethingStarted.account, somethingStarted.folderId,
                        somethingStarted.folderCompleted, somethingStarted.folderTotal);
            }

        }
    }

    @Override
    public synchronized void synchronizeMailboxStarted(Account account, long folderId) {
        Memory memory = getMemory(account, folderId);
        memory.syncingState = MemorizingState.STARTED;
        memory.folderCompleted = 0;
        memory.folderTotal = 0;
    }

    @Override
    public synchronized void synchronizeMailboxFinished(Account account, long folderId) {
        Memory memory = getMemory(account, folderId);
        memory.syncingState = MemorizingState.FINISHED;
    }

    @Override
    public synchronized void synchronizeMailboxFailed(Account account, long folderId,
            String message) {

        Memory memory = getMemory(account, folderId);
        memory.syncingState = MemorizingState.FAILED;
        memory.failureMessage = message;
    }

    @Override
    public synchronized void synchronizeMailboxProgress(Account account, long folderId, int completed,
            int total) {
        Memory memory = getMemory(account, folderId);
        memory.folderCompleted = completed;
        memory.folderTotal = total;
    }

    private Memory getMemory(Account account, long folderId) {
        Memory memory = memories.get(getMemoryKey(account, folderId));
        if (memory == null) {
            memory = new Memory(account, folderId);
            memories.put(getMemoryKey(memory.account, memory.folderId), memory);
        }
        return memory;
    }

    private static String getMemoryKey(Account account, long folderId) {
        return account.getUuid() + ":" + folderId;
    }

    private enum MemorizingState { STARTED, FINISHED, FAILED }

    private static class Memory {
        Account account;
        long folderId;
        MemorizingState syncingState = null;
        String failureMessage = null;

        int folderCompleted = 0;
        int folderTotal = 0;

        Memory(Account account, long folderId) {
            this.account = account;
            this.folderId = folderId;
        }
    }
}
