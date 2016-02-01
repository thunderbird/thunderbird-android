package com.fsck.k9.mail.store.imap;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import android.text.TextUtils;
import android.util.Log;

import com.fsck.k9.mail.Body;
import com.fsck.k9.mail.FetchProfile;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.K9MailLib;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessageRetrievalListener;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Part;
import com.fsck.k9.mail.filter.EOLConvertingOutputStream;
import com.fsck.k9.mail.internet.MimeBodyPart;
import com.fsck.k9.mail.internet.MimeHeader;
import com.fsck.k9.mail.internet.MimeMessageHelper;
import com.fsck.k9.mail.internet.MimeMultipart;
import com.fsck.k9.mail.internet.MimeUtility;
import com.fsck.k9.mail.store.imap.ImapStore.ImapMessage;
import com.fsck.k9.mail.store.imap.ImapStore.ImapSearcher;

import static com.fsck.k9.mail.K9MailLib.LOG_TAG;


class ImapFolder extends Folder<ImapMessage> {
    private static final int MORE_MESSAGES_WINDOW_SIZE = 500;
    private static final int FETCH_WINDOW_SIZE = 100;
    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    private String mName;
    protected volatile int mMessageCount = -1;
    protected volatile long uidNext = -1L;
    protected volatile ImapConnection mConnection;
    private int mMode;
    private volatile boolean mExists;
    protected ImapStore store = null;
    Map<Long, String> msgSeqUidMap = new ConcurrentHashMap<Long, String>();
    private boolean mInSearch = false;

    public ImapFolder(ImapStore nStore, String name) {
        super();
        store = nStore;
        this.mName = name;
    }

    public String getPrefixedName() throws MessagingException {
        String prefixedName = "";
        if (!store.getStoreConfig().getInboxFolderName().equalsIgnoreCase(mName)) {
            ImapConnection connection;
            synchronized (this) {
                if (mConnection == null) {
                    connection = store.getConnection();
                } else {
                    connection = mConnection;
                }
            }
            try {

                connection.open();
            } catch (IOException ioe) {
                throw new MessagingException("Unable to get IMAP prefix", ioe);
            } finally {
                if (mConnection == null) {
                    store.releaseConnection(connection);
                }
            }
            prefixedName = store.getCombinedPrefix();
        }

        prefixedName += mName;
        return prefixedName;
    }

    protected List<ImapResponse> executeSimpleCommand(String command) throws MessagingException, IOException {
        return handleUntaggedResponses(mConnection.executeSimpleCommand(command));
    }

    protected List<ImapResponse> executeSimpleCommand(String command, boolean sensitve, UntaggedHandler untaggedHandler) throws MessagingException, IOException {
        return handleUntaggedResponses(mConnection.executeSimpleCommand(command, sensitve, untaggedHandler));
    }

    @Override
    public void open(int mode) throws MessagingException {
        internalOpen(mode);

        if (mMessageCount == -1) {
            throw new MessagingException(
                "Did not find message count during open");
        }
    }

    public List<ImapResponse> internalOpen(int mode) throws MessagingException {
        if (isOpen() && mMode == mode) {
            // Make sure the connection is valid. If it's not we'll close it down and continue
            // on to get a new one.
            try {
                return executeSimpleCommand(Commands.NOOP);
            } catch (IOException ioe) {
                /* don't throw */ ioExceptionHandler(mConnection, ioe);
            }
        }
        store.releaseConnection(mConnection);
        synchronized (this) {
            mConnection = store.getConnection();
        }
        // * FLAGS (\Answered \Flagged \Deleted \Seen \Draft NonJunk
        // $MDNSent)
        // * OK [PERMANENTFLAGS (\Answered \Flagged \Deleted \Seen \Draft
        // NonJunk $MDNSent \*)] Flags permitted.
        // * 23 EXISTS
        // * 0 RECENT
        // * OK [UIDVALIDITY 1125022061] UIDs valid
        // * OK [UIDNEXT 57576] Predicted next UID
        // 2 OK [READ-WRITE] Select completed.
        try {
            msgSeqUidMap.clear();
            String command = String.format("%s %s", mode == OPEN_MODE_RW ? "SELECT"
                    : "EXAMINE", ImapStore.encodeString(store.encodeFolderName(getPrefixedName())));

            List<ImapResponse> responses = executeSimpleCommand(command);

            /*
             * If the command succeeds we expect the folder has been opened read-write unless we
             * are notified otherwise in the responses.
             */
            mMode = mode;

            for (ImapResponse response : responses) {
                if (response.size() >= 2) {
                    Object bracketedObj = response.get(1);
                    if (!(bracketedObj instanceof ImapList)) {
                        continue;
                    }
                    ImapList bracketed = (ImapList) bracketedObj;
                    if (bracketed.isEmpty()) {
                        continue;
                    }

                    ImapList flags = bracketed.getKeyedList("PERMANENTFLAGS");
                    if (flags != null) {
                        // parse: * OK [PERMANENTFLAGS (\Answered \Flagged \Deleted
                        // \Seen \Draft NonJunk $label1 \*)] Flags permitted.
                        parseFlags(flags);
                    } else {
                        Object keyObj = bracketed.get(0);
                        if (keyObj instanceof String) {
                            String key = (String) keyObj;
                            if (response.getTag() != null) {

                                if ("READ-ONLY".equalsIgnoreCase(key)) {
                                    mMode = OPEN_MODE_RO;
                                } else if ("READ-WRITE".equalsIgnoreCase(key)) {
                                    mMode = OPEN_MODE_RW;
                                }
                            }
                        }
                    }
                }
            }
            mExists = true;
            return responses;
        } catch (IOException ioe) {
            throw ioExceptionHandler(mConnection, ioe);
        } catch (MessagingException me) {
            Log.e(LOG_TAG, "Unable to open connection for " + getLogId(), me);
            throw me;
        }
    }

    /**
     * Parses an string like PERMANENTFLAGS (\Answered \Flagged \Deleted // \Seen \Draft NonJunk
     * $label1 \*)
     *
     * the parsed flags are stored in the mPermanentFlagsIndex
     * @param flags
     *            the imapflags as strings
     */
    private void parseFlags(ImapList flags) {
        for (Object flag : flags) {
            flag = flag.toString().toLowerCase(Locale.US);
            if (flag.equals("\\deleted")) {
                store.getPermanentFlagsIndex().add(Flag.DELETED);
            } else if (flag.equals("\\answered")) {
                store.getPermanentFlagsIndex().add(Flag.ANSWERED);
            } else if (flag.equals("\\seen")) {
                store.getPermanentFlagsIndex().add(Flag.SEEN);
            } else if (flag.equals("\\flagged")) {
                store.getPermanentFlagsIndex().add(Flag.FLAGGED);
            } else if (flag.equals("$forwarded")) {
                store.getPermanentFlagsIndex().add(Flag.FORWARDED);
            } else if (flag.equals("\\*")) {
                mCanCreateKeywords = true;
            }
        }
    }

    @Override
    public boolean isOpen() {
        return mConnection != null;
    }

    @Override
    public int getMode() {
        return mMode;
    }

    @Override
    public void close() {
        if (mMessageCount != -1) {
            mMessageCount = -1;
        }
        if (!isOpen()) {
            return;
        }

        synchronized (this) {
            // If we are mid-search and we get a close request, we gotta trash the connection.
            if (mInSearch && mConnection != null) {
                Log.i(LOG_TAG, "IMAP search was aborted, shutting down connection.");
                mConnection.close();
            } else {
                store.releaseConnection(mConnection);
            }
            mConnection = null;
        }
    }

    @Override
    public String getName() {
        return mName;
    }

    /**
     * Check if a given folder exists on the server.
     *
     * @param folderName
     *     The name of the folder encoded as quoted string.
     *     See {@link ImapStore#encodeString}
     *
     * @return
     *     {@code True}, if the folder exists. {@code False}, otherwise.
     */
    private boolean exists(String folderName) throws MessagingException {
        try {
            // Since we don't care about RECENT, we'll use that for the check, because we're checking
            // a folder other than ourself, and don't want any untagged responses to cause a change
            // in our own fields
            mConnection.executeSimpleCommand(String.format("STATUS %s (RECENT)", folderName));
            return true;
        } catch (IOException ioe) {
            throw ioExceptionHandler(mConnection, ioe);
        } catch (NegativeImapResponseException ie) {
            // We got a response, but it was not "OK"
            return false;
        }
    }

    @Override
    public boolean exists() throws MessagingException {
        if (mExists) {
            return true;
        }
        /*
         * This method needs to operate in the unselected mode as well as the selected mode
         * so we must get the connection ourselves if it's not there. We are specifically
         * not calling checkOpen() since we don't care if the folder is open.
         */
        ImapConnection connection;
        synchronized (this) {
            if (mConnection == null) {
                connection = store.getConnection();
            } else {
                connection = mConnection;
            }
        }
        try {
            connection.executeSimpleCommand(String.format("STATUS %s (UIDVALIDITY)",
                                            ImapStore.encodeString(store.encodeFolderName(getPrefixedName()))));
            mExists = true;
            return true;
        } catch (NegativeImapResponseException ie) {
            // We got a response, but it was not "OK"
            return false;
        } catch (IOException ioe) {
            throw ioExceptionHandler(connection, ioe);
        } finally {
            if (mConnection == null) {
                store.releaseConnection(connection);
            }
        }
    }

    @Override
    public boolean create(FolderType type) throws MessagingException {
        /*
         * This method needs to operate in the unselected mode as well as the selected mode
         * so we must get the connection ourselves if it's not there. We are specifically
         * not calling checkOpen() since we don't care if the folder is open.
         */
        ImapConnection connection;
        synchronized (this) {
            if (mConnection == null) {
                connection = store.getConnection();
            } else {
                connection = mConnection;
            }
        }
        try {
            connection.executeSimpleCommand(String.format("CREATE %s",
                                            ImapStore.encodeString(store.encodeFolderName(getPrefixedName()))));
            return true;
        } catch (NegativeImapResponseException ie) {
            // We got a response, but it was not "OK"
            return false;
        } catch (IOException ioe) {
            throw ioExceptionHandler(mConnection, ioe);
        } finally {
            if (mConnection == null) {
                store.releaseConnection(connection);
            }
        }
    }

    /**
     * Copies the given messages to the specified folder.
     *
     * <p>
     * <strong>Note:</strong>
     * Only the UIDs of the given {@link Message} instances are used. It is assumed that all
     * UIDs represent valid messages in this folder.
     * </p>
     *
     * @param messages
     *         The messages to copy to the specfied folder.
     * @param folder
     *         The name of the target folder.
     *
     * @return The mapping of original message UIDs to the new server UIDs.
     */
    @Override
    public Map<String, String> copyMessages(List<? extends Message> messages, Folder folder)
            throws MessagingException {
        if (!(folder instanceof ImapFolder)) {
            throw new MessagingException("ImapFolder.copyMessages passed non-ImapFolder");
        }

        if (messages.isEmpty()) {
            return null;
        }

        ImapFolder iFolder = (ImapFolder)folder;
        checkOpen(); //only need READ access

        String[] uids = new String[messages.size()];
        for (int i = 0, count = messages.size(); i < count; i++) {
            uids[i] = messages.get(i).getUid();
        }

        try {
            String remoteDestName = ImapStore.encodeString(store.encodeFolderName(iFolder.getPrefixedName()));

            //TODO: Try to copy/move the messages first and only create the folder if the
            //      operation fails. This will save a roundtrip if the folder already exists.
            if (!exists(remoteDestName)) {
                /*
                 * If the remote folder doesn't exist we try to create it.
                 */
                if (K9MailLib.isDebug()) {
                    Log.i(LOG_TAG, "ImapFolder.copyMessages: attempting to create remote " +
                            "folder '" + remoteDestName + "' for " + getLogId());
                }

                iFolder.create(FolderType.HOLDS_MESSAGES);
            }

            //TODO: Split this into multiple commands if the command exceeds a certain length.
            List<ImapResponse> responses = executeSimpleCommand(String.format("UID COPY %s %s",
                                                  combine(uids, ','),
                                                  remoteDestName));

            // Get the tagged response for the UID COPY command
            ImapResponse response = responses.get(responses.size() - 1);

            Map<String, String> uidMap = null;
            if (response.size() > 1) {
                /*
                 * If the server supports UIDPLUS, then along with the COPY response it will
                 * return an COPYUID response code, e.g.
                 *
                 * 24 OK [COPYUID 38505 304,319:320 3956:3958] Success
                 *
                 * COPYUID is followed by UIDVALIDITY, the set of UIDs of copied messages from
                 * the source folder and the set of corresponding UIDs assigned to them in the
                 * destination folder.
                 *
                 * We can use the new UIDs included in this response to update our records.
                 */
                Object responseList = response.get(1);

                if (responseList instanceof ImapList) {
                    final ImapList copyList = (ImapList) responseList;
                    if (copyList.size() >= 4 && copyList.getString(0).equals("COPYUID")) {
                        List<String> srcUids = ImapUtility.getImapSequenceValues(
                                copyList.getString(2));
                        List<String> destUids = ImapUtility.getImapSequenceValues(
                                copyList.getString(3));

                        if (srcUids != null && destUids != null) {
                            if (srcUids.size() == destUids.size()) {
                                Iterator<String> srcUidsIterator = srcUids.iterator();
                                Iterator<String> destUidsIterator = destUids.iterator();
                                uidMap = new HashMap<String, String>();
                                while (srcUidsIterator.hasNext() &&
                                        destUidsIterator.hasNext()) {
                                    String srcUid = srcUidsIterator.next();
                                    String destUid = destUidsIterator.next();
                                    uidMap.put(srcUid, destUid);
                                }
                            } else {
                                if (K9MailLib.isDebug()) {
                                    Log.v(LOG_TAG, "Parse error: size of source UIDs " +
                                            "list is not the same as size of destination " +
                                            "UIDs list.");
                                }
                            }
                        } else {
                            if (K9MailLib.isDebug()) {
                                Log.v(LOG_TAG, "Parsing of the sequence set failed.");
                            }
                        }
                    }
                }
            }

            return uidMap;
        } catch (IOException ioe) {
            throw ioExceptionHandler(mConnection, ioe);
        }
    }

    @Override
    public Map<String, String> moveMessages(List<? extends Message> messages, Folder folder) throws MessagingException {
        if (messages.isEmpty())
            return null;
        Map<String, String> uidMap = copyMessages(messages, folder);
        setFlags(messages, Collections.singleton(Flag.DELETED), true);
        return uidMap;
    }

    @Override
    public void delete(List<? extends Message> messages, String trashFolderName) throws MessagingException {
        if (messages.isEmpty())
            return;

        if (trashFolderName == null || getName().equalsIgnoreCase(trashFolderName)) {
            setFlags(messages, Collections.singleton(Flag.DELETED), true);
        } else {
            ImapFolder remoteTrashFolder = (ImapFolder)getStore().getFolder(trashFolderName);
            String remoteTrashName = ImapStore.encodeString(store.encodeFolderName(remoteTrashFolder.getPrefixedName()));

            if (!exists(remoteTrashName)) {
                /*
                 * If the remote trash folder doesn't exist we try to create it.
                 */
                if (K9MailLib.isDebug())
                    Log.i(LOG_TAG, "IMAPMessage.delete: attempting to create remote '" + trashFolderName + "' folder for " + getLogId());
                remoteTrashFolder.create(FolderType.HOLDS_MESSAGES);
            }

            if (exists(remoteTrashName)) {
                if (K9MailLib.isDebug())
                    Log.d(LOG_TAG, "IMAPMessage.delete: copying remote " + messages.size() + " messages to '" + trashFolderName + "' for " + getLogId());

                moveMessages(messages, remoteTrashFolder);
            } else {
                throw new MessagingException("IMAPMessage.delete: remote Trash folder " + trashFolderName + " does not exist and could not be created for " + getLogId()
                                             , true);
            }
        }
    }


    @Override
    public int getMessageCount() {
        return mMessageCount;
    }


    private int getRemoteMessageCount(String criteria) throws MessagingException {
        checkOpen(); //only need READ access
        try {
            int count = 0;
            int start = 1;

            List<ImapResponse> responses = executeSimpleCommand(String.format(Locale.US, "SEARCH %d:* %s", start, criteria));
            for (ImapResponse response : responses) {
                if (ImapResponseParser.equalsIgnoreCase(response.get(0), "SEARCH")) {
                    count += response.size() - 1;
                }
            }
            return count;
        } catch (IOException ioe) {
            throw ioExceptionHandler(mConnection, ioe);
        }


    }

    @Override
    public int getUnreadMessageCount() throws MessagingException {
        return getRemoteMessageCount("UNSEEN NOT DELETED");
    }

    @Override
    public int getFlaggedMessageCount() throws MessagingException {
        return getRemoteMessageCount("FLAGGED NOT DELETED");
    }

    protected long getHighestUid() {
        try {
            ImapSearcher searcher = new ImapSearcher() {
                @Override
                public List<ImapResponse> search() throws IOException, MessagingException {
                    return executeSimpleCommand("UID SEARCH *:*");
                }
            };
            List<? extends Message> messages = search(searcher, null);
            if (messages.size() > 0) {
                return Long.parseLong(messages.get(0).getUid());
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Unable to find highest UID in folder " + getName(), e);
        }
        return -1L;

    }

    @Override
    public void delete(boolean recurse) throws MessagingException {
        throw new Error("ImapStore.delete() not yet implemented");
    }

    @Override
    public ImapMessage getMessage(String uid) throws MessagingException {
        return new ImapMessage(uid, this);
    }


    @Override
    public List<ImapMessage> getMessages(int start, int end, Date earliestDate, MessageRetrievalListener<ImapMessage> listener)
    throws MessagingException {
        return getMessages(start, end, earliestDate, false, listener);
    }

    protected List<ImapMessage> getMessages(final int start, final int end, Date earliestDate, final boolean includeDeleted, final MessageRetrievalListener<ImapMessage> listener)
    throws MessagingException {
        if (start < 1 || end < 1 || end < start) {
            throw new MessagingException(
                String.format(Locale.US, "Invalid message set %d %d",
                              start, end));
        }

        final String dateSearchString = getDateSearchString(earliestDate);

        ImapSearcher searcher = new ImapSearcher() {
            @Override
            public List<ImapResponse> search() throws IOException, MessagingException {
                return executeSimpleCommand(String.format(Locale.US, "UID SEARCH %d:%d%s%s", start, end, dateSearchString, includeDeleted ? "" : " NOT DELETED"));
            }
        };
        return search(searcher, listener);

    }

    private String getDateSearchString(Date earliestDate) {
        if (earliestDate == null) {
            return "";
        }

        synchronized (ImapStore.RFC3501_DATE) {
            return " SINCE " + ImapStore.RFC3501_DATE.format(earliestDate);
        }
    }

    @Override
    public boolean areMoreMessagesAvailable(int indexOfOldestMessage, Date earliestDate)
            throws IOException, MessagingException {

        checkOpen();

        if (indexOfOldestMessage == 1) {
            return false;
        }

        String dateSearchString = getDateSearchString(earliestDate);

        int endIndex = indexOfOldestMessage - 1;

        while (endIndex > 0) {
            int startIndex = Math.max(0, endIndex - MORE_MESSAGES_WINDOW_SIZE) + 1;

            if (existsNonDeletedMessageInRange(startIndex, endIndex, dateSearchString)) {
                return true;
            }

            endIndex = endIndex - MORE_MESSAGES_WINDOW_SIZE;
        }

        return false;
    }

    private boolean existsNonDeletedMessageInRange(int startIndex, int endIndex, String dateSearchString)
            throws MessagingException, IOException {

        String command = String.format(Locale.US, "SEARCH %d:%d%s NOT DELETED",
                startIndex, endIndex, dateSearchString);

        List<ImapResponse> responses = executeSimpleCommand(command);
        for (ImapResponse response : responses) {
            if (response.getTag() == null && ImapResponseParser.equalsIgnoreCase(response.get(0), "SEARCH")) {
                if (response.size() > 1) {
                    return true;
                }
            }
        }

        return false;
    }

    protected List<ImapMessage> getMessages(final List<Long> mesgSeqs,
                                                  final boolean includeDeleted,
                                                  final MessageRetrievalListener<ImapMessage> listener)
    throws MessagingException {
        ImapSearcher searcher = new ImapSearcher() {
            @Override
            public List<ImapResponse> search() throws IOException, MessagingException {
                return executeSimpleCommand(String.format("UID SEARCH %s%s", combine(mesgSeqs.toArray(), ','), includeDeleted ? "" : " NOT DELETED"));
            }
        };
        return search(searcher, listener);
    }

    protected List<? extends Message> getMessagesFromUids(final List<String> mesgUids,
                                                          final boolean includeDeleted,
                                                          final MessageRetrievalListener<ImapMessage> listener) throws MessagingException {
        ImapSearcher searcher = new ImapSearcher() {
            @Override
            public List<ImapResponse> search() throws IOException, MessagingException {
                return executeSimpleCommand(String.format("UID SEARCH UID %s%s",
                        combine(mesgUids.toArray(), ','), includeDeleted ? "" : " NOT DELETED"));
            }
        };
        return search(searcher, listener);
    }

    protected List<ImapMessage> search(ImapSearcher searcher, MessageRetrievalListener<ImapMessage> listener) throws MessagingException {
        checkOpen(); //only need READ access
        List<ImapMessage> messages = new ArrayList<ImapMessage>();
        try {
            List<Long> uids = new ArrayList<Long>();
            List<ImapResponse> responses = searcher.search(); //
            for (ImapResponse response : responses) {
                if (response.getTag() == null) {
                    if (ImapResponseParser.equalsIgnoreCase(response.get(0), "SEARCH")) {
                        for (int i = 1, count = response.size(); i < count; i++) {
                            uids.add(response.getLong(i));
                        }
                    }
                }
            }

            // Sort the uids in numerically decreasing order
            // By doing it in decreasing order, we ensure newest messages are dealt with first
            // This makes the most sense when a limit is imposed, and also prevents UI from going
            // crazy adding stuff at the top.
            Collections.sort(uids, Collections.reverseOrder());

            for (int i = 0, count = uids.size(); i < count; i++) {
                String uid = uids.get(i).toString();
                if (listener != null) {
                    listener.messageStarted(uid, i, count);
                }
                ImapMessage message = new ImapMessage(uid, this);
                messages.add(message);
                if (listener != null) {
                    listener.messageFinished(message, i, count);
                }
            }
        } catch (IOException ioe) {
            throw ioExceptionHandler(mConnection, ioe);
        }
        return messages;
    }


    @Override
    public List<ImapMessage> getMessages(MessageRetrievalListener<ImapMessage> listener) throws MessagingException {
        return getMessages(null, listener);
    }

    @Override
    public List<ImapMessage> getMessages(String[] uids, MessageRetrievalListener<ImapMessage> listener)
    throws MessagingException {
        checkOpen(); //only need READ access
        List<ImapMessage> messages = new ArrayList<ImapMessage>();
        try {
            if (uids == null) {
                List<ImapResponse> responses = executeSimpleCommand("UID SEARCH 1:* NOT DELETED");
                List<String> tempUids = new ArrayList<String>();
                for (ImapResponse response : responses) {
                    if (ImapResponseParser.equalsIgnoreCase(response.get(0), "SEARCH")) {
                        for (int i = 1, count = response.size(); i < count; i++) {
                            tempUids.add(response.getString(i));
                        }
                    }
                }
                uids = tempUids.toArray(EMPTY_STRING_ARRAY);
            }
            for (int i = 0, count = uids.length; i < count; i++) {
                if (listener != null) {
                    listener.messageStarted(uids[i], i, count);
                }
                ImapMessage message = new ImapMessage(uids[i], this);
                messages.add(message);
                if (listener != null) {
                    listener.messageFinished(message, i, count);
                }
            }
        } catch (IOException ioe) {
            throw ioExceptionHandler(mConnection, ioe);
        }
        return messages;
    }

    @Override
    public void fetch(List<ImapMessage> messages, FetchProfile fp, MessageRetrievalListener<ImapMessage> listener)
    throws MessagingException {
        if (messages == null || messages.isEmpty()) {
            return;
        }
        checkOpen(); //only need READ access
        List<String> uids = new ArrayList<String>(messages.size());
        HashMap<String, Message> messageMap = new HashMap<String, Message>();
        for (Message msg : messages) {
            String uid = msg.getUid();
            uids.add(uid);
            messageMap.put(uid, msg);
        }

        /*
         * Figure out what command we are going to run:
         * Flags - UID FETCH (FLAGS)
         * Envelope - UID FETCH ([FLAGS] INTERNALDATE UID RFC822.SIZE FLAGS BODY.PEEK[HEADER.FIELDS (date subject from content-type to cc)])
         *
         */
        Set<String> fetchFields = new LinkedHashSet<String>();
        fetchFields.add("UID");
        if (fp.contains(FetchProfile.Item.FLAGS)) {
            fetchFields.add("FLAGS");
        }
        if (fp.contains(FetchProfile.Item.ENVELOPE)) {
            fetchFields.add("INTERNALDATE");
            fetchFields.add("RFC822.SIZE");
            fetchFields.add("BODY.PEEK[HEADER.FIELDS (date subject from content-type to cc " +
                    "reply-to message-id references in-reply-to " + K9MailLib.IDENTITY_HEADER + ")]");
        }
        if (fp.contains(FetchProfile.Item.STRUCTURE)) {
            fetchFields.add("BODYSTRUCTURE");
        }
        if (fp.contains(FetchProfile.Item.BODY_SANE)) {
            // If the user wants to download unlimited-size messages, don't go only for the truncated body
            if (store.getStoreConfig().getMaximumAutoDownloadMessageSize() > 0) {
                fetchFields.add(String.format(Locale.US, "BODY.PEEK[]<0.%d>", store.getStoreConfig().getMaximumAutoDownloadMessageSize()));
            } else {
                fetchFields.add("BODY.PEEK[]");
            }
        }
        if (fp.contains(FetchProfile.Item.BODY)) {
            fetchFields.add("BODY.PEEK[]");
        }



        for (int windowStart = 0; windowStart < messages.size(); windowStart += (FETCH_WINDOW_SIZE)) {
            List<String> uidWindow = uids.subList(windowStart, Math.min((windowStart + FETCH_WINDOW_SIZE), messages.size()));

            try {
                mConnection.sendCommand(String.format("UID FETCH %s (%s)",
                                                      combine(uidWindow.toArray(new String[uidWindow.size()]), ','),
                                                      combine(fetchFields.toArray(new String[fetchFields.size()]), ' ')
                                                     ), false);
                ImapResponse response;
                int messageNumber = 0;

                ImapResponseCallback callback = null;
                if (fp.contains(FetchProfile.Item.BODY) || fp.contains(FetchProfile.Item.BODY_SANE)) {
                    callback = new FetchBodyCallback(messageMap);
                }

                do {
                    response = mConnection.readResponse(callback);

                    if (response.getTag() == null && ImapResponseParser.equalsIgnoreCase(response.get(1), "FETCH")) {
                        ImapList fetchList = (ImapList)response.getKeyedValue("FETCH");
                        String uid = fetchList.getKeyedString("UID");
                        long msgSeq = response.getLong(0);
                        if (uid != null) {
                            try {
                                msgSeqUidMap.put(msgSeq, uid);
                                if (K9MailLib.isDebug()) {
                                    Log.v(LOG_TAG, "Stored uid '" + uid + "' for msgSeq " + msgSeq + " into map " /*+ msgSeqUidMap.toString() */);
                                }
                            } catch (Exception e) {
                                Log.e(LOG_TAG, "Unable to store uid '" + uid + "' for msgSeq " + msgSeq);
                            }
                        }

                        Message message = messageMap.get(uid);
                        if (message == null) {
                            if (K9MailLib.isDebug())
                                Log.d(LOG_TAG, "Do not have message in messageMap for UID " + uid + " for " + getLogId());

                            handleUntaggedResponse(response);
                            continue;
                        }
                        if (listener != null) {
                            listener.messageStarted(uid, messageNumber++, messageMap.size());
                        }

                        ImapMessage imapMessage = (ImapMessage) message;

                        Object literal = handleFetchResponse(imapMessage, fetchList);

                        if (literal != null) {
                            if (literal instanceof String) {
                                String bodyString = (String)literal;
                                InputStream bodyStream = new ByteArrayInputStream(bodyString.getBytes());
                                imapMessage.parse(bodyStream);
                            } else if (literal instanceof Integer) {
                                // All the work was done in FetchBodyCallback.foundLiteral()
                            } else {
                                // This shouldn't happen
                                throw new MessagingException("Got FETCH response with bogus parameters");
                            }
                        }

                        if (listener != null) {
                            listener.messageFinished(imapMessage, messageNumber, messageMap.size());
                        }
                    } else {
                        handleUntaggedResponse(response);
                    }

                } while (response.getTag() == null);
            } catch (IOException ioe) {
                throw ioExceptionHandler(mConnection, ioe);
            }
        }
    }


    @Override
    public void fetchPart(Message message, Part part, MessageRetrievalListener<Message> listener)
    throws MessagingException {
        checkOpen(); //only need READ access

        String partId = part.getServerExtra();

        String fetch;
        if ("TEXT".equalsIgnoreCase(partId)) {
            fetch = String.format(Locale.US, "BODY.PEEK[TEXT]<0.%d>",
                    store.getStoreConfig().getMaximumAutoDownloadMessageSize());
        } else {
            fetch = String.format("BODY.PEEK[%s]", partId);
        }

        try {
            mConnection.sendCommand(
                String.format("UID FETCH %s (UID %s)", message.getUid(), fetch),
                false);

            ImapResponse response;
            int messageNumber = 0;

            ImapResponseCallback callback = new FetchPartCallback(part);

            do {
                response = mConnection.readResponse(callback);

                if ((response.getTag() == null) &&
                        (ImapResponseParser.equalsIgnoreCase(response.get(1), "FETCH"))) {
                    ImapList fetchList = (ImapList)response.getKeyedValue("FETCH");
                    String uid = fetchList.getKeyedString("UID");

                    if (!message.getUid().equals(uid)) {
                        if (K9MailLib.isDebug())
                            Log.d(LOG_TAG, "Did not ask for UID " + uid + " for " + getLogId());

                        handleUntaggedResponse(response);
                        continue;
                    }
                    if (listener != null) {
                        listener.messageStarted(uid, messageNumber++, 1);
                    }

                    ImapMessage imapMessage = (ImapMessage) message;

                    Object literal = handleFetchResponse(imapMessage, fetchList);

                    if (literal != null) {
                        if (literal instanceof Body) {
                            // Most of the work was done in FetchAttchmentCallback.foundLiteral()
                            MimeMessageHelper.setBody(part, (Body) literal);
                        } else if (literal instanceof String) {
                            String bodyString = (String)literal;
                            InputStream bodyStream = new ByteArrayInputStream(bodyString.getBytes());

                            String contentTransferEncoding = part
                                    .getHeader(MimeHeader.HEADER_CONTENT_TRANSFER_ENCODING)[0];
                            String contentType = part
                                    .getHeader(MimeHeader.HEADER_CONTENT_TYPE)[0];
                            MimeMessageHelper.setBody(part, MimeUtility.createBody(bodyStream,
                                    contentTransferEncoding, contentType));
                        } else {
                            // This shouldn't happen
                            throw new MessagingException("Got FETCH response with bogus parameters");
                        }
                    }

                    if (listener != null) {
                        listener.messageFinished(message, messageNumber, 1);
                    }
                } else {
                    handleUntaggedResponse(response);
                }

            } while (response.getTag() == null);
        } catch (IOException ioe) {
            throw ioExceptionHandler(mConnection, ioe);
        }
    }

    // Returns value of body field
    private Object handleFetchResponse(ImapMessage message, ImapList fetchList) throws MessagingException {
        Object result = null;
        if (fetchList.containsKey("FLAGS")) {
            ImapList flags = fetchList.getKeyedList("FLAGS");
            if (flags != null) {
                for (int i = 0, count = flags.size(); i < count; i++) {
                    String flag = flags.getString(i);
                    if (flag.equalsIgnoreCase("\\Deleted")) {
                        message.setFlagInternal(Flag.DELETED, true);
                    } else if (flag.equalsIgnoreCase("\\Answered")) {
                        message.setFlagInternal(Flag.ANSWERED, true);
                    } else if (flag.equalsIgnoreCase("\\Seen")) {
                        message.setFlagInternal(Flag.SEEN, true);
                    } else if (flag.equalsIgnoreCase("\\Flagged")) {
                        message.setFlagInternal(Flag.FLAGGED, true);
                    } else if (flag.equalsIgnoreCase("$Forwarded")) {
                        message.setFlagInternal(Flag.FORWARDED, true);
                        /* a message contains FORWARDED FLAG -> so we can also create them */
                        store.getPermanentFlagsIndex().add(Flag.FORWARDED);
                    }
                }
            }
        }

        if (fetchList.containsKey("INTERNALDATE")) {
            Date internalDate = fetchList.getKeyedDate("INTERNALDATE");
            message.setInternalDate(internalDate);
        }

        if (fetchList.containsKey("RFC822.SIZE")) {
            int size = fetchList.getKeyedNumber("RFC822.SIZE");
            message.setSize(size);
        }

        if (fetchList.containsKey("BODYSTRUCTURE")) {
            ImapList bs = fetchList.getKeyedList("BODYSTRUCTURE");
            if (bs != null) {
                try {
                    parseBodyStructure(bs, message, "TEXT");
                } catch (MessagingException e) {
                    if (K9MailLib.isDebug())
                        Log.d(LOG_TAG, "Error handling message for " + getLogId(), e);
                    message.setBody(null);
                }
            }
        }

        if (fetchList.containsKey("BODY")) {
            int index = fetchList.getKeyIndex("BODY") + 2;
            int size = fetchList.size();
            if (index < size) {
                result = fetchList.getObject(index);

                // Check if there's an origin octet
                if (result instanceof String) {
                    String originOctet = (String) result;
                    if (originOctet.startsWith("<") && (index + 1) < size) {
                        result = fetchList.getObject(index + 1);
                    }
                }
            }
        }

        return result;
    }

    /**
     * Handle any untagged responses that the caller doesn't care to handle themselves.
     */
    protected List<ImapResponse> handleUntaggedResponses(List<ImapResponse> responses) {
        for (ImapResponse response : responses) {
            handleUntaggedResponse(response);
        }
        return responses;
    }

    protected void handlePossibleUidNext(ImapResponse response) {
        if (ImapResponseParser.equalsIgnoreCase(response.get(0), "OK") && response.size() > 1) {
            Object bracketedObj = response.get(1);
            if (bracketedObj instanceof ImapList) {
                ImapList bracketed = (ImapList)bracketedObj;

                if (bracketed.size() > 1) {
                    Object keyObj = bracketed.get(0);
                    if (keyObj instanceof String) {
                        String key = (String)keyObj;
                        if ("UIDNEXT".equalsIgnoreCase(key)) {
                            uidNext = bracketed.getLong(1);
                            if (K9MailLib.isDebug())
                                Log.d(LOG_TAG, "Got UidNext = " + uidNext + " for " + getLogId());
                        }
                    }
                }


            }
        }
    }

    /**
     * Handle an untagged response that the caller doesn't care to handle themselves.
     */
    protected void handleUntaggedResponse(ImapResponse response) {
        if (response.getTag() == null && response.size() > 1) {
            if (ImapResponseParser.equalsIgnoreCase(response.get(1), "EXISTS")) {
                mMessageCount = response.getNumber(0);
                if (K9MailLib.isDebug())
                    Log.d(LOG_TAG, "Got untagged EXISTS with value " + mMessageCount + " for " + getLogId());
            }
            handlePossibleUidNext(response);

            if (ImapResponseParser.equalsIgnoreCase(response.get(1), "EXPUNGE") && mMessageCount > 0) {
                mMessageCount--;
                if (K9MailLib.isDebug())
                    Log.d(LOG_TAG, "Got untagged EXPUNGE with mMessageCount " + mMessageCount + " for " + getLogId());
            }
//            if (response.size() > 1) {
//                Object bracketedObj = response.get(1);
//                if (bracketedObj instanceof ImapList)
//                {
//                    ImapList bracketed = (ImapList)bracketedObj;
//
//                    if (!bracketed.isEmpty())
//                    {
//                        Object keyObj = bracketed.get(0);
//                        if (keyObj instanceof String)
//                        {
//                            String key = (String)keyObj;
//                            if ("ALERT".equalsIgnoreCase(key))
//                            {
//                                StringBuilder sb = new StringBuilder();
//                                for (int i = 2, count = response.size(); i < count; i++) {
//                                    sb.append(response.get(i).toString());
//                                    sb.append(' ');
//                                }
//
//                                Log.w(LOG_TAG, "ALERT: " + sb.toString() + " for " + getLogId());
//                            }
//                        }
//                    }
//
//
//                }
//            }
        }
        //Log.i(LOG_TAG, "mMessageCount = " + mMessageCount + " for " + getLogId());
    }

    private void parseBodyStructure(ImapList bs, Part part, String id)
    throws MessagingException {
        if (bs.get(0) instanceof ImapList) {
            /*
             * This is a multipart/*
             */
            MimeMultipart mp = new MimeMultipart();
            for (int i = 0, count = bs.size(); i < count; i++) {
                if (bs.get(i) instanceof ImapList) {
                    /*
                     * For each part in the message we're going to add a new BodyPart and parse
                     * into it.
                     */
                    MimeBodyPart bp = new MimeBodyPart();
                    if (id.equalsIgnoreCase("TEXT")) {
                        parseBodyStructure(bs.getList(i), bp, Integer.toString(i + 1));
                    } else {
                        parseBodyStructure(bs.getList(i), bp, id + "." + (i + 1));
                    }
                    mp.addBodyPart(bp);
                } else {
                    /*
                     * We've got to the end of the children of the part, so now we can find out
                     * what type it is and bail out.
                     */
                    String subType = bs.getString(i);
                    mp.setSubType(subType.toLowerCase(Locale.US));
                    break;
                }
            }
            MimeMessageHelper.setBody(part, mp);
        } else {
            /*
             * This is a body. We need to add as much information as we can find out about
             * it to the Part.
             */

            /*
             *  0| 0  body type
             *  1| 1  body subtype
             *  2| 2  body parameter parenthesized list
             *  3| 3  body id (unused)
             *  4| 4  body description (unused)
             *  5| 5  body encoding
             *  6| 6  body size
             *  -| 7  text lines (only for type TEXT, unused)
             * Extensions (optional):
             *  7| 8  body MD5 (unused)
             *  8| 9  body disposition
             *  9|10  body language (unused)
             * 10|11  body location (unused)
             */

            String type = bs.getString(0);
            String subType = bs.getString(1);
            String mimeType = (type + "/" + subType).toLowerCase(Locale.US);

            ImapList bodyParams = null;
            if (bs.get(2) instanceof ImapList) {
                bodyParams = bs.getList(2);
            }
            String encoding = bs.getString(5);
            int size = bs.getNumber(6);

            if (MimeUtility.isMessage(mimeType)) {
//                  A body type of type MESSAGE and subtype RFC822
//                  contains, immediately after the basic fields, the
//                  envelope structure, body structure, and size in
//                  text lines of the encapsulated message.
//                    [MESSAGE, RFC822, [NAME, Fwd: [#HTR-517941]:  update plans at 1am Friday - Memory allocation - displayware.eml], NIL, NIL, 7BIT, 5974, NIL, [INLINE, [FILENAME*0, Fwd: [#HTR-517941]:  update plans at 1am Friday - Memory all, FILENAME*1, ocation - displayware.eml]], NIL]
                /*
                 * This will be caught by fetch and handled appropriately.
                 */
                throw new MessagingException("BODYSTRUCTURE message/rfc822 not yet supported.");
            }

            /*
             * Set the content type with as much information as we know right now.
             */
            StringBuilder contentType = new StringBuilder();
            contentType.append(mimeType);

            if (bodyParams != null) {
                /*
                 * If there are body params we might be able to get some more information out
                 * of them.
                 */
                for (int i = 0, count = bodyParams.size(); i < count; i += 2) {
                    contentType.append(String.format(";\r\n %s=\"%s\"",
                                       bodyParams.getString(i),
                                       bodyParams.getString(i + 1)));
                }
            }

            part.setHeader(MimeHeader.HEADER_CONTENT_TYPE, contentType.toString());

            // Extension items
            ImapList bodyDisposition = null;
            if (("text".equalsIgnoreCase(type))
                    && (bs.size() > 9)
                    && (bs.get(9) instanceof ImapList)) {
                bodyDisposition = bs.getList(9);
            } else if (!("text".equalsIgnoreCase(type))
                       && (bs.size() > 8)
                       && (bs.get(8) instanceof ImapList)) {
                bodyDisposition = bs.getList(8);
            }

            StringBuilder contentDisposition = new StringBuilder();

            if (bodyDisposition != null && !bodyDisposition.isEmpty()) {
                if (!"NIL".equalsIgnoreCase(bodyDisposition.getString(0))) {
                    contentDisposition.append(bodyDisposition.getString(0).toLowerCase(Locale.US));
                }

                if ((bodyDisposition.size() > 1)
                        && (bodyDisposition.get(1) instanceof ImapList)) {
                    ImapList bodyDispositionParams = bodyDisposition.getList(1);
                    /*
                     * If there is body disposition information we can pull some more information
                     * about the attachment out.
                     */
                    for (int i = 0, count = bodyDispositionParams.size(); i < count; i += 2) {
                        contentDisposition.append(String.format(";\r\n %s=\"%s\"",
                                                  bodyDispositionParams.getString(i).toLowerCase(Locale.US),
                                                  bodyDispositionParams.getString(i + 1)));
                    }
                }
            }

            if (MimeUtility.getHeaderParameter(contentDisposition.toString(), "size") == null) {
                contentDisposition.append(String.format(Locale.US, ";\r\n size=%d", size));
            }

            /*
             * Set the content disposition containing at least the size. Attachment
             * handling code will use this down the road.
             */
            part.setHeader(MimeHeader.HEADER_CONTENT_DISPOSITION, contentDisposition.toString());


            /*
             * Set the Content-Transfer-Encoding header. Attachment code will use this
             * to parse the body.
             */
            part.setHeader(MimeHeader.HEADER_CONTENT_TRANSFER_ENCODING, encoding);

            if (part instanceof ImapMessage) {
                ((ImapMessage) part).setSize(size);
            }
            part.setServerExtra(id);
        }

    }

    /**
     * Appends the given messages to the selected folder.
     *
     * <p>
     * This implementation also determines the new UIDs of the given messages on the IMAP
     * server and changes the messages' UIDs to the new server UIDs.
     * </p>
     *
     * @param messages
     *         The messages to append to the folder.
     *
     * @return The mapping of original message UIDs to the new server UIDs.
     */
    @Override
    public Map<String, String> appendMessages(List<? extends Message> messages) throws MessagingException {
        open(OPEN_MODE_RW);
        checkOpen();
        try {
            Map<String, String> uidMap = new HashMap<String, String>();
            for (Message message : messages) {
                mConnection.sendCommand(
                    String.format(Locale.US, "APPEND %s (%s) {%d}",
                                  ImapStore.encodeString(store.encodeFolderName(getPrefixedName())),
                                  combineFlags(message.getFlags()),
                                  message.calculateSize()), false);

                ImapResponse response;
                do {
                    response = mConnection.readResponse();
                    handleUntaggedResponse(response);
                    if (response.isContinuationRequested()) {
                        EOLConvertingOutputStream eolOut = new EOLConvertingOutputStream(mConnection.getOutputStream());
                        message.writeTo(eolOut);
                        eolOut.write('\r');
                        eolOut.write('\n');
                        eolOut.flush();
                    }
                } while (response.getTag() == null);

                if (response.size() > 1) {
                    /*
                     * If the server supports UIDPLUS, then along with the APPEND response it
                     * will return an APPENDUID response code, e.g.
                     *
                     * 11 OK [APPENDUID 2 238268] APPEND completed
                     *
                     * We can use the UID included in this response to update our records.
                     */
                    Object responseList = response.get(1);

                    if (responseList instanceof ImapList) {
                        ImapList appendList = (ImapList) responseList;
                        if (appendList.size() >= 3 &&
                                appendList.getString(0).equals("APPENDUID")) {

                            String newUid = appendList.getString(2);

                            if (!TextUtils.isEmpty(newUid)) {
                                message.setUid(newUid);
                                uidMap.put(message.getUid(), newUid);
                                continue;
                            }
                        }
                    }
                }

                /*
                 * This part is executed in case the server does not support UIDPLUS or does
                 * not implement the APPENDUID response code.
                 */
                String newUid = getUidFromMessageId(message);
                if (K9MailLib.isDebug()) {
                    Log.d(LOG_TAG, "Got UID " + newUid + " for message for " + getLogId());
                }

                if (!TextUtils.isEmpty(newUid)) {
                    uidMap.put(message.getUid(), newUid);
                    message.setUid(newUid);
                }
            }

            /*
             * We need uidMap to be null if new UIDs are not available to maintain consistency
             * with the behavior of other similar methods (copyMessages, moveMessages) which
             * return null.
             */
            return (uidMap.isEmpty()) ? null : uidMap;
        } catch (IOException ioe) {
            throw ioExceptionHandler(mConnection, ioe);
        }
    }

    @Override
    public String getUidFromMessageId(Message message) throws MessagingException {
        try {
            /*
            * Try to find the UID of the message we just appended using the
            * Message-ID header.
            */
            String[] messageIdHeader = message.getHeader("Message-ID");

            if (messageIdHeader.length == 0) {
                if (K9MailLib.isDebug())
                    Log.d(LOG_TAG, "Did not get a message-id in order to search for UID  for " + getLogId());
                return null;
            }
            String messageId = messageIdHeader[0];
            if (K9MailLib.isDebug())
                Log.d(LOG_TAG, "Looking for UID for message with message-id " + messageId + " for " + getLogId());

            List<ImapResponse> responses =
                executeSimpleCommand(
                    String.format("UID SEARCH HEADER MESSAGE-ID %s", ImapStore.encodeString(messageId)));
            for (ImapResponse response1 : responses) {
                if (response1.getTag() == null && ImapResponseParser.equalsIgnoreCase(response1.get(0), "SEARCH")
                        && response1.size() > 1) {
                    return response1.getString(1);
                }
            }
            return null;
        } catch (IOException ioe) {
            throw new MessagingException("Could not find UID for message based on Message-ID", ioe);
        }
    }


    @Override
    public void expunge() throws MessagingException {
        open(OPEN_MODE_RW);
        checkOpen();
        try {
            executeSimpleCommand("EXPUNGE");
        } catch (IOException ioe) {
            throw ioExceptionHandler(mConnection, ioe);
        }
    }

    private String combineFlags(Iterable<Flag> flags) {
        List<String> flagNames = new ArrayList<String>();
        for (Flag flag : flags) {
            if (flag == Flag.SEEN) {
                flagNames.add("\\Seen");
            } else if (flag == Flag.DELETED) {
                flagNames.add("\\Deleted");
            } else if (flag == Flag.ANSWERED) {
                flagNames.add("\\Answered");
            } else if (flag == Flag.FLAGGED) {
                flagNames.add("\\Flagged");
            } else if (flag == Flag.FORWARDED
                    && (mCanCreateKeywords || store.getPermanentFlagsIndex().contains(Flag.FORWARDED))) {
                flagNames.add("$Forwarded");
            }

        }
        return combine(flagNames.toArray(new String[flagNames.size()]), ' ');
    }


    @Override
    public void setFlags(Set<Flag> flags, boolean value)
    throws MessagingException {
        open(OPEN_MODE_RW);
        checkOpen();


        try {
            executeSimpleCommand(String.format("UID STORE 1:* %sFLAGS.SILENT (%s)",
                                               value ? "+" : "-", combineFlags(flags)));
        } catch (IOException ioe) {
            throw ioExceptionHandler(mConnection, ioe);
        }
    }

    @Override
    public String getNewPushState(String oldPushStateS, Message message) {
        try {
            String messageUidS = message.getUid();
            long messageUid = Long.parseLong(messageUidS);
            ImapPushState oldPushState = ImapPushState.parse(oldPushStateS);
            if (messageUid >= oldPushState.uidNext) {
                long uidNext = messageUid + 1;
                ImapPushState newPushState = new ImapPushState(uidNext);
                return newPushState.toString();
            } else {
                return null;
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception while updated push state for " + getLogId(), e);
            return null;
        }
    }


    @Override
    public void setFlags(List<? extends Message> messages, final Set<Flag> flags, boolean value)
    throws MessagingException {
        open(OPEN_MODE_RW);
        checkOpen();
        String[] uids = new String[messages.size()];
        for (int i = 0, count = messages.size(); i < count; i++) {
            uids[i] = messages.get(i).getUid();
        }
        try {
            executeSimpleCommand(String.format("UID STORE %s %sFLAGS.SILENT (%s)",
                                               combine(uids, ','),
                                               value ? "+" : "-",
                                               combineFlags(flags)));
        } catch (IOException ioe) {
            throw ioExceptionHandler(mConnection, ioe);
        }
    }

    private void checkOpen() throws MessagingException {
        if (!isOpen()) {
            throw new MessagingException("Folder " + getPrefixedName() + " is not open.");
        }
    }

    private MessagingException ioExceptionHandler(ImapConnection connection, IOException ioe) {
        Log.e(LOG_TAG, "IOException for " + getLogId(), ioe);
        if (connection != null) {
            connection.close();
        }
        close();
        return new MessagingException("IO Error", ioe);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof ImapFolder) {
            return ((ImapFolder)o).getName().equalsIgnoreCase(getName());
        }
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return getName().hashCode();
    }

    protected ImapStore getStore() {
        return store;
    }

    protected String getLogId() {
        String id = store.getStoreConfig().toString() + ":" + getName() + "/" + Thread.currentThread().getName();
        if (mConnection != null) {
            id += "/" + mConnection.getLogId();
        }
        return id;
    }

    /**
     * Search the remote ImapFolder.
     * @param queryString String to query for.
     * @param requiredFlags Mandatory flags
     * @param forbiddenFlags Flags to exclude
     * @return List of messages found
     * @throws MessagingException On any error.
     */
    @Override
    public List<ImapMessage> search(final String queryString, final Set<Flag> requiredFlags, final Set<Flag> forbiddenFlags)
        throws MessagingException {

        if (!store.getStoreConfig().allowRemoteSearch()) {
            throw new MessagingException("Your settings do not allow remote searching of this account");
        }

        // Setup the searcher
        final ImapSearcher searcher = new ImapSearcher() {
            @Override
            public List<ImapResponse> search() throws IOException, MessagingException {
                String imapQuery = "UID SEARCH ";
                if (requiredFlags != null) {
                    for (Flag f : requiredFlags) {
                        switch (f) {
                            case DELETED:
                                imapQuery += "DELETED ";
                                break;

                            case SEEN:
                                imapQuery += "SEEN ";
                                break;

                            case ANSWERED:
                                imapQuery += "ANSWERED ";
                                break;

                            case FLAGGED:
                                imapQuery += "FLAGGED ";
                                break;

                            case DRAFT:
                                imapQuery += "DRAFT ";
                                break;

                            case RECENT:
                                imapQuery += "RECENT ";
                                break;

                            default:
                                break;
                        }
                    }
                }
                if (forbiddenFlags != null) {
                    for (Flag f : forbiddenFlags) {
                        switch (f) {
                            case DELETED:
                                imapQuery += "UNDELETED ";
                                break;

                            case SEEN:
                                imapQuery += "UNSEEN ";
                                break;

                            case ANSWERED:
                                imapQuery += "UNANSWERED ";
                                break;

                            case FLAGGED:
                                imapQuery += "UNFLAGGED ";
                                break;

                            case DRAFT:
                                imapQuery += "UNDRAFT ";
                                break;

                            case RECENT:
                                imapQuery += "UNRECENT ";
                                break;

                            default:
                                break;
                        }
                    }
                }
                final String encodedQry = ImapStore.encodeString(queryString);
                if (store.getStoreConfig().isRemoteSearchFullText()) {
                    imapQuery += "TEXT " + encodedQry;
                } else {
                    imapQuery += "OR SUBJECT " + encodedQry + " FROM " + encodedQry;
                }
                return executeSimpleCommand(imapQuery);
            }
        };

        // Execute the search
        try {
            open(OPEN_MODE_RO);
            checkOpen();

            mInSearch = true;
            // don't pass listener--we don't want to add messages until we've downloaded them
            return search(searcher, null);
        } finally {
            mInSearch = false;
        }
    }

    private static String combine(Object[] parts, char separator) {
        if (parts == null) {
            return null;
        }
        return TextUtils.join(String.valueOf(separator), parts);
    }
}
