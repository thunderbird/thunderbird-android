package com.fsck.k9.mail.store.webdav;

import com.fsck.k9.mail.FetchProfile;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.FolderType;
import com.fsck.k9.mail.K9MailLib;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessageRetrievalListener;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.filter.EOLConvertingOutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.StringEntity;
import timber.log.Timber;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static com.fsck.k9.mail.K9MailLib.DEBUG_PROTOCOL_WEBDAV;
import static com.fsck.k9.mail.helper.UrlEncodingHelper.encodeUtf8;

/**
 * A WebDav Folder
 */
public class WebDavFolder {
    private String mName;
    private FolderType type = FolderType.REGULAR;
    private String mFolderUrl;
    private boolean mIsOpen = false;
    private int mMessageCount = 0;
    private int mUnreadMessageCount = 0;
    private WebDavStore store;

    protected WebDavStore getStore() {
        return store;
    }

    public WebDavFolder(WebDavStore nStore, String name) {
        super();
        store = nStore;
        this.mName = name;
        buildFolderUrl();
    }

    public FolderType getType() {
        return type;
    }

    public void setType(FolderType type) {
        this.type = type;
    }

    private void buildFolderUrl() {
        String encodedName;
        String[] urlParts = this.mName.split("/");
        String url = "";
        for (int i = 0, count = urlParts.length; i < count; i++) {
            if (i != 0) {
                url = url + "/" + encodeUtf8(urlParts[i]);
            } else {
                url = encodeUtf8(urlParts[i]);
            }
        }
        encodedName = url;

        encodedName = encodedName.replaceAll("\\+", "%20");

        this.mFolderUrl = store.getUrl();
        if (!store.getUrl().endsWith("/")) {
            this.mFolderUrl += "/";
        }
        this.mFolderUrl += encodedName;
    }

    public void setUrl(String url) {
        if (url != null) {
            this.mFolderUrl = url;
        }
    }

    public void open() throws MessagingException {
        store.getHttpClient();
        this.mIsOpen = true;
    }

    public Map<String, String> copyMessages(List<WebDavMessage> messages, WebDavFolder folder)
            throws MessagingException {
        moveOrCopyMessages(messages, folder.getServerId(), false);
        return null;
    }

    public Map<String, String> moveMessages(List<WebDavMessage> messages, WebDavFolder folder)
            throws MessagingException {
        moveOrCopyMessages(messages, folder.getServerId(), true);
        return null;
    }

    private void moveOrCopyMessages(List<WebDavMessage> messages, String folderName, boolean isMove)
            throws MessagingException {
        String[] uids = new String[messages.size()];

        for (int i = 0, count = messages.size(); i < count; i++) {
            uids[i] = messages.get(i).getUid();
        }
        String messageBody;
        Map<String, String> headers = new HashMap<>();
        Map<String, String> uidToUrl = getMessageUrls(uids);
        String[] urls = new String[uids.length];

        for (int i = 0, count = uids.length; i < count; i++) {
            urls[i] = uidToUrl.get(uids[i]);
            if (urls[i] == null) {
                WebDavMessage message = messages.get(i);
                urls[i] = message.getUrl();
            }
        }

        messageBody = store.getMoveOrCopyMessagesReadXml(urls, isMove);
        WebDavFolder destFolder = store.getFolder(folderName);
        headers.put("Destination", destFolder.mFolderUrl);
        headers.put("Brief", "t");
        headers.put("If-Match", "*");
        String action = (isMove ? "BMOVE" : "BCOPY");
        Timber.v("Moving %d messages to %s", messages.size(), destFolder.mFolderUrl);

        store.processRequest(mFolderUrl, action, messageBody, headers, false);
    }

    private int getMessageCount(boolean read) throws MessagingException {
        String isRead;
        int messageCount = 0;
        Map<String, String> headers = new HashMap<>();
        String messageBody;

        if (read) {
            isRead = "True";
        } else {
            isRead = "False";
        }

        messageBody = store.getMessageCountXml(isRead);
        headers.put("Brief", "t");
        DataSet dataset = store.processRequest(this.mFolderUrl, "SEARCH", messageBody, headers);
        if (dataset != null) {
            messageCount = dataset.getMessageCount();
        }
        if (K9MailLib.isDebug() && DEBUG_PROTOCOL_WEBDAV) {
            Timber.v("Counted messages and webdav returned: %d", messageCount);
        }

        return messageCount;
    }

    public int getMessageCount() throws MessagingException {
        open();
        this.mMessageCount = getMessageCount(true);
        return this.mMessageCount;
    }

    public int getUnreadMessageCount() throws MessagingException {
        open();
        this.mUnreadMessageCount = getMessageCount(false);
        return this.mUnreadMessageCount;
    }

    public boolean isOpen() {
        return this.mIsOpen;
    }

    public String getServerId() {
        return this.mName;
    }

    public String getName() {
        return this.mName;
    }

    public void close() {
        this.mMessageCount = 0;
        this.mUnreadMessageCount = 0;
        this.mIsOpen = false;
    }

    public WebDavMessage getMessage(String uid) {
        return new WebDavMessage(uid, this);
    }

    public List<WebDavMessage> getMessages(int start, int end, MessageRetrievalListener<WebDavMessage> listener)
            throws MessagingException {
        List<WebDavMessage> messages = new ArrayList<>();
        String[] uids;
        Map<String, String> headers = new HashMap<>();
        int uidsLength;

        String messageBody;
        int prevStart = start;

        /** Reverse the message range since 0 index is newest */
        start = this.mMessageCount - end;
        end = start + (end - prevStart);

        if (start < 0 || end < 0 || end < start) {
            throw new MessagingException(String.format(Locale.US, "Invalid message set %d %d", start, end));
        }

        if (start == 0 && end < 10) {
            end = 10;
        }

        /** Verify authentication */
        messageBody = store.getMessagesXml();

        headers.put("Brief", "t");
        headers.put("Range", "rows=" + start + "-" + end);
        DataSet dataset = store.processRequest(this.mFolderUrl, "SEARCH", messageBody, headers);
        uids = dataset.getUids();
        Map<String, String> uidToUrl = dataset.getUidToUrl();
        uidsLength = uids.length;

        for (int i = 0; i < uidsLength; i++) {
            if (listener != null) {
                listener.messageStarted(uids[i], i, uidsLength);
            }
            WebDavMessage message = new WebDavMessage(uids[i], this);
            message.setUrl(uidToUrl.get(uids[i]));
            messages.add(message);

            if (listener != null) {
                listener.messageFinished(message, i, uidsLength);
            }
        }

        return messages;
    }

    public boolean areMoreMessagesAvailable(int indexOfOldestMessage, Date earliestDate) {
        return indexOfOldestMessage > 1;
    }

    private Map<String, String> getMessageUrls(String[] uids) throws MessagingException {
        Map<String, String> headers = new HashMap<>();
        String messageBody;

        /** Retrieve and parse the XML entity for our messages */
        messageBody = store.getMessageUrlsXml(uids);
        headers.put("Brief", "t");

        DataSet dataset = store.processRequest(this.mFolderUrl, "SEARCH", messageBody, headers);

        return dataset.getUidToUrl();
    }

    public void fetch(List<WebDavMessage> messages, FetchProfile fp, MessageRetrievalListener<WebDavMessage> listener,
            int maxDownloadSize) throws MessagingException {
        if (messages == null || messages.isEmpty()) {
            return;
        }

        /**
         * Fetch message envelope information for the array
         */
        if (fp.contains(FetchProfile.Item.ENVELOPE)) {
            fetchEnvelope(messages, listener);
        }
        /**
         * Fetch message flag info for the array
         */
        if (fp.contains(FetchProfile.Item.FLAGS)) {
            fetchFlags(messages, listener);
        }

        if (fp.contains(FetchProfile.Item.BODY_SANE)) {
            if (maxDownloadSize > 0) {
                fetchMessages(messages, listener, (maxDownloadSize / 76));
            } else {
                fetchMessages(messages, listener, -1);
            }
        }
        if (fp.contains(FetchProfile.Item.BODY)) {
            fetchMessages(messages, listener, -1);
        }
    }

    /**
     * Fetches the full messages or up to {@param lines} lines and passes them to the message parser.
     */
    private void fetchMessages(List<WebDavMessage> messages, MessageRetrievalListener<WebDavMessage> listener, int lines)
            throws MessagingException {
        WebDavHttpClient httpclient;
        httpclient = store.getHttpClient();

        /**
         * We can't hand off to processRequest() since we need the stream to parse.
         */
        for (int i = 0, count = messages.size(); i < count; i++) {
            WebDavMessage wdMessage = messages.get(i);
            int statusCode = 0;

            if (listener != null) {
                listener.messageStarted(wdMessage.getUid(), i, count);
            }

            /**
             * If fetch is called outside of the initial list (ie, a locally stored message), it may not have a URL
             * associated. Verify and fix that
             */
            if (wdMessage.getUrl().equals("")) {
                wdMessage.setUrl(getMessageUrls(new String[]{wdMessage.getUid()}).get(wdMessage.getUid()));
                Timber.i("Fetching messages with UID = '%s', URL = '%s'", wdMessage.getUid(), wdMessage.getUrl());
                if (wdMessage.getUrl().equals("")) {
                    throw new MessagingException("Unable to get URL for message");
                }
            }

            try {
                Timber.i("Fetching message with UID = '%s', URL = '%s'", wdMessage.getUid(), wdMessage.getUrl());
                HttpGet httpget = new HttpGet(new URI(wdMessage.getUrl()));
                HttpResponse response;
                HttpEntity entity;

                httpget.setHeader("translate", "f");
                if (store.getAuthentication() == WebDavConstants.AUTH_TYPE_BASIC) {
                    httpget.setHeader("Authorization", store.getAuthString());
                }
                response = httpclient.executeOverride(httpget, store.getHttpContext());

                statusCode = response.getStatusLine().getStatusCode();

                entity = response.getEntity();

                if (statusCode < 200 ||
                        statusCode > 300) {
                    throw new IOException("Error during with code " + statusCode + " during fetch: "
                            + response.getStatusLine().toString());
                }

                if (entity != null) {
                    InputStream istream = null;
                    StringBuilder buffer = new StringBuilder();
                    String tempText;
                    String resultText;
                    BufferedReader reader = null;
                    int currentLines = 0;

                    try {
                        istream = WebDavHttpClient.getUngzippedContent(entity);

                        if (lines != -1) {
                            //Convert the ungzipped input stream into a StringBuilder
                            //containing the given line count
                            reader = new BufferedReader(new InputStreamReader(istream), 8192);

                            while ((tempText = reader.readLine()) != null &&
                                    (currentLines < lines)) {
                                buffer.append(tempText).append("\r\n");
                                currentLines++;
                            }

                            IOUtils.closeQuietly(istream);

                            resultText = buffer.toString();
                            istream = new ByteArrayInputStream(resultText.getBytes("UTF-8"));
                        }
                        //Parse either the entire message stream, or a stream of the given lines
                        wdMessage.parse(istream);

                    } catch (IOException ioe) {
                        Timber.e(ioe, "IOException during message parsing");
                        throw new MessagingException("I/O Error", ioe);
                    } finally {
                        IOUtils.closeQuietly(reader);
                        IOUtils.closeQuietly(istream);
                    }
                } else {
                    Timber.v("Empty response");
                }

            } catch (IllegalArgumentException iae) {
                Timber.e(iae, "IllegalArgumentException caught");
                throw new MessagingException("IllegalArgumentException caught", iae);
            } catch (URISyntaxException use) {
                Timber.e(use, "URISyntaxException caught");
                throw new MessagingException("URISyntaxException caught", use);
            } catch (IOException ioe) {
                Timber.e(ioe, "Non-success response code loading message, response code was %d, URL: %s",
                        statusCode, wdMessage.getUrl());
                throw new MessagingException("Failure code " + statusCode, ioe);
            }

            if (listener != null) {
                listener.messageFinished(wdMessage, i, count);
            }
        }
    }

    /**
     * Fetches and sets the message flags for the supplied messages. The idea is to have this be recursive so that
     * we do a series of medium calls instead of one large massive call or a large number of smaller calls.
     */
    private void fetchFlags(List<WebDavMessage> startMessages, MessageRetrievalListener<WebDavMessage> listener) throws MessagingException {
        HashMap<String, String> headers = new HashMap<>();
        String messageBody;
        List<WebDavMessage> messages = new ArrayList<>(20);
        String[] uids;

        if (startMessages == null ||
                startMessages.isEmpty()) {
            return;
        }

        if (startMessages.size() > 20) {
            List<WebDavMessage> newMessages = new ArrayList<>(startMessages.size() - 20);
            for (int i = 0, count = startMessages.size(); i < count; i++) {
                if (i < 20) {
                    messages.add(startMessages.get(i));
                } else {
                    newMessages.add(startMessages.get(i));
                }
            }

            fetchFlags(newMessages, listener);
        } else {
            messages.addAll(startMessages);
        }

        uids = new String[messages.size()];

        for (int i = 0, count = messages.size(); i < count; i++) {
            uids[i] = messages.get(i).getUid();
        }

        messageBody = store.getMessageFlagsXml(uids);
        headers.put("Brief", "t");
        DataSet dataset = store.processRequest(this.mFolderUrl, "SEARCH", messageBody, headers);

        if (dataset == null) {
            throw new MessagingException("Data Set from request was null");
        }

        Map<String, Boolean> uidToReadStatus = dataset.getUidToRead();

        for (WebDavMessage message : messages) {
            try {
                message.setFlag(Flag.SEEN, uidToReadStatus.get(message.getUid()));
            } catch (NullPointerException e) {
                Timber.v(e, "Under some weird circumstances, " +
                        "setting the read status when syncing from webdav threw an NPE. Skipping.");
            }

        }
    }

    /**
     * Fetches and parses the message envelopes for the supplied messages. The idea is to have this be recursive so
     * that we do a series of medium calls instead of one large massive call or a large number of smaller calls.
     * Call it a happy balance
     */
    private void fetchEnvelope(List<WebDavMessage> startMessages, MessageRetrievalListener<WebDavMessage> listener)
            throws MessagingException {
        Map<String, String> headers = new HashMap<>();
        String messageBody;
        String[] uids;
        List<WebDavMessage> messages = new ArrayList<>(10);

        if (startMessages == null ||
                startMessages.isEmpty()) {
            return;
        }

        if (startMessages.size() > 10) {
            List<WebDavMessage> newMessages = new ArrayList<>(startMessages.size() - 10);
            for (int i = 0, count = startMessages.size(); i < count; i++) {
                if (i < 10) {
                    messages.add(i, startMessages.get(i));
                } else {
                    newMessages.add(i - 10, startMessages.get(i));
                }
            }

            fetchEnvelope(newMessages, listener);
        } else {
            messages.addAll(startMessages);
        }

        uids = new String[messages.size()];

        for (int i = 0, count = messages.size(); i < count; i++) {
            uids[i] = messages.get(i).getUid();
        }

        messageBody = store.getMessageEnvelopeXml(uids);
        headers.put("Brief", "t");
        DataSet dataset = store.processRequest(this.mFolderUrl, "SEARCH", messageBody, headers);

        Map<String, ParsedMessageEnvelope> envelopes = dataset.getMessageEnvelopes();

        int count = messages.size();
        for (int i = messages.size() - 1; i >= 0; i--) {
            WebDavMessage message = messages.get(i);
            if (listener != null) {
                listener.messageStarted(messages.get(i).getUid(), i, count);
            }

            ParsedMessageEnvelope envelope = envelopes.get(message.getUid());
            if (envelope != null) {
                message.setNewHeaders(envelope);
                message.setFlag(Flag.SEEN, envelope.getReadStatus());
            } else {
                Timber.e("Asked to get metadata for a non-existent message: %s", message.getUid());
            }

            if (listener != null) {
                listener.messageFinished(messages.get(i), i, count);
            }
        }
    }

    public void setFlags(List<WebDavMessage> messages, final Set<Flag> flags, boolean value)
            throws MessagingException {
        String[] uids = new String[messages.size()];

        for (int i = 0, count = messages.size(); i < count; i++) {
            uids[i] = messages.get(i).getUid();
        }

        for (Flag flag : flags) {
            if (flag == Flag.SEEN) {
                markServerMessagesRead(uids, value);
            } else if (flag == Flag.DELETED) {
                deleteServerMessages(uids);
            }
        }
    }

    private void markServerMessagesRead(String[] uids, boolean read) throws MessagingException {
        String messageBody;
        Map<String, String> headers = new HashMap<>();
        Map<String, String> uidToUrl = getMessageUrls(uids);
        String[] urls = new String[uids.length];

        for (int i = 0, count = uids.length; i < count; i++) {
            urls[i] = uidToUrl.get(uids[i]);
        }

        messageBody = store.getMarkMessagesReadXml(urls, read);
        headers.put("Brief", "t");
        headers.put("If-Match", "*");

        store.processRequest(this.mFolderUrl, "BPROPPATCH", messageBody, headers, false);
    }

    private void deleteServerMessages(String[] uids) throws MessagingException {
        Map<String, String> uidToUrl = getMessageUrls(uids);

        for (String uid : uids) {
            Map<String, String> headers = new HashMap<>();
            String url = uidToUrl.get(uid);
            String destinationUrl = generateDeleteUrl(url);

            /**
             * If the destination is the same as the origin, assume delete forever
             */
            if (destinationUrl.equals(url)) {
                headers.put("Brief", "t");
                store.processRequest(url, "DELETE", null, headers, false);
            } else {
                headers.put("Destination", generateDeleteUrl(url));
                headers.put("Brief", "t");
                store.processRequest(url, "MOVE", null, headers, false);
            }
        }
    }

    private String generateDeleteUrl(String startUrl) {
        String[] urlParts = startUrl.split("/");
        String filename = urlParts[urlParts.length - 1];

        return store.getUrl() + "Deleted%20Items/" + filename;
    }

    public Map<String, String> appendMessages(List<Message> messages) throws MessagingException {
        appendWebDavMessages(messages);
        return null;
    }

    public List<WebDavMessage> appendWebDavMessages(List<Message> messages) throws MessagingException {
        List<WebDavMessage> retMessages = new ArrayList<>(messages.size());

        WebDavHttpClient httpclient = store.getHttpClient();

        for (Message message : messages) {
            HttpGeneric httpmethod;
            HttpResponse response;
            StringEntity bodyEntity;
            int statusCode;

            try {
                ByteArrayOutputStream out;

                long size = message.getSize();
                if (size > Integer.MAX_VALUE) {
                    throw new MessagingException("message size > Integer.MAX_VALUE!");
                }
                out = new ByteArrayOutputStream((int) size);

                open();
                EOLConvertingOutputStream msgOut = new EOLConvertingOutputStream(
                        new BufferedOutputStream(out, 1024));
                message.writeTo(msgOut);
                msgOut.flush();

                bodyEntity = new StringEntity(out.toString(), "UTF-8");
                bodyEntity.setContentType("message/rfc822");

                String messageURL = mFolderUrl;
                if (!messageURL.endsWith("/")) {
                    messageURL += "/";
                }
                messageURL += encodeUtf8(message.getUid() + ":" + System.currentTimeMillis() + ".eml");

                Timber.i("Uploading message as %s", messageURL);

                store.sendRequest(messageURL, "PUT", bodyEntity, null, true);

                WebDavMessage retMessage = new WebDavMessage(message.getUid(), this);

                retMessage.setUrl(messageURL);
                retMessages.add(retMessage);
            } catch (Exception e) {
                throw new MessagingException("Unable to append", e);
            }
        }
        return retMessages;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof WebDavFolder) {
            return ((WebDavFolder) o).mName.equals(mName);
        }
        return super.equals(o);
    }

    public String getUrl() {
        return mFolderUrl;
    }
}
