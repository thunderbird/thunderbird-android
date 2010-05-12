
package com.fsck.k9.mail.store;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.PeekableInputStream;
import com.fsck.k9.Utility;
import com.fsck.k9.mail.*;
import com.fsck.k9.mail.internet.*;
import com.fsck.k9.mail.store.ImapResponseParser.ImapList;
import com.fsck.k9.mail.store.ImapResponseParser.ImapResponse;
import com.fsck.k9.mail.transport.CountingOutputStream;
import com.fsck.k9.mail.transport.EOLConvertingOutputStream;
import com.jcraft.jzlib.JZlib;
import com.jcraft.jzlib.ZInputStream;
import com.jcraft.jzlib.ZOutputStream;
import com.beetstra.jutf7.CharsetProvider;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManager;


import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Security;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <pre>
 * TODO Need to start keeping track of UIDVALIDITY
 * TODO Need a default response handler for things like folder updates
 * TODO In fetch(), if we need a ImapMessage and were given
 * something else we can try to do a pre-fetch first.
 * </pre>
 */
public class ImapStore extends Store
{
    public static final int CONNECTION_SECURITY_NONE = 0;
    public static final int CONNECTION_SECURITY_TLS_OPTIONAL = 1;
    public static final int CONNECTION_SECURITY_TLS_REQUIRED = 2;
    public static final int CONNECTION_SECURITY_SSL_REQUIRED = 3;
    public static final int CONNECTION_SECURITY_SSL_OPTIONAL = 4;

    private enum AuthType { PLAIN, CRAM_MD5 };

    private static final int IDLE_READ_TIMEOUT_INCREMENT = 5 * 60 * 1000;
    private static final int IDLE_FAILURE_COUNT_LIMIT = 10;
    private static int MAX_DELAY_TIME = 5 * 60 * 1000; // 5 minutes
    private static int NORMAL_DELAY_TIME = 5000;

    private static final Flag[] PERMANENT_FLAGS = { Flag.DELETED, Flag.SEEN };

    private static final String CAPABILITY_IDLE = "IDLE";
    private static final String COMMAND_IDLE = "IDLE";
    private static final String CAPABILITY_NAMESPACE = "NAMESPACE";
    private static final String COMMAND_NAMESPACE = "NAMESPACE";

    private static final String CAPABILITY_CAPABILITY = "CAPABILITY";
    private static final String COMMAND_CAPABILITY = "CAPABILITY";

    private static final String CAPABILITY_COMPRESS_DEFLATE = "COMPRESS=DEFLATE";
    private static final String COMMAND_COMPRESS_DEFLATE = "COMPRESS DEFLATE";

    private String mHost;
    private int mPort;
    private String mUsername;
    private String mPassword;
    private int mConnectionSecurity;
    private AuthType mAuthType;
    private volatile String mPathPrefix;
    private volatile String mCombinedPrefix = null;
    private volatile String mPathDelimeter;

    private LinkedList<ImapConnection> mConnections =
        new LinkedList<ImapConnection>();

    /**
     * Charset used for converting folder names to and from UTF-7 as defined by RFC 3501.
     */
    private Charset mModifiedUtf7Charset;

    /**
     * Cache of ImapFolder objects. ImapFolders are attached to a given folder on the server
     * and as long as their associated connection remains open they are reusable between
     * requests. This cache lets us make sure we always reuse, if possible, for a given
     * folder name.
     */
    private HashMap<String, ImapFolder> mFolderCache = new HashMap<String, ImapFolder>();

    /**
     * imap://auth:user:password@server:port CONNECTION_SECURITY_NONE
     * imap+tls://auth:user:password@server:port CONNECTION_SECURITY_TLS_OPTIONAL
     * imap+tls+://auth:user:password@server:port CONNECTION_SECURITY_TLS_REQUIRED
     * imap+ssl+://auth:user:password@server:port CONNECTION_SECURITY_SSL_REQUIRED
     * imap+ssl://auth:user:password@server:port CONNECTION_SECURITY_SSL_OPTIONAL
     *
     * @param _uri
     */
    public ImapStore(Account account) throws MessagingException
    {
        super(account);
        URI uri;
        try
        {
            uri = new URI(mAccount.getStoreUri());
        }
        catch (URISyntaxException use)
        {
            throw new MessagingException("Invalid ImapStore URI", use);
        }

        String scheme = uri.getScheme();
        if (scheme.equals("imap"))
        {
            mConnectionSecurity = CONNECTION_SECURITY_NONE;
            mPort = 143;
        }
        else if (scheme.equals("imap+tls"))
        {
            mConnectionSecurity = CONNECTION_SECURITY_TLS_OPTIONAL;
            mPort = 143;
        }
        else if (scheme.equals("imap+tls+"))
        {
            mConnectionSecurity = CONNECTION_SECURITY_TLS_REQUIRED;
            mPort = 143;
        }
        else if (scheme.equals("imap+ssl+"))
        {
            mConnectionSecurity = CONNECTION_SECURITY_SSL_REQUIRED;
            mPort = 993;
        }
        else if (scheme.equals("imap+ssl"))
        {
            mConnectionSecurity = CONNECTION_SECURITY_SSL_OPTIONAL;
            mPort = 993;
        }
        else
        {
            throw new MessagingException("Unsupported protocol");
        }

        mHost = uri.getHost();

        if (uri.getPort() != -1)
        {
            mPort = uri.getPort();
        }

        if (uri.getUserInfo() != null)
        {
            try
            {
                String[] userInfoParts = uri.getUserInfo().split(":");
                if (userInfoParts.length == 2)
                {
                    mAuthType = AuthType.PLAIN;
                    mUsername = URLDecoder.decode(userInfoParts[0], "UTF-8");
                    mPassword = URLDecoder.decode(userInfoParts[1], "UTF-8");
                }
                else
                {
                    mAuthType = AuthType.valueOf(userInfoParts[0]);
                    mUsername = URLDecoder.decode(userInfoParts[1], "UTF-8");
                    mPassword = URLDecoder.decode(userInfoParts[2], "UTF-8");
                }
            }
            catch (UnsupportedEncodingException enc)
            {
                // This shouldn't happen since the encoding is hardcoded to UTF-8
                Log.e(K9.LOG_TAG, "Couldn't urldecode username or password.", enc);
            }
        }

        if ((uri.getPath() != null) && (uri.getPath().length() > 0))
        {
            mPathPrefix = uri.getPath().substring(1);
            if (mPathPrefix != null && mPathPrefix.trim().length() == 0)
            {
                mPathPrefix = null;
            }
        }

        mModifiedUtf7Charset = new CharsetProvider().charsetForName("X-RFC-3501");
    }

    @Override
    public Folder getFolder(String name) throws MessagingException
    {
        ImapFolder folder;
        synchronized (mFolderCache)
        {
            folder = mFolderCache.get(name);
            if (folder == null)
            {
                folder = new ImapFolder(this, name);
                mFolderCache.put(name, folder);
            }
        }
        return folder;
    }

    private String getCombinedPrefix()
    {
        if (mCombinedPrefix == null)
        {
            if (mPathPrefix != null)
            {
                String tmpPrefix = mPathPrefix.trim();
                String tmpDelim = (mPathDelimeter != null ? mPathDelimeter.trim() : "");
                if (tmpPrefix.endsWith(tmpDelim))
                {
                    mCombinedPrefix = tmpPrefix;
                }
                else if (tmpPrefix.length() > 0)
                {
                    mCombinedPrefix = tmpPrefix + tmpDelim;
                }
                else
                {
                    mCombinedPrefix = "";
                }
            }
            else
            {
                mCombinedPrefix = "";
            }
        }
        return mCombinedPrefix;
    }

    @Override
    public List<? extends Folder> getPersonalNamespaces() throws MessagingException
    {
        ImapConnection connection = getConnection();
        try
        {
            LinkedList<Folder> folders = new LinkedList<Folder>();

            List<ImapResponse> responses =
                connection.executeSimpleCommand(String.format("LIST \"\" \"%s*\"",
                                                getCombinedPrefix()));

            for (ImapResponse response : responses)
            {
                if (ImapResponseParser.equalsIgnoreCase(response.get(0), "LIST"))
                {
                    boolean includeFolder = true;
                    String folder = decodeFolderName(response.getString(3));

                    if (mPathDelimeter == null)
                    {
                        mPathDelimeter = response.getString(2);
                        mCombinedPrefix = null;
                    }

                    if (folder.equalsIgnoreCase(K9.INBOX))
                    {
                        continue;
                    }
                    else
                    {

                        if (getCombinedPrefix().length() > 0)
                        {
                            if (folder.length() >= getCombinedPrefix().length())
                            {
                                folder = folder.substring(getCombinedPrefix().length());
                            }
                            if (!decodeFolderName(response.getString(3)).equalsIgnoreCase(getCombinedPrefix() + folder))
                            {
                                includeFolder = false;
                            }
                        }
                    }

                    ImapList attributes = response.getList(1);
                    for (int i = 0, count = attributes.size(); i < count; i++)
                    {
                        String attribute = attributes.getString(i);
                        if (attribute.equalsIgnoreCase("\\NoSelect"))
                        {
                            includeFolder = false;
                        }
                    }
                    if (includeFolder)
                    {
                        folders.add(getFolder(folder));
                    }
                }
            }
            folders.add(getFolder("INBOX"));
            return folders;
        }
        catch (IOException ioe)
        {
            connection.close();
            throw new MessagingException("Unable to get folder list.", ioe);
        }
        finally
        {
            releaseConnection(connection);
        }
    }

    @Override
    public void checkSettings() throws MessagingException
    {
        try
        {
            ImapConnection connection = new ImapConnection();
            connection.open();
            connection.close();
        }
        catch (IOException ioe)
        {
            throw new MessagingException("Unable to connect.", ioe);
        }
    }

    /**
     * Gets a connection if one is available for reuse, or creates a new one if not.
     * @return
     */
    private ImapConnection getConnection() throws MessagingException
    {
        synchronized (mConnections)
        {
            ImapConnection connection = null;
            while ((connection = mConnections.poll()) != null)
            {
                try
                {
                    connection.executeSimpleCommand("NOOP");
                    break;
                }
                catch (IOException ioe)
                {
                    connection.close();
                }
            }
            if (connection == null)
            {
                connection = new ImapConnection();
            }
            return connection;
        }
    }

    private void releaseConnection(ImapConnection connection)
    {
        if (connection != null && connection.isOpen())
        {
            synchronized (mConnections)
            {
                mConnections.offer(connection);
            }
        }
    }

    private String encodeFolderName(String name)
    {
        try
        {
            ByteBuffer bb = mModifiedUtf7Charset.encode(name);
            byte[] b = new byte[bb.limit()];
            bb.get(b);
            return new String(b, "US-ASCII");
        }
        catch (UnsupportedEncodingException uee)
        {
            /*
             * The only thing that can throw this is getBytes("US-ASCII") and if US-ASCII doesn't
             * exist we're totally screwed.
             */
            throw new RuntimeException("Unable to encode folder name: " + name, uee);
        }
    }

    private String decodeFolderName(String name)
    {
        /*
         * Convert the encoded name to US-ASCII, then pass it through the modified UTF-7
         * decoder and return the Unicode String.
         */
        try
        {
            byte[] encoded = name.getBytes("US-ASCII");
            CharBuffer cb = mModifiedUtf7Charset.decode(ByteBuffer.wrap(encoded));
            return cb.toString();
        }
        catch (UnsupportedEncodingException uee)
        {
            /*
             * The only thing that can throw this is getBytes("US-ASCII") and if US-ASCII doesn't
             * exist we're totally screwed.
             */
            throw new RuntimeException("Unable to decode folder name: " + name, uee);
        }
    }

    @Override
    public boolean isMoveCapable()
    {
        return true;
    }

    @Override
    public boolean isCopyCapable()
    {
        return true;
    }
    @Override
    public boolean isPushCapable()
    {
        return true;
    }
    @Override
    public boolean isExpungeCapable()
    {
        return true;
    }


    class ImapFolder extends Folder
    {
        private String mName;
        protected int mMessageCount = -1;
        protected int uidNext = -1;
        protected ImapConnection mConnection;
        private OpenMode mMode;
        private boolean mExists;
        private ImapStore store = null;

        public ImapFolder(ImapStore nStore, String name)
        {
            super(nStore.getAccount());
            store = nStore;
            this.mName = name;
        }

        public String getPrefixedName()
        {
            String prefixedName = "";
            if (!K9.INBOX.equalsIgnoreCase(mName))
            {
                prefixedName = getCombinedPrefix();
            }

            prefixedName += mName;
            return prefixedName;
        }

        protected List<ImapResponse> executeSimpleCommand(String command) throws MessagingException, IOException
        {
            return handleUntaggedResponses(mConnection.executeSimpleCommand(command));
        }

        protected List<ImapResponse> executeSimpleCommand(String command, boolean sensitve, UntaggedHandler untaggedHandler) throws MessagingException, IOException
        {
            return handleUntaggedResponses(mConnection.executeSimpleCommand(command, sensitve, untaggedHandler));
        }

        @Override
        public void open(OpenMode mode) throws MessagingException
        {
            internalOpen(mode);

            if (mMessageCount == -1)
            {
                throw new MessagingException(
                    "Did not find message count during open");
            }
        }

        public List<ImapResponse> internalOpen(OpenMode mode) throws MessagingException
        {
            if (isOpen() && mMode == mode)
            {
                // Make sure the connection is valid. If it's not we'll close it down and continue
                // on to get a new one.
                try
                {
                    List<ImapResponse> responses = executeSimpleCommand("NOOP");
                    return responses;
                }
                catch (IOException ioe)
                {
                    ioExceptionHandler(mConnection, ioe);
                }
            }
            releaseConnection(mConnection);
            synchronized (this)
            {
                mConnection = getConnection();
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
            try
            {
                String command = String.format((mode == OpenMode.READ_WRITE ? "SELECT" : "EXAMINE") + " \"%s\"",
                                               encodeFolderName(getPrefixedName()));

                List<ImapResponse> responses = executeSimpleCommand(command);

                /*
                 * If the command succeeds we expect the folder has been opened read-write
                 * unless we are notified otherwise in the responses.
                 */
                mMode = mode;

                for (ImapResponse response : responses)
                {
                    if (response.mTag != null && response.size() >= 2)
                    {
                        Object bracketedObj = response.get(1);
                        if (bracketedObj instanceof ImapList)
                        {
                            ImapList bracketed = (ImapList)bracketedObj;

                            if (bracketed.size() > 0)
                            {
                                Object keyObj = bracketed.get(0);
                                if (keyObj instanceof String)
                                {
                                    String key = (String)keyObj;

                                    if ("READ-ONLY".equalsIgnoreCase(key))
                                    {
                                        mMode = OpenMode.READ_ONLY;
                                    }
                                    else if ("READ-WRITE".equalsIgnoreCase(key))
                                    {
                                        mMode = OpenMode.READ_WRITE;
                                    }
                                }
                            }
                        }

                    }
                }

                mExists = true;
                return responses;
            }
            catch (IOException ioe)
            {
                throw ioExceptionHandler(mConnection, ioe);
            }
            catch (MessagingException me)
            {
                Log.e(K9.LOG_TAG, "Unable to open connection for " + getLogId(), me);
                throw me;
            }

        }

        @Override
        public boolean isOpen()
        {
            return mConnection != null;
        }

        @Override
        public OpenMode getMode() throws MessagingException
        {
            return mMode;
        }

        @Override
        public void close()
        {
            if (mMessageCount != -1)
            {
                mMessageCount = -1;
            }
            if (!isOpen())
            {
                return;
            }

            synchronized (this)
            {
                releaseConnection(mConnection);
                mConnection = null;
            }
        }

        @Override
        public String getName()
        {
            return mName;
        }

        private boolean exists(String folderName) throws MessagingException
        {
            try
            {
                // Since we don't care about RECENT, we'll use that for the check, because we're checking
                // a folder other than ourself, and don't want any untagged responses to cause a change
                // in our own fields
                mConnection.executeSimpleCommand(String.format("STATUS \"%s\" (RECENT)", folderName));
                return true;
            }
            catch (IOException ioe)
            {
                throw ioExceptionHandler(mConnection, ioe);
            }
            catch (MessagingException me)
            {
                return false;
            }
        }

        @Override
        public boolean exists() throws MessagingException
        {
            if (mExists)
            {
                return true;
            }
            /*
             * This method needs to operate in the unselected mode as well as the selected mode
             * so we must get the connection ourselves if it's not there. We are specifically
             * not calling checkOpen() since we don't care if the folder is open.
             */
            ImapConnection connection = null;
            synchronized (this)
            {
                if (mConnection == null)
                {
                    connection = getConnection();
                }
                else
                {
                    connection = mConnection;
                }
            }
            try
            {
                connection.executeSimpleCommand(String.format("STATUS \"%s\" (UIDVALIDITY)",
                                                encodeFolderName(getPrefixedName())));
                mExists = true;
                return true;
            }
            catch (MessagingException me)
            {
                return false;
            }
            catch (IOException ioe)
            {
                throw ioExceptionHandler(connection, ioe);
            }
            finally
            {
                if (mConnection == null)
                {
                    releaseConnection(connection);
                }
            }
        }

        @Override
        public boolean create(FolderType type) throws MessagingException
        {
            /*
             * This method needs to operate in the unselected mode as well as the selected mode
             * so we must get the connection ourselves if it's not there. We are specifically
             * not calling checkOpen() since we don't care if the folder is open.
             */
            ImapConnection connection = null;
            synchronized (this)
            {
                if (mConnection == null)
                {
                    connection = getConnection();
                }
                else
                {
                    connection = mConnection;
                }
            }
            try
            {
                connection.executeSimpleCommand(String.format("CREATE \"%s\"",
                                                encodeFolderName(getPrefixedName())));
                return true;
            }
            catch (MessagingException me)
            {
                return false;
            }
            catch (IOException ioe)
            {
                throw ioExceptionHandler(mConnection, ioe);
            }
            finally
            {
                if (mConnection == null)
                {
                    releaseConnection(connection);
                }
            }
        }

        @Override
        public void copyMessages(Message[] messages, Folder folder) throws MessagingException
        {
            if (folder instanceof ImapFolder == false)
            {
                throw new MessagingException("ImapFolder.copyMessages passed non-ImapFolder");
            }

            if (messages.length == 0)
                return;

            ImapFolder iFolder = (ImapFolder)folder;
            checkOpen();
            String[] uids = new String[messages.length];
            for (int i = 0, count = messages.length; i < count; i++)
            {
                uids[i] = messages[i].getUid();
            }
            try
            {
                String remoteDestName = encodeFolderName(iFolder.getPrefixedName());

                if (!exists(remoteDestName))
                {
                    /*
                     * If the remote trash folder doesn't exist we try to create it.
                     */
                    if (K9.DEBUG)
                        Log.i(K9.LOG_TAG, "IMAPMessage.copyMessages: attempting to create remote '" + remoteDestName + "' folder for " + getLogId());
                    iFolder.create(FolderType.HOLDS_MESSAGES);
                }

                if (exists(remoteDestName))
                {
                    executeSimpleCommand(String.format("UID COPY %s \"%s\"",
                                                       Utility.combine(uids, ','),
                                                       encodeFolderName(iFolder.getPrefixedName())));
                }
                else
                {
                    throw new MessagingException("IMAPMessage.copyMessages: remote destination folder " + folder.getName()
                                                 + " does not exist and could not be created for " + getLogId()
                                                 , true);
                }
            }
            catch (IOException ioe)
            {
                throw ioExceptionHandler(mConnection, ioe);
            }
        }

        @Override
        public void moveMessages(Message[] messages, Folder folder) throws MessagingException
        {
            if (messages.length == 0)
                return;
            copyMessages(messages, folder);
            setFlags(messages, new Flag[] { Flag.DELETED }, true);
        }

        @Override
        public void delete(Message[] messages, String trashFolderName) throws MessagingException
        {
            if (messages.length == 0)
                return;

            if (trashFolderName == null || getName().equalsIgnoreCase(trashFolderName))
            {
                setFlags(messages, new Flag[] { Flag.DELETED }, true);
            }
            else
            {
                ImapFolder remoteTrashFolder = (ImapFolder)getStore().getFolder(trashFolderName);
                String remoteTrashName = encodeFolderName(remoteTrashFolder.getPrefixedName());

                if (!exists(remoteTrashName))
                {
                    /*
                     * If the remote trash folder doesn't exist we try to create it.
                     */
                    if (K9.DEBUG)
                        Log.i(K9.LOG_TAG, "IMAPMessage.delete: attempting to create remote '" + trashFolderName + "' folder for " + getLogId());
                    remoteTrashFolder.create(FolderType.HOLDS_MESSAGES);
                }

                if (exists(remoteTrashName))
                {
                    if (K9.DEBUG)
                        Log.d(K9.LOG_TAG, "IMAPMessage.delete: copying remote " + messages.length + " messages to '" + trashFolderName + "' for " + getLogId());

                    moveMessages(messages, remoteTrashFolder);
                }
                else
                {
                    throw new MessagingException("IMAPMessage.delete: remote Trash folder " + trashFolderName + " does not exist and could not be created for " + getLogId()
                                                 , true);
                }
            }
        }


        @Override
        public int getMessageCount()
        {
            return mMessageCount;
        }

        @Override
        public int getUnreadMessageCount() throws MessagingException
        {
            checkOpen();
            try
            {
                int count = 0;
                int start = mMessageCount - 299;
                if (start < 1)
                {
                    start = 1;
                }
                List<ImapResponse> responses = executeSimpleCommand(String.format("SEARCH %d:* UNSEEN NOT DELETED", start));
                for (ImapResponse response : responses)
                {
                    if (ImapResponseParser.equalsIgnoreCase(response.get(0), "SEARCH"))
                    {
                        count += response.size() - 1;
                    }
                }
                return count;
            }
            catch (IOException ioe)
            {
                throw ioExceptionHandler(mConnection, ioe);
            }
        }

        @Override
        public int getFlaggedMessageCount() throws MessagingException
        {
            checkOpen();
            try
            {
                int count = 0;
                int start = mMessageCount - 299;
                if (start < 1)
                {
                    start = 1;
                }
                List<ImapResponse> responses = executeSimpleCommand(String.format("SEARCH %d:* FLAGGED NOT DELETED", start));
                for (ImapResponse response : responses)
                {
                    if (ImapResponseParser.equalsIgnoreCase(response.get(0), "SEARCH"))
                    {
                        count += response.size() - 1;
                    }
                }
                return count;
            }
            catch (IOException ioe)
            {
                throw ioExceptionHandler(mConnection, ioe);
            }
        }

        protected int getHighestUid()
        {
            try
            {
                ImapSearcher searcher = new ImapSearcher()
                {
                    public List<ImapResponse> search() throws IOException, MessagingException
                    {
                        return executeSimpleCommand(String.format("UID SEARCH *:* "));
                    }
                };
                Message[] messages = search(searcher, null);
                if (messages.length > 0)
                {
                    return Integer.parseInt(messages[0].getUid());
                }
            }
            catch (Exception e)
            {
                Log.e(K9.LOG_TAG, "Unable to find highest UID in folder " + getName(), e);
            }
            return -1;

        }

        @Override
        public void delete(boolean recurse) throws MessagingException
        {
            throw new Error("ImapStore.delete() not yet implemented");
        }

        @Override
        public Message getMessage(String uid) throws MessagingException
        {
            return new ImapMessage(uid, this);
        }


        @Override
        public Message[] getMessages(int start, int end, MessageRetrievalListener listener)
        throws MessagingException
        {
            return getMessages(start, end, false, listener);
        }

        protected Message[] getMessages(final int start, final int end, final boolean includeDeleted, final MessageRetrievalListener listener)
        throws MessagingException
        {
            if (start < 1 || end < 1 || end < start)
            {
                throw new MessagingException(
                    String.format("Invalid message set %d %d",
                                  start, end));
            }
            ImapSearcher searcher = new ImapSearcher()
            {
                public List<ImapResponse> search() throws IOException, MessagingException
                {
                    return executeSimpleCommand(String.format("UID SEARCH %d:%d" + (includeDeleted ? "" : " NOT DELETED"), start, end));
                }
            };
            return search(searcher, listener);

        }
        protected Message[] getMessages(final List<Integer> mesgSeqs, final boolean includeDeleted, final MessageRetrievalListener listener)
        throws MessagingException
        {
            ImapSearcher searcher = new ImapSearcher()
            {
                public List<ImapResponse> search() throws IOException, MessagingException
                {
                    return executeSimpleCommand(String.format("UID SEARCH %s" + (includeDeleted ? "" : " NOT DELETED"), Utility.combine(mesgSeqs.toArray(), ',')));
                }
            };
            return search(searcher, listener);
        }

        private Message[] search(ImapSearcher searcher, MessageRetrievalListener listener) throws MessagingException
        {

            checkOpen();
            ArrayList<Message> messages = new ArrayList<Message>();
            try
            {
                ArrayList<Integer> uids = new ArrayList<Integer>();
                List<ImapResponse> responses = searcher.search(); //
                for (ImapResponse response : responses)
                {
                    if (response.mTag == null)
                    {
                        if (ImapResponseParser.equalsIgnoreCase(response.get(0), "SEARCH"))
                        {
                            for (int i = 1, count = response.size(); i < count; i++)
                            {
                                uids.add(Integer.parseInt(response.getString(i)));
                            }
                        }
                    }
                }

                // Sort the uids in numerically ascending order
                Collections.sort(uids);
                for (int i = 0, count = uids.size(); i < count; i++)
                {
                    if (listener != null)
                    {
                        listener.messageStarted("" + uids.get(i), i, count);
                    }
                    ImapMessage message = new ImapMessage("" + uids.get(i), this);
                    messages.add(message);
                    if (listener != null)
                    {
                        listener.messageFinished(message, i, count);
                    }
                }
            }
            catch (IOException ioe)
            {
                throw ioExceptionHandler(mConnection, ioe);
            }
            return messages.toArray(new Message[] {});
        }


        @Override
        public Message[] getMessages(MessageRetrievalListener listener) throws MessagingException
        {
            return getMessages(null, listener);
        }

        @Override
        public Message[] getMessages(String[] uids, MessageRetrievalListener listener)
        throws MessagingException
        {
            checkOpen();
            ArrayList<Message> messages = new ArrayList<Message>();
            try
            {
                if (uids == null)
                {
                    List<ImapResponse> responses = executeSimpleCommand("UID SEARCH 1:* NOT DELETED");
                    ArrayList<String> tempUids = new ArrayList<String>();
                    for (ImapResponse response : responses)
                    {
                        if (ImapResponseParser.equalsIgnoreCase(response.get(0), "SEARCH"))
                        {
                            for (int i = 1, count = response.size(); i < count; i++)
                            {
                                tempUids.add(response.getString(i));
                            }
                        }
                    }
                    uids = tempUids.toArray(new String[] {});
                }
                for (int i = 0, count = uids.length; i < count; i++)
                {
                    if (listener != null)
                    {
                        listener.messageStarted(uids[i], i, count);
                    }
                    ImapMessage message = new ImapMessage(uids[i], this);
                    messages.add(message);
                    if (listener != null)
                    {
                        listener.messageFinished(message, i, count);
                    }
                }
            }
            catch (IOException ioe)
            {
                throw ioExceptionHandler(mConnection, ioe);
            }
            return messages.toArray(new Message[] {});
        }

        @Override
        public void fetch(Message[] messages, FetchProfile fp, MessageRetrievalListener listener)
        throws MessagingException
        {
            if (messages == null || messages.length == 0)
            {
                return;
            }
            checkOpen();
            String[] uids = new String[messages.length];
            HashMap<String, Message> messageMap = new HashMap<String, Message>();
            for (int i = 0, count = messages.length; i < count; i++)
            {
                uids[i] = messages[i].getUid();
                messageMap.put(uids[i], messages[i]);
            }

            /*
             * Figure out what command we are going to run:
             * Flags - UID FETCH (FLAGS)
             * Envelope - UID FETCH ([FLAGS] INTERNALDATE UID RFC822.SIZE FLAGS BODY.PEEK[HEADER.FIELDS (date subject from content-type to cc)])
             *
             */
            LinkedHashSet<String> fetchFields = new LinkedHashSet<String>();
            fetchFields.add("UID");
            if (fp.contains(FetchProfile.Item.FLAGS))
            {
                fetchFields.add("FLAGS");
            }
            if (fp.contains(FetchProfile.Item.ENVELOPE))
            {
                fetchFields.add("INTERNALDATE");
                fetchFields.add("RFC822.SIZE");
                fetchFields.add("BODY.PEEK[HEADER.FIELDS (date subject from content-type to cc reply-to "
                                + K9.K9MAIL_IDENTITY + ")]");
            }
            if (fp.contains(FetchProfile.Item.STRUCTURE))
            {
                fetchFields.add("BODYSTRUCTURE");
            }
            if (fp.contains(FetchProfile.Item.BODY_SANE))
            {
                fetchFields.add(String.format("BODY.PEEK[]<0.%d>", FETCH_BODY_SANE_SUGGESTED_SIZE));
            }
            if (fp.contains(FetchProfile.Item.BODY))
            {
                fetchFields.add("BODY.PEEK[]");
            }
            for (Object o : fp)
            {
                if (o != null && o instanceof Part)
                {
                    Part part = (Part) o;
                    String[] parts = part.getHeader(MimeHeader.HEADER_ANDROID_ATTACHMENT_STORE_DATA);
                    if (parts != null)
                    {
                        String partId = parts[0];
                        if ("TEXT".equalsIgnoreCase(partId))
                        {
                            fetchFields.add(String.format("BODY.PEEK[TEXT]<0.%d>", FETCH_BODY_SANE_SUGGESTED_SIZE));
                        }
                        else
                        {
                            fetchFields.add("BODY.PEEK[" + partId + "]");
                        }
                    }
                }
            }

            try
            {
                mConnection.sendCommand(String.format("UID FETCH %s (%s)",
                                                      Utility.combine(uids, ','),
                                                      Utility.combine(fetchFields.toArray(new String[fetchFields.size()]), ' ')
                                                     ), false);
                ImapResponse response;
                int messageNumber = 0;
                do
                {
                    response = mConnection.readResponse();
                    if (K9.DEBUG)
                        Log.v(K9.LOG_TAG, "response for fetch: " + response + " for " + getLogId());

                    if (response.mTag == null && ImapResponseParser.equalsIgnoreCase(response.get(1), "FETCH"))
                    {
                        ImapList fetchList = (ImapList)response.getKeyedValue("FETCH");
                        String uid = fetchList.getKeyedString("UID");

                        Message message = messageMap.get(uid);
                        if (message == null)
                        {
                            if (K9.DEBUG)
                                Log.d(K9.LOG_TAG, "Do not have message in messageMap for UID " + uid + " for " + getLogId());

                            handleUntaggedResponse(response);
                            continue;
                        }
                        if (listener != null)
                        {
                            listener.messageStarted(uid, messageNumber++, messageMap.size());
                        }

                        ImapMessage imapMessage = (ImapMessage) message;

                        if (fetchList.containsKey("FLAGS"))
                        {
                            ImapList flags = fetchList.getKeyedList("FLAGS");
                            if (flags != null)
                            {
                                for (int i = 0, count = flags.size(); i < count; i++)
                                {
                                    String flag = flags.getString(i);
                                    if (flag.equalsIgnoreCase("\\Deleted"))
                                    {
                                        imapMessage.setFlagInternal(Flag.DELETED, true);
                                    }
                                    else if (flag.equalsIgnoreCase("\\Answered"))
                                    {
                                        imapMessage.setFlagInternal(Flag.ANSWERED, true);
                                    }
                                    else if (flag.equalsIgnoreCase("\\Seen"))
                                    {
                                        imapMessage.setFlagInternal(Flag.SEEN, true);
                                    }
                                    else if (flag.equalsIgnoreCase("\\Flagged"))
                                    {
                                        imapMessage.setFlagInternal(Flag.FLAGGED, true);
                                    }
                                }
                            }
                        }

                        if (fetchList.containsKey("INTERNALDATE"))
                        {
                            Date internalDate = fetchList.getKeyedDate("INTERNALDATE");
                            message.setInternalDate(internalDate);
                        }
                        if (fetchList.containsKey("RFC822.SIZE"))
                        {
                            int size = fetchList.getKeyedNumber("RFC822.SIZE");
                            imapMessage.setSize(size);
                        }
                        if (fetchList.containsKey("BODYSTRUCTURE"))
                        {
                            ImapList bs = fetchList.getKeyedList("BODYSTRUCTURE");
                            if (bs != null)
                            {
                                try
                                {
                                    parseBodyStructure(bs, message, "TEXT");
                                }
                                catch (MessagingException e)
                                {
                                    if (K9.DEBUG)
                                        Log.d(K9.LOG_TAG, "Error handling message for " + getLogId(), e);
                                    message.setBody(null);
                                }
                            }
                        }

                        if (fetchList.containsKey("BODY"))
                        {
                            Part part = null;
                            for (Object o : fp)
                            {
                                if (o instanceof Part)
                                {
                                    part = (Part) o;
                                    break;
                                }
                            }

                            int index = fetchList.getKeyIndex("BODY") + 2;
                            Object literal = fetchList.getObject(index);

                            // Check if there's an origin octet
                            if (literal instanceof String)
                            {
                                String originOctet = (String)literal;
                                if (originOctet.startsWith("<"))
                                {
                                    literal = fetchList.getObject(index + 1);
                                }
                            }

                            InputStream bodyStream;
                            if (literal instanceof InputStream)
                            {
                                bodyStream = (InputStream)literal;
                            }
                            else if (literal instanceof String)
                            {
                                String bodyString = (String)literal;

                                if (K9.DEBUG)
                                    Log.v(K9.LOG_TAG, "Part is a String: '" + bodyString + "' for " + getLogId());

                                bodyStream = new ByteArrayInputStream(bodyString.getBytes());
                            }
                            else
                            {
                                // This shouldn't happen
                                throw new MessagingException("Got FETCH response with bogus parameters");
                            }

                            if (part != null)
                            {
                                String contentTransferEncoding = part.getHeader(
                                                                     MimeHeader.HEADER_CONTENT_TRANSFER_ENCODING)[0];
                                part.setBody(MimeUtility.decodeBody(bodyStream, contentTransferEncoding));
                            }
                            else
                            {
                                imapMessage.parse(bodyStream);
                            }
                        }

                        if (listener != null)
                        {
                            listener.messageFinished(message, messageNumber, messageMap.size());
                        }
                    }
                    else
                    {
                        handleUntaggedResponse(response);
                    }

                    while (response.more());

                }
                while (response.mTag == null);
            }
            catch (IOException ioe)
            {
                throw ioExceptionHandler(mConnection, ioe);
            }
        }

        @Override
        public Flag[] getPermanentFlags() throws MessagingException
        {
            return PERMANENT_FLAGS;
        }

        /**
         * Handle any untagged responses that the caller doesn't care to handle themselves.
         * @param responses
         */
        protected List<ImapResponse> handleUntaggedResponses(List<ImapResponse> responses)
        {
            for (ImapResponse response : responses)
            {
                handleUntaggedResponse(response);
            }
            return responses;
        }

        protected void handlePossibleUidNext(ImapResponse response)
        {
            if (ImapResponseParser.equalsIgnoreCase(response.get(0), "OK") && response.size() > 1)
            {
                Object bracketedObj = response.get(1);
                if (bracketedObj instanceof ImapList)
                {
                    ImapList bracketed = (ImapList)bracketedObj;

                    if (bracketed.size() > 1)
                    {
                        Object keyObj = bracketed.get(0);
                        if (keyObj instanceof String)
                        {
                            String key = (String)keyObj;
                            if ("UIDNEXT".equalsIgnoreCase(key))
                            {
                                uidNext = bracketed.getNumber(1);
                                if (K9.DEBUG)
                                    Log.d(K9.LOG_TAG, "Got UidNext = " + uidNext + " for " + getLogId());
                            }
                        }
                    }


                }
            }
        }

        /**
         * Handle an untagged response that the caller doesn't care to handle themselves.
         * @param response
         */
        protected void handleUntaggedResponse(ImapResponse response)
        {
            if (response.mTag == null && response.size() > 1)
            {
                if (ImapResponseParser.equalsIgnoreCase(response.get(1), "EXISTS"))
                {
                    mMessageCount = response.getNumber(0);
                    if (K9.DEBUG)
                        Log.d(K9.LOG_TAG, "Got untagged EXISTS with value " + mMessageCount + " for " + getLogId());
                }
                handlePossibleUidNext(response);

                if (ImapResponseParser.equalsIgnoreCase(response.get(1), "EXPUNGE") && mMessageCount > 0)
                {
                    mMessageCount--;
                    if (K9.DEBUG)
                        Log.d(K9.LOG_TAG, "Got untagged EXPUNGE with value " + mMessageCount + " for " + getLogId());
                }
//            if (response.size() > 1) {
//                Object bracketedObj = response.get(1);
//                if (bracketedObj instanceof ImapList)
//                {
//                    ImapList bracketed = (ImapList)bracketedObj;
//
//                    if (bracketed.size() > 0)
//                    {
//                        Object keyObj = bracketed.get(0);
//                        if (keyObj instanceof String)
//                        {
//                            String key = (String)keyObj;
//                            if ("ALERT".equalsIgnoreCase(key))
//                            {
//                                StringBuffer sb = new StringBuffer();
//                                for (int i = 2, count = response.size(); i < count; i++) {
//                                    sb.append(response.get(i).toString());
//                                    sb.append(' ');
//                                }
//
//                                Log.w(K9.LOG_TAG, "ALERT: " + sb.toString() + " for " + getLogId());
//                            }
//                        }
//                    }
//
//
//                }
//            }
            }
            //Log.i(K9.LOG_TAG, "mMessageCount = " + mMessageCount + " for " + getLogId());
        }

        private void parseBodyStructure(ImapList bs, Part part, String id)
        throws MessagingException
        {
            if (bs.get(0) instanceof ImapList)
            {
                /*
                 * This is a multipart/*
                 */
                MimeMultipart mp = new MimeMultipart();
                for (int i = 0, count = bs.size(); i < count; i++)
                {
                    if (bs.get(i) instanceof ImapList)
                    {
                        /*
                         * For each part in the message we're going to add a new BodyPart and parse
                         * into it.
                         */
                        ImapBodyPart bp = new ImapBodyPart();
                        if (id.equalsIgnoreCase("TEXT"))
                        {
                            parseBodyStructure(bs.getList(i), bp, Integer.toString(i + 1));
                        }
                        else
                        {
                            parseBodyStructure(bs.getList(i), bp, id + "." + (i + 1));
                        }
                        mp.addBodyPart(bp);
                    }
                    else
                    {
                        /*
                         * We've got to the end of the children of the part, so now we can find out
                         * what type it is and bail out.
                         */
                        String subType = bs.getString(i);
                        mp.setSubType(subType.toLowerCase());
                        break;
                    }
                }
                part.setBody(mp);
            }
            else
            {
                /*
                 * This is a body. We need to add as much information as we can find out about
                 * it to the Part.
                 */

                /*
                 body type
                 body subtype
                 body parameter parenthesized list
                 body id
                 body description
                 body encoding
                 body size
                 */


                String type = bs.getString(0);
                String subType = bs.getString(1);
                String mimeType = (type + "/" + subType).toLowerCase();

                ImapList bodyParams = null;
                if (bs.get(2) instanceof ImapList)
                {
                    bodyParams = bs.getList(2);
                }
                String encoding = bs.getString(5);
                int size = bs.getNumber(6);

                if (MimeUtility.mimeTypeMatches(mimeType, "message/rfc822"))
                {
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
                String contentType = String.format("%s", mimeType);

                if (bodyParams != null)
                {
                    /*
                     * If there are body params we might be able to get some more information out
                     * of them.
                     */
                    for (int i = 0, count = bodyParams.size(); i < count; i += 2)
                    {
                        contentType += String.format(";\n %s=\"%s\"",
                                                     bodyParams.getString(i),
                                                     bodyParams.getString(i + 1));
                    }
                }

                part.setHeader(MimeHeader.HEADER_CONTENT_TYPE, contentType);

                // Extension items
                ImapList bodyDisposition = null;
                if (("text".equalsIgnoreCase(type))
                        && (bs.size() > 8)
                        && (bs.get(9) instanceof ImapList))
                {
                    bodyDisposition = bs.getList(9);
                }
                else if (!("text".equalsIgnoreCase(type))
                         && (bs.size() > 7)
                         && (bs.get(8) instanceof ImapList))
                {
                    bodyDisposition = bs.getList(8);
                }

                String contentDisposition = "";

                if (bodyDisposition != null && bodyDisposition.size() > 0)
                {
                    if (!"NIL".equalsIgnoreCase(bodyDisposition.getString(0)))
                    {
                        contentDisposition = bodyDisposition.getString(0).toLowerCase();
                    }

                    if ((bodyDisposition.size() > 1)
                            && (bodyDisposition.get(1) instanceof ImapList))
                    {
                        ImapList bodyDispositionParams = bodyDisposition.getList(1);
                        /*
                         * If there is body disposition information we can pull some more information
                         * about the attachment out.
                         */
                        for (int i = 0, count = bodyDispositionParams.size(); i < count; i += 2)
                        {
                            contentDisposition += String.format(";\n %s=\"%s\"",
                                                                bodyDispositionParams.getString(i).toLowerCase(),
                                                                bodyDispositionParams.getString(i + 1));
                        }
                    }
                }

                if (MimeUtility.getHeaderParameter(contentDisposition, "size") == null)
                {
                    contentDisposition += String.format(";\n size=%d", size);
                }

                /*
                 * Set the content disposition containing at least the size. Attachment
                 * handling code will use this down the road.
                 */
                part.setHeader(MimeHeader.HEADER_CONTENT_DISPOSITION, contentDisposition);


                /*
                 * Set the Content-Transfer-Encoding header. Attachment code will use this
                 * to parse the body.
                 */
                part.setHeader(MimeHeader.HEADER_CONTENT_TRANSFER_ENCODING, encoding);

                if (part instanceof ImapMessage)
                {
                    ((ImapMessage) part).setSize(size);
                }
                else if (part instanceof ImapBodyPart)
                {
                    ((ImapBodyPart) part).setSize(size);
                }
                else
                {
                    throw new MessagingException("Unknown part type " + part.toString());
                }
                part.setHeader(MimeHeader.HEADER_ANDROID_ATTACHMENT_STORE_DATA, id);
            }

        }

        /**
         * Appends the given messages to the selected folder. This implementation also determines
         * the new UID of the given message on the IMAP server and sets the Message's UID to the
         * new server UID.
         */
        @Override
        public void appendMessages(Message[] messages) throws MessagingException
        {
            checkOpen();
            try
            {
                for (Message message : messages)
                {
                    CountingOutputStream out = new CountingOutputStream();
                    EOLConvertingOutputStream eolOut = new EOLConvertingOutputStream(out);
                    message.writeTo(eolOut);
                    eolOut.flush();

                    mConnection.sendCommand(
                        String.format("APPEND \"%s\" (%s) {%d}",
                                      encodeFolderName(getPrefixedName()),
                                      combineFlags(message.getFlags()),
                                      out.getCount()), false);
                    ImapResponse response;
                    do
                    {
                        response = mConnection.readResponse();
                        handleUntaggedResponse(response);
                        if (response.mCommandContinuationRequested)
                        {
                            eolOut = new EOLConvertingOutputStream(mConnection.mOut);
                            message.writeTo(eolOut);
                            eolOut.write('\r');
                            eolOut.write('\n');
                            eolOut.flush();
                        }
                        while (response.more());
                    }
                    while (response.mTag == null);

                    String newUid = getUidFromMessageId(message);
                    if (K9.DEBUG)
                        Log.d(K9.LOG_TAG, "Got UID " + newUid + " for message for " + getLogId());

                    if (newUid != null)
                    {
                        message.setUid(newUid);
                    }


                }
            }
            catch (IOException ioe)
            {
                throw ioExceptionHandler(mConnection, ioe);
            }
        }

        @Override
        public String getUidFromMessageId(Message message) throws MessagingException
        {
            try
            {
                /*
                * Try to find the UID of the message we just appended using the
                * Message-ID header.
                */
                String[] messageIdHeader = message.getHeader("Message-ID");

                if (messageIdHeader == null || messageIdHeader.length == 0)
                {
                    if (K9.DEBUG)
                        Log.d(K9.LOG_TAG, "Did not get a message-id in order to search for UID  for " + getLogId());
                    return null;
                }
                String messageId = messageIdHeader[0];
                if (K9.DEBUG)
                    Log.d(K9.LOG_TAG, "Looking for UID for message with message-id " + messageId + " for " + getLogId());

                List<ImapResponse> responses =
                    executeSimpleCommand(
                        String.format("UID SEARCH HEADER MESSAGE-ID %s", messageId));
                for (ImapResponse response1 : responses)
                {
                    if (response1.mTag == null && ImapResponseParser.equalsIgnoreCase(response1.get(0), "SEARCH")
                            && response1.size() > 1)
                    {
                        return response1.getString(1);
                    }
                }
                return null;
            }
            catch (IOException ioe)
            {
                throw new MessagingException("Could not find UID for message based on Message-ID", ioe);
            }
        }


        @Override
        public void expunge() throws MessagingException
        {
            checkOpen();
            try
            {
                executeSimpleCommand("EXPUNGE");
            }
            catch (IOException ioe)
            {
                throw ioExceptionHandler(mConnection, ioe);
            }
        }

        private String combineFlags(Flag[] flags)
        {
            ArrayList<String> flagNames = new ArrayList<String>();
            for (int i = 0, count = flags.length; i < count; i++)
            {
                Flag flag = flags[i];
                if (flag == Flag.SEEN)
                {
                    flagNames.add("\\Seen");
                }
                else if (flag == Flag.DELETED)
                {
                    flagNames.add("\\Deleted");
                }
                else if (flag == Flag.ANSWERED)
                {
                    flagNames.add("\\Answered");
                }
                else if (flag == Flag.FLAGGED)
                {
                    flagNames.add("\\Flagged");
                }

            }
            return Utility.combine(flagNames.toArray(new String[flagNames.size()]), ' ');
        }


        @Override
        public void setFlags(Flag[] flags, boolean value)
        throws MessagingException
        {
            checkOpen();


            try
            {
                executeSimpleCommand(String.format("UID STORE 1:* %sFLAGS.SILENT (%s)",
                                                   value ? "+" : "-", combineFlags(flags)));
            }
            catch (IOException ioe)
            {
                throw ioExceptionHandler(mConnection, ioe);
            }
        }

        @Override
        public String getNewPushState(String oldPushStateS, Message message)
        {
            try
            {
                String messageUidS = message.getUid();
                int messageUid = Integer.parseInt(messageUidS);
                ImapPushState oldPushState = ImapPushState.parse(oldPushStateS);
                if (messageUid >= oldPushState.uidNext)
                {
                    int uidNext = messageUid + 1;
                    ImapPushState newPushState = new ImapPushState(uidNext);
                    return newPushState.toString();
                }
                else
                {
                    return null;
                }
            }
            catch (Exception e)
            {
                Log.e(K9.LOG_TAG, "Exception while updated push state for " + getLogId(), e);
                return null;
            }
        }


        @Override
        public void setFlags(Message[] messages, Flag[] flags, boolean value)
        throws MessagingException
        {
            checkOpen();
            String[] uids = new String[messages.length];
            for (int i = 0, count = messages.length; i < count; i++)
            {
                uids[i] = messages[i].getUid();
            }
            ArrayList<String> flagNames = new ArrayList<String>();
            for (int i = 0, count = flags.length; i < count; i++)
            {
                Flag flag = flags[i];
                if (flag == Flag.SEEN)
                {
                    flagNames.add("\\Seen");
                }
                else if (flag == Flag.DELETED)
                {
                    flagNames.add("\\Deleted");
                }
                else if (flag == Flag.ANSWERED)
                {
                    flagNames.add("\\Answered");
                }
                else if (flag == Flag.FLAGGED)
                {
                    flagNames.add("\\Flagged");
                }
            }
            try
            {
                executeSimpleCommand(String.format("UID STORE %s %sFLAGS.SILENT (%s)",
                                                   Utility.combine(uids, ','),
                                                   value ? "+" : "-",
                                                   Utility.combine(flagNames.toArray(new String[flagNames.size()]), ' ')));
            }
            catch (IOException ioe)
            {
                throw ioExceptionHandler(mConnection, ioe);
            }
        }

        private void checkOpen() throws MessagingException
        {
            if (!isOpen())
            {
                throw new MessagingException("Folder " + getPrefixedName() + " is not open.");
            }
        }

        private MessagingException ioExceptionHandler(ImapConnection connection, IOException ioe)
        throws MessagingException
        {
            Log.e(K9.LOG_TAG, "IOException for " + getLogId(), ioe);
            connection.close();
            close();
            return new MessagingException("IO Error", ioe);
        }

        @Override
        public boolean equals(Object o)
        {
            if (o instanceof ImapFolder)
            {
                return ((ImapFolder)o).getPrefixedName().equalsIgnoreCase(getPrefixedName());
            }
            return super.equals(o);
        }

        @Override
        public int hashCode()
        {
            return getPrefixedName().hashCode();
        }

        protected ImapStore getStore()
        {
            return store;
        }

        protected String getLogId()
        {
            String id = getAccount().getDescription() + ":" + getName() + "/" + Thread.currentThread().getName();
            if (mConnection != null)
            {
                id += "/" + mConnection.getLogId();
            }
            return id;
        }
    }

    /**
     * A cacheable class that stores the details for a single IMAP connection.
     */
    class ImapConnection
    {
        private Socket mSocket;
        private PeekableInputStream mIn;
        private OutputStream mOut;
        private ImapResponseParser mParser;
        private int mNextCommandTag;
        protected Set<String> capabilities = new HashSet<String>();

        private String getLogId()
        {
            return "conn" + hashCode();
        }

        private List<ImapResponse> receiveCapabilities(List<ImapResponse> responses)
        {
            for (ImapResponse response : responses)
            {
                ImapList capabilityList = null;
                if (response.size() > 0 && ImapResponseParser.equalsIgnoreCase(response.get(0), "OK"))
                {
                    for (Object thisPart : response)
                    {
                        if (thisPart instanceof ImapList)
                        {
                            ImapList thisList = (ImapList)thisPart;
                            if (ImapResponseParser.equalsIgnoreCase(thisList.get(0), CAPABILITY_CAPABILITY))
                            {
                                capabilityList = thisList;
                                break;
                            }
                        }
                    }
                }
                else if (response.mTag == null)
                {
                    capabilityList = response;
                }

                if (capabilityList != null)
                {
                    if (capabilityList.size() > 0 && ImapResponseParser.equalsIgnoreCase(capabilityList.get(0), CAPABILITY_CAPABILITY))
                    {
                        if (K9.DEBUG)
                        {
                            Log.d(K9.LOG_TAG, "Saving " + capabilityList.size() + " capabilities for " + getLogId());
                        }
                        for (Object capability : capabilityList)
                        {
                            if (capability instanceof String)
                            {
//                                if (K9.DEBUG)
//                                {
//                                    Log.v(K9.LOG_TAG, "Saving capability '" + capability + "' for " + getLogId());
//                                }
                                capabilities.add((String)capability);
                            }
                        }

                    }
                }
            }
            return responses;
        }


        public void open() throws IOException, MessagingException
        {
            if (isOpen())
            {
                return;
            }

            boolean authSuccess = false;

            mNextCommandTag = 1;
            try
            {
                Security.setProperty("networkaddress.cache.ttl", "0");
            }
            catch (Exception e)
            {
                Log.w(K9.LOG_TAG, "Could not set DNS ttl to 0 for " + getLogId(), e);
            }

            try
            {

                SocketAddress socketAddress = new InetSocketAddress(mHost, mPort);

                if (K9.DEBUG)
                    Log.i(K9.LOG_TAG, "Connection " + getLogId() + " connecting to " + mHost + " @ IP addr " + socketAddress);

                if (mConnectionSecurity == CONNECTION_SECURITY_SSL_REQUIRED ||
                        mConnectionSecurity == CONNECTION_SECURITY_SSL_OPTIONAL)
                {
                    SSLContext sslContext = SSLContext.getInstance("TLS");
                    final boolean secure = mConnectionSecurity == CONNECTION_SECURITY_SSL_REQUIRED;
                    sslContext.init(null, new TrustManager[]
                                    {
                                        TrustManagerFactory.get(mHost, secure)
                                    }, new SecureRandom());
                    mSocket = sslContext.getSocketFactory().createSocket();
                    mSocket.connect(socketAddress, SOCKET_CONNECT_TIMEOUT);
                }
                else
                {
                    mSocket = new Socket();
                    mSocket.connect(socketAddress, SOCKET_CONNECT_TIMEOUT);
                }

                setReadTimeout(Store.SOCKET_READ_TIMEOUT);

                mIn = new PeekableInputStream(new BufferedInputStream(mSocket.getInputStream(),
                                              1024));
                mParser = new ImapResponseParser(mIn);
                mOut = mSocket.getOutputStream();

                capabilities.clear();
                ImapResponse nullResponse = mParser.readResponse();
                if (K9.DEBUG)
                    Log.v(K9.LOG_TAG, getLogId() + "<<<" + nullResponse);

                List<ImapResponse> nullResponses = new LinkedList<ImapResponse>();
                nullResponses.add(nullResponse);
                receiveCapabilities(nullResponses);

                if (hasCapability(CAPABILITY_CAPABILITY) == false)
                {
                    if (K9.DEBUG)
                        Log.i(K9.LOG_TAG, "Did not get capabilities in banner, requesting CAPABILITY for " + getLogId());
                    List<ImapResponse> responses = receiveCapabilities(executeSimpleCommand(COMMAND_CAPABILITY));
                    if (responses.size() != 2)
                    {
                        throw new MessagingException("Invalid CAPABILITY response received");
                    }
                }

                if (mConnectionSecurity == CONNECTION_SECURITY_TLS_OPTIONAL
                        || mConnectionSecurity == CONNECTION_SECURITY_TLS_REQUIRED)
                {

                    if (hasCapability("STARTTLS"))
                    {
                        // STARTTLS
                        executeSimpleCommand("STARTTLS");

                        SSLContext sslContext = SSLContext.getInstance("TLS");
                        boolean secure = mConnectionSecurity == CONNECTION_SECURITY_TLS_REQUIRED;
                        sslContext.init(null, new TrustManager[]
                                        {
                                            TrustManagerFactory.get(mHost, secure)
                                        }, new SecureRandom());
                        mSocket = sslContext.getSocketFactory().createSocket(mSocket, mHost, mPort,
                                  true);
                        mSocket.setSoTimeout(Store.SOCKET_READ_TIMEOUT);
                        mIn = new PeekableInputStream(new BufferedInputStream(mSocket
                                                      .getInputStream(), 1024));
                        mParser = new ImapResponseParser(mIn);
                        mOut = mSocket.getOutputStream();
                    }
                    else if (mConnectionSecurity == CONNECTION_SECURITY_TLS_REQUIRED)
                    {
                        throw new MessagingException("TLS not supported but required");
                    }
                }

                mOut = new BufferedOutputStream(mOut, 1024);

                try
                {
                    if (mHost.endsWith("yahoo.com"))
                    {
                        if (K9.DEBUG)
                            Log.v(K9.LOG_TAG, "Found Yahoo! account.  Sending proprietary commands.");
                        executeSimpleCommand("ID (\"GUID\" \"1\")");
                    }
                    if (mAuthType == AuthType.CRAM_MD5)
                    {
                        authCramMD5();
                        // The authCramMD5 method called on the previous line does not allow for handling updated capabilities
                        // sent by the server.  So, to make sure we update to the post-authentication capability list
                        // we fetch the capabilities here.
                        if (K9.DEBUG)
                            Log.i(K9.LOG_TAG, "Updating capabilities after CRAM-MD5 authentication for " + getLogId());
                        List<ImapResponse> responses = receiveCapabilities(executeSimpleCommand(COMMAND_CAPABILITY));
                        if (responses.size() != 2)
                        {
                            throw new MessagingException("Invalid CAPABILITY response received");
                        }

                    }
                    else if (mAuthType == AuthType.PLAIN)
                    {
                        receiveCapabilities(executeSimpleCommand("LOGIN \"" + escapeString(mUsername) + "\" \"" + escapeString(mPassword) + "\"", true));
                    }
                    authSuccess = true;
                }
                catch (ImapException ie)
                {
                    throw new AuthenticationFailedException(ie.getAlertText(), ie);

                }
                catch (MessagingException me)
                {
                    throw new AuthenticationFailedException(null, me);
                }
                if (K9.DEBUG)
                {
                    Log.d(K9.LOG_TAG, CAPABILITY_COMPRESS_DEFLATE + " = " + hasCapability(CAPABILITY_COMPRESS_DEFLATE));
                }
                if (hasCapability(CAPABILITY_COMPRESS_DEFLATE))
                {
                    ConnectivityManager connectivityManager = (ConnectivityManager)K9.app.getSystemService(Context.CONNECTIVITY_SERVICE);
                    boolean useCompression = true;

                    NetworkInfo netInfo = connectivityManager.getActiveNetworkInfo();
                    if (netInfo != null)
                    {
                        int type = netInfo.getType();
                        if (K9.DEBUG)
                            Log.d(K9.LOG_TAG, "On network type " + type);
                        useCompression = mAccount.useCompression(type);

                    }
                    if (K9.DEBUG)
                        Log.d(K9.LOG_TAG, "useCompression " + useCompression);
                    if (useCompression)
                    {
                        try
                        {
                            executeSimpleCommand(COMMAND_COMPRESS_DEFLATE);
                            ZInputStream zInputStream = new ZInputStream(mSocket.getInputStream(), true);
                            zInputStream.setFlushMode(JZlib.Z_PARTIAL_FLUSH);
                            mIn = new PeekableInputStream(new BufferedInputStream(zInputStream, 1024));
                            mParser = new ImapResponseParser(mIn);
                            ZOutputStream zOutputStream = new ZOutputStream(mSocket.getOutputStream(), JZlib.Z_BEST_SPEED, true);
                            mOut = new BufferedOutputStream(zOutputStream, 1024);
                            zOutputStream.setFlushMode(JZlib.Z_PARTIAL_FLUSH);
                            if (K9.DEBUG)
                            {
                                Log.i(K9.LOG_TAG, "Compression enabled for " + getLogId());
                            }
                        }
                        catch (Exception e)
                        {
                            Log.e(K9.LOG_TAG, "Unable to negotiate compression", e);
                        }
                    }
                }


                if (K9.DEBUG)
                    Log.d(K9.LOG_TAG, "NAMESPACE = " + hasCapability(CAPABILITY_NAMESPACE)
                          + ", mPathPrefix = " + mPathPrefix);

                if (mPathPrefix == null)
                {
                    if (hasCapability(CAPABILITY_NAMESPACE))
                    {
                        if (K9.DEBUG)
                            Log.i(K9.LOG_TAG, "mPathPrefix is unset and server has NAMESPACE capability");
                        List<ImapResponse> namespaceResponses =
                            executeSimpleCommand(COMMAND_NAMESPACE);
                        for (ImapResponse response : namespaceResponses)
                        {
                            if (ImapResponseParser.equalsIgnoreCase(response.get(0), COMMAND_NAMESPACE))
                            {
                                if (K9.DEBUG)
                                    Log.d(K9.LOG_TAG, "Got NAMESPACE response " + response + " on " + getLogId());

                                Object personalNamespaces = response.get(1);
                                if (personalNamespaces != null && personalNamespaces instanceof ImapList)
                                {
                                    if (K9.DEBUG)
                                        Log.d(K9.LOG_TAG, "Got personal namespaces: " + personalNamespaces);
                                    ImapList bracketed = (ImapList)personalNamespaces;
                                    Object firstNamespace = bracketed.get(0);
                                    if (firstNamespace != null && firstNamespace instanceof ImapList)
                                    {
                                        if (K9.DEBUG)
                                            Log.d(K9.LOG_TAG, "Got first personal namespaces: " + firstNamespace);
                                        bracketed = (ImapList)firstNamespace;
                                        mPathPrefix = bracketed.getString(0);
                                        mPathDelimeter = bracketed.getString(1);
                                        mCombinedPrefix = null;
                                        if (K9.DEBUG)
                                            Log.d(K9.LOG_TAG, "Got path '" + mPathPrefix + "' and separator '" + mPathDelimeter + "'");
                                    }
                                }
                            }
                        }
                    }
                    else
                    {
                        if (K9.DEBUG)
                            Log.i(K9.LOG_TAG, "mPathPrefix is unset but server does not have NAMESPACE capability");
                        mPathPrefix = "";
                    }
                }
            }
            catch (SSLException e)
            {
                throw new CertificateValidationException(e.getMessage(), e);
            }
            catch (GeneralSecurityException gse)
            {
                throw new MessagingException(
                    "Unable to open connection to IMAP server due to security error.", gse);
            }
            catch (ConnectException ce)
            {
                String ceMess = ce.getMessage();
                String[] tokens = ceMess.split("-");
                if (tokens != null && tokens.length > 1 && tokens[1] != null)
                {
                    Log.e(K9.LOG_TAG, "Stripping host/port from ConnectionException for " + getLogId(), ce);
                    throw new ConnectException(tokens[1].trim());
                }
                else
                {
                    throw ce;
                }
            }
            finally
            {
                if (authSuccess == false)
                {
                    Log.e(K9.LOG_TAG, "Failed to login, closing connection for " + getLogId());
                    close();
                }
            }
        }

        protected void authCramMD5() throws AuthenticationFailedException, MessagingException
        {
            try
            {
                String tag = sendCommand("AUTHENTICATE CRAM-MD5", false);
                byte[] buf = new byte[ 1024 ];
                int b64NonceLen = 0;
                for (int i = 0; i < buf.length; i++)
                {
                    buf[ i ] = (byte)mIn.read();
                    if (buf[i] == 0x0a)
                    {
                        b64NonceLen = i;
                        break;
                    }
                }
                if (b64NonceLen == 0)
                {
                    throw new AuthenticationFailedException("Error negotiating CRAM-MD5: nonce too long.");
                }
                byte[] b64NonceTrim = new byte[ b64NonceLen - 2 ];
                System.arraycopy(buf, 1, b64NonceTrim, 0, b64NonceLen - 2);
                byte[] nonce = Base64.decodeBase64(b64NonceTrim);
                if (K9.DEBUG)
                {
                    Log.d(K9.LOG_TAG, "Got nonce: " + new String(b64NonceTrim, "US-ASCII"));
                    Log.d(K9.LOG_TAG, "Plaintext nonce: " + new String(nonce, "US-ASCII"));
                }

                byte[] ipad = new byte[64];
                byte[] opad = new byte[64];
                byte[] secretBytes = mPassword.getBytes("US-ASCII");
                MessageDigest md = MessageDigest.getInstance("MD5");
                if (secretBytes.length > 64)
                {
                    secretBytes = md.digest(secretBytes);
                }
                System.arraycopy(secretBytes, 0, ipad, 0, secretBytes.length);
                System.arraycopy(secretBytes, 0, opad, 0, secretBytes.length);
                for (int i = 0; i < ipad.length; i++) ipad[i] ^= 0x36;
                for (int i = 0; i < opad.length; i++) opad[i] ^= 0x5c;
                md.update(ipad);
                byte[] firstPass = md.digest(nonce);
                md.update(opad);
                byte[] result = md.digest(firstPass);
                String plainCRAM = mUsername + " " + new String(Hex.encodeHex(result));
                byte[] b64CRAM = Base64.encodeBase64(plainCRAM.getBytes("US-ASCII"));
                if (K9.DEBUG)
                {
                    Log.d(K9.LOG_TAG, "Username == " + mUsername);
                    Log.d(K9.LOG_TAG, "plainCRAM: " + plainCRAM);
                    Log.d(K9.LOG_TAG, "b64CRAM: " + new String(b64CRAM, "US-ASCII"));
                }

                mOut.write(b64CRAM);
                mOut.write(new byte[] { 0x0d, 0x0a });
                mOut.flush();
                int respLen = 0;
                for (int i = 0; i < buf.length; i++)
                {
                    buf[ i ] = (byte)mIn.read();
                    if (buf[i] == 0x0a)
                    {
                        respLen = i;
                        break;
                    }
                }
                String toMatch = tag + " OK";
                String respStr = new String(buf, 0, respLen);
                if (!respStr.startsWith(toMatch))
                {
                    throw new AuthenticationFailedException("CRAM-MD5 error: " + respStr);
                }
            }
            catch (IOException ioe)
            {
                throw new AuthenticationFailedException("CRAM-MD5 Auth Failed.");
            }
            catch (NoSuchAlgorithmException nsae)
            {
                throw new AuthenticationFailedException("MD5 Not Available.");
            }
        }

        protected void setReadTimeout(int millis) throws SocketException
        {
            mSocket.setSoTimeout(millis);
        }

        protected boolean isIdleCapable()
        {
            if (K9.DEBUG)
                Log.v(K9.LOG_TAG, "Connection " + getLogId() + " has " + capabilities.size() + " capabilities");

            return capabilities.contains(CAPABILITY_IDLE);
        }

        protected boolean hasCapability(String capability)
        {
            return capabilities.contains(capability);
        }

        private boolean isOpen()
        {
            return (mIn != null && mOut != null && mSocket != null && mSocket.isConnected() && !mSocket.isClosed());
        }

        private void close()
        {
//            if (isOpen()) {
//                try {
//                    executeSimpleCommand("LOGOUT");
//                } catch (Exception e) {
//
//                }
//            }
            try
            {
                mIn.close();
            }
            catch (Exception e)
            {

            }
            try
            {
                mOut.close();
            }
            catch (Exception e)
            {

            }
            try
            {
                mSocket.close();
            }
            catch (Exception e)
            {

            }
            mIn = null;
            mOut = null;
            mSocket = null;
        }

        private ImapResponse readResponse() throws IOException, MessagingException
        {
            try
            {
                ImapResponse response = mParser.readResponse();
                if (K9.DEBUG)
                    Log.v(K9.LOG_TAG, getLogId() + "<<<" + response);

                return response;
            }
            catch (IOException ioe)
            {
                close();
                throw ioe;
            }
        }

        private String escapeString(String in)
        {
            if (in == null)
            {
                return null;
            }
            String out = in.replaceAll("\\\\", "\\\\\\\\");
            out = out.replaceAll("\"", "\\\\\"");
            return out;
        }

        private void sendContinuation(String continuation) throws IOException
        {
            mOut.write(continuation.getBytes());
            mOut.write('\r');
            mOut.write('\n');
            mOut.flush();

            if (K9.DEBUG)
                Log.v(K9.LOG_TAG, getLogId() + ">>> " + continuation);

        }

        public String sendCommand(String command, boolean sensitive)
        throws MessagingException, IOException
        {
            try
            {
                open();
                String tag = Integer.toString(mNextCommandTag++);
                String commandToSend = tag + " " + command;
                mOut.write(commandToSend.getBytes());
                mOut.write('\r');
                mOut.write('\n');
                mOut.flush();

                if (K9.DEBUG)
                {
                    if (sensitive && !K9.DEBUG_SENSITIVE)
                    {
                        Log.v(K9.LOG_TAG, getLogId() + ">>> "
                              + "[Command Hidden, Enable Sensitive Debug Logging To Show]");
                    }
                    else
                    {
                        Log.v(K9.LOG_TAG, getLogId() + ">>> " + commandToSend);
                    }
                }

                return tag;
            }
            catch (IOException ioe)
            {
                close();
                throw ioe;
            }
            catch (ImapException ie)
            {
                close();
                throw ie;
            }
            catch (MessagingException me)
            {
                close();
                throw me;
            }
        }

        public List<ImapResponse> executeSimpleCommand(String command) throws IOException,
                    ImapException, MessagingException
        {
            return executeSimpleCommand(command, false);
        }

        public List<ImapResponse> executeSimpleCommand(String command, boolean sensitive) throws IOException,
                    ImapException, MessagingException
        {
            return executeSimpleCommand(command, sensitive, null);
        }

        private List<ImapResponse> executeSimpleCommand(String command, boolean sensitive, UntaggedHandler untaggedHandler)
        throws IOException, ImapException, MessagingException
        {
            String commandToLog = command;
            if (sensitive && !K9.DEBUG_SENSITIVE)
            {
                commandToLog = "*sensitive*";
            }


            if (K9.DEBUG)
                Log.v(K9.LOG_TAG, "Sending IMAP command " + commandToLog + " on connection " + getLogId());

            String tag = sendCommand(command, sensitive);
            if (K9.DEBUG)
                Log.v(K9.LOG_TAG, "Sent IMAP command " + commandToLog + " with tag " + tag + " for " + getLogId());

            ArrayList<ImapResponse> responses = new ArrayList<ImapResponse>();
            ImapResponse response;
            do
            {
                response = mParser.readResponse();
                if (K9.DEBUG)
                    Log.v(K9.LOG_TAG, getLogId() + "<<<" + response);

                if (response.mTag != null && response.mTag.equalsIgnoreCase(tag) == false)
                {
                    Log.w(K9.LOG_TAG, "After sending tag " + tag + ", got tag response from previous command " + response + " for " + getLogId());
                    Iterator<ImapResponse> iter = responses.iterator();
                    while (iter.hasNext())
                    {
                        ImapResponse delResponse = iter.next();
                        if (delResponse.mTag != null || delResponse.size() < 2
                                || (ImapResponseParser.equalsIgnoreCase(delResponse.get(1), "EXISTS") == false && ImapResponseParser.equalsIgnoreCase(delResponse.get(1), "EXPUNGE") == false))
                        {
                            iter.remove();
                        }
                    }
                    response.mTag = null;
                    continue;
                }
                if (untaggedHandler != null)
                {
                    untaggedHandler.handleAsyncUntaggedResponse(response);
                }
                responses.add(response);
            }
            while (response.mTag == null);
            if (response.size() < 1 || !ImapResponseParser.equalsIgnoreCase(response.get(0), "OK"))
            {
                throw new ImapException("Command: " + commandToLog + "; response: " + response.toString(), response.getAlertText());
            }
            return responses;
        }
    }

    class ImapMessage extends MimeMessage
    {
        ImapMessage(String uid, Folder folder)
        {
            this.mUid = uid;
            this.mFolder = folder;
        }

        public void setSize(int size)
        {
            this.mSize = size;
        }

        @Override
        public void parse(InputStream in) throws IOException, MessagingException
        {
            super.parse(in);
        }

        public void setFlagInternal(Flag flag, boolean set) throws MessagingException
        {
            super.setFlag(flag, set);
        }


        @Override
        public void setFlag(Flag flag, boolean set) throws MessagingException
        {
            super.setFlag(flag, set);
            mFolder.setFlags(new Message[] { this }, new Flag[] { flag }, set);
        }

        @Override
        public void delete(String trashFolderName) throws MessagingException
        {
            getFolder().delete(new Message[] { this }, trashFolderName);
        }
    }

    class ImapBodyPart extends MimeBodyPart
    {
        public ImapBodyPart() throws MessagingException
        {
            super();
        }

        public void setSize(int size)
        {
            this.mSize = size;
        }
    }

    class ImapException extends MessagingException
    {
        String mAlertText;

        public ImapException(String message, String alertText, Throwable throwable)
        {
            super(message, throwable);
            this.mAlertText = alertText;
        }

        public ImapException(String message, String alertText)
        {
            super(message);
            this.mAlertText = alertText;
        }

        public String getAlertText()
        {
            return mAlertText;
        }

        public void setAlertText(String alertText)
        {
            mAlertText = alertText;
        }
    }

    public class ImapFolderPusher extends ImapFolder implements UntaggedHandler
    {
        final PushReceiver receiver;
        Thread listeningThread = null;
        final AtomicBoolean stop = new AtomicBoolean(false);
        final AtomicBoolean idling = new AtomicBoolean(false);
        final AtomicBoolean doneSent = new AtomicBoolean(false);
        final AtomicInteger delayTime = new AtomicInteger(NORMAL_DELAY_TIME);
        final AtomicInteger idleFailureCount = new AtomicInteger(0);
        List<ImapResponse> storedUntaggedResponses = new ArrayList<ImapResponse>();

        public ImapFolderPusher(ImapStore store, String name, PushReceiver nReceiver)
        {
            super(store, name);
            receiver = nReceiver;
        }
        public void refresh() throws IOException, MessagingException
        {
            if (idling.get())
            {
                receiver.acquireWakeLock();
                sendDone();
            }
        }

        private void sendDone() throws IOException, MessagingException
        {
            if (doneSent.compareAndSet(false, true) == true)
            {
                mConnection.setReadTimeout(Store.SOCKET_READ_TIMEOUT);
                sendContinuation("DONE");
            }
        }

        private void sendContinuation(String continuation)
        throws MessagingException, IOException
        {
            if (mConnection != null)
            {
                mConnection.sendContinuation(continuation);
            }
        }

        public void start()
        {
            Runnable runner = new Runnable()
            {
                public void run()
                {
                    receiver.acquireWakeLock();
                    if (K9.DEBUG)
                        Log.i(K9.LOG_TAG, "Pusher starting for " + getLogId());

                    while (stop.get() != true)
                    {
                        try
                        {
                            int oldUidNext = -1;
                            try
                            {
                                String pushStateS = receiver.getPushState(getName());
                                ImapPushState pushState = ImapPushState.parse(pushStateS);
                                oldUidNext = pushState.uidNext;
                                if (K9.DEBUG)
                                    Log.i(K9.LOG_TAG, "Got oldUidNext " + oldUidNext + " for " + getLogId());
                            }
                            catch (Exception e)
                            {
                                Log.e(K9.LOG_TAG, "Unable to get oldUidNext for " + getLogId(), e);
                            }
                            ImapConnection oldConnection = mConnection;
                            List<ImapResponse> responses = internalOpen(OpenMode.READ_ONLY);
                            if (mConnection == null)
                            {
                                receiver.pushError("Could not establish connection for IDLE", null);
                                throw new MessagingException("Could not establish connection for IDLE");

                            }
                            if (mConnection.isIdleCapable() == false)
                            {
                                stop.set(true);
                                receiver.pushError("IMAP server is not IDLE capable: " + mConnection.toString(), null);
                                throw new MessagingException("IMAP server is not IDLE capable:" + mConnection.toString());
                            }

                            if (responses != null)
                            {
                                handleUntaggedResponses(responses);
                            }
                            if (mAccount.isPushPollOnConnect() && mConnection != oldConnection)
                            {
                                receiver.syncFolder(ImapFolderPusher.this);
                            }
                            int startUid = oldUidNext;

                            int newUidNext = uidNext;

                            if (newUidNext == -1)
                            {
                                if (K9.DEBUG)
                                {
                                    Log.d(K9.LOG_TAG, "uidNext is -1, using search to find highest UID");
                                }
                                int highestUid = getHighestUid();
                                if (highestUid != -1)
                                {
                                    if (K9.DEBUG)
                                        Log.d(K9.LOG_TAG, "highest UID = " + highestUid);
                                    newUidNext = highestUid + 1;
                                    if (K9.DEBUG)
                                        Log.d(K9.LOG_TAG, "highest UID = " + highestUid
                                              + ", set newUidNext to " + newUidNext);
                                }
                            }

                            if (startUid < newUidNext - mAccount.getDisplayCount())
                            {
                                startUid = newUidNext - mAccount.getDisplayCount();
                            }
                            if (startUid < 1)
                            {
                                startUid = 1;
                            }
                            if (newUidNext > startUid)
                            {

                                if (K9.DEBUG)
                                    Log.i(K9.LOG_TAG, "Needs sync from uid " + startUid  + " to " + newUidNext + " for " + getLogId());
                                List<Message> messages = new ArrayList<Message>();
                                for (int uid = startUid; uid < newUidNext; uid++)
                                {
                                    ImapMessage message = new ImapMessage("" + uid, ImapFolderPusher.this);
                                    messages.add(message);
                                }
                                if (messages.size() > 0)
                                {
                                    pushMessages(messages, true);
                                }

                            }
                            else
                            {
                                if (stop.get() == false)
                                {
                                    List<ImapResponse> untaggedResponses = null;
                                    if (storedUntaggedResponses.size() > 0)
                                    {
                                        if (K9.DEBUG)
                                            Log.i(K9.LOG_TAG, "Processing " + storedUntaggedResponses.size() + " from previous commands for " + getLogId());
                                        untaggedResponses = new ArrayList<ImapResponse>(storedUntaggedResponses);
                                    }
                                    else
                                    {
                                        if (K9.DEBUG)
                                            Log.i(K9.LOG_TAG, "About to IDLE for " + getLogId());

                                        receiver.setPushActive(getName(), true);
                                        idling.set(true);
                                        doneSent.set(false);
                                        mConnection.setReadTimeout((getAccount().getIdleRefreshMinutes() * 60 * 1000) + IDLE_READ_TIMEOUT_INCREMENT);
                                        untaggedResponses = executeSimpleCommand(COMMAND_IDLE, false, ImapFolderPusher.this);
                                        idling.set(false);

                                    }
                                    if (stop.get() == false)
                                    {
                                        storedUntaggedResponses.clear();
                                        processUntaggedResponses(untaggedResponses);
                                    }
                                    delayTime.set(NORMAL_DELAY_TIME);
                                    idleFailureCount.set(0);
                                }
                            }
                        }
                        catch (Exception e)
                        {
                            receiver.acquireWakeLock();
                            storedUntaggedResponses.clear();
                            idling.set(false);
                            receiver.setPushActive(getName(), false);
                            try
                            {
                                close();
                            }
                            catch (Exception me)
                            {
                                Log.e(K9.LOG_TAG, "Got exception while closing for exception for " + getLogId(), me);
                            }
                            if (stop.get() == true)
                            {
                                Log.i(K9.LOG_TAG, "Got exception while idling, but stop is set for " + getLogId());
                            }
                            else
                            {
                                receiver.pushError("Push error for " + getName(), e);
                                Log.e(K9.LOG_TAG, "Got exception while idling for " + getLogId(), e);
                                int delayTimeInt = delayTime.get();
                                receiver.sleep(delayTimeInt);
                                delayTimeInt *= 2;
                                if (delayTimeInt > MAX_DELAY_TIME)
                                {
                                    delayTimeInt = MAX_DELAY_TIME;
                                }
                                delayTime.set(delayTimeInt);
                                if (idleFailureCount.incrementAndGet() > IDLE_FAILURE_COUNT_LIMIT)
                                {
                                    Log.e(K9.LOG_TAG, "Disabling pusher for " + getLogId() + " after " + idleFailureCount.get() + " consecutive errors");
                                    receiver.pushError("Push disabled for " + getName() + " after " + idleFailureCount.get() + " consecutive errors", e);
                                    stop.set(true);
                                }

                            }
                        }
                    }
                    receiver.setPushActive(getName(), false);
                    try
                    {
                        if (K9.DEBUG)
                            Log.i(K9.LOG_TAG, "Pusher for " + getLogId() + " is exiting");
                        close();
                    }
                    catch (Exception me)
                    {
                        Log.e(K9.LOG_TAG, "Got exception while closing for " + getLogId(), me);
                    }
                    finally
                    {
                        receiver.releaseWakeLock();
                    }
                }
            };
            listeningThread = new Thread(runner);
            listeningThread.start();
        }

        @Override
        protected void handleUntaggedResponse(ImapResponse response)
        {
            if (response.mTag == null && response.size() > 1)
            {
                Object responseType = response.get(1);
                if (ImapResponseParser.equalsIgnoreCase(responseType, "FETCH")
                        || ImapResponseParser.equalsIgnoreCase(responseType, "EXPUNGE")
                        || ImapResponseParser.equalsIgnoreCase(responseType, "EXISTS"))
                {
                    if (K9.DEBUG)
                        Log.d(K9.LOG_TAG, "Storing response " + response + " for later processing");

                    storedUntaggedResponses.add(response);
                }
                handlePossibleUidNext(response);
            }
        }

        protected void processUntaggedResponses(List<ImapResponse> responses) throws MessagingException
        {
            boolean skipSync = false;
            int oldMessageCount = mMessageCount;
            if (oldMessageCount == -1)
            {
                skipSync = true;
            }
            List<Integer> flagSyncMsgSeqs = new ArrayList<Integer>();

            for (ImapResponse response : responses)
            {
                oldMessageCount += processUntaggedResponse(oldMessageCount, response, flagSyncMsgSeqs);
            }
            if (skipSync == false)
            {
                if (oldMessageCount < 0)
                {
                    oldMessageCount = 0;
                }
                if (mMessageCount > oldMessageCount)
                {
                    syncMessages(mMessageCount, true);
                }
            }
            if (K9.DEBUG)
                Log.d(K9.LOG_TAG, "There are " + flagSyncMsgSeqs + " messages needing flag sync for " + getLogId());

            if (flagSyncMsgSeqs.size() > 0)
            {
                syncMessages(flagSyncMsgSeqs);
            }
        }

        private void syncMessages(int end, boolean newArrivals) throws MessagingException
        {
            int oldUidNext = -1;
            try
            {
                String pushStateS = receiver.getPushState(getName());
                ImapPushState pushState = ImapPushState.parse(pushStateS);
                oldUidNext = pushState.uidNext;
                if (K9.DEBUG)
                    Log.i(K9.LOG_TAG, "Got oldUidNext " + oldUidNext + " for " + getLogId());
            }
            catch (Exception e)
            {
                Log.e(K9.LOG_TAG, "Unable to get oldUidNext for " + getLogId(), e);
            }

            Message[] messageArray = getMessages(end, end, true, null);
            if (messageArray != null && messageArray.length > 0)
            {
                int newUid = Integer.parseInt(messageArray[0].getUid());
                if (K9.DEBUG)
                    Log.i(K9.LOG_TAG, "Got newUid " + newUid + " for message " + end + " on " + getLogId());
                int startUid = oldUidNext;
                if (startUid < newUid - 10)
                {
                    startUid = newUid - 10;
                }
                if (startUid < 1)
                {
                    startUid = 1;
                }
                if (newUid >= startUid)
                {

                    if (K9.DEBUG)
                        Log.i(K9.LOG_TAG, "Needs sync from uid " + startUid  + " to " + newUid + " for " + getLogId());
                    List<Message> messages = new ArrayList<Message>();
                    for (int uid = startUid; uid <= newUid; uid++)
                    {
                        ImapMessage message = new ImapMessage("" + uid, ImapFolderPusher.this);
                        messages.add(message);
                    }
                    if (messages.size() > 0)
                    {
                        pushMessages(messages, true);
                    }
                }
            }
        }

        private void syncMessages(List<Integer> flagSyncMsgSeqs)
        {
            try
            {
                Message[] messageArray = null;

                messageArray = getMessages(flagSyncMsgSeqs, true, null);

                List<Message> messages = new ArrayList<Message>();
                for (Message message : messageArray)
                {
                    messages.add(message);
                }
                pushMessages(messages, false);

            }
            catch (Exception e)
            {
                receiver.pushError("Exception while processing Push untagged responses", e);
            }
        }

        protected int processUntaggedResponse(int oldMessageCount, ImapResponse response, List<Integer> flagSyncMsgSeqs)
        {
            super.handleUntaggedResponse(response);
            int messageCountDelta = 0;
            if (response.mTag == null && response.size() > 1)
            {
                try
                {
                    Object responseType = response.get(1);
                    if (ImapResponseParser.equalsIgnoreCase(responseType, "FETCH"))
                    {
                        int msgSeq = response.getNumber(0);
                        if (K9.DEBUG)
                            Log.d(K9.LOG_TAG, "Got untagged FETCH for msgseq " + msgSeq + " for " + getLogId());

                        if (flagSyncMsgSeqs.contains(msgSeq) == false)
                        {
                            flagSyncMsgSeqs.add(msgSeq);
                        }
                    }
                    if (ImapResponseParser.equalsIgnoreCase(responseType, "EXPUNGE"))
                    {
                        int msgSeq = response.getNumber(0);
                        if (msgSeq <= oldMessageCount)
                        {
                            messageCountDelta = -1;
                        }
                        if (K9.DEBUG)
                            Log.d(K9.LOG_TAG, "Got untagged EXPUNGE for msgseq " + msgSeq + " for " + getLogId());

                        List<Integer> newSeqs = new ArrayList<Integer>();
                        Iterator<Integer> flagIter = flagSyncMsgSeqs.iterator();
                        while (flagIter.hasNext())
                        {
                            Integer flagMsg = flagIter.next();
                            if (flagMsg >= msgSeq)
                            {
                                flagIter.remove();
                                if (flagMsg > msgSeq)
                                {
                                    newSeqs.add(flagMsg--);
                                }
                            }
                        }
                        flagSyncMsgSeqs.addAll(newSeqs);
                    }
                }
                catch (Exception e)
                {
                    Log.e(K9.LOG_TAG, "Could not handle untagged FETCH for " + getLogId(), e);
                }
            }
            return messageCountDelta;
        }


        private void pushMessages(List<Message> messages, boolean newArrivals)
        {
            RuntimeException holdException = null;
            try
            {
                if (newArrivals)
                {
                    receiver.messagesArrived(this, messages);
                }
                else
                {
                    receiver.messagesFlagsChanged(this, messages);
                }
            }
            catch (RuntimeException e)
            {
                holdException = e;
            }

            if (holdException != null)
            {
                throw holdException;
            }
        }

        public void stop()
        {
            stop.set(true);
            if (listeningThread != null)
            {
                listeningThread.interrupt();
            }
            if (mConnection != null)
            {
                if (K9.DEBUG)
                    Log.v(K9.LOG_TAG, "Closing mConnection to stop pushing for " + getLogId());
                mConnection.close();
            }
            else
            {
                Log.w(K9.LOG_TAG, "Attempt to interrupt null mConnection to stop pushing on folderPusher for " + getLogId());
            }
        }

        public void handleAsyncUntaggedResponse(ImapResponse response)
        {
            if (K9.DEBUG)
                Log.v(K9.LOG_TAG, "Got async response: " + response);

            if (stop.get() == true)
            {
                if (K9.DEBUG)
                    Log.d(K9.LOG_TAG, "Got async untagged response: " + response + ", but stop is set for " + getLogId());

                try
                {
                    sendDone();
                }
                catch (Exception e)
                {
                    Log.e(K9.LOG_TAG, "Exception while sending DONE for " + getLogId(), e);
                }
            }
            else
            {
                if (response.mTag == null)
                {
                    if (response.size() > 1)
                    {
                        boolean started = false;
                        Object responseType = response.get(1);
                        if (ImapResponseParser.equalsIgnoreCase(responseType, "EXISTS") || ImapResponseParser.equalsIgnoreCase(responseType, "EXPUNGE") ||
                                ImapResponseParser.equalsIgnoreCase(responseType,"FETCH"))
                        {
                            if (started == false)
                            {
                                receiver.acquireWakeLock();
                                started = true;
                            }

                            if (K9.DEBUG)
                                Log.d(K9.LOG_TAG, "Got useful async untagged response: " + response + " for " + getLogId());

                            try
                            {
                                sendDone();
                            }
                            catch (Exception e)
                            {
                                Log.e(K9.LOG_TAG, "Exception while sending DONE for " + getLogId(), e);
                            }
                        }
                    }
                    else if (response.mCommandContinuationRequested)
                    {
                        if (K9.DEBUG)
                            Log.d(K9.LOG_TAG, "Idling " + getLogId());

                        receiver.releaseWakeLock();
                    }
                }
            }
        }
    }
    @Override
    public Pusher getPusher(PushReceiver receiver)
    {
        return new ImapPusher(this, receiver);
    }

    public class ImapPusher implements Pusher
    {
        final ImapStore mStore;
        final PushReceiver mReceiver;
        private long lastRefresh = -1;

        HashMap<String, ImapFolderPusher> folderPushers = new HashMap<String, ImapFolderPusher>();

        public ImapPusher(ImapStore store, PushReceiver receiver)
        {
            mStore = store;
            mReceiver = receiver;
        }

        public void start(List<String> folderNames)
        {
            stop();
            synchronized (folderPushers)
            {
                setLastRefresh(System.currentTimeMillis());
                for (String folderName : folderNames)
                {
                    ImapFolderPusher pusher = folderPushers.get(folderName);
                    if (pusher == null)
                    {
                        pusher = new ImapFolderPusher(mStore, folderName, mReceiver);
                        folderPushers.put(folderName, pusher);
                        pusher.start();
                    }
                }
            }
        }

        public void refresh()
        {
            synchronized (folderPushers)
            {
                for (ImapFolderPusher folderPusher : folderPushers.values())
                {
                    try
                    {
                        folderPusher.refresh();
                    }
                    catch (Exception e)
                    {
                        Log.e(K9.LOG_TAG, "Got exception while refreshing for " + folderPusher.getName(), e);
                    }
                }
            }
        }

        public void stop()
        {
            if (K9.DEBUG)
                Log.i(K9.LOG_TAG, "Requested stop of IMAP pusher");

            synchronized (folderPushers)
            {
                for (ImapFolderPusher folderPusher : folderPushers.values())
                {
                    try
                    {
                        if (K9.DEBUG)
                            Log.i(K9.LOG_TAG, "Requesting stop of IMAP folderPusher " + folderPusher.getName());
                        folderPusher.stop();
                    }
                    catch (Exception e)
                    {
                        Log.e(K9.LOG_TAG, "Got exception while stopping " + folderPusher.getName(), e);
                    }
                }
                folderPushers.clear();
            }
        }

        public int getRefreshInterval()
        {
            return (getAccount().getIdleRefreshMinutes() * 60 * 1000);
        }

        public long getLastRefresh()
        {
            return lastRefresh;
        }

        public void setLastRefresh(long lastRefresh)
        {
            this.lastRefresh = lastRefresh;
        }

    }
    private interface UntaggedHandler
    {
        void handleAsyncUntaggedResponse(ImapResponse respose);
    }

    protected static class ImapPushState
    {
        protected int uidNext;
        protected ImapPushState(int nUidNext)
        {
            uidNext = nUidNext;
        }
        protected static ImapPushState parse(String pushState)
        {
            int newUidNext = -1;
            if (pushState != null)
            {
                StringTokenizer tokenizer = new StringTokenizer(pushState, ";");
                while (tokenizer.hasMoreTokens())
                {
                    StringTokenizer thisState = new StringTokenizer(tokenizer.nextToken(), "=");
                    if (thisState.hasMoreTokens())
                    {
                        String key = thisState.nextToken();

                        if ("uidNext".equalsIgnoreCase(key) && thisState.hasMoreTokens())
                        {
                            String value = thisState.nextToken();
                            try
                            {
                                newUidNext = Integer.parseInt(value);
                            }
                            catch (Exception e)
                            {
                                Log.e(K9.LOG_TAG, "Unable to part uidNext value " + value, e);
                            }

                        }
                    }
                }
            }
            return new ImapPushState(newUidNext);
        }
        @Override
        public String toString()
        {
            return "uidNext=" + uidNext;
        }

    }
    private interface ImapSearcher
    {
        List<ImapResponse> search() throws IOException, MessagingException;
    }
}
