package com.fsck.k9.controller;


import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.fsck.k9.Account;


class MemorizingMessagingListener extends MessagingListener {
    Map<String, Memory> memories = new HashMap<>(31);

    private Memory getMemory(Account account, String folderName) {
        Memory memory = memories.get(getMemoryKey(account, folderName));
        if (memory == null) {
            memory = new Memory(account, folderName);
            memories.put(getMemoryKey(memory.account, memory.folderName), memory);
        }
        return memory;
    }

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

    @Override
    public synchronized void synchronizeMailboxStarted(Account account, String folder) {
        Memory memory = getMemory(account, folder);
        memory.syncingState = MemorizingState.STARTED;
        memory.folderCompleted = 0;
        memory.folderTotal = 0;
    }

    @Override
    public synchronized void synchronizeMailboxFinished(Account account, String folder,
            int totalMessagesInMailbox, int numNewMessages) {
        Memory memory = getMemory(account, folder);
        memory.syncingState = MemorizingState.FINISHED;
        memory.syncingTotalMessagesInMailbox = totalMessagesInMailbox;
        memory.syncingNumNewMessages = numNewMessages;
    }

    @Override
    public synchronized void synchronizeMailboxFailed(Account account, String folder,
            String message) {

        Memory memory = getMemory(account, folder);
        memory.syncingState = MemorizingState.FAILED;
        memory.failureMessage = message;
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
                            other.synchronizeMailboxFinished(memory.account, memory.folderName,
                                    memory.syncingTotalMessagesInMailbox, memory.syncingNumNewMessages);
                            break;
                        case FAILED:
                            other.synchronizeMailboxFailed(memory.account, memory.folderName,
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
                            other.setPushActive(memory.account, memory.folderName, true);
                            break;
                        case FINISHED:
                            other.setPushActive(memory.account, memory.folderName, false);
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
                other.synchronizeMailboxStarted(syncStarted.account, syncStarted.folderName);
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
                other.synchronizeMailboxProgress(somethingStarted.account, somethingStarted.folderName,
                        somethingStarted.folderCompleted, somethingStarted.folderTotal);
            }

        }
    }

    @Override
    public synchronized void setPushActive(Account account, String folderName, boolean active) {
        Memory memory = getMemory(account, folderName);
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
    public synchronized void synchronizeMailboxProgress(Account account, String folderName, int completed,
            int total) {
        Memory memory = getMemory(account, folderName);
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

    private static String getMemoryKey(Account taccount, String tfolderName) {
        return taccount.getDescription() + ":" + tfolderName;
    }

    private enum MemorizingState { STARTED, FINISHED, FAILED }

    private static class Memory {
        Account account;
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

        Memory(Account nAccount, String nFolderName) {
            account = nAccount;
            folderName = nFolderName;
        }
    }
}
