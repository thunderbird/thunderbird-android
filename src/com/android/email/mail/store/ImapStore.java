
package com.android.email.mail.store;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.Security;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.SSLException;

import android.util.Log;

import com.android.email.Email;
import com.android.email.PeekableInputStream;
import com.android.email.Utility;
import com.android.email.mail.AuthenticationFailedException;
import com.android.email.mail.FetchProfile;
import com.android.email.mail.Flag;
import com.android.email.mail.Folder;
import com.android.email.mail.Message;
import com.android.email.mail.MessageRetrievalListener;
import com.android.email.mail.MessagingException;
import com.android.email.mail.Part;
import com.android.email.mail.PushReceiver;
import com.android.email.mail.Pusher;
import com.android.email.mail.Store;
import com.android.email.mail.CertificateValidationException;
import com.android.email.mail.Folder.FolderType;
import com.android.email.mail.internet.MimeBodyPart;
import com.android.email.mail.internet.MimeHeader;
import com.android.email.mail.internet.MimeMessage;
import com.android.email.mail.internet.MimeMultipart;
import com.android.email.mail.internet.MimeUtility;
import com.android.email.mail.store.ImapResponseParser.ImapList;
import com.android.email.mail.store.ImapResponseParser.ImapResponse;
import com.android.email.mail.store.LocalStore.LocalFolder;
import com.android.email.mail.store.LocalStore.LocalMessage;
import com.android.email.mail.transport.CountingOutputStream;
import com.android.email.mail.transport.EOLConvertingOutputStream;
import com.beetstra.jutf7.CharsetProvider;

/**
 * <pre>
 * TODO Need to start keeping track of UIDVALIDITY
 * TODO Need a default response handler for things like folder updates
 * TODO In fetch(), if we need a ImapMessage and were given
 * something else we can try to do a pre-fetch first.
 *
 * ftp://ftp.isi.edu/in-notes/rfc2683.txt When a client asks for
 * certain information in a FETCH command, the server may return the requested
 * information in any order, not necessarily in the order that it was requested.
 * Further, the server may return the information in separate FETCH responses
 * and may also return information that was not explicitly requested (to reflect
 * to the client changes in the state of the subject message).
 * </pre>
 */
public class ImapStore extends Store {
    public static final int CONNECTION_SECURITY_NONE = 0;
    public static final int CONNECTION_SECURITY_TLS_OPTIONAL = 1;
    public static final int CONNECTION_SECURITY_TLS_REQUIRED = 2;
    public static final int CONNECTION_SECURITY_SSL_REQUIRED = 3;
    public static final int CONNECTION_SECURITY_SSL_OPTIONAL = 4;
    
    private static final int IDLE_READ_TIMEOUT = 29 * 60 * 1000; // 29 minutes
    private static final int IDLE_REFRESH_INTERVAL = 20 * 60 * 1000; // 20 minutes

    private static final Flag[] PERMANENT_FLAGS = { Flag.DELETED, Flag.SEEN };

    private String mHost;
    private int mPort;
    private String mUsername;
    private String mPassword;
    private int mConnectionSecurity;
    private String mPathPrefix;
    private String mPathDelimeter;

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
     * imap://user:password@server:port CONNECTION_SECURITY_NONE
     * imap+tls://user:password@server:port CONNECTION_SECURITY_TLS_OPTIONAL
     * imap+tls+://user:password@server:port CONNECTION_SECURITY_TLS_REQUIRED
     * imap+ssl+://user:password@server:port CONNECTION_SECURITY_SSL_REQUIRED
     * imap+ssl://user:password@server:port CONNECTION_SECURITY_SSL_OPTIONAL
     *
     * @param _uri
     */
    public ImapStore(String _uri) throws MessagingException {
        URI uri;
        try {
            uri = new URI(_uri);
        } catch (URISyntaxException use) {
            throw new MessagingException("Invalid ImapStore URI", use);
        }

        String scheme = uri.getScheme();
        if (scheme.equals("imap")) {
            mConnectionSecurity = CONNECTION_SECURITY_NONE;
            mPort = 143;
        } else if (scheme.equals("imap+tls")) {
            mConnectionSecurity = CONNECTION_SECURITY_TLS_OPTIONAL;
            mPort = 143;
        } else if (scheme.equals("imap+tls+")) {
            mConnectionSecurity = CONNECTION_SECURITY_TLS_REQUIRED;
            mPort = 143;
        } else if (scheme.equals("imap+ssl+")) {
            mConnectionSecurity = CONNECTION_SECURITY_SSL_REQUIRED;
            mPort = 993;
        } else if (scheme.equals("imap+ssl")) {
            mConnectionSecurity = CONNECTION_SECURITY_SSL_OPTIONAL;
            mPort = 993;
        } else {
            throw new MessagingException("Unsupported protocol");
        }

        mHost = uri.getHost();

        if (uri.getPort() != -1) {
            mPort = uri.getPort();
        }

        if (uri.getUserInfo() != null) {
            String[] userInfoParts = uri.getUserInfo().split(":", 2);
            mUsername = userInfoParts[0];
            if (userInfoParts.length > 1) {
                mPassword = userInfoParts[1];
            }
        }

        if ((uri.getPath() != null) && (uri.getPath().length() > 0)) {
            mPathPrefix = uri.getPath().substring(1);
        }

        mModifiedUtf7Charset = new CharsetProvider().charsetForName("X-RFC-3501");
    }

    @Override
    public Folder getFolder(String name) throws MessagingException {
        ImapFolder folder;
        synchronized (mFolderCache) {
            folder = mFolderCache.get(name);
            if (folder == null) {
                folder = new ImapFolder(this, name);
                mFolderCache.put(name, folder);
            }
        }
        return folder;
    }


    @Override
    public Folder[] getPersonalNamespaces() throws MessagingException {
        ImapConnection connection = getConnection();
        try {
            ArrayList<Folder> folders = new ArrayList<Folder>();
	    if(mPathPrefix == null){ mPathPrefix = ""; }
            List<ImapResponse> responses =
                    connection.executeSimpleCommand(String.format("LIST \"\" \"%s*\"",
                        mPathPrefix));

            for (ImapResponse response : responses) {
                if (response.get(0).equals("LIST")) {
                    boolean includeFolder = true;
                    String folder = decodeFolderName(response.getString(3));

		    if(mPathDelimeter == null){ mPathDelimeter = response.getString(2); }

                    if (folder.equalsIgnoreCase(Email.INBOX)) {
                        continue;
                    }else{
			if(mPathPrefix.length() > 0){
			    if(folder.length() >= mPathPrefix.length() + 1){
				folder = folder.substring(mPathPrefix.length() + 1);
			    }
			    if(!decodeFolderName(response.getString(3)).equals(mPathPrefix + mPathDelimeter + folder)){
				includeFolder = false;
			    }
			}
		    }
		    
                    ImapList attributes = response.getList(1);
                    for (int i = 0, count = attributes.size(); i < count; i++) {
                        String attribute = attributes.getString(i);
                        if (attribute.equalsIgnoreCase("\\NoSelect")) {
                            includeFolder = false;
                        }
                    }
                    if (includeFolder) {
                        folders.add(getFolder(folder));
                    }
                }
            }
            folders.add(getFolder("INBOX"));
            return folders.toArray(new Folder[] {});
        } catch (IOException ioe) {
            connection.close();
            throw new MessagingException("Unable to get folder list.", ioe);
        } finally {
            releaseConnection(connection);
        }
    }

    @Override
    public void checkSettings() throws MessagingException {
        try {
            ImapConnection connection = new ImapConnection();
            connection.open();
            connection.close();
        }
        catch (IOException ioe) {
            throw new MessagingException("Unable to connect.", ioe);
        }
    }

    /**
     * Gets a connection if one is available for reuse, or creates a new one if not.
     * @return
     */
    private ImapConnection getConnection() throws MessagingException {
        synchronized (mConnections) {
            ImapConnection connection = null;
            while ((connection = mConnections.poll()) != null) {
                try {
                    connection.executeSimpleCommand("NOOP");
                    break;
                }
                catch (IOException ioe) {
                    connection.close();
                }
            }
            if (connection == null) {
                connection = new ImapConnection();
            }
            return connection;
        }
    }

    private void releaseConnection(ImapConnection connection) {
    		synchronized(mConnections)
    		{
    			mConnections.offer(connection);
    		}
    }

    private String encodeFolderName(String name) {
        try {
            ByteBuffer bb = mModifiedUtf7Charset.encode(name);
            byte[] b = new byte[bb.limit()];
            bb.get(b);
            return new String(b, "US-ASCII");
        }
        catch (UnsupportedEncodingException uee) {
            /*
             * The only thing that can throw this is getBytes("US-ASCII") and if US-ASCII doesn't
             * exist we're totally screwed.
             */
            throw new RuntimeException("Unable to encode folder name: " + name, uee);
        }
    }

    private String decodeFolderName(String name) {
        /*
         * Convert the encoded name to US-ASCII, then pass it through the modified UTF-7
         * decoder and return the Unicode String.
         */
        try {
            byte[] encoded = name.getBytes("US-ASCII");
            CharBuffer cb = mModifiedUtf7Charset.decode(ByteBuffer.wrap(encoded));
            return cb.toString();
        }
        catch (UnsupportedEncodingException uee) {
            /*
             * The only thing that can throw this is getBytes("US-ASCII") and if US-ASCII doesn't
             * exist we're totally screwed.
             */
            throw new RuntimeException("Unable to decode folder name: " + name, uee);
        }
    }
    
    @Override
    public boolean isMoveCapable() {
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
    public Pusher getPusher(PushReceiver receiver, List<String> names)
    {
        return new ImapPusher(this, receiver, names);
    }

    class ImapFolder extends Folder {
        private String mName;
        protected int mMessageCount = -1;
        protected int uidNext = -1;
        protected ImapConnection mConnection;
        private OpenMode mMode;
        private boolean mExists;
        private ImapStore store = null;

        public ImapFolder(ImapStore nStore, String name) {
        	store = nStore;
	    this.mName = name;
        }

	public String getPrefixedName() {
	    String prefixedName = "";
	    if (!Email.INBOX.equalsIgnoreCase(mName))
	    {
	        String prefix = mPathPrefix;
	        String delim = mPathDelimeter;
	        
    	    if (prefix != null && delim != null)
    	    {
    	        prefix = prefix.trim();
    	        delim = delim.trim();
    	        if (prefix.length() > 0 && delim.length() > 0)
    	        {
    	            prefixedName += mPathPrefix + mPathDelimeter;
    	        }
    	    }
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
	
	public void open(OpenMode mode) throws MessagingException
	{
	    internalOpen(mode);
	}

    	public List<ImapResponse> internalOpen(OpenMode mode) throws MessagingException {
    	    if (isOpen() && mMode == mode) {
    	        // Make sure the connection is valid. If it's not we'll close it down and continue
    	        // on to get a new one.
    	        try {
    	            List<ImapResponse> responses = executeSimpleCommand("NOOP");
    	            return responses;
    	        }
    	        catch (IOException ioe) {
    	            ioExceptionHandler(mConnection, ioe);
    	        }
    	    }
    	    synchronized (this) {
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
    	    try {
    
    	        if(mPathDelimeter == null)
    	        {
    	            List<ImapResponse> nameResponses =
    	                executeSimpleCommand(String.format("LIST \"\" \"*%s\"", encodeFolderName(mName)));
    	            for (ImapResponse response : nameResponses) {
    	                if (response.get(0).equals("LIST")) 
    	                {
    	                    mPathDelimeter = nameResponses.get(0).getString(2);
    	                    if (Email.DEBUG)
    	                    {
    	                        Log.d(Email.LOG_TAG, "Got path delimeter '" + mPathDelimeter + "'");
    	                    }
    	                }
    	            }
    	        }
    
    	        //                executeSimpleCommand("CLOSE");
    
    	        String command = String.format("SELECT \"%s\"",
    	                encodeFolderName(getPrefixedName()));
    
    	         List<ImapResponse> responses = executeSimpleCommand(command);
    	        
    	        /*
    	         * If the command succeeds we expect the folder has been opened read-write
    	         * unless we are notified otherwise in the responses.
    	         */
    	        mMode = OpenMode.READ_WRITE;
    
    	        for (ImapResponse response : responses) {
    	            if (response.mTag != null && response.size() >= 2) {
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
    	                        
        	                        if ("READ-ONLY".equalsIgnoreCase(key)) {
        	                            mMode = OpenMode.READ_ONLY;
        	                        }
        	                        else if ("READ-WRITE".equalsIgnoreCase(key)) {
        	                            mMode = OpenMode.READ_WRITE;
        	                        }
    	                        }
    	                    }
    	                }
    	                
    	            }
    	        }
    
    	        if (mMessageCount == -1) {
    	            throw new MessagingException(
    	                    "Did not find message count with command '" + command + "'");
    	        }
    	        mExists = true;
    	        return null;
    	    } catch (IOException ioe) {
    	        throw ioExceptionHandler(mConnection, ioe);
    	    }
    	   
    	}

        public boolean isOpen() {
            return mConnection != null;
        }

        @Override
        public OpenMode getMode() throws MessagingException {
            return mMode;
        }

        public void close(boolean expunge) throws MessagingException {
	    if (mMessageCount != -1)
	    {
	//	close();
		mMessageCount = -1;
	    }
            if (!isOpen()) {
                return;
            }
            if (expunge)
            {
            	expunge();
            }
            synchronized (this) {
                releaseConnection(mConnection);
                mConnection = null;
            }
        }

        public String getName() {
            return mName;
        }

        public boolean exists() throws MessagingException {
            if (mExists) {
                return true;
            }
            /*
             * This method needs to operate in the unselected mode as well as the selected mode
             * so we must get the connection ourselves if it's not there. We are specifically
             * not calling checkOpen() since we don't care if the folder is open.
             */
            ImapConnection connection = null;
            synchronized(this) {
                if (mConnection == null) {
                    connection = getConnection();
                }
                else {
                    connection = mConnection;
                }
            }
            try {
                connection.executeSimpleCommand(String.format("STATUS \"%s\" (UIDVALIDITY)",
                        encodeFolderName(getPrefixedName())));
                mExists = true;
                return true;
            }
            catch (MessagingException me) {
                return false;
            }
            catch (IOException ioe) {
                throw ioExceptionHandler(connection, ioe);
            }
            finally {
                if (mConnection == null) {
                    releaseConnection(connection);
                }
            }
        }

        public boolean create(FolderType type) throws MessagingException {
            /*
             * This method needs to operate in the unselected mode as well as the selected mode
             * so we must get the connection ourselves if it's not there. We are specifically
             * not calling checkOpen() since we don't care if the folder is open.
             */
            ImapConnection connection = null;
            synchronized(this) {
                if (mConnection == null) {
                    connection = getConnection();
                }
                else {
                    connection = mConnection;
                }
            }
            try {
                connection.executeSimpleCommand(String.format("CREATE \"%s\"",
                        encodeFolderName(getPrefixedName())));
                return true;
            }
            catch (MessagingException me) {
                return false;
            }
            catch (IOException ioe) {
                throw ioExceptionHandler(mConnection, ioe);
            }
            finally {
                if (mConnection == null) {
                    releaseConnection(connection);
                }
            }
        }

        @Override
        public void copyMessages(Message[] messages, Folder folder) throws MessagingException {
          if (folder instanceof ImapFolder == false) {
            throw new MessagingException("ImapFolder.copyMessages passed non-ImapFolder");
          }
          ImapFolder iFolder = (ImapFolder)folder;  
            checkOpen();
            String[] uids = new String[messages.length];
            for (int i = 0, count = messages.length; i < count; i++) {
                uids[i] = messages[i].getUid();
            }
            try {
                executeSimpleCommand(String.format("UID COPY %s \"%s\"",
                        Utility.combine(uids, ','),
                        encodeFolderName(iFolder.getPrefixedName())));
            }
            catch (IOException ioe) {
                throw ioExceptionHandler(mConnection, ioe);
            }
        }
        
        @Override
        public void moveMessages(Message[] messages, Folder folder) throws MessagingException {
          copyMessages(messages, folder);
          setFlags(messages, new Flag[] { Flag.DELETED }, true);
        }

        @Override
        public int getMessageCount() {
            return mMessageCount;
        }

        @Override
        public int getUnreadMessageCount() throws MessagingException {
            checkOpen();
            try {
                int unreadMessageCount = 0;
                List<ImapResponse> responses = executeSimpleCommand(
                        String.format("STATUS \"%s\" (UNSEEN)",
                                encodeFolderName(getPrefixedName())));
                for (ImapResponse response : responses) {
                    if (response.mTag == null && response.get(0).equals("STATUS")) {
                        ImapList status = response.getList(2);
                        unreadMessageCount = status.getKeyedNumber("UNSEEN");
                    }
                }
                return unreadMessageCount;
            }
            catch (IOException ioe) {
                throw ioExceptionHandler(mConnection, ioe);
            }
        }

        @Override
        public void delete(boolean recurse) throws MessagingException {
            throw new Error("ImapStore.delete() not yet implemented");
        }

        @Override
        public Message getMessage(String uid) throws MessagingException {
            return new ImapMessage(uid, this);
        }

        
        @Override
        public Message[] getMessages(int start, int end, MessageRetrievalListener listener)
                throws MessagingException {
            
            
            return getMessages(start, end, false, listener);
        }
        
        protected Message[] getMessages(int start, int end, boolean includeDeleted, MessageRetrievalListener listener)
                throws MessagingException {
            if (start < 1 || end < 1 || end < start) {
                throw new MessagingException(
                        String.format("Invalid message set %d %d",
                                start, end));
            }
            checkOpen();
            ArrayList<Message> messages = new ArrayList<Message>();
            try {
		boolean gotSearchValues = false;
                ArrayList<Integer> uids = new ArrayList<Integer>();
                List<ImapResponse> responses = executeSimpleCommand(String.format("UID SEARCH %d:%d" + (includeDeleted ? "" : " NOT DELETED"), start, end));
                for (ImapResponse response : responses) {
		    //		    Log.d(Email.LOG_TAG, "Got search response: " + response.get(0) + ", size " + response.size());
                    if (response.get(0).equals("SEARCH")) {
			gotSearchValues = true;
                        for (int i = 1, count = response.size(); i < count; i++) {
			    //			    Log.d(Email.LOG_TAG, "Got search response UID: " + response.getString(i));

                            uids.add(Integer.parseInt(response.getString(i)));
                        }
                    }
                }
		if (gotSearchValues == false)
		{
		    throw new MessagingException("Did not get proper search response");
		}
                // Sort the uids in numerically ascending order
                Collections.sort(uids);
                for (int i = 0, count = uids.size(); i < count; i++) {
                    if (listener != null) {
                        listener.messageStarted("" + uids.get(i), i, count);
                    }
                    ImapMessage message = new ImapMessage("" + uids.get(i), this);
                    messages.add(message);
                    if (listener != null) {
                        listener.messageFinished(message, i, count);
                    }
                }
            } catch (IOException ioe) {
                throw ioExceptionHandler(mConnection, ioe);
            }
            return messages.toArray(new Message[] {});
        }


        public Message[] getMessages(MessageRetrievalListener listener) throws MessagingException {
            return getMessages(null, listener);
        }

        public Message[] getMessages(String[] uids, MessageRetrievalListener listener)
                throws MessagingException {
            checkOpen();
            ArrayList<Message> messages = new ArrayList<Message>();
            try {
                if (uids == null) {
                    List<ImapResponse> responses = executeSimpleCommand("UID SEARCH 1:* NOT DELETED");
                    ArrayList<String> tempUids = new ArrayList<String>();
                    for (ImapResponse response : responses) {
                        if (response.get(0).equals("SEARCH")) {
                            for (int i = 1, count = response.size(); i < count; i++) {
                                tempUids.add(response.getString(i));
                            }
                        }
                    }
                    uids = tempUids.toArray(new String[] {});
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
            return messages.toArray(new Message[] {});
        }

        public void fetch(Message[] messages, FetchProfile fp, MessageRetrievalListener listener)
                throws MessagingException {
            if (messages == null || messages.length == 0) {
                return;
            }
            checkOpen();
            String[] uids = new String[messages.length];
            HashMap<String, Message> messageMap = new HashMap<String, Message>();
            for (int i = 0, count = messages.length; i < count; i++) {
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
            if (fp.contains(FetchProfile.Item.FLAGS)) {
                fetchFields.add("FLAGS");
            }
            if (fp.contains(FetchProfile.Item.ENVELOPE)) {
                fetchFields.add("INTERNALDATE");
                fetchFields.add("RFC822.SIZE");
                fetchFields.add("BODY.PEEK[HEADER.FIELDS (date subject from content-type to cc reply-to " 
                        + Email.K9MAIL_IDENTITY + ")]");
            }
            if (fp.contains(FetchProfile.Item.STRUCTURE)) {
                fetchFields.add("BODYSTRUCTURE");
            }
            if (fp.contains(FetchProfile.Item.BODY_SANE)) {
                fetchFields.add(String.format("BODY.PEEK[]<0.%d>", FETCH_BODY_SANE_SUGGESTED_SIZE));
            }
            if (fp.contains(FetchProfile.Item.BODY)) {
                fetchFields.add("BODY.PEEK[]");
            }
            for (Object o : fp) {
                if (o instanceof Part) {
                    Part part = (Part) o;
                    String partId = part.getHeader(MimeHeader.HEADER_ANDROID_ATTACHMENT_STORE_DATA)[0];
                    if ("TEXT".equals(partId)) {
                        fetchFields.add(String.format("BODY.PEEK[TEXT]<0.%d>", FETCH_BODY_SANE_SUGGESTED_SIZE));
                    }
                    else {
                        fetchFields.add("BODY.PEEK[" + partId + "]");
                    }
                }
            }

            try {
                String tag = mConnection.sendCommand(String.format("UID FETCH %s (%s)",
                        Utility.combine(uids, ','),
                        Utility.combine(fetchFields.toArray(new String[fetchFields.size()]), ' ')
                        ), false);
                ImapResponse response;
                int messageNumber = 0;
                do {
                    response = mConnection.readResponse();
                    handleUntaggedResponse(response);
                    if (Email.DEBUG)
                    {
                        Log.v(Email.LOG_TAG, "response for fetch: " + response);
                    }
                    if (response.mTag == null && response.get(1).equals("FETCH")) {
                        ImapList fetchList = (ImapList)response.getKeyedValue("FETCH");
                        String uid = fetchList.getKeyedString("UID");

                        Message message = messageMap.get(uid);
                        if (message == null)
                        {
                            Log.w(Email.LOG_TAG, "Do not have message in messageMap for UID " + uid);
                            continue;
                        }
                        if (listener != null) {
                            listener.messageStarted(uid, messageNumber++, messageMap.size());
                        }

                        if (fp.contains(FetchProfile.Item.FLAGS)) {
                            ImapList flags = fetchList.getKeyedList("FLAGS");
                            ImapMessage imapMessage = (ImapMessage) message;
                            if (flags != null) {
                                for (int i = 0, count = flags.size(); i < count; i++) {
                                    String flag = flags.getString(i);
                                    if (flag.equals("\\Deleted")) {
                                        imapMessage.setFlagInternal(Flag.DELETED, true);
                                    }
                                    else if (flag.equals("\\Answered")) {
                                        imapMessage.setFlagInternal(Flag.ANSWERED, true);
                                    }
                                    else if (flag.equals("\\Seen")) {
                                        imapMessage.setFlagInternal(Flag.SEEN, true);
                                    }
                                    else if (flag.equals("\\Flagged")) {
                                        imapMessage.setFlagInternal(Flag.FLAGGED, true);
                                    }
                                }
                            }
                        }
                        if (fp.contains(FetchProfile.Item.ENVELOPE)) {
                            Date internalDate = fetchList.getKeyedDate("INTERNALDATE");
                            int size = fetchList.getKeyedNumber("RFC822.SIZE");
                            InputStream headerStream = fetchList.getLiteral(fetchList.size() - 1);

                            ImapMessage imapMessage = (ImapMessage) message;

                            message.setInternalDate(internalDate);
                            imapMessage.setSize(size);
                            imapMessage.parse(headerStream);
                        }
                        if (fp.contains(FetchProfile.Item.STRUCTURE)) {
                            ImapList bs = fetchList.getKeyedList("BODYSTRUCTURE");
                            if (bs != null) {
                                try {
                                    parseBodyStructure(bs, message, "TEXT");
                                }
                                catch (MessagingException e) {
                                    if (Email.DEBUG) {
                                        Log.d(Email.LOG_TAG, "Error handling message", e);
                                    }
                                    message.setBody(null);
                                }
                            }
                        }
                        if (fp.contains(FetchProfile.Item.BODY)) {
                            InputStream bodyStream = fetchList.getLiteral(fetchList.size() - 1);
                            ImapMessage imapMessage = (ImapMessage) message;
                            imapMessage.parse(bodyStream);
                        }
                        if (fp.contains(FetchProfile.Item.BODY_SANE)) {
                            InputStream bodyStream = fetchList.getLiteral(fetchList.size() - 1);
                            ImapMessage imapMessage = (ImapMessage) message;
                            imapMessage.parse(bodyStream);
                        }
                        for (Object o : fp) {
                            if (o instanceof Part) {
                                Part part = (Part) o;
                                Object literal = fetchList.getObject(fetchList.size() - 1);
                                if (literal instanceof InputStream)
                                {
                                  //Log.i(Email.LOG_TAG, "Part is an InputStream/Literal");
                                  InputStream bodyStream = (InputStream)literal;
                                  String contentType = part.getContentType();
                                  String contentTransferEncoding = part.getHeader(
                                          MimeHeader.HEADER_CONTENT_TRANSFER_ENCODING)[0];
                                  part.setBody(MimeUtility.decodeBody(
                                          bodyStream,
                                          contentTransferEncoding));
                                }
                                else if (literal instanceof String)
                                {
                                  String bodyString = (String)literal;

                                  if (Email.DEBUG)
                                  {
                                      Log.v(Email.LOG_TAG, "Part is an String: " + bodyString);
                                  }
                                  InputStream bodyStream = new ByteArrayInputStream(bodyString.getBytes());
                                  String contentTransferEncoding = part.getHeader(
                                      MimeHeader.HEADER_CONTENT_TRANSFER_ENCODING)[0];
                                  part.setBody(MimeUtility.decodeBody(
                                      bodyStream,
                                      contentTransferEncoding));
                                }
                            }
                        }

                        if (listener != null) {
                            listener.messageFinished(message, messageNumber, messageMap.size());
                        }
                    }

                    while (response.more());

                } while (response.mTag == null);
            }
            catch (IOException ioe) {
                throw ioExceptionHandler(mConnection, ioe);
            }
        }

        @Override
        public Flag[] getPermanentFlags() throws MessagingException {
            return PERMANENT_FLAGS;
        }

        /**
         * Handle any untagged responses that the caller doesn't care to handle themselves.
         * @param responses
         */
        protected List<ImapResponse> handleUntaggedResponses(List<ImapResponse> responses) {
            for (ImapResponse response : responses) {
                handleUntaggedResponse(response);
            }
            return responses;
        }

        /**
         * Handle an untagged response that the caller doesn't care to handle themselves.
         * @param response
         */
        protected void handleUntaggedResponse(ImapResponse response) {
            //Log.i(Email.LOG_TAG, "Got response with size " + response.size() + ": " + response);
          if (response.mTag == null && response.size() > 1)
          {
            if (response.get(1).equals("EXISTS")) {
                mMessageCount = response.getNumber(0);
                if (Email.DEBUG)
                {
                    Log.d(Email.LOG_TAG, "Got untagged EXISTS with value " + mMessageCount);
                }
            }
            if (response.get(0).equals("OK") && response.size() > 1) {
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
                            if ("UIDNEXT".equals(key))
                            {
                                uidNext = bracketed.getNumber(1);
                                if (Email.DEBUG)
                                {
                                    Log.d(Email.LOG_TAG, "Got UidNext = " + uidNext);
                                }
                            }
                        }
                    }
                    
                    
                }
            }
            else if (response.get(1).equals("EXPUNGE") && mMessageCount > 0) {
              mMessageCount--;
              if (Email.DEBUG)
              {
                  Log.d(Email.LOG_TAG, "Got untagged EXPUNGE with value " + mMessageCount);
              }
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
//                            if ("ALERT".equals(key))
//                            {
//                                StringBuffer sb = new StringBuffer();
//                                for (int i = 2, count = response.size(); i < count; i++) {
//                                    sb.append(response.get(i).toString());
//                                    sb.append(' ');
//                                }
//                                
//                                Log.w(Email.LOG_TAG, "ALERT: " + sb.toString());
//                            }
//                        }
//                    }
//                    
//                    
//                }
//            }
          }
          //Log.i(Email.LOG_TAG, "mMessageCount = " + mMessageCount);
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
                        ImapBodyPart bp = new ImapBodyPart();
                        if (id.equals("TEXT")) {
                            parseBodyStructure(bs.getList(i), bp, Integer.toString(i + 1));
                        }
                        else {
                            parseBodyStructure(bs.getList(i), bp, id + "." + (i + 1));
                        }
                        mp.addBodyPart(bp);
                    }
                    else {
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
            else{
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
                if (bs.get(2) instanceof ImapList) {
                    bodyParams = bs.getList(2);
                }
                String encoding = bs.getString(5);
                int size = bs.getNumber(6);

                if (MimeUtility.mimeTypeMatches(mimeType, "message/rfc822")) {
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

                if (bodyParams != null) {
                    /*
                     * If there are body params we might be able to get some more information out
                     * of them.
                     */
                    for (int i = 0, count = bodyParams.size(); i < count; i += 2) {
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
                        && (bs.get(9) instanceof ImapList)) {
                    bodyDisposition = bs.getList(9);
                }
                else if (!("text".equalsIgnoreCase(type))
                        && (bs.size() > 7)
                        && (bs.get(8) instanceof ImapList)) {
                    bodyDisposition = bs.getList(8);
                }

                String contentDisposition = "";

                if (bodyDisposition != null && bodyDisposition.size() > 0) {
                    if (!"NIL".equalsIgnoreCase(bodyDisposition.getString(0))) {
                        contentDisposition = bodyDisposition.getString(0).toLowerCase();
                    }

                    if ((bodyDisposition.size() > 1)
                            && (bodyDisposition.get(1) instanceof ImapList)) {
                        ImapList bodyDispositionParams = bodyDisposition.getList(1);
                        /*
                         * If there is body disposition information we can pull some more information
                         * about the attachment out.
                         */
                        for (int i = 0, count = bodyDispositionParams.size(); i < count; i += 2) {
                            contentDisposition += String.format(";\n %s=\"%s\"",
                                    bodyDispositionParams.getString(i).toLowerCase(),
                                    bodyDispositionParams.getString(i + 1));
                        }
                    }
                }

                if (MimeUtility.getHeaderParameter(contentDisposition, "size") == null) {
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

                if (part instanceof ImapMessage) {
                    ((ImapMessage) part).setSize(size);
                }
                else if (part instanceof ImapBodyPart) {
                    ((ImapBodyPart) part).setSize(size);
                }
                else {
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
        public void appendMessages(Message[] messages) throws MessagingException {
            checkOpen();
            try {
                for (Message message : messages) {
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
                    do {
                        response = mConnection.readResponse();
                        handleUntaggedResponse(response);
                        if (response.mCommandContinuationRequested) {
                            eolOut = new EOLConvertingOutputStream(mConnection.mOut);
                            message.writeTo(eolOut);
                            eolOut.write('\r');
                            eolOut.write('\n');
                            eolOut.flush();
                        }
                        while (response.more());
                    } while(response.mTag == null);

                    String newUid = getUidFromMessageId(message);
                    if (Email.DEBUG)
                    {
                    	Log.d(Email.LOG_TAG, "Got UID " + newUid + " for message");
                    }          
                    
                    if (newUid != null)
                    {
                    	message.setUid(newUid);
                    }
                    
                   
                }
            }
            catch (IOException ioe) {
                throw ioExceptionHandler(mConnection, ioe);
            }
        }
        
        public String getUidFromMessageId(Message message) throws MessagingException
        {
        	try
        	{
	        	 /*
	           * Try to find the UID of the message we just appended using the
	           * Message-ID header.
	           */
	          String[] messageIdHeader = message.getHeader("Message-ID");

	          if (messageIdHeader == null || messageIdHeader.length == 0) {
	          	if (Email.DEBUG)
	            {
	          		Log.d(Email.LOG_TAG, "Did not get a message-id in order to search for UID");
	            }
	            return null;
	          }
	          String messageId = messageIdHeader[0];
	          if (Email.DEBUG)
            {
            	Log.d(Email.LOG_TAG, "Looking for UID for message with message-id " + messageId);
            }   
	
	          List<ImapResponse> responses =
	              executeSimpleCommand(
	                      String.format("UID SEARCH (HEADER MESSAGE-ID %s)", messageId));
	          for (ImapResponse response1 : responses) {
	              if (response1.mTag == null && response1.get(0).equals("SEARCH")
	                      && response1.size() > 1) {
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
        

        public Message[] expunge() throws MessagingException {
            checkOpen();
            try {
                executeSimpleCommand("EXPUNGE");
            } catch (IOException ioe) {
                throw ioExceptionHandler(mConnection, ioe);
            }
            return null;
        }

        private void close() throws MessagingException {
            checkOpen();
            try {
                executeSimpleCommand("CLOSE");
            } catch (IOException ioe) {
                
            }
        }

        private String combineFlags(Flag[] flags)
        {
          ArrayList<String> flagNames = new ArrayList<String>();
          for (int i = 0, count = flags.length; i < count; i++) {
              Flag flag = flags[i];
              if (flag == Flag.SEEN) {
                  flagNames.add("\\Seen");
              }
              else if (flag == Flag.DELETED) {
                  flagNames.add("\\Deleted");
              }
              else if (flag == Flag.ANSWERED) {
                flagNames.add("\\Answered");
              }
              else if (flag == Flag.FLAGGED) {
                flagNames.add("\\Flagged");
              }
              
          }
          return Utility.combine(flagNames.toArray(new String[flagNames.size()]), ' ');
        }
        
        
        @Override
				public void setFlags(Flag[] flags, boolean value)
				        throws MessagingException {
				    checkOpen();
	
				   
				    try {
				        executeSimpleCommand(String.format("UID STORE 1:* %sFLAGS.SILENT (%s)",
				                value ? "+" : "-", combineFlags(flags) ));
				    }
				    catch (IOException ioe) {
				        throw ioExceptionHandler(mConnection, ioe);
				    }
				}
        
        public String getNewPushState(String oldPushStateS, Message message)
        {
            try
            {
                String messageUidS = message.getUid();
                int messageUid = Integer.parseInt(messageUidS);
                ImapPushState oldPushState = ImapPushState.parse(oldPushStateS);
//                Log.d(Email.LOG_TAG, "getNewPushState comparing oldUidNext " + oldPushState.uidNext 
//                        + " to message uid " + messageUid);
                if (messageUid >= oldPushState.uidNext)
                {
                    int uidNext = messageUid + 1;
                    ImapPushState newPushState = new ImapPushState(uidNext);
                    //Log.d(Email.LOG_TAG, "newPushState = " + newPushState);
                    return newPushState.toString();
                }
                else
                {
                    return null;
                }
            }
            catch (Exception e)
            {
                Log.e(Email.LOG_TAG, "Exception while updated push state", e);
                return null;
            }
        }
        

        public void setFlags(Message[] messages, Flag[] flags, boolean value)
                throws MessagingException {
            checkOpen();
            String[] uids = new String[messages.length];
            for (int i = 0, count = messages.length; i < count; i++) {
                uids[i] = messages[i].getUid();
            }
            ArrayList<String> flagNames = new ArrayList<String>();
            for (int i = 0, count = flags.length; i < count; i++) {
                Flag flag = flags[i];
                if (flag == Flag.SEEN) {
                    flagNames.add("\\Seen");
                }
                else if (flag == Flag.DELETED) {
                    flagNames.add("\\Deleted");
                }
                else if (flag == Flag.ANSWERED) {
                  flagNames.add("\\Answered");
                }
                else if (flag == Flag.FLAGGED) {
                  flagNames.add("\\Flagged");
                }
            }
            try {
                executeSimpleCommand(String.format("UID STORE %s %sFLAGS.SILENT (%s)",
                        Utility.combine(uids, ','),
                        value ? "+" : "-",
                        Utility.combine(flagNames.toArray(new String[flagNames.size()]), ' ')));
            }
            catch (IOException ioe) {
                throw ioExceptionHandler(mConnection, ioe);
            }
        }

        private void checkOpen() throws MessagingException {
            if (!isOpen()) {
                throw new MessagingException("Folder " + getPrefixedName() + " is not open.");
            }
        }

        private MessagingException ioExceptionHandler(ImapConnection connection, IOException ioe)
                throws MessagingException {
            connection.close();
            close(false);
            return new MessagingException("IO Error", ioe);
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof ImapFolder) {
                return ((ImapFolder)o).getPrefixedName().equals(getPrefixedName());
            }
            return super.equals(o);
        }

				protected ImapStore getStore()
				{
					return store;
				}
    }

    /**
     * A cacheable class that stores the details for a single IMAP connection.
     */
    class ImapConnection {
        private Socket mSocket;
        private PeekableInputStream mIn;
        private OutputStream mOut;
        private ImapResponseParser mParser;
        private int mNextCommandTag;
        protected Set<String> capabilities = new HashSet<String>();

        public void open() throws IOException, MessagingException {
            if (isOpen()) {
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
            	Log.w(Email.LOG_TAG, "Could not set DNS ttl to 0", e);
            }
            
            try {
                
              SocketAddress socketAddress = new InetSocketAddress(mHost, mPort);
              
              Log.i(Email.LOG_TAG, "Connecting to " + mHost + " @ IP addr " + socketAddress);
              
                if (mConnectionSecurity == CONNECTION_SECURITY_SSL_REQUIRED ||
                        mConnectionSecurity == CONNECTION_SECURITY_SSL_OPTIONAL) {
                    SSLContext sslContext = SSLContext.getInstance("TLS");
                    final boolean secure = mConnectionSecurity == CONNECTION_SECURITY_SSL_REQUIRED;
                    sslContext.init(null, new TrustManager[] {
                            TrustManagerFactory.get(mHost, secure)
                    }, new SecureRandom());
                    mSocket = sslContext.getSocketFactory().createSocket();
                    mSocket.connect(socketAddress, SOCKET_CONNECT_TIMEOUT);
                } else {
                    mSocket = new Socket();
                    mSocket.connect(socketAddress, SOCKET_CONNECT_TIMEOUT);
                }

                setReadTimeout(Store.SOCKET_READ_TIMEOUT);

                mIn = new PeekableInputStream(new BufferedInputStream(mSocket.getInputStream(),
                        1024));
                mParser = new ImapResponseParser(mIn);
                mOut = mSocket.getOutputStream();

                mParser.readResponse();
                List<ImapResponse> responses = executeSimpleCommand("CAPABILITY");
                if (responses.size() != 2) {
                    throw new MessagingException("Invalid CAPABILITY response received");
                }
                capabilities.clear();
                for (ImapResponse response : responses)
                {
                    if (response.mTag == null)
                    {
                        if (response.size() > 0)
                        {
                            for (Object capability : response)
                            {
                                if (capability instanceof String)
                                {
                                    if (Email.DEBUG)
                                    {
                                        Log.v(Email.LOG_TAG, "Saving capability '" + capability + "' for connection " + this.hashCode());
                                    }
                                    capabilities.add((String)capability);
                                }
                            }
                            
                        }
                    }
                }
                
                if (mConnectionSecurity == CONNECTION_SECURITY_TLS_OPTIONAL
                        || mConnectionSecurity == CONNECTION_SECURITY_TLS_REQUIRED) {
                   
                    if (responses.get(0).contains("STARTTLS")) {
                        // STARTTLS
                        executeSimpleCommand("STARTTLS");

                        SSLContext sslContext = SSLContext.getInstance("TLS");
                        boolean secure = mConnectionSecurity == CONNECTION_SECURITY_TLS_REQUIRED;
                        sslContext.init(null, new TrustManager[] {
                                TrustManagerFactory.get(mHost, secure)
                        }, new SecureRandom());
                        mSocket = sslContext.getSocketFactory().createSocket(mSocket, mHost, mPort,
                                true);
                        mSocket.setSoTimeout(Store.SOCKET_READ_TIMEOUT);
                        mIn = new PeekableInputStream(new BufferedInputStream(mSocket
                                .getInputStream(), 1024));
                        mParser = new ImapResponseParser(mIn);
                        mOut = mSocket.getOutputStream();
                    } else if (mConnectionSecurity == CONNECTION_SECURITY_TLS_REQUIRED) {
                        throw new MessagingException("TLS not supported but required");
                    }
                }

                mOut = new BufferedOutputStream(mOut);

                try {
                    // TODO eventually we need to add additional authentication
                    // options such as SASL
                    executeSimpleCommand("LOGIN \"" + escapeString(mUsername) + "\" \"" + escapeString(mPassword) + "\"", true);
                    authSuccess = true;
                } catch (ImapException ie) {
                    throw new AuthenticationFailedException(ie.getAlertText(), ie);

                } catch (MessagingException me) {
                    throw new AuthenticationFailedException(null, me);
                }
            } catch (SSLException e) {
                throw new CertificateValidationException(e.getMessage(), e);
            } catch (GeneralSecurityException gse) {
                throw new MessagingException(
                        "Unable to open connection to IMAP server due to security error.", gse);
            }
            catch (ConnectException ce)
            {
            	String ceMess = ce.getMessage();
            	String[] tokens = ceMess.split("-");
            	if (tokens != null && tokens.length > 1 && tokens[1] != null)
            	{
            		Log.e(Email.LOG_TAG, "Stripping host/port from ConnectionException", ce);
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
                Log.e(Email.LOG_TAG, "Failed to login, closing connection");
                close();
              }
            }
        }
        
        protected void setReadTimeout(int millis) throws SocketException
        {
            mSocket.setSoTimeout(millis);
        }
        
        protected boolean isIdleCapable()
        {
            if (Email.DEBUG)
            {
                Log.v(Email.LOG_TAG, "Connection " + this.hashCode() + " has " + capabilities.size() + " capabilities");
                for (String capability : capabilities)
                {
                    Log.v(Email.LOG_TAG, "Have capability '" + capability + "'");
                }
            }
            return capabilities.contains("IDLE");
        }

        public boolean isOpen() {
            return (mIn != null && mOut != null && mSocket != null && mSocket.isConnected() && !mSocket
                    .isClosed());
        }

        public void close() {
//            if (isOpen()) {
//                try {
//                    executeSimpleCommand("LOGOUT");
//                } catch (Exception e) {
//
//                }
//            }
            try {
                mIn.close();
            } catch (Exception e) {

            }
            try {
                mOut.close();
            } catch (Exception e) {

            }
            try {
                mSocket.close();
            } catch (Exception e) {

            }
            mIn = null;
            mOut = null;
            mSocket = null;
        }

        public ImapResponse readResponse() throws IOException, MessagingException {
          try {
            return mParser.readResponse();
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
        
        public void sendContinuation(String continuation) throws IOException
        {
            mOut.write(continuation.getBytes());
            mOut.write('\r');
            mOut.write('\n');
            mOut.flush();
            
            if (Email.DEBUG) {
                Log.v(Email.LOG_TAG, ">>> " + continuation);
            }
            
        }

        public String sendCommand(String command, boolean sensitive)
            throws MessagingException, IOException {
          try {
            open();
            String tag = Integer.toString(mNextCommandTag++);
            String commandToSend = tag + " " + command;
            mOut.write(commandToSend.getBytes());
            mOut.write('\r');
            mOut.write('\n');
            mOut.flush();
           
            if (Email.DEBUG) {
                if (sensitive && !Email.DEBUG_SENSITIVE) {
                    Log.v(Email.LOG_TAG, ">>> "
                            + "[Command Hidden, Enable Sensitive Debug Logging To Show]");
                } else {
                    Log.v(Email.LOG_TAG, ">>> " + commandToSend);
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
                ImapException, MessagingException {
            return executeSimpleCommand(command, false);
        }
        
        public List<ImapResponse> executeSimpleCommand(String command, boolean sensitive) throws IOException,
        ImapException, MessagingException {
            return executeSimpleCommand(command, sensitive, null);
        }
        
        public List<ImapResponse> executeSimpleCommand(String command, boolean sensitive, UntaggedHandler untaggedHandler)
        throws IOException, ImapException, MessagingException {
            String commandToLog = command;
            if (sensitive && !Email.DEBUG_SENSITIVE)
            {
                commandToLog = "*sensitive*";
            }
            
            
          if (Email.DEBUG)
          {
            Log.v(Email.LOG_TAG, "Sending IMAP command " + commandToLog + " on connection " + this.hashCode());
          }
          String tag = sendCommand(command, sensitive);
          if (Email.DEBUG)
          {
            Log.v(Email.LOG_TAG, "Sent IMAP command " + commandToLog + " with tag " + tag);
          }
          ArrayList<ImapResponse> responses = new ArrayList<ImapResponse>();
          ImapResponse response;
          do {
            response = mParser.readResponse();
            if (Email.DEBUG)
            {
              Log.v(Email.LOG_TAG, "Got IMAP response " + response);
            }
            if (response.mTag != null && response.mTag.equals(tag) == false)
            {
              Log.w(Email.LOG_TAG, "After sending tag " + tag + ", got tag response from previous command " + response);
              Iterator<ImapResponse> iter = responses.iterator();
              while (iter.hasNext())
              {
                ImapResponse delResponse = iter.next();
                if (delResponse.mTag != null || delResponse.size() < 2 
                    || ("EXISTS".equals(delResponse.get(1)) == false && "EXPUNGE".equals(delResponse.get(1)) == false))
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
          } while (response.mTag == null);
          if (response.size() < 1 || !response.get(0).equals("OK")) {
            throw new ImapException("Command: " + commandToLog + "; response: " + response.toString(), response.getAlertText());
          }
          return responses;
        }
    }

    class ImapMessage extends MimeMessage {
        ImapMessage(String uid, Folder folder) throws MessagingException {
            this.mUid = uid;
            this.mFolder = folder;
        }

        public void setSize(int size) {
            this.mSize = size;
        }

        public void parse(InputStream in) throws IOException, MessagingException {
            super.parse(in);
        }

        public void setFlagInternal(Flag flag, boolean set) throws MessagingException {
            super.setFlag(flag, set);
        }
        
        
        @Override
        public void setFlag(Flag flag, boolean set) throws MessagingException {
            super.setFlag(flag, set);
            mFolder.setFlags(new Message[] { this }, new Flag[] { flag }, set);
        }
        
        @Override
        public void delete(String trashFolderName) throws MessagingException
        {
        	ImapFolder iFolder = (ImapFolder)getFolder();
        	if (iFolder.getName().equals(trashFolderName))
        	{
        	  setFlag(Flag.DELETED, true);
        	  iFolder.expunge();
        	}
        	else
        	{
  	        ImapFolder remoteTrashFolder = (ImapFolder)iFolder.getStore().getFolder(trashFolderName);
  	        /*
  	         * Attempt to copy the remote message to the remote trash folder.
  	         */
  	        remoteTrashFolder.mExists = false;  // Force redetection of Trash folder; some desktops delete it
  	        if (!remoteTrashFolder.exists()) {
  	            /*
  	             * If the remote trash folder doesn't exist we try to create it.
  	             */
  	        		Log.i(Email.LOG_TAG, "IMAPMessage.delete: attempting to create remote " + trashFolderName + " folder");
  	            remoteTrashFolder.create(FolderType.HOLDS_MESSAGES);
  	        }
  	
  	        if (remoteTrashFolder.exists()) {
  	        	if (Email.DEBUG)
  	        	{
  	        		Log.d(Email.LOG_TAG, "IMAPMessage.delete: copying remote message to " + trashFolderName);
  	        	}
  	          iFolder.copyMessages(new Message[] { this }, remoteTrashFolder);
  	          setFlag(Flag.DELETED, true);
  	          iFolder.expunge();
  	        }
  	        else
  	        {
  	          throw new MessagingException("IMAPMessage.delete: remote Trash folder " + trashFolderName + " does not exist and could not be created"
  	                  , true);
  	        }
        	}
        }
 
    }

    class ImapBodyPart extends MimeBodyPart {
        public ImapBodyPart() throws MessagingException {
            super();
        }

        public void setSize(int size) {
            this.mSize = size;
        }
    }

    class ImapException extends MessagingException {
        String mAlertText;

        public ImapException(String message, String alertText, Throwable throwable) {
            super(message, throwable);
            this.mAlertText = alertText;
        }

        public ImapException(String message, String alertText) {
            super(message);
            this.mAlertText = alertText;
        }

        public String getAlertText() {
            return mAlertText;
        }

        public void setAlertText(String alertText) {
            mAlertText = alertText;
        }
    }
    
    public static int MAX_DELAY_TIME = 240000;
    public static int NORMAL_DELAY_TIME = 10000;
    
    public class ImapFolderPusher extends ImapFolder implements UntaggedHandler
    {
        PushReceiver receiver = null;
        Thread listeningThread = null;
        AtomicBoolean stop = new AtomicBoolean(false);
        AtomicBoolean idling = new AtomicBoolean(false);
        AtomicBoolean doneSent = new AtomicBoolean(false);
        private int delayTime = NORMAL_DELAY_TIME;
        
        public ImapFolderPusher(ImapStore store, String name, PushReceiver nReceiver)
        {
            super(store, name);
            receiver = nReceiver;
        }
        public void refresh() throws IOException, MessagingException
        {
            if (idling.get())
            {
                receiver.pushInProgress();
                sendDone();
            }
        }
        
        private void sendDone() throws IOException, MessagingException
        {
            if (doneSent.compareAndSet(false, true) == true)
            {
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

        public void start() throws MessagingException
        {
            receiver.pushInProgress();
            Runnable runner = new Runnable()
            {
                public void run()
                {
                    Log.i(Email.LOG_TAG, "Pusher for " + getName() + " is running");
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
                                Log.i(Email.LOG_TAG, "Got oldUidNext " + oldUidNext);
                            }
                            catch (Exception e)
                            {
                                Log.e(Email.LOG_TAG, "Unable to get oldUidNext", e);
                            }
                           
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
                            mConnection.setReadTimeout(IDLE_READ_TIMEOUT);
                            
                            if (responses != null)
                            {
                                handleUntaggedResponses(responses);
                            }
                            if (uidNext > oldUidNext)
                            {
                                int startUid = oldUidNext;
                                if (startUid < uidNext - 100)
                                {
                                    startUid = uidNext - 100;
                                }
                                if (startUid < 1)
                                {
                                    startUid = 1;
                                }
                                
                                Log.i(Email.LOG_TAG, "Needs sync from uid " + startUid  + " to " + uidNext);
                                List<Message> messages = new ArrayList<Message>();
                                for (int uid = startUid; uid < uidNext; uid++ )
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
                                Log.i(Email.LOG_TAG, "About to IDLE " + getName());
                                
                                receiver.setPushActive(getName(), true);
                                idling.set(true);
                                doneSent.set(false);
                                executeSimpleCommand("IDLE", false, ImapFolderPusher.this);
                                idling.set(false);
                                receiver.setPushActive(getName(), false);
                                delayTime = NORMAL_DELAY_TIME;
                            }
                        } 
                        catch (Exception e)
                        {
                            idling.set(false);
                            receiver.setPushActive(getName(), false);
                            try
                            {
                                close(false);
                            }
                            catch (Exception me)
                            {
                                Log.e(Email.LOG_TAG, "Got exception while closing for exception", me);
                            }
                            if (stop.get() == true)
                            {
                                Log.i(Email.LOG_TAG, "Got exception while idling, but stop is set");
                            }
                            else
                            {
                                Log.e(Email.LOG_TAG, "Got exception while idling", e);
                                try
                                {
                                    Thread.sleep(delayTime);
                                    delayTime *= 2;
                                    if (delayTime > MAX_DELAY_TIME)
                                    {
                                        delayTime = MAX_DELAY_TIME;
                                    }
                                }
                                catch (Exception ie)
                                {
                                    Log.e(Email.LOG_TAG, "Got exception while delaying after push exception", ie);
                                }
                            }
                        }
                    }
                    try
                    {
                        close(false);
                        receiver.pushComplete();
                        Log.i(Email.LOG_TAG, "Pusher for " + getName() + " is exiting");
                    }
                    catch (Exception me)
                    {
                        Log.e(Email.LOG_TAG, "Got exception while closing", me);
                    }
                }
            };
            listeningThread = new Thread(runner);
            listeningThread.start();
        }

        List<Integer> flagSyncMsgSeqs = new ArrayList<Integer>();
        
        protected List<ImapResponse> handleUntaggedResponses(List<ImapResponse> responses) {
            flagSyncMsgSeqs.clear();
            int oldMessageCount = mMessageCount;

            super.handleUntaggedResponses(responses);

            List<Integer> flagSyncMsgSeqsCopy = new ArrayList<Integer>();
            flagSyncMsgSeqsCopy.addAll(flagSyncMsgSeqs);
            
            
            if (Email.DEBUG)
            {
                Log.d(Email.LOG_TAG, "oldMessageCount = " + oldMessageCount + ", new mMessageCount = " + mMessageCount);
            }
            if (oldMessageCount > 0 && mMessageCount > oldMessageCount)
            {
                syncMessages(oldMessageCount + 1, mMessageCount, true);
            }
            if (Email.DEBUG)
            {
                Log.d(Email.LOG_TAG, "There are " + flagSyncMsgSeqsCopy + " messages needing flag sync");
            }
            // TODO: Identify ranges and call syncMessages on said identified ranges
            for (Integer msgSeq : flagSyncMsgSeqsCopy)
            {
                syncMessages(msgSeq, msgSeq, false);
            }

            return responses;
        }
        
        private void syncMessages(int start, int end, boolean newArrivals)
        {
            try
            {
                Message[] messageArray = null;
 
                messageArray = getMessages(start, end, true, null);
                
                List<Message> messages = new ArrayList<Message>();
                for (Message message : messageArray)
                {
                    messages.add(message);
                }
                pushMessages(messages, newArrivals);
                
            }
            catch (Exception e)
            {
                receiver.pushError("Exception while processing Push untagged responses", e);
            }
        }
        
        protected void handleUntaggedResponse(ImapResponse response) {
            super.handleUntaggedResponse(response);
            if (response.mTag == null && response.size() > 1)
            {
                try
                {
                    Object responseType = response.get(1);
                    if ("FETCH".equals(responseType))
                    {
                        int msgSeq = response.getNumber(0);
                        if (Email.DEBUG)
                        {
                            Log.d(Email.LOG_TAG, "Got untagged FETCH for msgseq " + msgSeq);
                        }
                        flagSyncMsgSeqs.add(msgSeq);
                    }
                }
                catch (Exception e)
                {
                    Log.e(Email.LOG_TAG, "Could not handle untagged FETCH", e);
                }
            }
          }

        
        private void pushMessages(List<Message> messages, boolean newArrivals)
        {
            RuntimeException holdException = null;
            try
            {
                if (newArrivals)
                {
                    receiver.messagesArrived(getName(), messages);
                }
                else
                {
                    receiver.messagesFlagsChanged(getName(), messages);
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

        public void stop() throws MessagingException
        {
            stop.set(true);
            
            if (mConnection != null)
            {
                if (Email.DEBUG)
                {
                    Log.v(Email.LOG_TAG, "Closing mConnection to stop pushing");
                }
                mConnection.close();
            }
            else
            {
                Log.w(Email.LOG_TAG, "Attempt to interrupt null mConnection to stop pushing" + " on folderPusher " + getName());
            }
        }
        
        public void handleAsyncUntaggedResponse(ImapResponse response)
        {
            if (Email.DEBUG)
            {
                Log.v(Email.LOG_TAG, "Got async response: " + response);
            }
            if (response.mTag == null)
            {
                if (response.size() > 1)
                {
                    boolean started = false;
                    Object responseType = response.get(1);
                    if ("EXISTS".equals(responseType) || "EXPUNGE".equals(responseType) ||
                        "FETCH".equals(responseType))
                    {
                        if (started == false)
                        {
                            receiver.pushInProgress();
                            started = true;
                        }
                        if (Email.DEBUG)
                        {
                            Log.d(Email.LOG_TAG, "Got useful async untagged response: " + response);
                        }
                        try
                        {
                            sendDone();
                        }
                        catch (Exception e)
                        {
                            Log.e(Email.LOG_TAG, "Exception while sending DONE", e);
                        }
                    }
                }
                else if (response.size() > 0)
                {
                    if ("idling".equals(response.get(0)))
                    {
                        if (Email.DEBUG)
                        {
                            Log.d(Email.LOG_TAG, "Idling");
                        }
                        receiver.pushComplete();
                    }
                }
            }
        }
    }
    
    public class ImapPusher implements Pusher
    {
        List<ImapFolderPusher> folderPushers = new ArrayList<ImapFolderPusher>();
        
        public ImapPusher(ImapStore store, PushReceiver receiver, List<String> folderNames)
        {
            for (String folderName : folderNames)
            {
                ImapFolderPusher pusher = new ImapFolderPusher(store, folderName, receiver);
                folderPushers.add(pusher);
            }
        }

        public void refresh()
        {
            for (ImapFolderPusher folderPusher : folderPushers)
            {
                try
                {
                    folderPusher.refresh();
                }
                catch (Exception e)
                {
                    Log.e(Email.LOG_TAG, "Got exception while refreshing");
                }
            }
            
        }

        public void start()
        {
            for (ImapFolderPusher folderPusher : folderPushers)
            {
                try
                {
                    folderPusher.start();
                }
                catch (Exception e)
                {
                    Log.e(Email.LOG_TAG, "Got exception while starting");
                }
            }
        }

        public void stop()
        {
            Log.i(Email.LOG_TAG, "Requested stop of IMAP pusher");
            for (ImapFolderPusher folderPusher : folderPushers)
            {
                try
                {
                    Log.i(Email.LOG_TAG, "Requesting stop of IMAP folderPusher " + folderPusher.getName());
                    folderPusher.stop();
                }
                catch (Exception e)
                {
                    Log.e(Email.LOG_TAG, "Got exception while stopping", e);
                }
            }
            folderPushers.clear();
        }

        public int getRefreshInterval()
        {
            return IDLE_REFRESH_INTERVAL; 
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
                        
                        if ("uidNext".equals(key) && thisState.hasMoreTokens())
                        {
                            String value = thisState.nextToken();
                            try
                            {
                                newUidNext = Integer.parseInt(value);
                              //  Log.i(Email.LOG_TAG, "Parsed uidNext " + newUidNext);
                            }
                            catch (Exception e)
                            {
                                Log.e(Email.LOG_TAG, "Unable to part uidNext value " + value, e);
                            }
                            
                        }
                    }
                }
            }
            return new ImapPushState(newUidNext);
        }
        public String toString()
        {
            return "uidNext=" + uidNext;
        }
        
    }
}
