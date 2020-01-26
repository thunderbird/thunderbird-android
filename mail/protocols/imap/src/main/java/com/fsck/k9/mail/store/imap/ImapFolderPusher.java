package com.fsck.k9.mail.store.imap;


import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.fsck.k9.mail.AuthenticationFailedException;
import com.fsck.k9.mail.CertificateValidationException;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.K9MailLib;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.PushReceiver;
import com.fsck.k9.mail.power.PowerManager;
import com.fsck.k9.mail.power.WakeLock;
import timber.log.Timber;

import static com.fsck.k9.mail.K9MailLib.PUSH_WAKE_LOCK_TIMEOUT;
import static com.fsck.k9.mail.NetworkTimeouts.SOCKET_READ_TIMEOUT;
import static com.fsck.k9.mail.store.imap.ImapResponseParser.equalsIgnoreCase;


class ImapFolderPusher extends ImapFolder {
    private static final int IDLE_READ_TIMEOUT_INCREMENT = 5 * 60 * 1000;
    private static final int IDLE_FAILURE_COUNT_LIMIT = 10;
    private static final int MAX_DELAY_TIME = 5 * 60 * 1000; // 5 minutes
    private static final int NORMAL_DELAY_TIME = 5000;


    private final PushReceiver pushReceiver;
    private final Object threadLock = new Object();
    private final IdleStopper idleStopper = new IdleStopper();
    private final WakeLock wakeLock;
    private final List<ImapResponse> storedUntaggedResponses = new ArrayList<>();
    private Thread listeningThread;
    private volatile boolean stop = false;
    private volatile boolean idling = false;


    public ImapFolderPusher(ImapStore store, String serverId, PushReceiver pushReceiver, PowerManager powerManager) {
        super(store, serverId);
        this.pushReceiver = pushReceiver;

        String tag = "ImapFolderPusher " + store.getStoreConfig().toString() + ":" + getServerId();
        wakeLock = powerManager.newWakeLock(tag);
        wakeLock.setReferenceCounted(false);
    }

    public void start() {
        synchronized (threadLock) {
            if (listeningThread != null) {
                throw new IllegalStateException("start() called twice");
            }

            listeningThread = new Thread(new PushRunnable());
            listeningThread.start();
        }
    }

    public void refresh() {
        if (idling) {
            wakeLock.acquire(PUSH_WAKE_LOCK_TIMEOUT);
            idleStopper.stopIdle();
        }
    }

    public void stop() {
        synchronized (threadLock) {
            if (listeningThread == null) {
                throw new IllegalStateException("stop() called twice");
            }

            stop = true;

            listeningThread.interrupt();
            listeningThread = null;
        }

        ImapConnection conn = connection;
        if (conn != null) {
            if (K9MailLib.isDebug()) {
                Timber.v("Closing connection to stop pushing for %s", getLogId());
            }

            conn.close();
        } else {
            Timber.w("Attempt to interrupt null connection to stop pushing on folderPusher for %s", getLogId());
        }
    }

    @Override
    protected void handleUntaggedResponse(ImapResponse response) {
        if (response.getTag() == null && response.size() > 1) {
            Object responseType = response.get(1);
            if (equalsIgnoreCase(responseType, "FETCH") || equalsIgnoreCase(responseType, "EXPUNGE") ||
                    equalsIgnoreCase(responseType, "EXISTS")) {

                if (K9MailLib.isDebug()) {
                    Timber.d("Storing response %s for later processing", response);
                }

                synchronized (storedUntaggedResponses) {
                    storedUntaggedResponses.add(response);
                }
            }

            handlePossibleUidNext(response);
        }
    }

    private void superHandleUntaggedResponse(ImapResponse response) {
        super.handleUntaggedResponse(response);
    }


    private class PushRunnable implements Runnable, UntaggedHandler {
        private int delayTime = NORMAL_DELAY_TIME;
        private int idleFailureCount = 0;
        private boolean needsPoll = false;

        @Override
        public void run() {
            wakeLock.acquire(PUSH_WAKE_LOCK_TIMEOUT);

            if (K9MailLib.isDebug()) {
                Timber.i("Pusher starting for %s", getLogId());
            }

            long lastUidNext = -1L;
            while (!stop) {
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

                    boolean openedNewConnection = openConnectionIfNecessary();

                    if (stop) {
                        break;
                    }

                    boolean pushPollOnConnect = store.getStoreConfig().isPushPollOnConnect();
                    if (pushPollOnConnect && (openedNewConnection || needsPoll)) {
                        needsPoll = false;
                        syncFolderOnConnect();
                    }

                    if (stop) {
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
                            Timber.i("About to IDLE for %s", getLogId());
                        }

                        prepareForIdle();

                        ImapConnection conn = connection;
                        setReadTimeoutForIdle(conn);
                        sendIdle(conn);

                        returnFromIdle();
                    }
                } catch (AuthenticationFailedException e) {
                    reacquireWakeLockAndCleanUp();

                    if (K9MailLib.isDebug()) {
                        Timber.e(e, "Authentication failed. Stopping ImapFolderPusher.");
                    }

                    pushReceiver.authenticationFailed();
                    stop = true;
                } catch (CertificateValidationException e) {
                    reacquireWakeLockAndCleanUp();

                    Timber.e(e, "Certificate check failed. Stopping ImapFolderPusher.");
                    stop = true;
                    pushReceiver.pushError("Push error for " + getServerId(), e);
                } catch (Exception e) {
                    reacquireWakeLockAndCleanUp();

                    if (stop) {
                        Timber.i("Got exception while idling, but stop is set for %s", getLogId());
                    } else {
                        pushReceiver.pushError("Push error for " + getServerId(), e);
                        Timber.e("Got exception while idling for %s", getLogId());

                        pushReceiver.sleep(wakeLock, delayTime);

                        delayTime *= 2;
                        if (delayTime > MAX_DELAY_TIME) {
                            delayTime = MAX_DELAY_TIME;
                        }

                        idleFailureCount++;
                        if (idleFailureCount > IDLE_FAILURE_COUNT_LIMIT) {
                            Timber.e("Disabling pusher for %s after %d consecutive errors", getLogId(), idleFailureCount);
                            pushReceiver.pushError("Push disabled for " + getServerId() + " after " + idleFailureCount +
                                    " consecutive errors", e);
                            stop = true;
                        }
                    }
                }
            }

            pushReceiver.setPushActive(getServerId(), false);

            try {
                if (K9MailLib.isDebug()) {
                    Timber.i("Pusher for %s is exiting", getLogId());
                }

                close();
            } catch (Exception me) {
                Timber.e(me, "Got exception while closing for %s", getLogId());
            } finally {
                wakeLock.release();
            }
        }

        private void reacquireWakeLockAndCleanUp() {
            wakeLock.acquire(PUSH_WAKE_LOCK_TIMEOUT);

            clearStoredUntaggedResponses();
            idling = false;
            pushReceiver.setPushActive(getServerId(), false);

            try {
                connection.close();
            } catch (Exception me) {
                Timber.e(me, "Got exception while closing for exception for %s", getLogId());
            }

            connection = null;
        }

        private long getNewUidNext() throws MessagingException {
            long newUidNext = uidNext;
            if (newUidNext != -1L) {
                return newUidNext;
            }

            if (K9MailLib.isDebug()) {
                Timber.d("uidNext is -1, using search to find highest UID");
            }

            long highestUid = getHighestUid();
            if (highestUid == -1L) {
                return -1L;
            }

            newUidNext = highestUid + 1;

            if (K9MailLib.isDebug()) {
                Timber.d("highest UID = %d, set newUidNext to %d", highestUid, newUidNext);
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
            pushReceiver.setPushActive(getServerId(), true);
            idling = true;
        }

        private void sendIdle(ImapConnection conn) throws MessagingException, IOException {
            String tag = conn.sendCommand(Commands.IDLE, false);

            List<ImapResponse> responses;
            try {
                try {
                    responses = conn.readStatusResponse(tag, Commands.IDLE, this);
                } finally {
                    idleStopper.stopAcceptingDoneContinuation();
                }
            } catch (IOException e) {
                conn.close();
                throw e;
            }

            handleUntaggedResponses(responses);
        }

        private void returnFromIdle() {
            idling = false;
            delayTime = NORMAL_DELAY_TIME;
            idleFailureCount = 0;
        }

        private boolean openConnectionIfNecessary() throws MessagingException {
            ImapConnection oldConnection = connection;
            internalOpen(OPEN_MODE_RO);

            ImapConnection conn = connection;

            checkConnectionNotNull(conn);
            checkConnectionIdleCapable(conn);

            return conn != oldConnection;
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
                stop = true;

                String message = "IMAP server is not IDLE capable: " + conn.toString();
                pushReceiver.pushError(message, null);

                throw new MessagingException(message);
            }
        }

        private void setReadTimeoutForIdle(ImapConnection conn) throws SocketException {
            int idleRefreshTimeout = store.getStoreConfig().getIdleRefreshMinutes() * 60 * 1000;
            conn.setReadTimeout(idleRefreshTimeout + IDLE_READ_TIMEOUT_INCREMENT);
        }

        @Override
        public void handleAsyncUntaggedResponse(ImapResponse response) {
            if (K9MailLib.isDebug()) {
                Timber.v("Got async response: %s", response);
            }

            if (stop) {
                if (K9MailLib.isDebug()) {
                    Timber.d("Got async untagged response: %s, but stop is set for %s", response, getLogId());
                }

                idleStopper.stopIdle();
            } else {
                if (response.getTag() == null) {
                    if (response.size() > 1) {
                        Object responseType = response.get(1);
                        if (equalsIgnoreCase(responseType, "EXISTS") || equalsIgnoreCase(responseType, "EXPUNGE") ||
                                equalsIgnoreCase(responseType, "FETCH")) {

                            wakeLock.acquire(PUSH_WAKE_LOCK_TIMEOUT);

                            if (K9MailLib.isDebug()) {
                                Timber.d("Got useful async untagged response: %s for %s", response, getLogId());
                            }

                            idleStopper.stopIdle();
                        }
                    } else if (response.isContinuationRequested()) {
                        if (K9MailLib.isDebug()) {
                            Timber.d("Idling %s", getLogId());
                        }

                        idleStopper.startAcceptingDoneContinuation(connection);
                        wakeLock.release();
                    }
                }
            }
        }

        private void clearStoredUntaggedResponses() {
            synchronized (storedUntaggedResponses) {
                storedUntaggedResponses.clear();
            }
        }

        private void processStoredUntaggedResponses() throws MessagingException {
            while (true) {
                List<ImapResponse> untaggedResponses = getAndClearStoredUntaggedResponses();
                if (untaggedResponses.isEmpty()) {
                    break;
                }

                if (K9MailLib.isDebug()) {
                    Timber.i("Processing %d untagged responses from previous commands for %s",
                            untaggedResponses.size(), getLogId());
                }

                processUntaggedResponses(untaggedResponses);
            }
        }

        private List<ImapResponse> getAndClearStoredUntaggedResponses() {
            synchronized (storedUntaggedResponses) {
                if (storedUntaggedResponses.isEmpty()) {
                    return Collections.emptyList();
                }

                List<ImapResponse> untaggedResponses = new ArrayList<>(storedUntaggedResponses);
                storedUntaggedResponses.clear();

                return untaggedResponses;
            }
        }

        private void processUntaggedResponses(List<ImapResponse> responses) throws MessagingException {
            boolean skipSync = false;

            int oldMessageCount = messageCount;
            if (oldMessageCount == -1) {
                skipSync = true;
            }

            List<Long> flagSyncMsgSeqs = new ArrayList<>();
            List<String> removeMsgUids = new LinkedList<>();

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
                Timber.d("UIDs for messages needing flag sync are %s for %s", flagSyncMsgSeqs, getLogId());
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
            superHandleUntaggedResponse(response);

            int messageCountDelta = 0;
            if (response.getTag() == null && response.size() > 1) {
                try {
                    Object responseType = response.get(1);
                    if (equalsIgnoreCase(responseType, "FETCH")) {
                        Timber.i("Got FETCH %s", response);

                        long msgSeq = response.getLong(0);

                        if (K9MailLib.isDebug()) {
                            Timber.d("Got untagged FETCH for msgseq %d for %s", msgSeq, getLogId());
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
                            Timber.d("Got untagged EXPUNGE for msgseq %d for %s", msgSeq, getLogId());
                        }

                        List<Long> newSeqs = new ArrayList<>();
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

                        List<Long> msgSeqs = new ArrayList<>(msgSeqUidMap.keySet());
                        Collections.sort(msgSeqs);  // Have to do comparisons in order because of msgSeq reductions

                        for (long msgSeqNum : msgSeqs) {
                            if (K9MailLib.isDebug()) {
                                Timber.v("Comparing EXPUNGEd msgSeq %d to %d", msgSeq, msgSeqNum);
                            }

                            if (msgSeqNum == msgSeq) {
                                String uid = msgSeqUidMap.get(msgSeqNum);

                                if (K9MailLib.isDebug()) {
                                    Timber.d("Scheduling removal of UID %s because msgSeq %d was expunged", uid, msgSeqNum);
                                }

                                removeMsgUids.add(uid);
                                msgSeqUidMap.remove(msgSeqNum);
                            } else if (msgSeqNum > msgSeq) {
                                String uid = msgSeqUidMap.get(msgSeqNum);

                                if (K9MailLib.isDebug()) {
                                    Timber.d("Reducing msgSeq for UID %s from %d to %d", uid, msgSeqNum, (msgSeqNum - 1));
                                }

                                msgSeqUidMap.remove(msgSeqNum);
                                msgSeqUidMap.put(msgSeqNum - 1, uid);
                            }
                        }
                    }
                } catch (Exception e) {
                    Timber.e(e, "Could not handle untagged FETCH for %s", getLogId());
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
                    Timber.i("Got newUid %s for message %d on %s", newUid, end, getLogId());
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
                        Timber.i("Needs sync from uid %d to %d for %s", startUid, newUid, getLogId());
                    }

                    List<Message> messages = new ArrayList<>();
                    for (long uid = startUid; uid <= newUid; uid++) {
                        ImapMessage message = new ImapMessage(Long.toString(uid));
                        messages.add(message);
                    }

                    if (!messages.isEmpty()) {
                        pushReceiver.messagesArrived(getServerId(), messages);
                    }
                }
            }
        }

        private void syncMessages(List<Long> flagSyncMsgSeqs) {
            try {
                Set<Long> messageSeqSet = new HashSet<>(flagSyncMsgSeqs);
                List<? extends Message> messageList = getMessages(messageSeqSet, true, null);

                List<Message> messages = new ArrayList<>(messageList);
                pushReceiver.messagesFlagsChanged(getServerId(), messages);
            } catch (Exception e) {
                pushReceiver.pushError("Exception while processing Push untagged responses", e);
            }
        }

        private void removeMessages(List<String> removeUids) {
            List<Message> messages = new ArrayList<>(removeUids.size());

            try {
                List<ImapMessage> existingMessages = getMessagesFromUids(removeUids);
                for (Message existingMessage : existingMessages) {
                    needsPoll = true;
                    msgSeqUidMap.clear();

                    String existingUid = existingMessage.getUid();
                    Timber.w("Message with UID %s still exists on server, not expunging", existingUid);

                    removeUids.remove(existingUid);
                }

                for (String uid : removeUids) {
                    ImapMessage message = new ImapMessage(uid);

                    try {
                        message.setFlag(Flag.DELETED, true);
                    } catch (MessagingException me) {
                        Timber.e("Unable to set DELETED flag on message %s", message.getUid());
                    }

                    messages.add(message);
                }

                pushReceiver.messagesRemoved(getServerId(), messages);
            } catch (Exception e) {
                Timber.e("Cannot remove EXPUNGEd messages");
            }
        }

        private void syncFolderOnConnect() throws MessagingException {
            processStoredUntaggedResponses();

            if (messageCount == -1) {
                throw new MessagingException("Message count = -1 for idling");
            }

            pushReceiver.syncFolder(getServerId());
        }

        private void notifyMessagesArrived(long startUid, long uidNext) {
            if (K9MailLib.isDebug()) {
                Timber.i("Needs sync from uid %d to %d for %s", startUid, uidNext, getLogId());
            }

            int count = (int) (uidNext - startUid);
            List<Message> messages = new ArrayList<>(count);

            for (long uid = startUid; uid < uidNext; uid++) {
                ImapMessage message = new ImapMessage(Long.toString(uid));
                messages.add(message);
            }

            pushReceiver.messagesArrived(getServerId(), messages);
        }

        private long getOldUidNext() {
            long oldUidNext = -1L;
            try {
                String serializedPushState = pushReceiver.getPushState(getServerId());
                ImapPushState pushState = ImapPushState.parse(serializedPushState);
                oldUidNext = pushState.uidNext;

                if (K9MailLib.isDebug()) {
                    Timber.i("Got oldUidNext %d for %s", oldUidNext, getLogId());
                }
            } catch (Exception e) {
                Timber.e(e, "Unable to get oldUidNext for %s", getLogId());
            }

            return oldUidNext;
        }
    }

    /**
     * Ensure the DONE continuation is only sent when the IDLE command was sent and hasn't completed yet.
     */
    private static class IdleStopper {
        private boolean acceptDoneContinuation = false;
        private ImapConnection imapConnection;


        public synchronized void startAcceptingDoneContinuation(ImapConnection connection) {
            if (connection == null) {
                throw new NullPointerException("connection must not be null");
            }

            acceptDoneContinuation = true;
            imapConnection = connection;
        }

        public synchronized void stopAcceptingDoneContinuation() {
            acceptDoneContinuation = false;
            imapConnection = null;
        }

        public synchronized void stopIdle() {
            if (acceptDoneContinuation) {
                acceptDoneContinuation = false;
                sendDone();
            }
        }

        private void sendDone() {
            try {
                imapConnection.setReadTimeout(SOCKET_READ_TIMEOUT);
                imapConnection.sendContinuation("DONE");
            } catch (IOException e) {
                imapConnection.close();
            }
        }
    }
}
