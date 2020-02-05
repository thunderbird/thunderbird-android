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
                            other.synchronizeMailboxFinished(memory.account, memory.folderServerId);
                            break;
                        case FAILED:
                            other.synchronizeMailboxFailed(memory.account, memory.folderServerId,
                                    memory.failureMessage);
                            break;
                    }
                }
            }
            Memory somethingStarted = null;
            if (syncStarted != null) {
                other.synchronizeMailboxStarted(syncStarted.account, syncStarted.folderServerId);
                somethingStarted = syncStarted;
            }
            if (somethingStarted != null && somethingStarted.folderTotal > 0) {
                other.synchronizeMailboxProgress(somethingStarted.account, somethingStarted.folderServerId,
                        somethingStarted.folderCompleted, somethingStarted.folderTotal);
            }

        }
    }

    @Override
    public synchronized void synchronizeMailboxStarted(Account account, String folderServerId) {
        Memory memory = getMemory(account, folderServerId);
        memory.syncingState = MemorizingState.STARTED;
        memory.folderCompleted = 0;
        memory.folderTotal = 0;
    }

    @Override
    public synchronized void synchronizeMailboxFinished(Account account, String folderServerId) {
        Memory memory = getMemory(account, folderServerId);
        memory.syncingState = MemorizingState.FINISHED;
    }

    @Override
    public synchronized void synchronizeMailboxFailed(Account account, String folderServerId,
            String message) {

        Memory memory = getMemory(account, folderServerId);
        memory.syncingState = MemorizingState.FAILED;
        memory.failureMessage = message;
    }

    @Override
    public synchronized void synchronizeMailboxProgress(Account account, String folderServerId, int completed,
            int total) {
        Memory memory = getMemory(account, folderServerId);
        memory.folderCompleted = completed;
        memory.folderTotal = total;
    }

    private Memory getMemory(Account account, String folderServerId) {
        Memory memory = memories.get(getMemoryKey(account, folderServerId));
        if (memory == null) {
            memory = new Memory(account, folderServerId);
            memories.put(getMemoryKey(memory.account, memory.folderServerId), memory);
        }
        return memory;
    }

    private static String getMemoryKey(Account account, String folderServerId) {
        return account.getDescription() + ":" + folderServerId;
    }

    private enum MemorizingState { STARTED, FINISHED, FAILED }

    private static class Memory {
        Account account;
        String folderServerId;
        MemorizingState syncingState = null;
        String failureMessage = null;

        int folderCompleted = 0;
        int folderTotal = 0;

        Memory(Account account, String folderServerId) {
            this.account = account;
            this.folderServerId = folderServerId;
        }
    }
}
