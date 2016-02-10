package com.fsck.k9.mail.store.imap;


import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import android.content.Context;
import android.os.PowerManager;
import android.util.Log;

import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.K9MailLib;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.PushReceiver;
import com.fsck.k9.mail.power.TracingPowerManager;
import com.fsck.k9.mail.power.TracingPowerManager.TracingWakeLock;
import com.fsck.k9.mail.store.RemoteStore;

import static com.fsck.k9.mail.K9MailLib.LOG_TAG;
import static com.fsck.k9.mail.K9MailLib.PUSH_WAKE_LOCK_TIMEOUT;
import static com.fsck.k9.mail.store.imap.ImapResponseParser.equalsIgnoreCase;


class ImapFolderPusher extends ImapFolder implements UntaggedHandler {
    private static final int IDLE_READ_TIMEOUT_INCREMENT = 5 * 60 * 1000;
    private static final int IDLE_FAILURE_COUNT_LIMIT = 10;
    private static final int MAX_DELAY_TIME = 5 * 60 * 1000; // 5 minutes
    private static final int NORMAL_DELAY_TIME = 5000;


    private final PushReceiver pushReceiver;
    private final AtomicBoolean stop = new AtomicBoolean(false);
    private final AtomicBoolean idling = new AtomicBoolean(false);
    private final AtomicBoolean doneSent = new AtomicBoolean(false);
    private final AtomicInteger delayTime = new AtomicInteger(NORMAL_DELAY_TIME);
    private final AtomicInteger idleFailureCount = new AtomicInteger(0);
    private final AtomicBoolean needsPoll = new AtomicBoolean(false);
    private final Object threadLock = new Object();
    private List<ImapResponse> storedUntaggedResponses = new ArrayList<ImapResponse>();
    private TracingWakeLock wakeLock = null;
    private Thread listeningThread;


    public ImapFolderPusher(ImapStore store, String name, PushReceiver pushReceiver) {
        super(store, name);
        this.pushReceiver = pushReceiver;

        Context context = pushReceiver.getContext();
        TracingPowerManager powerManager = TracingPowerManager.getPowerManager(context);
        String tag = "ImapFolderPusher " + store.getStoreConfig().toString() + ":" + getName();
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, tag);
        wakeLock.setReferenceCounted(false);
    }

    public void start() {
        synchronized (threadLock) {
            listeningThread = new Thread(new PushRunnable());
            listeningThread.start();
        }
    }

    public void refresh() throws IOException, MessagingException {
        if (idling.get()) {
            wakeLock.acquire(PUSH_WAKE_LOCK_TIMEOUT);
            sendDone();
        }
    }

    public void stop() {
        stop.set(true);

        interruptListeningThread();

        ImapConnection conn = connection;
        if (conn != null) {
            if (K9MailLib.isDebug()) {
                Log.v(LOG_TAG, "Closing connection to stop pushing for " + getLogId());
            }

            conn.close();
        } else {
            Log.w(LOG_TAG, "Attempt to interrupt null connection to stop pushing on folderPusher for " + getLogId());
        }
    }

    private void interruptListeningThread() {
        synchronized (threadLock) {
            if (listeningThread != null) {
                listeningThread.interrupt();
            }
        }
    }

    private void sendDone() throws IOException, MessagingException {
        if (doneSent.compareAndSet(false, true)) {
            ImapConnection connection = this.connection;
            if (connection != null) {
                connection.setReadTimeout(RemoteStore.SOCKET_READ_TIMEOUT);
                sendContinuation(connection, "DONE");
            }
        }
    }

    private static void sendContinuation(ImapConnection connection, String continuation) throws IOException {
        connection.sendContinuation(continuation);
    }

    private void setReadTimeoutForIdle(ImapConnection conn) throws SocketException {
        int idleRefreshTimeout = store.getStoreConfig().getIdleRefreshMinutes() * 60 * 1000;
        conn.setReadTimeout(idleRefreshTimeout + IDLE_READ_TIMEOUT_INCREMENT);
    }

    @Override
    public void handleAsyncUntaggedResponse(ImapResponse response) {
        if (K9MailLib.isDebug()) {
            Log.v(LOG_TAG, "Got async response: " + response);
        }

        if (stop.get()) {
            if (K9MailLib.isDebug()) {
                Log.d(LOG_TAG, "Got async untagged response: " + response + ", but stop is set for " + getLogId());
            }

            try {
                sendDone();
            } catch (Exception e) {
                Log.e(LOG_TAG, "Exception while sending DONE for " + getLogId(), e);
            }
        } else {
            if (response.getTag() == null) {
                if (response.size() > 1) {
                    Object responseType = response.get(1);
                    if (equalsIgnoreCase(responseType, "EXISTS") || equalsIgnoreCase(responseType, "EXPUNGE") ||
                            equalsIgnoreCase(responseType, "FETCH")) {

                        wakeLock.acquire(PUSH_WAKE_LOCK_TIMEOUT);

                        if (K9MailLib.isDebug()) {
                            Log.d(LOG_TAG, "Got useful async untagged response: " + response + " for " + getLogId());
                        }

                        try {
                            sendDone();
                        } catch (Exception e) {
                            Log.e(LOG_TAG, "Exception while sending DONE for " + getLogId(), e);
                        }
                    }
                } else if (response.isContinuationRequested()) {
                    if (K9MailLib.isDebug()) {
                        Log.d(LOG_TAG, "Idling " + getLogId());
                    }

                    wakeLock.release();
                }
            }
        }
    }

    @Override
    protected void handleUntaggedResponse(ImapResponse response) {
        if (response.getTag() == null && response.size() > 1) {
            Object responseType = response.get(1);
            if (equalsIgnoreCase(responseType, "FETCH") || equalsIgnoreCase(responseType, "EXPUNGE") ||
                    equalsIgnoreCase(responseType, "EXISTS")) {

                if (K9MailLib.isDebug()) {
                    Log.d(LOG_TAG, "Storing response " + response + " for later processing");
                }

                storedUntaggedResponses.add(response);
            }

            handlePossibleUidNext(response);
        }
    }

    private void processStoredUntaggedResponses() throws MessagingException {
        List<ImapResponse> untaggedResponses;
        while (!storedUntaggedResponses.isEmpty()) {
            if (K9MailLib.isDebug()) {
                Log.i(LOG_TAG, "Processing " + storedUntaggedResponses.size() +
                        " untagged responses from previous commands for " + getLogId());
            }

            untaggedResponses = new ArrayList<ImapResponse>(storedUntaggedResponses);
            storedUntaggedResponses.clear();
            processUntaggedResponses(untaggedResponses);
        }
    }

    private void processUntaggedResponses(List<ImapResponse> responses) throws MessagingException {
        boolean skipSync = false;

        int oldMessageCount = messageCount;
        if (oldMessageCount == -1) {
            skipSync = true;
        }

        List<Long> flagSyncMsgSeqs = new ArrayList<Long>();
        List<String> removeMsgUids = new LinkedList<String>();

        for (ImapResponse response : responses) {
            oldMessageCount += processUntaggedResponse(oldMessageCount, response, flagSyncMsgSeqs, removeMsgUids);
        }

        if (!skipSync) {
            if (oldMessageCount < 0) {
                oldMessageCount = 0;
            }

            if (messageCount > oldMessageCount) {
                syncMessages(messageCount);
            }
        }

        if (K9MailLib.isDebug()) {
            Log.d(LOG_TAG, "UIDs for messages needing flag sync are " + flagSyncMsgSeqs + "  for " + getLogId());
        }

        if (!flagSyncMsgSeqs.isEmpty()) {
            syncMessages(flagSyncMsgSeqs);
        }

        if (!removeMsgUids.isEmpty()) {
            removeMessages(removeMsgUids);
        }
    }

    private int processUntaggedResponse(long oldMessageCount, ImapResponse response, List<Long> flagSyncMsgSeqs,
            List<String> removeMsgUids) {
        super.handleUntaggedResponse(response);

        int messageCountDelta = 0;
        if (response.getTag() == null && response.size() > 1) {
            try {
                Object responseType = response.get(1);
                if (equalsIgnoreCase(responseType, "FETCH")) {
                    Log.i(LOG_TAG, "Got FETCH " + response);

                    long msgSeq = response.getLong(0);

                    if (K9MailLib.isDebug()) {
                        Log.d(LOG_TAG, "Got untagged FETCH for msgseq " + msgSeq + " for " + getLogId());
                    }

                    if (!flagSyncMsgSeqs.contains(msgSeq)) {
                        flagSyncMsgSeqs.add(msgSeq);
                    }
                }

                if (equalsIgnoreCase(responseType, "EXPUNGE")) {
                    long msgSeq = response.getLong(0);
                    if (msgSeq <= oldMessageCount) {
                        messageCountDelta = -1;
                    }

                    if (K9MailLib.isDebug()) {
                        Log.d(LOG_TAG, "Got untagged EXPUNGE for msgseq " + msgSeq + " for " + getLogId());
                    }

                    List<Long> newSeqs = new ArrayList<Long>();
                    Iterator<Long> flagIter = flagSyncMsgSeqs.iterator();
                    while (flagIter.hasNext()) {
                        long flagMsg = flagIter.next();
                        if (flagMsg >= msgSeq) {
                            flagIter.remove();
                            if (flagMsg > msgSeq) {
                                newSeqs.add(flagMsg);
                            }
                        }
                    }

                    flagSyncMsgSeqs.addAll(newSeqs);

                    List<Long> msgSeqs = new ArrayList<Long>(msgSeqUidMap.keySet());
                    Collections.sort(msgSeqs);  // Have to do comparisons in order because of msgSeq reductions

                    for (long msgSeqNum : msgSeqs) {
                        if (K9MailLib.isDebug()) {
                            Log.v(LOG_TAG, "Comparing EXPUNGEd msgSeq " + msgSeq + " to " + msgSeqNum);
                        }

                        if (msgSeqNum == msgSeq) {
                            String uid = msgSeqUidMap.get(msgSeqNum);

                            if (K9MailLib.isDebug()) {
                                Log.d(LOG_TAG, "Scheduling removal of UID " + uid + " because msgSeq " + msgSeqNum +
                                        " was expunged");
                            }

                            removeMsgUids.add(uid);
                            msgSeqUidMap.remove(msgSeqNum);
                        } else if (msgSeqNum > msgSeq) {
                            String uid = msgSeqUidMap.get(msgSeqNum);

                            if (K9MailLib.isDebug()) {
                                Log.d(LOG_TAG, "Reducing msgSeq for UID " + uid + " from " + msgSeqNum + " to " +
                                        (msgSeqNum - 1));
                            }

                            msgSeqUidMap.remove(msgSeqNum);
                            msgSeqUidMap.put(msgSeqNum - 1, uid);
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(LOG_TAG, "Could not handle untagged FETCH for " + getLogId(), e);
            }
        }

        return messageCountDelta;
    }

    private void syncMessages(int end) throws MessagingException {
        long oldUidNext = getOldUidNext();

        List<ImapMessage> messageList = getMessages(end, end, null, true, null);

        if (messageList != null && messageList.size() > 0) {
            long newUid = Long.parseLong(messageList.get(0).getUid());

            if (K9MailLib.isDebug()) {
                Log.i(LOG_TAG, "Got newUid " + newUid + " for message " + end + " on " + getLogId());
            }

            long startUid = oldUidNext;
            if (startUid < newUid - 10) {
                startUid = newUid - 10;
            }

            if (startUid < 1) {
                startUid = 1;
            }

            if (newUid >= startUid) {
                if (K9MailLib.isDebug()) {
                    Log.i(LOG_TAG, "Needs sync from uid " + startUid + " to " + newUid + " for " + getLogId());
                }

                List<Message> messages = new ArrayList<Message>();
                for (long uid = startUid; uid <= newUid; uid++) {
                    ImapMessage message = new ImapMessage(Long.toString(uid), ImapFolderPusher.this);
                    messages.add(message);
                }

                if (!messages.isEmpty()) {
                    pushReceiver.messagesArrived(this, messages);
                }
            }
        }
    }

    private void syncMessages(List<Long> flagSyncMsgSeqs) {
        try {
            List<? extends Message> messageList = getMessages(flagSyncMsgSeqs, true, null);

            List<Message> messages = new ArrayList<Message>();
            messages.addAll(messageList);
            pushReceiver.messagesFlagsChanged(this, messages);
        } catch (Exception e) {
            pushReceiver.pushError("Exception while processing Push untagged responses", e);
        }
    }

    private void removeMessages(List<String> removeUids) {
        List<Message> messages = new ArrayList<Message>(removeUids.size());

        try {
            List<ImapMessage> existingMessages = getMessagesFromUids(removeUids);
            for (Message existingMessage : existingMessages) {
                needsPoll.set(true);
                msgSeqUidMap.clear();

                String existingUid = existingMessage.getUid();
                Log.w(LOG_TAG, "Message with UID " + existingUid + " still exists on server, not expunging");

                removeUids.remove(existingUid);
            }

            for (String uid : removeUids) {
                ImapMessage message = new ImapMessage(uid, this);

                try {
                    message.setFlagInternal(Flag.DELETED, true);
                } catch (MessagingException me) {
                    Log.e(LOG_TAG, "Unable to set DELETED flag on message " + message.getUid());
                }

                messages.add(message);
            }

            pushReceiver.messagesRemoved(this, messages);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Cannot remove EXPUNGEd messages", e);
        }
    }

    private void syncFolderOnConnect() throws MessagingException {
        List<ImapResponse> untaggedResponses = new ArrayList<ImapResponse>(storedUntaggedResponses);
        storedUntaggedResponses.clear();
        processUntaggedResponses(untaggedResponses);

        if (messageCount == -1) {
            throw new MessagingException("Message count = -1 for idling");
        }

        pushReceiver.syncFolder(ImapFolderPusher.this);
    }

    private void notifyMessagesArrived(long startUid, long uidNext) {
        if (K9MailLib.isDebug()) {
            Log.i(LOG_TAG, "Needs sync from uid " + startUid + " to " + uidNext + " for " + getLogId());
        }

        int count = (int) (uidNext - startUid);
        List<Message> messages = new ArrayList<Message>(count);

        for (long uid = startUid; uid < uidNext; uid++) {
            ImapMessage message = new ImapMessage(Long.toString(uid), this);
            messages.add(message);
        }

        pushReceiver.messagesArrived(this, messages);
    }

    private long getOldUidNext() {
        long oldUidNext = -1L;
        try {
            String serializedPushState = pushReceiver.getPushState(getName());
            ImapPushState pushState = ImapPushState.parse(serializedPushState);
            oldUidNext = pushState.uidNext;

            if (K9MailLib.isDebug()) {
                Log.i(LOG_TAG, "Got oldUidNext " + oldUidNext + " for " + getLogId());
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Unable to get oldUidNext for " + getLogId(), e);
        }

        return oldUidNext;
    }


    private class PushRunnable implements Runnable {
        @Override
        public void run() {
            wakeLock.acquire(PUSH_WAKE_LOCK_TIMEOUT);

            if (K9MailLib.isDebug()) {
                Log.i(LOG_TAG, "Pusher starting for " + getLogId());
            }

            long lastUidNext = -1L;
            while (!stop.get()) {
                try {
                    long oldUidNext = getOldUidNext();

                        /*
                         * This makes sure 'oldUidNext' is never smaller than 'UIDNEXT' from
                         * the last loop iteration. This way we avoid looping endlessly causing
                         * the battery to drain.
                         *
                         * See issue 4907
                         */
                    if (oldUidNext < lastUidNext) {
                        oldUidNext = lastUidNext;
                    }

                    ImapConnection oldConnection = connection;
                    internalOpen(OPEN_MODE_RO);

                    ImapConnection conn = connection;

                    checkConnectionNotNull(conn);
                    checkConnectionIdleCapable(conn);

                    if (stop.get()) {
                        break;
                    }

                    boolean pushPollOnConnect = store.getStoreConfig().isPushPollOnConnect();
                    boolean openedNewConnection = conn != oldConnection;
                    if (pushPollOnConnect && (openedNewConnection || needsPoll.getAndSet(false))) {
                        syncFolderOnConnect();
                    }

                    if (stop.get()) {
                        break;
                    }

                    long newUidNext = getNewUidNext();
                    lastUidNext = newUidNext;
                    long startUid = getStartUid(oldUidNext, newUidNext);

                    if (newUidNext > startUid) {
                        notifyMessagesArrived(startUid, newUidNext);
                    } else {
                        processStoredUntaggedResponses();

                        if (K9MailLib.isDebug()) {
                            Log.i(LOG_TAG, "About to IDLE for " + getLogId());
                        }

                        prepareForIdle();

                        setReadTimeoutForIdle(conn);
                        executeSimpleCommand(Commands.IDLE, false, ImapFolderPusher.this);

                        returnFromIdle();
                    }
                } catch (Exception e) {
                    wakeLock.acquire(PUSH_WAKE_LOCK_TIMEOUT);

                    storedUntaggedResponses.clear();
                    idling.set(false);
                    pushReceiver.setPushActive(getName(), false);

                    try {
                        close();
                    } catch (Exception me) {
                        Log.e(LOG_TAG, "Got exception while closing for exception for " + getLogId(), me);
                    }

                    if (stop.get()) {
                        Log.i(LOG_TAG, "Got exception while idling, but stop is set for " + getLogId());
                    } else {
                        pushReceiver.pushError("Push error for " + getName(), e);
                        Log.e(LOG_TAG, "Got exception while idling for " + getLogId(), e);

                        int delayTimeInt = delayTime.get();
                        pushReceiver.sleep(wakeLock, delayTimeInt);

                        delayTimeInt *= 2;
                        if (delayTimeInt > MAX_DELAY_TIME) {
                            delayTimeInt = MAX_DELAY_TIME;
                        }
                        delayTime.set(delayTimeInt);

                        if (idleFailureCount.incrementAndGet() > IDLE_FAILURE_COUNT_LIMIT) {
                            Log.e(LOG_TAG, "Disabling pusher for " + getLogId() + " after " +
                                    idleFailureCount.get() + " consecutive errors");
                            pushReceiver.pushError("Push disabled for " + getName() + " after " +
                                    idleFailureCount.get() + " consecutive errors", e);
                            stop.set(true);
                        }
                    }
                }
            }

            pushReceiver.setPushActive(getName(), false);

            try {
                if (K9MailLib.isDebug()) {
                    Log.i(LOG_TAG, "Pusher for " + getLogId() + " is exiting");
                }

                close();
            } catch (Exception me) {
                Log.e(LOG_TAG, "Got exception while closing for " + getLogId(), me);
            } finally {
                wakeLock.release();
            }
        }

        private long getNewUidNext() throws MessagingException {
            long newUidNext = uidNext;
            if (newUidNext != -1L) {
                return newUidNext;
            }

            if (K9MailLib.isDebug()) {
                Log.d(LOG_TAG, "uidNext is -1, using search to find highest UID");
            }

            long highestUid = getHighestUid();
            if (highestUid == -1L) {
                return -1L;
            }

            newUidNext = highestUid + 1;

            if (K9MailLib.isDebug()) {
                Log.d(LOG_TAG, "highest UID = " + highestUid + ", set newUidNext to " + newUidNext);
            }

            return newUidNext;
        }

        private long getStartUid(long oldUidNext, long newUidNext) {
            long startUid = oldUidNext;
            int displayCount = store.getStoreConfig().getDisplayCount();

            if (startUid < newUidNext - displayCount) {
                startUid = newUidNext - displayCount;
            }

            if (startUid < 1) {
                startUid = 1;
            }

            return startUid;
        }

        private void prepareForIdle() {
            pushReceiver.setPushActive(getName(), true);
            idling.set(true);
            doneSent.set(false);
        }

        private void returnFromIdle() {
            idling.set(false);
            delayTime.set(NORMAL_DELAY_TIME);
            idleFailureCount.set(0);
        }

        private void checkConnectionNotNull(ImapConnection conn) throws MessagingException {
            if (conn == null) {
                String message = "Could not establish connection for IDLE";
                pushReceiver.pushError(message, null);

                throw new MessagingException(message);
            }
        }

        private void checkConnectionIdleCapable(ImapConnection conn) throws MessagingException {
            if (!conn.isIdleCapable()) {
                stop.set(true);

                String message = "IMAP server is not IDLE capable: " + conn.toString();
                pushReceiver.pushError(message, null);

                throw new MessagingException(message);
            }
        }
    }
}
