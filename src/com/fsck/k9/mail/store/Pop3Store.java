
package com.fsck.k9.mail.store;

import android.util.Config;
import android.util.Log;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.Utility;
import com.fsck.k9.mail.*;
import com.fsck.k9.mail.Folder.OpenMode;
import com.fsck.k9.mail.internet.MimeMessage;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManager;
import java.io.*;
import java.net.*;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class Pop3Store extends Store
{
    public static final int CONNECTION_SECURITY_NONE = 0;
    public static final int CONNECTION_SECURITY_TLS_OPTIONAL = 1;
    public static final int CONNECTION_SECURITY_TLS_REQUIRED = 2;
    public static final int CONNECTION_SECURITY_SSL_REQUIRED = 3;
    public static final int CONNECTION_SECURITY_SSL_OPTIONAL = 4;

    private static final Flag[] PERMANENT_FLAGS = { Flag.DELETED };

    private String mHost;
    private int mPort;
    private String mUsername;
    private String mPassword;
    private int mConnectionSecurity;
    private HashMap<String, Folder> mFolders = new HashMap<String, Folder>();
    private Pop3Capabilities mCapabilities;

//    /**
//     * Detected latency, used for usage scaling.
//     * Usage scaling occurs when it is neccesary to get information about
//     * messages that could result in large data loads. This value allows
//     * the code that loads this data to decide between using large downloads
//     * (high latency) or multiple round trips (low latency) to accomplish
//     * the same thing.
//     * Default is Integer.MAX_VALUE implying massive latency so that the large
//     * download method is used by default until latency data is collected.
//     */
//    private int mLatencyMs = Integer.MAX_VALUE;
//
//    /**
//     * Detected throughput, used for usage scaling.
//     * Usage scaling occurs when it is neccesary to get information about
//     * messages that could result in large data loads. This value allows
//     * the code that loads this data to decide between using large downloads
//     * (high latency) or multiple round trips (low latency) to accomplish
//     * the same thing.
//     * Default is Integer.MAX_VALUE implying massive bandwidth so that the
//     * large download method is used by default until latency data is
//     * collected.
//     */
//    private int mThroughputKbS = Integer.MAX_VALUE;

    /**
     * pop3://user:password@server:port CONNECTION_SECURITY_NONE
     * pop3+tls://user:password@server:port CONNECTION_SECURITY_TLS_OPTIONAL
     * pop3+tls+://user:password@server:port CONNECTION_SECURITY_TLS_REQUIRED
     * pop3+ssl+://user:password@server:port CONNECTION_SECURITY_SSL_REQUIRED
     * pop3+ssl://user:password@server:port CONNECTION_SECURITY_SSL_OPTIONAL
     */
    public Pop3Store(Account account) throws MessagingException
    {
        super(account);

        URI uri;
        try
        {
            uri = new URI(mAccount.getStoreUri());
        }
        catch (URISyntaxException use)
        {
            throw new MessagingException("Invalid Pop3Store URI", use);
        }

        String scheme = uri.getScheme();
        if (scheme.equals("pop3"))
        {
            mConnectionSecurity = CONNECTION_SECURITY_NONE;
            mPort = 110;
        }
        else if (scheme.equals("pop3+tls"))
        {
            mConnectionSecurity = CONNECTION_SECURITY_TLS_OPTIONAL;
            mPort = 110;
        }
        else if (scheme.equals("pop3+tls+"))
        {
            mConnectionSecurity = CONNECTION_SECURITY_TLS_REQUIRED;
            mPort = 110;
        }
        else if (scheme.equals("pop3+ssl+"))
        {
            mConnectionSecurity = CONNECTION_SECURITY_SSL_REQUIRED;
            mPort = 995;
        }
        else if (scheme.equals("pop3+ssl"))
        {
            mConnectionSecurity = CONNECTION_SECURITY_SSL_OPTIONAL;
            mPort = 995;
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
                mUsername = URLDecoder.decode(userInfoParts[0], "UTF-8");
                if (userInfoParts.length > 1)
                {
                    mPassword = URLDecoder.decode(userInfoParts[1], "UTF-8");
                }
            }
            catch (UnsupportedEncodingException enc)
            {
                // This shouldn't happen since the encoding is hardcoded to UTF-8
                Log.e(K9.LOG_TAG, "Couldn't urldecode username or password.", enc);
            }
        }
    }

    @Override
    public Folder getFolder(String name) throws MessagingException
    {
        Folder folder = mFolders.get(name);
        if (folder == null)
        {
            folder = new Pop3Folder(name);
            mFolders.put(folder.getName(), folder);
        }
        return folder;
    }

    @Override
    public List<? extends Folder> getPersonalNamespaces() throws MessagingException
    {
        List<Folder> folders = new LinkedList<Folder>();
        folders.add(getFolder("INBOX"));
        return folders;
    }

    @Override
    public void checkSettings() throws MessagingException
    {
        Pop3Folder folder = new Pop3Folder("INBOX");
        folder.open(OpenMode.READ_WRITE);
        if (!mCapabilities.uidl)
        {
            /*
             * Run an additional test to see if UIDL is supported on the server. If it's not we
             * can't service this account.
             */

            /*
             * If the server doesn't support UIDL it will return a - response, which causes
             * executeSimpleCommand to throw a MessagingException, exiting this method.
             */
            folder.executeSimpleCommand("UIDL");

        }
        folder.close();
    }

    class Pop3Folder extends Folder
    {
        private Socket mSocket;
        private InputStream mIn;
        private OutputStream mOut;
        private HashMap<String, Pop3Message> mUidToMsgMap = new HashMap<String, Pop3Message>();
        private HashMap<Integer, Pop3Message> mMsgNumToMsgMap = new HashMap<Integer, Pop3Message>();
        private HashMap<String, Integer> mUidToMsgNumMap = new HashMap<String, Integer>();
        private String mName;
        private int mMessageCount;

        public Pop3Folder(String name)
        {
            super(Pop3Store.this.mAccount);
            this.mName = name;
            if (mName.equalsIgnoreCase("INBOX"))
            {
                mName = "INBOX";
            }
        }

        @Override
        public synchronized void open(OpenMode mode) throws MessagingException
        {
            if (isOpen())
            {
                return;
            }

            if (!mName.equalsIgnoreCase("INBOX"))
            {
                throw new MessagingException("Folder does not exist");
            }

            try
            {
                SocketAddress socketAddress = new InetSocketAddress(mHost, mPort);
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
                    mIn = new BufferedInputStream(mSocket.getInputStream(), 1024);
                    mOut = new BufferedOutputStream(mSocket.getOutputStream(), 512);
                }
                else
                {
                    mSocket = new Socket();
                    mSocket.connect(socketAddress, SOCKET_CONNECT_TIMEOUT);
                    mIn = new BufferedInputStream(mSocket.getInputStream(), 1024);
                    mOut = new BufferedOutputStream(mSocket.getOutputStream(), 512);
                }

                mSocket.setSoTimeout(Store.SOCKET_READ_TIMEOUT);
                if (!isOpen())
                {
                    throw new MessagingException("Unable to connect socket");
                }

                // Eat the banner
                executeSimpleCommand(null);

                mCapabilities = getCapabilities();

                if (mConnectionSecurity == CONNECTION_SECURITY_TLS_OPTIONAL
                        || mConnectionSecurity == CONNECTION_SECURITY_TLS_REQUIRED)
                {
                    if (mCapabilities.stls)
                    {
                        writeLine("STLS");

                        SSLContext sslContext = SSLContext.getInstance("TLS");
                        boolean secure = mConnectionSecurity == CONNECTION_SECURITY_TLS_REQUIRED;
                        sslContext.init(null, new TrustManager[]
                                        {
                                            TrustManagerFactory.get(mHost, secure)
                                        }, new SecureRandom());
                        mSocket = sslContext.getSocketFactory().createSocket(mSocket, mHost, mPort,
                                  true);
                        mSocket.setSoTimeout(Store.SOCKET_READ_TIMEOUT);
                        mIn = new BufferedInputStream(mSocket.getInputStream(), 1024);
                        mOut = new BufferedOutputStream(mSocket.getOutputStream(), 512);
                        if (!isOpen())
                        {
                            throw new MessagingException("Unable to connect socket");
                        }
                    }
                    else if (mConnectionSecurity == CONNECTION_SECURITY_TLS_REQUIRED)
                    {
                        throw new MessagingException("TLS not supported but required");
                    }
                }

                try
                {
                    executeSimpleCommand("USER " + mUsername);
                    executeSimpleCommand("PASS " + mPassword);
                }
                catch (MessagingException me)
                {
                    throw new AuthenticationFailedException(null, me);
                }
            }
            catch (SSLException e)
            {
                throw new CertificateValidationException(e.getMessage(), e);
            }
            catch (GeneralSecurityException gse)
            {
                throw new MessagingException(
                    "Unable to open connection to POP server due to security error.", gse);
            }
            catch (IOException ioe)
            {
                throw new MessagingException("Unable to open connection to POP server.", ioe);
            }

            String response = executeSimpleCommand("STAT");
            String[] parts = response.split(" ");
            mMessageCount = Integer.parseInt(parts[1]);

            mUidToMsgMap.clear();
            mMsgNumToMsgMap.clear();
            mUidToMsgNumMap.clear();
        }

        @Override
        public boolean isOpen()
        {
            return (mIn != null && mOut != null && mSocket != null
                    && mSocket.isConnected() && !mSocket.isClosed());
        }

        @Override
        public OpenMode getMode() throws MessagingException
        {
            return OpenMode.READ_WRITE;
        }

        @Override
        public void close()
        {
            try
            {
                executeSimpleCommand("QUIT");
            }
            catch (Exception e)
            {
                /*
                 * QUIT may fail if the connection is already closed. We don't care. It's just
                 * being friendly.
                 */
            }

            closeIO();
        }

        private void closeIO()
        {
            try
            {
                mIn.close();
            }
            catch (Exception e)
            {
                /*
                 * May fail if the connection is already closed.
                 */
            }
            try
            {
                mOut.close();
            }
            catch (Exception e)
            {
                /*
                 * May fail if the connection is already closed.
                 */
            }
            try
            {
                mSocket.close();
            }
            catch (Exception e)
            {
                /*
                 * May fail if the connection is already closed.
                 */
            }
            mIn = null;
            mOut = null;
            mSocket = null;
        }

        @Override
        public String getName()
        {
            return mName;
        }

        @Override
        public boolean create(FolderType type) throws MessagingException
        {
            return false;
        }

        @Override
        public boolean exists() throws MessagingException
        {
            return mName.equalsIgnoreCase("INBOX");
        }

        @Override
        public int getMessageCount()
        {
            return mMessageCount;
        }

        @Override
        public int getUnreadMessageCount() throws MessagingException
        {
            return -1;
        }
        @Override
        public int getFlaggedMessageCount() throws MessagingException
        {
            return -1;
        }

        @Override
        public Message getMessage(String uid) throws MessagingException
        {
            Pop3Message message = mUidToMsgMap.get(uid);
            if (message == null)
            {
                message = new Pop3Message(uid, this);
            }
            return message;
        }

        @Override
        public Message[] getMessages(int start, int end, MessageRetrievalListener listener)
        throws MessagingException
        {
            if (start < 1 || end < 1 || end < start)
            {
                throw new MessagingException(String.format("Invalid message set %d %d",
                                             start, end));
            }
            try
            {
                indexMsgNums(start, end);
            }
            catch (IOException ioe)
            {
                throw new MessagingException("getMessages", ioe);
            }
            ArrayList<Message> messages = new ArrayList<Message>();
            int i = 0;
            for (int msgNum = start; msgNum <= end; msgNum++)
            {
                Pop3Message message = mMsgNumToMsgMap.get(msgNum);
                if (listener != null)
                {
                    listener.messageStarted(message.getUid(), i++, (end - start) + 1);
                }
                messages.add(message);
                if (listener != null)
                {
                    listener.messageFinished(message, i++, (end - start) + 1);
                }
            }
            return messages.toArray(new Message[messages.size()]);
        }

        /**
         * Ensures that the given message set (from start to end inclusive)
         * has been queried so that uids are available in the local cache.
         * @param start
         * @param end
         * @throws MessagingException
         * @throws IOException
         */
        private void indexMsgNums(int start, int end)
        throws MessagingException, IOException
        {
            int unindexedMessageCount = 0;
            for (int msgNum = start; msgNum <= end; msgNum++)
            {
                if (mMsgNumToMsgMap.get(msgNum) == null)
                {
                    unindexedMessageCount++;
                }
            }
            if (unindexedMessageCount == 0)
            {
                return;
            }
            if (unindexedMessageCount < 50 && mMessageCount > 5000)
            {
                /*
                 * In extreme cases we'll do a UIDL command per message instead of a bulk
                 * download.
                 */
                for (int msgNum = start; msgNum <= end; msgNum++)
                {
                    Pop3Message message = mMsgNumToMsgMap.get(msgNum);
                    if (message == null)
                    {
                        String response = executeSimpleCommand("UIDL " + msgNum);
                        int uidIndex = response.lastIndexOf(' ');
                        String msgUid = response.substring(uidIndex + 1);
                        message = new Pop3Message(msgUid, this);
                        indexMessage(msgNum, message);
                    }
                }
            }
            else
            {
                String response = executeSimpleCommand("UIDL");
                while ((response = readLine()) != null)
                {
                    if (response.equals("."))
                    {
                        break;
                    }
                    String[] uidParts = response.split(" ");
                    if ((uidParts.length >= 3) && "+OK".equals(uidParts[0]))
                    {
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
                    if (uidParts.length >= 2)
                    {
                        Integer msgNum = Integer.valueOf(uidParts[0]);
                        String msgUid = uidParts[1];
                        if (msgNum >= start && msgNum <= end)
                        {
                            Pop3Message message = mMsgNumToMsgMap.get(msgNum);
                            if (message == null)
                            {
                                message = new Pop3Message(msgUid, this);
                                indexMessage(msgNum, message);
                            }
                        }
                    }
                }
            }
        }

        private void indexUids(ArrayList<String> uids)
        throws MessagingException, IOException
        {
            HashSet<String> unindexedUids = new HashSet<String>();
            for (String uid : uids)
            {
                if (mUidToMsgMap.get(uid) == null)
                {
                    if (Config.LOGD)
                    {
                        Log.d(K9.LOG_TAG, "Need to index UID " + uid);
                    }
                    unindexedUids.add(uid);
                }
            }
            if (unindexedUids.size() == 0)
            {
                return;
            }
            /*
             * If we are missing uids in the cache the only sure way to
             * get them is to do a full UIDL list. A possible optimization
             * would be trying UIDL for the latest X messages and praying.
             */
            String response = executeSimpleCommand("UIDL");
            while ((response = readLine()) != null)
            {
                if (response.equals("."))
                {
                    break;
                }
                String[] uidParts = response.split(" ");
                Integer msgNum = Integer.valueOf(uidParts[0]);
                String msgUid = uidParts[1];
                if (unindexedUids.contains(msgUid))
                {
                    if (Config.LOGD)
                    {
                        Log.d(K9.LOG_TAG, "Got msgNum " + msgNum + " for UID " + msgUid);
                    }

                    Pop3Message message = mUidToMsgMap.get(msgUid);
                    if (message == null)
                    {
                        message = new Pop3Message(msgUid, this);
                    }
                    indexMessage(msgNum, message);
                }
            }
        }

        private void indexMessage(int msgNum, Pop3Message message)
        {
            if (Config.LOGD)
            {
                Log.d(K9.LOG_TAG, "Adding index for UID " + message.getUid() + " to msgNum " + msgNum);
            }
            mMsgNumToMsgMap.put(msgNum, message);
            mUidToMsgMap.put(message.getUid(), message);
            mUidToMsgNumMap.put(message.getUid(), msgNum);
        }

        @Override
        public Message[] getMessages(MessageRetrievalListener listener) throws MessagingException
        {
            throw new UnsupportedOperationException("Pop3: No getMessages");
        }

        @Override
        public Message[] getMessages(String[] uids, MessageRetrievalListener listener)
        throws MessagingException
        {
            throw new UnsupportedOperationException("Pop3: No getMessages by uids");
        }

        /**
         * Fetch the items contained in the FetchProfile into the given set of
         * Messages in as efficient a manner as possible.
         * @param messages
         * @param fp
         * @throws MessagingException
         */
        @Override
        public void fetch(Message[] messages, FetchProfile fp, MessageRetrievalListener listener)
        throws MessagingException
        {
            if (messages == null || messages.length == 0)
            {
                return;
            }
            ArrayList<String> uids = new ArrayList<String>();
            for (Message message : messages)
            {
                uids.add(message.getUid());
            }
            try
            {
                indexUids(uids);
            }
            catch (IOException ioe)
            {
                throw new MessagingException("fetch", ioe);
            }
            try
            {
                if (fp.contains(FetchProfile.Item.ENVELOPE))
                {
                    /*
                     * We pass the listener only if there are other things to do in the
                     * FetchProfile. Since fetchEnvelop works in bulk and eveything else
                     * works one at a time if we let fetchEnvelope send events the
                     * event would get sent twice.
                     */
                    fetchEnvelope(messages, fp.size() == 1 ? listener : null);
                }
            }
            catch (IOException ioe)
            {
                throw new MessagingException("fetch", ioe);
            }
            for (int i = 0, count = messages.length; i < count; i++)
            {
                Message message = messages[i];
                if (!(message instanceof Pop3Message))
                {
                    throw new MessagingException("Pop3Store.fetch called with non-Pop3 Message");
                }
                Pop3Message pop3Message = (Pop3Message)message;
                try
                {
                    if (listener != null && !fp.contains(FetchProfile.Item.ENVELOPE))
                    {
                        listener.messageStarted(pop3Message.getUid(), i, count);
                    }
                    if (fp.contains(FetchProfile.Item.BODY))
                    {
                        fetchBody(pop3Message, -1);
                    }
                    else if (fp.contains(FetchProfile.Item.BODY_SANE))
                    {
                        /*
                         * To convert the suggested download size we take the size
                         * divided by the maximum line size (76).
                         */
                        fetchBody(pop3Message,
                                  FETCH_BODY_SANE_SUGGESTED_SIZE / 76);
                    }
                    else if (fp.contains(FetchProfile.Item.STRUCTURE))
                    {
                        /*
                         * If the user is requesting STRUCTURE we are required to set the body
                         * to null since we do not support the function.
                         */
                        pop3Message.setBody(null);
                    }
                    if (listener != null && !(fp.contains(FetchProfile.Item.ENVELOPE) && fp.size() == 1))
                    {
                        listener.messageFinished(message, i, count);
                    }
                }
                catch (IOException ioe)
                {
                    throw new MessagingException("Unable to fetch message", ioe);
                }
            }
        }

        private void fetchEnvelope(Message[] messages,
                                   MessageRetrievalListener listener)  throws IOException, MessagingException
        {
            int unsizedMessages = 0;
            for (Message message : messages)
            {
                if (message.getSize() == -1)
                {
                    unsizedMessages++;
                }
            }
            if (unsizedMessages == 0)
            {
                return;
            }
            if (unsizedMessages < 50 && mMessageCount > 5000)
            {
                /*
                 * In extreme cases we'll do a command per message instead of a bulk request
                 * to hopefully save some time and bandwidth.
                 */
                for (int i = 0, count = messages.length; i < count; i++)
                {
                    Message message = messages[i];
                    if (!(message instanceof Pop3Message))
                    {
                        throw new MessagingException("Pop3Store.fetch called with non-Pop3 Message");
                    }
                    Pop3Message pop3Message = (Pop3Message)message;
                    if (listener != null)
                    {
                        listener.messageStarted(pop3Message.getUid(), i, count);
                    }
                    String response = executeSimpleCommand(String.format("LIST %d",
                                                           mUidToMsgNumMap.get(pop3Message.getUid())));
                    String[] listParts = response.split(" ");
                    //int msgNum = Integer.parseInt(listParts[1]);
                    int msgSize = Integer.parseInt(listParts[2]);
                    pop3Message.setSize(msgSize);
                    if (listener != null)
                    {
                        listener.messageFinished(pop3Message, i, count);
                    }
                }
            }
            else
            {
                HashSet<String> msgUidIndex = new HashSet<String>();
                for (Message message : messages)
                {
                    msgUidIndex.add(message.getUid());
                }
                int i = 0, count = messages.length;
                String response = executeSimpleCommand("LIST");
                while ((response = readLine()) != null)
                {
                    if (response.equals("."))
                    {
                        break;
                    }
                    String[] listParts = response.split(" ");
                    int msgNum = Integer.parseInt(listParts[0]);
                    int msgSize = Integer.parseInt(listParts[1]);
                    Pop3Message pop3Message = mMsgNumToMsgMap.get(msgNum);
                    if (pop3Message != null && msgUidIndex.contains(pop3Message.getUid()))
                    {
                        if (listener != null)
                        {
                            listener.messageStarted(pop3Message.getUid(), i, count);
                        }
                        pop3Message.setSize(msgSize);
                        if (listener != null)
                        {
                            listener.messageFinished(pop3Message, i, count);
                        }
                        i++;
                    }
                }
            }
        }

        /**
         * Fetches the body of the given message, limiting the stored data
         * to the specified number of lines. If lines is -1 the entire message
         * is fetched. This is implemented with RETR for lines = -1 or TOP
         * for any other value. If the server does not support TOP it is
         * emulated with RETR and extra lines are thrown away.
         * @param message
         * @param lines
         */
        private void fetchBody(Pop3Message message, int lines)
        throws IOException, MessagingException
        {
            String response = null;
            if (lines == -1 || !mCapabilities.top)
            {
                response = executeSimpleCommand(String.format("RETR %d",
                                                mUidToMsgNumMap.get(message.getUid())));
            }
            else
            {
                response = executeSimpleCommand(String.format("TOP %d %d",
                                                mUidToMsgNumMap.get(message.getUid()),
                                                lines));
            }
            if (response != null)
            {
                try
                {
                    message.parse(new Pop3ResponseInputStream(mIn));
                    if (lines == -1 || !mCapabilities.top)
                    {
                        message.setFlag(Flag.X_DOWNLOADED_FULL, true);
                    }
                }
                catch (MessagingException me)
                {
                    /*
                     * If we're only downloading headers it's possible
                     * we'll get a broken MIME message which we're not
                     * real worried about. If we've downloaded the body
                     * and can't parse it we need to let the user know.
                     */
                    if (lines == -1)
                    {
                        throw me;
                    }
                }
            }
        }

        @Override
        public Flag[] getPermanentFlags() throws MessagingException
        {
            return PERMANENT_FLAGS;
        }

        @Override
        public void appendMessages(Message[] messages) throws MessagingException
        {
        }

        @Override
        public void delete(boolean recurse) throws MessagingException
        {
        }

        @Override
        public void delete(Message[] msgs, String trashFolderName) throws MessagingException
        {
            setFlags(msgs, new Flag[] { Flag.DELETED }, true);
        }

        @Override
        public String getUidFromMessageId(Message message) throws MessagingException
        {
            return null;
        }

        @Override
        public void setFlags(Flag[] flags, boolean value)
        throws MessagingException
        {
            Message[] messages = getMessages(null);
            setFlags(messages, flags, value);
        }

        @Override
        public void setFlags(Message[] messages, Flag[] flags, boolean value)
        throws MessagingException
        {
            if (!value || !Utility.arrayContains(flags, Flag.DELETED))
            {
                /*
                 * The only flagging we support is setting the Deleted flag.
                 */
                return;
            }
            ArrayList<String> uids = new ArrayList<String>();
            try
            {
                for (Message message : messages)
                {
                    uids.add(message.getUid());
                }

                indexUids(uids);
            }
            catch (IOException ioe)
            {
                throw new MessagingException("Could not get message number for uid " + uids, ioe);
            }
            for (Message message : messages)
            {

                Integer msgNum = mUidToMsgNumMap.get(message.getUid());
                if (msgNum == null)
                {
                    MessagingException me = new MessagingException("Could not delete message " + message.getUid()
                            + " because no msgNum found; permanent error");
                    me.setPermanentFailure(true);
                    throw me;
                }
                executeSimpleCommand(String.format("DELE %s", msgNum));
            }
        }

//        private boolean isRoundTripModeSuggested() {
//            long roundTripMethodMs =
//                (uncachedMessageCount * 2 * mLatencyMs);
//            long bulkMethodMs =
//                    (mMessageCount * 58) / (mThroughputKbS * 1024 / 8) * 1000;
//        }

        private String readLine() throws IOException
        {
            StringBuffer sb = new StringBuffer();
            int d = mIn.read();
            if (d == -1)
            {
                throw new IOException("End of stream reached while trying to read line.");
            }
            do
            {
                if (((char)d) == '\r')
                {
                    continue;
                }
                else if (((char)d) == '\n')
                {
                    break;
                }
                else
                {
                    sb.append((char)d);
                }
            }
            while ((d = mIn.read()) != -1);
            String ret = sb.toString();
            if (Config.LOGD)
            {
                if (K9.DEBUG)
                {
                    Log.d(K9.LOG_TAG, "<<< " + ret);
                }
            }
            return ret;
        }

        private void writeLine(String s) throws IOException
        {
            if (Config.LOGD)
            {
                if (K9.DEBUG)
                {
                    Log.d(K9.LOG_TAG, ">>> " + s);
                }
            }
            mOut.write(s.getBytes());
            mOut.write('\r');
            mOut.write('\n');
            mOut.flush();
        }

        private Pop3Capabilities getCapabilities() throws IOException, MessagingException
        {
            Pop3Capabilities capabilities = new Pop3Capabilities();
            try
            {
                String response = executeSimpleCommand("CAPA");
                while ((response = readLine()) != null)
                {
                    if (response.equals("."))
                    {
                        break;
                    }
                    if (response.equalsIgnoreCase("STLS"))
                    {
                        capabilities.stls = true;
                    }
                    else if (response.equalsIgnoreCase("UIDL"))
                    {
                        capabilities.uidl = true;
                    }
                    else if (response.equalsIgnoreCase("PIPELINING"))
                    {
                        capabilities.pipelining = true;
                    }
                    else if (response.equalsIgnoreCase("USER"))
                    {
                        capabilities.user = true;
                    }
                    else if (response.equalsIgnoreCase("TOP"))
                    {
                        capabilities.top = true;
                    }
                }
            }
            catch (MessagingException me)
            {
                /*
                 * The server may not support the CAPA command, so we just eat this Exception
                 * and allow the empty capabilities object to be returned.
                 */
            }
            return capabilities;
        }

        private String executeSimpleCommand(String command) throws MessagingException
        {
            try
            {
                open(OpenMode.READ_WRITE);
                if (Config.LOGV)
                {
                    Log.v(K9.LOG_TAG, "POP3: command '" + command + "'");
                }
                if (command != null)
                {
                    writeLine(command);
                }

                String response = readLine();
                if (Config.LOGV)
                {
                    Log.v(K9.LOG_TAG, "POP3: response '" + command + "'");
                }

                if (response.length() > 1 && response.charAt(0) == '-')
                {
                    throw new MessagingException(response);
                }

                return response;
            }
            catch (MessagingException me)
            {
                throw me;
            }
            catch (Exception e)
            {
                closeIO();
                throw new MessagingException("Unable to execute POP3 command", e);
            }
        }

        @Override
        public boolean supportsFetchingFlags()
        {
            return false;
        }//isFlagSupported

        @Override
        public boolean equals(Object o)
        {
            if (o instanceof Pop3Folder)
            {
                return ((Pop3Folder) o).mName.equals(mName);
            }
            return super.equals(o);
        }
        
        @Override
        public int hashCode()
        {
            return mName.hashCode();
        }

    }//Pop3Folder

    class Pop3Message extends MimeMessage
    {
        public Pop3Message(String uid, Pop3Folder folder) throws MessagingException
        {
            mUid = uid;
            mFolder = folder;
            mSize = -1;
            mFlags.add(Flag.X_NO_SEEN_INFO);
        }

        public void setSize(int size)
        {
            mSize = size;
        }

        @Override
        protected void parse(InputStream in) throws IOException, MessagingException
        {
            super.parse(in);
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
            //  try
            //  {
            //  Poor POP3 users, we can't copy the message to the Trash folder, but they still want a delete
            setFlag(Flag.DELETED, true);
            //   }
//         catch (MessagingException me)
//         {
//          Log.w(K9.LOG_TAG, "Could not delete non-existant message", me);
//         }
        }
    }

    class Pop3Capabilities
    {
        public boolean stls;
        public boolean top;
        public boolean user;
        public boolean uidl;
        public boolean pipelining;

        @Override
        public String toString()
        {
            return String.format("STLS %b, TOP %b, USER %b, UIDL %b, PIPELINING %b",
                                 stls,
                                 top,
                                 user,
                                 uidl,
                                 pipelining);
        }
    }

    class Pop3ResponseInputStream extends InputStream
    {
        InputStream mIn;
        boolean mStartOfLine = true;
        boolean mFinished;

        public Pop3ResponseInputStream(InputStream in)
        {
            mIn = in;
        }

        @Override
        public int read() throws IOException
        {
            if (mFinished)
            {
                return -1;
            }
            int d = mIn.read();
            if (mStartOfLine && d == '.')
            {
                d = mIn.read();
                if (d == '\r')
                {
                    mFinished = true;
                    mIn.read();
                    return -1;
                }
            }

            mStartOfLine = (d == '\n');

            return d;
        }
    }
}
