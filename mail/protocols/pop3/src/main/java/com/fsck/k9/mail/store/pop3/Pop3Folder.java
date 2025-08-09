package com.fsck.k9.mail.store.pop3;


import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import net.thunderbird.core.logging.legacy.Log;
import com.fsck.k9.mail.FetchProfile;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.K9MailLib;
import com.fsck.k9.mail.MessageRetrievalListener;
import net.thunderbird.core.common.exception.MessagingException;
import org.jetbrains.annotations.NotNull;

import static com.fsck.k9.mail.K9MailLib.DEBUG_PROTOCOL_POP3;
import static com.fsck.k9.mail.store.pop3.Pop3Commands.*;


/**
 * POP3 only supports one folder, "Inbox". So the folder name is the ID here.
 */
public class Pop3Folder {
    public static final String INBOX = "INBOX";


    private Pop3Store pop3Store;
    private Map<String, Pop3Message> uidToMsgMap = new HashMap<>();
    private Map<Integer, Pop3Message> msgNumToMsgMap = new HashMap<>();
    private Map<String, Integer> uidToMsgNumMap = new HashMap<>();
    private String name;
    private int messageCount;
    private Pop3Connection connection;

    Pop3Folder(Pop3Store pop3Store, String name) {
        super();
        this.pop3Store = pop3Store;
        this.name = name;
    }

    public synchronized void open() throws MessagingException {
        if (isOpen()) {
            return;
        }

        if (!INBOX.equals(name)) {
            throw new MessagingException("Folder does not exist");
        }

        connection = pop3Store.createConnection();
        try {
            connection.open();

            String response = connection.executeSimpleCommand(STAT_COMMAND);
            String[] parts = response.split(" ");
            messageCount = Integer.parseInt(parts[1]);

            uidToMsgMap.clear();
            msgNumToMsgMap.clear();
            uidToMsgNumMap.clear();
        } catch (IOException e) {
            handleIOException(e);
        }
    }

    public boolean isOpen() {
        return connection != null && connection.isOpen();
    }

    public void close() {
        try {
            if (isOpen()) {
                connection.executeSimpleCommand(QUIT_COMMAND);
            }
        } catch (Exception e) {
            /*
             * QUIT may fail if the connection is already closed. We don't care. It's just
             * being friendly.
             */
        }

        if (connection != null) {
            connection.close();
            connection = null;
        }
    }

    public String getServerId() {
        return name;
    }

    public String getName() {
        return name;
    }

    public int getMessageCount() {
        return messageCount;
    }

    @NotNull
    public Pop3Message getMessage(String uid) {
        Pop3Message message = uidToMsgMap.get(uid);
        if (message == null) {
            message = new Pop3Message(uid);
        }
        return message;
    }

    public List<Pop3Message> getMessages(int start, int end, MessageRetrievalListener<Pop3Message> listener)
    throws MessagingException {
        if (start < 1 || end < 1 || end < start) {
            throw new MessagingException(String.format(Locale.US, "Invalid message set %d %d",
                                         start, end));
        }
        try {
            indexMsgNums(start, end);
        } catch (IOException ioe) {
            handleIOException(ioe);
        }
        List<Pop3Message> messages = new ArrayList<>();
        for (int msgNum = start; msgNum <= end; msgNum++) {
            Pop3Message message = msgNumToMsgMap.get(msgNum);
            if (message == null) {
                /*
                 * There could be gaps in the message numbers or malformed
                 * responses which lead to "gaps" in msgNumToMsgMap.
                 *
                 * See issue 2252
                 */
                continue;
            }

            messages.add(message);
            if (listener != null) {
                listener.messageFinished(message);
            }
        }
        return messages;
    }

    public boolean areMoreMessagesAvailable(int indexOfOldestMessage) {
        return indexOfOldestMessage > 1;
    }

    /**
     * Ensures that the given message set (from start to end inclusive)
     * has been queried so that uids are available in the local cache.
     */
    private void indexMsgNums(int start, int end) throws MessagingException, IOException {
        int unindexedMessageCount = 0;
        for (int msgNum = start; msgNum <= end; msgNum++) {
            if (msgNumToMsgMap.get(msgNum) == null) {
                unindexedMessageCount++;
            }
        }
        if (unindexedMessageCount == 0) {
            return;
        }
        if (unindexedMessageCount < 50 && messageCount > 5000) {
            /*
             * In extreme cases we'll do a UIDL command per message instead of a bulk
             * download.
             */
            for (int msgNum = start; msgNum <= end; msgNum++) {
                Pop3Message message = msgNumToMsgMap.get(msgNum);
                if (message == null) {
                    String response = connection.executeSimpleCommand(UIDL_COMMAND + " " + msgNum);
                    // response = "+OK msgNum msgUid"
                    String[] uidParts = response.split(" +");
                    if (uidParts.length < 3 || !"+OK".equals(uidParts[0])) {
                        Log.e("ERR response: %s", response);
                        return;
                    }
                    String msgUid = uidParts[2];
                    message = new Pop3Message(msgUid);
                    indexMessage(msgNum, message);
                }
            }
        } else {
            connection.executeSimpleCommand(UIDL_COMMAND);
            String response;
            while ((response = connection.readLine()) != null) {
                if (response.equals(".")) {
                    break;
                }

                /*
                 * Yet another work-around for buggy server software:
                 * split the response into message number and unique identifier, no matter how many spaces it has
                 *
                 * Example for a malformed response:
                 * 1   2011071307115510400ae3e9e00bmu9
                 *
                 * Note the three spaces between message number and unique identifier.
                 * See issue 3546
                 */

                String[] uidParts = response.split(" +");
                if ((uidParts.length >= 3) && "+OK".equals(uidParts[0])) {
                    /*
                     * At least one server software places a "+OK" in
                     * front of every line in the unique-id listing.
                     *
                     * Fix up the array if we detected this behavior.
                     * See Issue 1237
                     */
                    uidParts[0] = uidParts[1];
                    uidParts[1] = uidParts[2];
                }
                if (uidParts.length >= 2) {
                    Integer msgNum = Integer.valueOf(uidParts[0]);
                    String msgUid = uidParts[1];
                    if (msgNum >= start && msgNum <= end) {
                        Pop3Message message = msgNumToMsgMap.get(msgNum);
                        if (message == null) {
                            message = new Pop3Message(msgUid);
                            indexMessage(msgNum, message);
                        }
                    }
                }
            }
        }
    }

    private void indexUids(List<String> uids)
    throws MessagingException, IOException {
        Set<String> unindexedUids = new HashSet<>();
        for (String uid : uids) {
            if (uidToMsgMap.get(uid) == null) {
                if (K9MailLib.isDebug() && DEBUG_PROTOCOL_POP3) {
                    Log.d("Need to index UID %s", uid);
                }
                unindexedUids.add(uid);
            }
        }
        if (unindexedUids.isEmpty()) {
            return;
        }
        /*
         * If we are missing uids in the cache the only sure way to
         * get them is to do a full UIDL list. A possible optimization
         * would be trying UIDL for the latest X messages and praying.
         */
        connection.executeSimpleCommand(UIDL_COMMAND);
        String response;
        while ((response = connection.readLine()) != null) {
            if (response.equals(".")) {
                break;
            }
            String[] uidParts = response.split(" +");

            // Ignore messages without a unique-id
            if (uidParts.length >= 2) {
                Integer msgNum = Integer.valueOf(uidParts[0]);
                String msgUid = uidParts[1];
                if (unindexedUids.contains(msgUid)) {
                    if (K9MailLib.isDebug() && DEBUG_PROTOCOL_POP3) {
                        Log.d("Got msgNum %d for UID %s", msgNum, msgUid);
                    }

                    Pop3Message message = uidToMsgMap.get(msgUid);
                    if (message == null) {
                        message = new Pop3Message(msgUid);
                    }
                    indexMessage(msgNum, message);
                }
            }
        }
    }

    private void indexMessage(int msgNum, Pop3Message message) {
        if (K9MailLib.isDebug() && DEBUG_PROTOCOL_POP3) {
            Log.d("Adding index for UID %s to msgNum %d", message.getUid(), msgNum);
        }
        msgNumToMsgMap.put(msgNum, message);
        uidToMsgMap.put(message.getUid(), message);
        uidToMsgNumMap.put(message.getUid(), msgNum);
    }

    /**
     * Fetch the items contained in the FetchProfile into the given set of
     * Messages in as efficient a manner as possible.
     * @param messages Messages to populate
     * @param fp The contents to populate
     */
    public void fetch(List<Pop3Message> messages, FetchProfile fp,
            MessageRetrievalListener<Pop3Message> listener, int maxDownloadSize)
    throws MessagingException {
        if (messages == null || messages.isEmpty()) {
            return;
        }
        List<String> uids = new ArrayList<>();
        for (Pop3Message message : messages) {
            uids.add(message.getUid());
        }
        try {
            indexUids(uids);
        } catch (IOException ioe) {
            handleIOException(ioe);
        }
        try {
            if (fp.contains(FetchProfile.Item.ENVELOPE)) {
                /*
                 * We pass the listener only if there are other things to do in the
                 * FetchProfile. Since fetchEnvelop works in bulk and eveything else
                 * works one at a time if we let fetchEnvelope send events the
                 * event would get sent twice.
                 */
                fetchEnvelope(messages, fp.size() == 1 ? listener : null);
            }
        } catch (IOException ioe) {
            handleIOException(ioe);
        }
        for (Pop3Message pop3Message : messages) {
            try {
                if (fp.contains(FetchProfile.Item.BODY)) {
                    fetchBody(pop3Message, -1);
                } else if (fp.contains(FetchProfile.Item.BODY_SANE)) {
                    /*
                     * To convert the suggested download size we take the size
                     * divided by the maximum line size (76).
                     */
                    if (maxDownloadSize > 0) {
                        fetchBody(pop3Message, maxDownloadSize / 76);
                    } else {
                        fetchBody(pop3Message, -1);
                    }
                } else if (fp.contains(FetchProfile.Item.STRUCTURE)) {
                    /*
                     * If the user is requesting STRUCTURE we are required to set the body
                     * to null since we do not support the function.
                     */
                    pop3Message.setBody(null);
                }
                if (listener != null && !(fp.contains(FetchProfile.Item.ENVELOPE) && fp.size() == 1)) {
                    listener.messageFinished(pop3Message);
                }
            } catch (IOException ioe) {
                handleIOException(ioe);
            }
        }
    }

    private void fetchEnvelope(List<Pop3Message> messages,
                               MessageRetrievalListener<Pop3Message> listener)  throws IOException, MessagingException {
        int unsizedMessages = 0;
        for (Pop3Message message : messages) {
            if (message.getSize() == -1) {
                unsizedMessages++;
            }
        }
        if (unsizedMessages == 0) {
            return;
        }
        if (unsizedMessages < 50 && messageCount > 5000) {
            /*
             * In extreme cases we'll do a command per message instead of a bulk request
             * to hopefully save some time and bandwidth.
             */
            for (Pop3Message message : messages) {
                String response = connection.executeSimpleCommand(
                        String.format(Locale.US, LIST_COMMAND + " %d",
                                uidToMsgNumMap.get(message.getUid())));
                String[] listParts = response.split(" ");
                //int msgNum = Integer.parseInt(listParts[1]);
                int msgSize = Integer.parseInt(listParts[2]);
                message.setSize(msgSize);
                if (listener != null) {
                    listener.messageFinished(message);
                }
            }
        } else {
            Set<String> msgUidIndex = new HashSet<>();
            for (Pop3Message message : messages) {
                msgUidIndex.add(message.getUid());
            }
            int i = 0, count = messages.size();
            connection.executeSimpleCommand(LIST_COMMAND);
            String response;
            while ((response = connection.readLine()) != null) {
                if (response.equals(".")) {
                    break;
                }
                String[] listParts = response.split(" ");
                int msgNum = Integer.parseInt(listParts[0]);
                int msgSize = Integer.parseInt(listParts[1]);
                Pop3Message pop3Message = msgNumToMsgMap.get(msgNum);
                if (pop3Message != null && msgUidIndex.contains(pop3Message.getUid())) {
                    pop3Message.setSize(msgSize);
                    if (listener != null) {
                        listener.messageFinished(pop3Message);
                    }
                    i++;
                }
            }
        }
    }

    /**
     * Fetches the body of the given message, limiting the downloaded data to the specified
     * number of lines if possible.
     *
     * If lines is -1 the entire message is fetched. This is implemented with RETR for
     * lines = -1 or TOP for any other value. If the server does not support TOP, RETR is used
     * instead.
     */
    private void fetchBody(Pop3Message message, int lines)
    throws IOException, MessagingException {
        String response = null;

        // Try hard to use the TOP command if we're not asked to download the whole message.
        if (lines != -1 && (!connection.isTopNotAdvertised() || connection.supportsTop())) {
            try {
                if (K9MailLib.isDebug() && DEBUG_PROTOCOL_POP3 && !connection.supportsTop()) {
                    Log.d("This server doesn't support the CAPA command. " +
                          "Checking to see if the TOP command is supported nevertheless.");
                }

                response = connection.executeSimpleCommand(
                        String.format(Locale.US, TOP_COMMAND + " %d %d",
                                uidToMsgNumMap.get(message.getUid()), lines));
                // TOP command is supported. Remember this for the next time.
                connection.setSupportsTop(true);
            } catch (Pop3ErrorResponse e) {
                if (connection.supportsTop()) {
                    // The TOP command should be supported but something went wrong.
                    throw e;
                } else {
                    if (K9MailLib.isDebug() && DEBUG_PROTOCOL_POP3) {
                        Log.d("The server really doesn't support the TOP " +
                              "command. Using RETR instead.");
                    }

                    // Don't try to use the TOP command again.
                    connection.setTopNotAdvertised(false);
                }
            }
        }

        if (response == null) {
            connection.executeSimpleCommand(String.format(Locale.US, RETR_COMMAND + " %d",
                                 uidToMsgNumMap.get(message.getUid())));
        }

        try {
            message.parse(new Pop3ResponseInputStream(connection.getInputStream()));

            // TODO: if we've received fewer lines than requested we also have the complete message.
            if (lines == -1 || !connection.supportsTop()) {
                message.setFlag(Flag.X_DOWNLOADED_FULL, true);
            }
        } catch (MessagingException me) {
            /*
             * If we're only downloading headers it's possible
             * we'll get a broken MIME message which we're not
             * real worried about. If we've downloaded the body
             * and can't parse it we need to let the user know.
             */
            if (lines == -1) {
                throw me;
            }
        }
    }

    public void setFlags(List<Pop3Message> messages, final Set<Flag> flags, boolean value)
    throws MessagingException {
        if (!value || !flags.contains(Flag.DELETED)) {
            /*
             * The only flagging we support is setting the Deleted flag.
             */
            return;
        }
        List<String> uids = new ArrayList<>();
        try {
            for (Pop3Message message : messages) {
                uids.add(message.getUid());
            }

            indexUids(uids);
        } catch (IOException ioe) {
            handleIOException(ioe);
        }
        for (Pop3Message message : messages) {

            Integer msgNum = uidToMsgNumMap.get(message.getUid());
            if (msgNum == null) {
                throw new MessagingException(
                        "Could not delete message " + message.getUid() + " because no msgNum found; permanent error",
                        true
                );
            }
            open();
            try {
                connection.executeSimpleCommand(String.format(DELE_COMMAND + " %s", msgNum));
            } catch (IOException e) {
                handleIOException(e);
            }
        }
    }

    public boolean isFlagSupported(Flag flag) {
        return (flag == Flag.DELETED);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Pop3Folder) {
            return ((Pop3Folder) o).name.equals(name);
        }
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    void requestUidl() throws MessagingException {
        try {
            if (!connection.supportsUidl()) {
                /*
                 * Run an additional test to see if UIDL is supported on the server. If it's not we
                 * can't service this account.
                 */

                /*
                 * If the server doesn't support UIDL it will return a - response, which causes
                 * executeSimpleCommand to throw a MessagingException, exiting this method.
                 */
                connection.executeSimpleCommand(UIDL_COMMAND);
            }
        } catch (IOException e) {
            handleIOException(e);
        }
    }

    private void handleIOException(IOException exception) throws MessagingException {
        Pop3Connection connection = this.connection;
        if (connection != null) {
            connection.close();
        }

        // For now we wrap IOExceptions in a MessagingException
        throw new MessagingException("I/O error", exception);
    }
}
