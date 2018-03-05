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
            Memory sendStarted = null;
            Memory processingStarted = null;

            for (Memory memory : memories.values()) {

                if (memory.syncingState != null) {
                    switch (memory.syncingState) {
                        case STARTED:
                            syncStarted = memory;
                            break;
                        case FINISHED:
                            other.synchronizeMailboxFinished(memory.account, memory.folderServerId,
                                    memory.syncingTotalMessagesInMailbox, memory.syncingNumNewMessages);
                            break;
                        case FAILED:
                            other.synchronizeMailboxFailed(memory.account, memory.folderServerId,
                                    memory.failureMessage);
                            break;
                    }
                }

                if (memory.sendingState != null) {
                    switch (memory.sendingState) {
                        case STARTED:
                            sendStarted = memory;
                            break;
                        case FINISHED:
                            other.sendPendingMessagesCompleted(memory.account);
                            break;
                        case FAILED:
                            other.sendPendingMessagesFailed(memory.account);
                            break;
                    }
                }
                if (memory.pushingState != null) {
                    switch (memory.pushingState) {
                        case STARTED:
                            other.setPushActive(memory.account, memory.folderServerId, true);
                            break;
                        case FINISHED:
                            other.setPushActive(memory.account, memory.folderServerId, false);
                            break;
                        case FAILED:
                            break;
                    }
                }
                if (memory.processingState != null) {
                    switch (memory.processingState) {
                        case STARTED:
                            processingStarted = memory;
                            break;
                        case FINISHED:
                        case FAILED:
                            other.pendingCommandsFinished(memory.account);
                            break;
                    }
                }
            }
            Memory somethingStarted = null;
            if (syncStarted != null) {
                other.synchronizeMailboxStarted(syncStarted.account, syncStarted.folderServerId,
                        syncStarted.folderName);
                somethingStarted = syncStarted;
            }
            if (sendStarted != null) {
                other.sendPendingMessagesStarted(sendStarted.account);
                somethingStarted = sendStarted;
            }
            if (processingStarted != null) {
                other.pendingCommandsProcessing(processingStarted.account);
                if (processingStarted.processingCommandTitle != null) {
                    other.pendingCommandStarted(processingStarted.account,
                            processingStarted.processingCommandTitle);

                } else {
                    other.pendingCommandCompleted(processingStarted.account, null);
                }
                somethingStarted = processingStarted;
            }
            if (somethingStarted != null && somethingStarted.folderTotal > 0) {
                other.synchronizeMailboxProgress(somethingStarted.account, somethingStarted.folderServerId,
                        somethingStarted.folderCompleted, somethingStarted.folderTotal);
            }

        }
    }

    @Override
    public synchronized void synchronizeMailboxStarted(Account account, String folderServerId,
            String folderName) {
        Memory memory = getMemory(account, folderServerId);
        memory.folderName = folderName;
        memory.syncingState = MemorizingState.STARTED;
        memory.folderCompleted = 0;
        memory.folderTotal = 0;
    }

    @Override
    public synchronized void synchronizeMailboxFinished(Account account, String folderServerId,
            int totalMessagesInMailbox, int numNewMessages) {
        Memory memory = getMemory(account, folderServerId);
        memory.syncingState = MemorizingState.FINISHED;
        memory.syncingTotalMessagesInMailbox = totalMessagesInMailbox;
        memory.syncingNumNewMessages = numNewMessages;
    }

    @Override
    public synchronized void synchronizeMailboxFailed(Account account, String folderServerId,
            String message) {

        Memory memory = getMemory(account, folderServerId);
        memory.syncingState = MemorizingState.FAILED;
        memory.failureMessage = message;
    }

    @Override
    public synchronized void setPushActive(Account account, String folderServerId, boolean active) {
        Memory memory = getMemory(account, folderServerId);
        memory.pushingState = (active ? MemorizingState.STARTED : MemorizingState.FINISHED);
    }

    @Override
    public synchronized void sendPendingMessagesStarted(Account account) {
        Memory memory = getMemory(account, null);
        memory.sendingState = MemorizingState.STARTED;
        memory.folderCompleted = 0;
        memory.folderTotal = 0;
    }

    @Override
    public synchronized void sendPendingMessagesCompleted(Account account) {
        Memory memory = getMemory(account, null);
        memory.sendingState = MemorizingState.FINISHED;
    }

    @Override
    public synchronized void sendPendingMessagesFailed(Account account) {
        Memory memory = getMemory(account, null);
        memory.sendingState = MemorizingState.FAILED;
    }


    @Override
    public synchronized void synchronizeMailboxProgress(Account account, String folderServerId, int completed,
            int total) {
        Memory memory = getMemory(account, folderServerId);
        memory.folderCompleted = completed;
        memory.folderTotal = total;
    }


    @Override
    public synchronized void pendingCommandsProcessing(Account account) {
        Memory memory = getMemory(account, null);
        memory.processingState = MemorizingState.STARTED;
        memory.folderCompleted = 0;
        memory.folderTotal = 0;
    }

    @Override
    public synchronized void pendingCommandsFinished(Account account) {
        Memory memory = getMemory(account, null);
        memory.processingState = MemorizingState.FINISHED;
    }

    @Override
    public synchronized void pendingCommandStarted(Account account, String commandTitle) {
        Memory memory = getMemory(account, null);
        memory.processingCommandTitle = commandTitle;
    }

    @Override
    public synchronized void pendingCommandCompleted(Account account, String commandTitle) {
        Memory memory = getMemory(account, null);
        memory.processingCommandTitle = null;
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
        String folderName;
        MemorizingState syncingState = null;
        MemorizingState sendingState = null;
        MemorizingState pushingState = null;
        MemorizingState processingState = null;
        String failureMessage = null;

        int syncingTotalMessagesInMailbox;
        int syncingNumNewMessages;

        int folderCompleted = 0;
        int folderTotal = 0;
        String processingCommandTitle = null;

        Memory(Account account, String folderServerId) {
            this.account = account;
            this.folderServerId = folderServerId;
        }
    }
}
