package com.fsck.k9.mail.store;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.ParserConfigurationException;

import com.fsck.k9.k9;
import com.fsck.k9.mail.FetchProfile;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessageRetrievalListener;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Store;
import com.fsck.k9.mail.internet.MimeBodyPart;
import com.fsck.k9.mail.internet.MimeMessage;

import org.apache.http.HttpResponse;
import org.apache.http.HttpEntity;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * <pre>
 * Uses WebDAV formatted HTTP calls to an MS Exchange server to fetch emails
 * and email information.  This has only been tested on an MS Exchange
 * Server 2003.  It uses Form-Based authentication and requires that
 * Outlook Web Access be enabled on the server.
 * </pre>
 */
public class WebDavStore extends Store {
    public static final int CONNECTION_SECURITY_NONE = 0;
    public static final int CONNECTION_SECURITY_TLS_OPTIONAL = 1;
    public static final int CONNECTION_SECURITY_TLS_REQUIRED = 2;
    public static final int CONNECTION_SECURITY_SSL_REQUIRED = 3;
    public static final int CONNECTION_SECURITY_SSL_OPTIONAL = 4;

    private static final Flag[] PERMANENT_FLAGS = { Flag.DELETED, Flag.SEEN, Flag.ANSWERED };
    
    private int mConnectionSecurity;
    private String mUsername; /* Stores the username for authentications */
    private String alias;
    private String mPassword; /* Stores the password for authentications */
    private String mUrl;      /* Stores the base URL for the server */

    private CookieStore mAuthCookies; /* Stores cookies from authentication */
    private boolean mAuthenticated = false; /* Stores authentication state */
    private long mLastAuth = -1; /* Stores the timestamp of last auth */
    private long mAuthTimeout = 5 * 60;
    
    /**
     * webdav://user:password@server:port CONNECTION_SECURITY_NONE
     * webdav+tls://user:password@server:port CONNECTION_SECURITY_TLS_OPTIONAL
     * webdav+tls+://user:password@server:port CONNECTION_SECURITY_TLS_REQUIRED
     * webdav+ssl+://user:password@server:port CONNECTION_SECURITY_SSL_REQUIRED
     * webdav+ssl://user:password@server:port CONNECTION_SECURITY_SSL_OPTIONAL
     *
     * @param _uri
     */
    public WebDavStore(String _uri) throws MessagingException {
        URI uri;

        try {
            Log.d(k9.LOG_TAG, ">>> New WebDavStore created");
            uri = new URI(_uri);
        } catch (URISyntaxException use) {
            Log.d(k9.LOG_TAG, ">>> Exception creating URI");
            throw new MessagingException("Invalid WebDavStore URI", use);
        }
        String scheme = uri.getScheme();
        if (scheme.equals("webdav")) {
            mConnectionSecurity = CONNECTION_SECURITY_NONE;
        } else if (scheme.equals("webdav+ssl")) {
            mConnectionSecurity = CONNECTION_SECURITY_SSL_OPTIONAL;
        } else if (scheme.equals("webdav+ssl+")) {
            mConnectionSecurity = CONNECTION_SECURITY_SSL_REQUIRED;
        } else if (scheme.equals("webdav+tls")) {
            mConnectionSecurity = CONNECTION_SECURITY_TLS_OPTIONAL;
        } else if (scheme.equals("webdav+tls+")) {
            mConnectionSecurity = CONNECTION_SECURITY_TLS_REQUIRED;
        } else {
            throw new MessagingException("Unsupported protocol");
        }

        String host = uri.getHost();

        if (host.startsWith("http")) {
            String[] hostParts = host.split("://", 2);
            if (hostParts.length > 1) {
                host = hostParts[1];
            }
        }

        if (mConnectionSecurity == CONNECTION_SECURITY_TLS_REQUIRED ||
            mConnectionSecurity == CONNECTION_SECURITY_SSL_REQUIRED ||
            mConnectionSecurity == CONNECTION_SECURITY_TLS_OPTIONAL ||
            mConnectionSecurity == CONNECTION_SECURITY_SSL_OPTIONAL) {
            this.mUrl = "https://" + host;
        } else {
            this.mUrl = "http://" + host;
        }
        
        if (uri.getUserInfo() != null) {
            String[] userInfoParts = uri.getUserInfo().split(":", 2);
            mUsername = userInfoParts[0];
            String userParts[] = mUsername.split("/", 2);

            if (userParts.length > 1) {
                alias = userParts[1];
            } else {
                alias = mUsername;
            }
            if (userInfoParts.length > 1) {
                mPassword = userInfoParts[1];
            }
        }
        Log.d(k9.LOG_TAG, ">>> New WebDavStore creation complete");
    }


    @Override
    public void checkSettings() throws MessagingException {
        Log.e(k9.LOG_TAG, "WebDavStore.checkSettings() not implemented");
    }

    @Override
    public Folder[] getPersonalNamespaces() throws MessagingException {
        Log.d(k9.LOG_TAG, ">>> getPersonalNamespaces called");
        ArrayList<Folder> folderList = new ArrayList<Folder>();
        DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpEntity responseEntity;
        HttpGeneric httpmethod;
        HttpResponse response;
        StringEntity messageEntity;
        String messageBody;
        int status_code;
        
        if (needAuth()) {
            authenticate();
        }

        if (this.mAuthenticated == false ||
            this.mAuthCookies == null) {
            return folderList.toArray(new Folder[] {});
        }

        try {
            /** Set up and execute the request */
            httpclient.setCookieStore(this.mAuthCookies);
            messageBody = getFolderListXml();
            messageEntity = new StringEntity(messageBody);
            messageEntity.setContentType("text/xml");
            
            httpmethod = new HttpGeneric(this.mUrl + "/Exchange/" + this.mUsername);
            httpmethod.setMethod("SEARCH");
            httpmethod.setEntity(messageEntity);
            httpmethod.setHeader("Brief", "t");

            response = httpclient.execute(httpmethod);
            status_code = response.getStatusLine().getStatusCode();

            if (status_code < 200 ||
                status_code > 300) {
                throw new IOException("Error getting folder listing");
            }

            responseEntity = response.getEntity();

            if (responseEntity != null) {
                /** Parse the returned data */
                try {
                    InputStream istream = responseEntity.getContent();
                    SAXParserFactory spf = SAXParserFactory.newInstance();
                    SAXParser sp = spf.newSAXParser();

                    XMLReader xr = sp.getXMLReader();

                    WebDavHandler myHandler = new WebDavHandler();
                    xr.setContentHandler(myHandler);

                    xr.parse(new InputSource(istream));

                    ParsedDataSet dataset = myHandler.getDataSet();

                    String[] folderUrls = dataset.getHrefs();
                    int urlLength = folderUrls.length;

                    for (int i = 0; i < urlLength; i++) {
                        String[] urlParts = folderUrls[i].split("/");
                        folderList.add(getFolder(java.net.URLDecoder.decode(urlParts[urlParts.length - 1], "UTF-8")));
                    }
                } catch (SAXException se) {
                    Log.e(k9.LOG_TAG, "Error with SAXParser " + se);
                } catch (ParserConfigurationException pce) {
                    Log.e(k9.LOG_TAG, "Error with SAXParser " + pce);
                }
            }
        } catch (UnsupportedEncodingException uee) {
            Log.e(k9.LOG_TAG, "Error with encoding " + uee);
        } catch (IOException ioe) {
            Log.e(k9.LOG_TAG, "IOException " + ioe);
        } 

        Log.d(k9.LOG_TAG, ">>> getPersonalNamespaces finished");
        return folderList.toArray(new WebDavFolder[] {});
    }

    @Override
    public Folder getFolder(String name) throws MessagingException {
        WebDavFolder folder;
        Log.d(k9.LOG_TAG, ">>> getFolder called");
        folder = new WebDavFolder(name);
        return folder;
    }

    /***************************************************************
     * WebDAV XML Request body retrieval functions
     */

    private String getFolderListXml() {
        StringBuffer buffer = new StringBuffer(200);
        buffer.append("<?xml version='1.0' ?>");
        buffer.append("<a:searchrequest xmlns:a='DAV:'><a:sql>\r\n");
        buffer.append("SELECT \"DAV:ishidden\"\r\n");
        buffer.append(" FROM \"\"\r\n");
        buffer.append(" WHERE \"DAV:ishidden\"=False AND \"DAV:isfolder\"=True\r\n");
        buffer.append("</a:sql></a:searchrequest>\r\n");

        return buffer.toString();
            
    }

    private String getMessageCountXml(String messageState) {
        StringBuffer buffer = new StringBuffer(200);
        buffer.append("<?xml version='1.0' ?>");
        buffer.append("<a:searchrequest xmlns:a='DAV:'><a:sql>\r\n");
        buffer.append("SELECT \"DAV:visiblecount\"\r\n");
        buffer.append(" FROM \"\"\r\n");
        buffer.append(" WHERE \"DAV:ishidden\"=False AND \"DAV:isfolder\"=False AND \"urn:schemas:httpmail:read\"="+messageState+"\r\n");
        buffer.append(" GROUP BY \"DAV:ishidden\"\r\n");
        buffer.append("</a:sql></a:searchrequest>\r\n");
        return buffer.toString();
    }

    private String getMessagesXml() {
        StringBuffer buffer = new StringBuffer(200);
        buffer.append("<?xml version='1.0' ?>");
        buffer.append("<a:searchrequest xmlns:a='DAV:'><a:sql>\r\n");
        buffer.append("SELECT \"DAV:uid\"\r\n");
        buffer.append(" FROM \"\"\r\n");
        buffer.append(" WHERE \"DAV:ishidden\"=False AND \"DAV:isfolder\"=False\r\n");
        buffer.append("</a:sql></a:searchrequest>\r\n");
        return buffer.toString();
    }

    private String getMessageUrlsXml(String[] uids) {
        StringBuffer buffer = new StringBuffer(600);
        buffer.append("<?xml version='1.0' ?>");
        buffer.append("<a:searchrequest xmlns:a='DAV:'><a:sql>\r\n");
        buffer.append("SELECT \"urn:schemas:httpmail:read\"\r\n");
        buffer.append(" FROM \"\"\r\n");
        buffer.append(" WHERE \"DAV:ishidden\"=False AND \"DAV:isfolder\"=False AND ");
        for (int i = 0, count = uids.length; i < count; i++) {
            if (i != 0) {
                buffer.append("  OR ");
            }

            buffer.append(" \"DAV:uid\"='"+uids[i]+"' ");

        }
        buffer.append("\r\n");
        buffer.append("</a:sql></a:searchrequest>\r\n");
        return buffer.toString();
    }
    
    private String getMessageFlagsXml(String[] uids) throws MessagingException {
        if (uids.length == 0) {
            Log.d(k9.LOG_TAG, ">>> 0 length array for uids in getMessageFlagsXml");
            throw new MessagingException("Attempt to get flags on 0 length array for uids");
        }
        
        StringBuffer buffer = new StringBuffer(200);
        buffer.append("<?xml version='1.0' ?>");
        buffer.append("<a:searchrequest xmlns:a='DAV:'><a:sql>\r\n");
        buffer.append("SELECT \"urn:schemas:httpmail:read\"\r\n");
        buffer.append(" FROM \"\"\r\n");
        buffer.append(" WHERE \"DAV:ishidden\"=False AND \"DAV:isfolder\"=False AND ");

        for (int i = 0, count = uids.length; i < count; i++) {
            if (i != 0) {
                buffer.append(" OR ");
            }
            buffer.append(" \"DAV:uid\"='"+uids[i]+"' ");
        }
        buffer.append("\r\n");
        buffer.append("</a:sql></a:searchrequest>\r\n");
        return buffer.toString();
    }

    private String getMarkMessagesReadXml(String[] urls) {
        Log.d(k9.LOG_TAG, ">>> Generating XML");
        StringBuffer buffer = new StringBuffer(600);
        buffer.append("<?xml version='1.0' ?>\r\n");
        buffer.append("<a:propertyupdate xmlns:a='DAV:' xmlns:b='urn:schemas:httpmail:'>\r\n");
        buffer.append("<a:target>\r\n");

        for (int i = 0, count = urls.length; i < count; i++) {
            buffer.append(" <a:href>"+urls[i].substring(urls[i].lastIndexOf('/') + 1)+"</a:href>\r\n");
        }
        
        buffer.append("</a:target>\r\n");
        buffer.append("<a:set>\r\n");
        buffer.append(" <a:prop>\r\n");
        buffer.append("  <b:read>1</b:read>\r\n");
        buffer.append(" </a:prop>\r\n");
        buffer.append("</a:set>\r\n");
        buffer.append("</a:propertyupdate>\r\n");
        return buffer.toString();
    }
    
    /***************************************************************
     * Authentication related methods
     */

    /**
     * Performs Form Based authentication regardless of the current
     * authentication state
     */
    public void authenticate() {
        Log.d(k9.LOG_TAG, ">>> authenticate() called");
        try {
            this.mAuthCookies = doAuthentication(this.mUsername, this.mPassword, this.mUrl);
        } catch (IOException ioe) {
            Log.e(k9.LOG_TAG, "Error during authentication: " + ioe);
            this.mAuthCookies = null;
        }
        
        if (this.mAuthCookies == null) {
            this.mAuthenticated = false;
        } else {
            this.mAuthenticated = true;
            this.mLastAuth = System.currentTimeMillis()/1000;
        }
        Log.d(k9.LOG_TAG, ">>> authenticate completed");
    }

    /**
     * Determines if a new authentication is needed.
     * Returns true if new authentication is needed.
     */
    public boolean needAuth() {
        Log.d(k9.LOG_TAG, ">>> needAuth called");
        boolean status = false;
        long currentTime = -1;
        if (this.mAuthenticated == false) {
            status = true;
        }
        
        currentTime = System.currentTimeMillis()/1000;
        if ((currentTime - this.mLastAuth) > (this.mAuthTimeout)) {
            status = true;
        }
        return status;
    }

    /**
     * Performs the Form Based Authentication
     * Returns the CookieStore object for later use or null
     */
    public CookieStore doAuthentication(String username, String password,
                                        String url) throws IOException {
        Log.d(k9.LOG_TAG, ">>> doAuthentication called");
        String authPath = "/exchweb/bin/auth/owaauth.dll";
        CookieStore cookies = null;
            
        /* Browser Client */
        DefaultHttpClient httpclient = new DefaultHttpClient();

        /* Post Method */
        HttpPost httppost = new HttpPost(url + authPath);

        /** Build the POST data to use */
        ArrayList<BasicNameValuePair> pairs = new ArrayList();
        pairs.add(new BasicNameValuePair("username", username));
        pairs.add(new BasicNameValuePair("password", password));
        pairs.add(new BasicNameValuePair("destination", url + "/Exchange/"));
        pairs.add(new BasicNameValuePair("flags", "0"));
        pairs.add(new BasicNameValuePair("SubmitCreds", "Log+On"));
        pairs.add(new BasicNameValuePair("forcedownlevel", "0"));
        pairs.add(new BasicNameValuePair("trusted", "0"));

        try {
            UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(pairs);

            httppost.setEntity(formEntity);

            /** Perform the actual POST */
            HttpResponse response = httpclient.execute(httppost);
            HttpEntity entity = response.getEntity();
            int status_code = response.getStatusLine().getStatusCode();
            
            /** Verify success */
            if (status_code > 300 ||
                status_code < 200) {
                throw new IOException("Error during authentication: "+status_code);
            }
            
            cookies = httpclient.getCookieStore();
            if (cookies == null) {
                throw new IOException("Error during authentication: No Cookies");
            }
        } catch (UnsupportedEncodingException uee) {
            Log.e(k9.LOG_TAG, "Error encoding POST data for authencation");
        }
        Log.d(k9.LOG_TAG, ">>> doAuthentication finished");
        return cookies;
    }

	public CookieStore getAuthCookies() {
		return mAuthCookies;
	}
	public String getAlias() {
		return alias;
	}
	public String getUrl() {
		return mUrl;
	}

    /*************************************************************************
     * Helper and Inner classes
     */

    /**
     * A WebDav Folder
     */
    class WebDavFolder extends Folder {
        private String mName;
        private String mLocalUsername;
        private String mFolderUrl;
        private boolean mIsOpen = false;
        private int mMessageCount = 0;
        private int mUnreadMessageCount = 0;
        
        public WebDavFolder(String name) {
            String[] userParts;
            String encodedName = new String();
            try {
                encodedName = java.net.URLEncoder.encode(name, "UTF-8");
            } catch (UnsupportedEncodingException uee) {
                Log.e(k9.LOG_TAG, "UnsupportedEncodingException URLEncoding folder name, skipping encoded");
                encodedName = name;
            }

            encodedName = encodedName.replaceAll("\\+", "%20");
            this.mName = name;
            userParts = WebDavStore.this.mUsername.split("/", 2);

            if (userParts.length > 1) {
                this.mLocalUsername = userParts[1];
            } else {
                this.mLocalUsername = WebDavStore.this.mUsername;
            }

            
            this.mFolderUrl = WebDavStore.this.mUrl + "/Exchange/" + this.mLocalUsername + "/" + encodedName;
        }

        @Override
        public void open(OpenMode mode) throws MessagingException {
            Log.d(k9.LOG_TAG, ">>> open called on folder "+this.mName);
            if (needAuth()) {
                authenticate();
            }

            if (WebDavStore.this.mAuthCookies == null) {
                return;
            }

            this.mIsOpen = true;
        }

        private int getMessageCount(boolean read, CookieStore authCookies) {
            Log.d(k9.LOG_TAG, ">>> getMessageCount called on folder "+this.mName);
            String isRead;
            int messageCount = 0;

            DefaultHttpClient httpclient = new DefaultHttpClient();
            HttpGeneric httpmethod;
            HttpResponse response;
            HttpEntity responseEntity;
            StringEntity bodyEntity;
            String messageBody;
            int statusCode;
            
            if (read) {
                isRead = new String("True");
            } else {
                isRead = new String("False");
            }

            httpclient.setCookieStore(authCookies);

            messageBody = getMessageCountXml(isRead);
            
            try {
                bodyEntity = new StringEntity(messageBody);
                bodyEntity.setContentType("text/xml");

                httpmethod = new HttpGeneric(this.mFolderUrl);
                httpmethod.setMethod("SEARCH");
                httpmethod.setEntity(bodyEntity);
                httpmethod.setHeader("Brief", "t");

                response = httpclient.execute(httpmethod);
                statusCode = response.getStatusLine().getStatusCode();

                if (statusCode < 200 ||
                    statusCode > 300) {
                    throw new IOException("Error getting message count, status code was " + statusCode);
                }

                responseEntity = response.getEntity();

                if (responseEntity != null) {
                    try {
                        ParsedDataSet dataset = new ParsedDataSet();
                        InputStream istream = responseEntity.getContent();
                        SAXParserFactory spf = SAXParserFactory.newInstance();
                        SAXParser sp = spf.newSAXParser();

                        XMLReader xr = sp.getXMLReader();
                        WebDavHandler myHandler = new WebDavHandler();
                        xr.setContentHandler(myHandler);

                        xr.parse(new InputSource(istream));

                        dataset = myHandler.getDataSet();
                        messageCount = dataset.getMessageCount();

                        istream.close();
                    } catch (SAXException se) {
                        Log.e(k9.LOG_TAG, "SAXException in getMessageCount " + se);
                    } catch (ParserConfigurationException pce) {
                        Log.e(k9.LOG_TAG, "ParserConfigurationException in getMessageCount " + pce);
                    }
                }
            } catch (UnsupportedEncodingException uee) {
                Log.e(k9.LOG_TAG, "UnsupportedEncodingException in getMessageCount() " + uee);
            } catch (IOException ioe) {
                Log.e(k9.LOG_TAG, "IOException in getMessageCount() " + ioe);
            }
            Log.d(k9.LOG_TAG, ">>> getMessageCount finished");
            return messageCount;
        }

        @Override
        public int getMessageCount() throws MessagingException {
            open(OpenMode.READ_WRITE);
            this.mMessageCount = getMessageCount(true, WebDavStore.this.mAuthCookies);

            return this.mMessageCount;
        }

        @Override
        public int getUnreadMessageCount() throws MessagingException {
            open(OpenMode.READ_WRITE);
            this.mUnreadMessageCount = getMessageCount(false, WebDavStore.this.mAuthCookies);

            return this.mUnreadMessageCount;
        }

        @Override
        public boolean isOpen() {
            return this.mIsOpen;
        }

        @Override
        public OpenMode getMode() throws MessagingException {
            return OpenMode.READ_WRITE;
        }

        @Override
        public String getName() {
            return this.mName;
        }

        @Override
        public boolean exists() {
            return true;
        }

        @Override
        public void close(boolean expunge) throws MessagingException {
            this.mMessageCount = 0;
            this.mUnreadMessageCount = 0;
                
            this.mIsOpen = false;
        }

        @Override
        public boolean create(FolderType type) throws MessagingException {
            return true;
        }

        @Override
        public void delete(boolean recursive) throws MessagingException {
            throw new Error("WebDavFolder.delete() not implemeneted");
        }

        @Override
        public Message getMessage(String uid) throws MessagingException {
            return new WebDavMessage(uid, this);
        }

        @Override
        public Message[] getMessages(int start, int end, MessageRetrievalListener listener)
                throws MessagingException {
            Log.d(k9.LOG_TAG, ">>> getMessages(int, int, MessageRetrievalListener) called on " + this.mName);
            DefaultHttpClient httpclient = new DefaultHttpClient();
            ArrayList<Message> messages = new ArrayList<Message>();
            String[] uids;
            String[] urls;

            String messageBody;
            int prevStart = start;

            /** Reverse the message range since 0 index is newest */
            start = this.mMessageCount - end;
            end = this.mMessageCount - prevStart;

            if (start < 0 || end < 0 || end < start) {
                throw new MessagingException(String.format("Invalid message set %d %d", start, end));
            }

            /** Verify authentication */
            if (needAuth()) {
                authenticate();
            }

            if (WebDavStore.this.mAuthenticated == false ||
                WebDavStore.this.mAuthCookies == null) {
                return messages.toArray(new Message[] {});
            }
            
            /** Retrieve and parse the XML entity for our messages */
            httpclient.setCookieStore(WebDavStore.this.mAuthCookies);
            messageBody = getMessagesXml();

            try {
                int status_code = -1;
                StringEntity messageEntity = new StringEntity(messageBody);
                HttpGeneric httpmethod = new HttpGeneric(this.mFolderUrl);
                HttpResponse response;
                HttpEntity entity;
                
                messageEntity.setContentType("text/xml");
                httpmethod.setMethod("SEARCH");
                httpmethod.setEntity(messageEntity);
                httpmethod.setHeader("Brief", "t");
                httpmethod.setHeader("Range", "rows=" + start + "-" + end);

                response = httpclient.execute(httpmethod);
                status_code = response.getStatusLine().getStatusCode();

                if (status_code < 200 ||
                    status_code > 300) {
                    throw new IOException("Error getting messages, returned HTTP Response code " + status_code);
                }

                entity = response.getEntity();

                if (entity != null) {
                    try {
                        InputStream istream = entity.getContent();
                        SAXParserFactory spf = SAXParserFactory.newInstance();
                        SAXParser sp = spf.newSAXParser();
                        XMLReader xr = sp.getXMLReader();
                        WebDavHandler myHandler = new WebDavHandler();
                        ParsedDataSet dataset;
                        int uidsLength = 0;
                        int urlsLength = 0;
                        
                        xr.setContentHandler(myHandler);
                        xr.parse(new InputSource(istream));

                        dataset = myHandler.getDataSet();

                        uids = dataset.getUids();
                        urls = dataset.getHrefs();
                        uidsLength = uids.length;
                        urlsLength = urls.length;

                        if (uidsLength != urlsLength) {
                            Log.d(k9.LOG_TAG, ">>> Mismatched results for UIDs and URLs in getMessages");
                            throw new MessagingException("Mismatched results for UIDs and URLs in getMessages");
                        }

                        for (int i = 0; i < uidsLength; i++) {
                            if (listener != null) {
                                listener.messageStarted(uids[i], i, uidsLength);
                            }
                            Log.d(k9.LOG_TAG, ">>> Adding message of UID " + uids[i] + " and URL of " + urls[i]);
                            WebDavMessage message = new WebDavMessage(uids[i], this);
                            message.setUrl(urls[i]);
                            messages.add(message);
                            
                            if (listener != null) {
                                listener.messageFinished(message, i, uidsLength);
                            }
                        }
                    } catch (SAXException se) {
                        Log.e(k9.LOG_TAG, "SAXException in getMessages() " + se);
                    } catch (ParserConfigurationException pce) {
                        Log.e(k9.LOG_TAG, "ParserConfigurationException in getMessages() " + pce);
                    }
                }
            } catch (UnsupportedEncodingException uee) {
                Log.e(k9.LOG_TAG, "UnsupportedEncodingException: " + uee);
            } catch (IOException ioe) {
                Log.e(k9.LOG_TAG, "IOException: " + ioe);
            }
            Log.d(k9.LOG_TAG, ">>> getMessages finished");
            return messages.toArray(new Message[] {});
        }

        @Override
        public Message[] getMessages(MessageRetrievalListener listener) throws MessagingException {
            Log.d(k9.LOG_TAG, ">>> getMessages(MessageRetrievalListener) called on "+this.mName);
            return getMessages(null, listener);
        }

        @Override
        public Message[] getMessages(String[] uids, MessageRetrievalListener listener) throws MessagingException {
            Log.d(k9.LOG_TAG, ">>> getMessages(String[], MessageRetrievalListener) called on "+this.mName);
            ArrayList<Message> messageList = new ArrayList<Message>();
            Message[] messages;
            
            if (uids == null) {
                messages = getMessages(0, k9.DEFAULT_VISIBLE_LIMIT, listener);
            } else {
                for (int i = 0, count = uids.length; i < count; i++) {
                    if (listener != null) {
                        listener.messageStarted(uids[i], i, count);
                    }

                    WebDavMessage message = new WebDavMessage(uids[i], this);
                    messageList.add(message);
                    
                    if (listener != null) {
                        listener.messageFinished(message, i, count);
                    }
                }
                messages = messageList.toArray(new Message[] {});
            }
            Log.d(k9.LOG_TAG, ">>> getMessages finished");
            return messages;
        }

        private String[] getMessageUrls(String[] uids) {
            Log.d(k9.LOG_TAG, ">>> getMessageUrls");
            ArrayList<String> urls = new ArrayList<String>();
            DefaultHttpClient httpclient = new DefaultHttpClient();
            String messageBody;

            /** Verify authentication */
            if (needAuth()) {
                authenticate();
            }

            if (WebDavStore.this.mAuthenticated == false ||
                WebDavStore.this.mAuthCookies == null) {
                return urls.toArray(new String[] {});
            }

            Log.d(k9.LOG_TAG, ">>> Auth passed");
            /** Retrieve and parse the XML entity for our messages */
            httpclient.setCookieStore(WebDavStore.this.mAuthCookies);
            messageBody = getMessageUrlsXml(uids);
            Log.d(k9.LOG_TAG, ">>> We have the message body, it was: " + messageBody);
            try {
                int status_code = -1;
                StringEntity messageEntity = new StringEntity(messageBody);
                HttpGeneric httpmethod = new HttpGeneric(this.mFolderUrl);
                HttpResponse response;
                HttpEntity entity;
                
                messageEntity.setContentType("text/xml");
                httpmethod.setMethod("SEARCH");
                httpmethod.setEntity(messageEntity);
                httpmethod.setHeader("Brief", "t");

                response = httpclient.execute(httpmethod);
                status_code = response.getStatusLine().getStatusCode();

                if (status_code < 200 ||
                    status_code > 300) {
                    throw new IOException("Error getting messages, returned HTTP Response code " + status_code);
                }

                entity = response.getEntity();

                if (entity != null) {
                    try {
                        InputStream istream = entity.getContent();
                        SAXParserFactory spf = SAXParserFactory.newInstance();
                        SAXParser sp = spf.newSAXParser();
                        XMLReader xr = sp.getXMLReader();
                        WebDavHandler myHandler = new WebDavHandler();
                        ParsedDataSet dataset;
                        int uidsLength = 0;
                        int urlsLength = 0;
                        
                        xr.setContentHandler(myHandler);
                        xr.parse(new InputSource(istream));

                        dataset = myHandler.getDataSet();
                        HashMap<String, String> uidToUrl = dataset.getUidToUrl();
                        for (int i = 0, count = uids.length; i < count; i++) {
                            urls.add(uidToUrl.get(uids[i]));
                        }
                        /**                        urls = dataset.getHrefs();*/
                    } catch (SAXException se) {
                        Log.e(k9.LOG_TAG, "SAXException in getMessages() " + se);
                    } catch (ParserConfigurationException pce) {
                        Log.e(k9.LOG_TAG, "ParserConfigurationException in getMessages() " + pce);
                    }
                }
            } catch (UnsupportedEncodingException uee) {
                Log.e(k9.LOG_TAG, "UnsupportedEncodingException: " + uee);
            } catch (IOException ioe) {
                Log.e(k9.LOG_TAG, "IOException: " + ioe);
            }

            return urls.toArray(new String[] {});
        }
        
        @Override
        public void fetch(Message[] messages, FetchProfile fp, MessageRetrievalListener listener)
                throws MessagingException {
            Log.d(k9.LOG_TAG, "Fetch called");
            Boolean[] readStatus = new Boolean[0];
            if (messages == null ||
                messages.length == 0) {
                return;
            }

            /**
             * Check for flags and get the status here since it can be pulled with
             * just one request.  Flags will be set inside the for loop.
             * Listener isn't started yet since it isn't a per-message lookup.
             */
            if (fp.contains(FetchProfile.Item.FLAGS)) {
                Log.d(k9.LOG_TAG, ">>> fetch message FLAGS");
                DefaultHttpClient httpclient = new DefaultHttpClient();
                String messageBody = new String();
                String[] uids = new String[messages.length];

                for (int i = 0, count = messages.length; i < count; i++) {
                    uids[i] = messages[i].getUid();
                    Log.d(k9.LOG_TAG, ">>> Adding message UID of " + uids[i]);
                }

                httpclient.setCookieStore(WebDavStore.this.mAuthCookies);
                messageBody = getMessageFlagsXml(uids);
                Log.d(k9.LOG_TAG, ">>> Message body was: \n"+messageBody);
                try {
                    int status_code = -1;
                    StringEntity messageEntity = new StringEntity(messageBody);
                    HttpGeneric httpmethod = new HttpGeneric(this.mFolderUrl);
                    HttpResponse response;
                    HttpEntity entity;
                
                    messageEntity.setContentType("text/xml");
                    httpmethod.setMethod("SEARCH");
                    httpmethod.setEntity(messageEntity);
                    httpmethod.setHeader("Brief", "t");

                    response = httpclient.execute(httpmethod);
                    status_code = response.getStatusLine().getStatusCode();

                    if (status_code < 200 ||
                        status_code > 300) {
                        throw new IOException("Error getting message flags, returned HTTP Response code " + status_code);
                    }

                    entity = response.getEntity();

                    if (entity != null) {
                        try {
                            InputStream istream = entity.getContent();
                            SAXParserFactory spf = SAXParserFactory.newInstance();
                            SAXParser sp = spf.newSAXParser();
                            XMLReader xr = sp.getXMLReader();
                            WebDavHandler myHandler = new WebDavHandler();
                            ParsedDataSet dataset;
                        
                            xr.setContentHandler(myHandler);
                            xr.parse(new InputSource(istream));

                            dataset = myHandler.getDataSet();
                            readStatus = dataset.getReadArray();
                        } catch (SAXException se) {
                            Log.e(k9.LOG_TAG, "SAXException in fetch() " + se);
                        } catch (ParserConfigurationException pce) {
                            Log.e(k9.LOG_TAG, "ParserConfigurationException in fetch() " + pce);
                        }
                    }
                } catch (UnsupportedEncodingException uee) {
                    Log.e(k9.LOG_TAG, "UnsupportedEncodingException: " + uee);
                } catch (IOException ioe) {
                    Log.e(k9.LOG_TAG, "IOException: " + ioe);
                }
                /**
                if (readStatus.length != uids.length) {
                    Log.d(k9.LOG_TAG, "Uids and readstatus were wrong lengths, uids: "+uids.length+" readStatus "+readStatus.length);
                    throw new MessagingException("WebdavStore fetch for flags called and mismatched results were received");
                    }*/
            }
            
            for (int i = 0, count = messages.length; i < count; i++) {
                if (!(messages[i] instanceof WebDavMessage)) {
                    throw new MessagingException("WebDavStore fetch called with non-WebDavMessage");
                }
                WebDavMessage wdMessage = (WebDavMessage) messages[i];
                
                if (listener != null) {
                    Log.d(k9.LOG_TAG, ">>> Starting listener");
                    listener.messageStarted(wdMessage.getUid(), i, count);
                }

                if (fp.contains(FetchProfile.Item.FLAGS)) {
                    wdMessage.setFlagInternal(Flag.SEEN, readStatus[i]);
                }
                
                /**
                 * Message fetching that we can pull as a stream
                 */
                if (fp.contains(FetchProfile.Item.ENVELOPE) ||
                    fp.contains(FetchProfile.Item.BODY) ||
                    fp.contains(FetchProfile.Item.BODY_SANE)) {

                    DefaultHttpClient httpclient = new DefaultHttpClient();
                    InputStream istream = null;
                    InputStream resultStream = null;
                    HttpGet httpget;
                    HttpEntity entity;
                    HttpResponse response;
                    int statusCode = 0;

                    try {
                        httpclient.setCookieStore(WebDavStore.this.mAuthCookies);
                        httpget = new HttpGet(new URI(wdMessage.getUrl()));
                        httpget.setHeader("translate", "f");

                        response = httpclient.execute(httpget);
                        statusCode = response.getStatusLine().getStatusCode();

                        if (statusCode < 200 ||
                            statusCode > 300) {
                            throw new IOException("Status Code in invalid range");
                        }

                        entity = response.getEntity();
                        if (entity != null) {
                            String resultText = new String();
                            String tempText = new String();
                            BufferedReader reader;

                            resultText = "";
                            istream = entity.getContent();
                            if (fp.contains(FetchProfile.Item.BODY)) {

                            } else if (fp.contains(FetchProfile.Item.ENVELOPE)) {
                                reader = new BufferedReader(new InputStreamReader(istream), 4096);

                                while ((tempText = reader.readLine()) != null &&
                                       !(tempText.equals(""))) {
                                    if (resultText.equals("")) {
                                        resultText = tempText;
                                    } else {
                                        resultText = resultText + "\r\n" + tempText;
                                    }
                                }

                                istream.close();
                                istream = new ByteArrayInputStream(resultText.getBytes("UTF-8"));
                            }

                            wdMessage.parse(istream);
                        }
                
                    } catch (IllegalArgumentException iae) {
                        Log.e(k9.LOG_TAG, "IllegalArgumentException caught " + iae);
                    } catch (URISyntaxException use) {
                        Log.e(k9.LOG_TAG, "URISyntaxException caught " + use);
                    } catch (IOException ioe) {
                        Log.e(k9.LOG_TAG, "Non-success response code loading message, response code was " + statusCode);
                    }
                }

                if (listener != null) {
                    Log.e(k9.LOG_TAG, "Messages fetched and parsed, setting finished for this one");
                    listener.messageFinished(wdMessage, i, count);
                }
            }
        }

        @Override
        public Flag[] getPermanentFlags() throws MessagingException {
            return PERMANENT_FLAGS;
        }

        @Override
        public void setFlags(Message[] messages, Flag[] flags, boolean value)
                throws MessagingException {
            Log.d(k9.LOG_TAG, ">>> setFlags called");
            String[] uids = new String[messages.length];
            
            if (needAuth()) {
                Log.d(k9.LOG_TAG, ">>> needAuth was true");
                authenticate();
            }

            if (WebDavStore.this.mAuthenticated == false ||
                WebDavStore.this.mAuthCookies == null) {
                Log.d(k9.LOG_TAG, ">>> For some reason we aren't authenticated");
                return;
            }

            for (int i = 0, count = messages.length; i < count; i++) {
                uids[i] = messages[i].getUid();
            }

            for (int i = 0, count = flags.length; i < count; i++) {
                Flag flag = flags[i];

                if (flag == Flag.SEEN) {
                    markServerMessagesRead(uids);
                } else if (flag == Flag.DELETED) {
                    deleteServerMessages(uids);
                }
            }
        }

        private void markServerMessagesRead(String[] uids) throws MessagingException {
            DefaultHttpClient httpclient = new DefaultHttpClient();
            String messageBody = new String();
            String[] urls = getMessageUrls(uids);
            Log.d(k9.LOG_TAG, ">>> Setting messages as read");

            httpclient.setCookieStore(WebDavStore.this.mAuthCookies);

            messageBody = getMarkMessagesReadXml(urls);

            try {
                int status_code = -1;
                StringEntity messageEntity = new StringEntity(messageBody);
                HttpGeneric httpmethod = new HttpGeneric(this.mFolderUrl + "/");
                HttpResponse response;
                HttpEntity entity;
                
                messageEntity.setContentType("text/xml");
                httpmethod.setMethod("BPROPPATCH");
                httpmethod.setEntity(messageEntity);
                httpmethod.setHeader("Brief", "t");
                httpmethod.setHeader("If-Match", "*");

                response = httpclient.execute(httpmethod);
                status_code = response.getStatusLine().getStatusCode();

                if (status_code < 200 ||
                    status_code > 300) {
                    throw new IOException("Error marking messages as read, returned HTTP Response code " + status_code);
                }

                Log.d(k9.LOG_TAG, "Success, respones code was: " + status_code);
                entity = response.getEntity();
            } catch (UnsupportedEncodingException uee) {
                Log.e(k9.LOG_TAG, "UnsupportedEncodingException: " + uee);
            } catch (IOException ioe) {
                Log.e(k9.LOG_TAG, "IOException: " + ioe);
            }
            Log.d(k9.LOG_TAG, "Message marked as read\n");
        }

        private void deleteServerMessages(String[] uids) throws MessagingException {
            DefaultHttpClient httpclient = new DefaultHttpClient();
            String[] urls = getMessageUrls(uids);

            Log.d(k9.LOG_TAG, ">>> deleteServerMessages");

            httpclient.setCookieStore(WebDavStore.this.mAuthCookies);
            
            for (int i = 0, count = urls.length; i < count; i++) {
                try {
                    int status_code = -1;
                    HttpGeneric httpmethod = new HttpGeneric(urls[i]);
                    HttpResponse response;
                    HttpEntity entity;

                    httpmethod.setMethod("DELETE");
                    httpmethod.setHeader("Brief", "t");

                    response = httpclient.execute(httpmethod);
                    status_code = response.getStatusLine().getStatusCode();

                    if (status_code < 200 ||
                        status_code > 300) {
                        throw new IOException("Error deleting message url: "+urls[i]+" \nResponse Code: "+status_code);
                    }
                } catch (UnsupportedEncodingException uee) {
                    Log.e(k9.LOG_TAG, "UnsupportedEncodingException: " + uee);
                } catch (IOException ioe) {
                    Log.e(k9.LOG_TAG, "IOException: " + ioe);
                }
                Log.d(k9.LOG_TAG, "Message deleted");
            }
        }
        
        @Override
        public void appendMessages(Message[] messages) throws MessagingException {
            appendMessages(messages, false);
        }

        public void appendMessages(Message[] messages, boolean copy) throws MessagingException {

        }

        @Override
        public void copyMessages(Message[] msgs, Folder folder) throws MessagingException {

        }

        @Override
        public Message[] expunge() throws MessagingException {
            return null;
        }

        @Override
        public boolean equals(Object o) {
            return false;
        }
    }
    
    /**
     * A WebDav Message
     */
    class WebDavMessage extends MimeMessage {
        private String mUrl = null;
        
        WebDavMessage(String uid, Folder folder) throws MessagingException {
            Log.d(k9.LOG_TAG, ">>> WebDavMessage created with uid " + uid);
            this.mUid = uid;
            this.mFolder = folder;
        }

        public void setUrl(String url) {
            Log.d(k9.LOG_TAG, ">>> WDM.setUrl called with URL " +url);
            String[] urlParts = url.split("/");
            int length = urlParts.length;
            String end = urlParts[length - 1];
            
            this.mUrl = new String();
            url = new String();

            /**
             * We have to decode, then encode the URL because Exchange likes to
             * not properly encode all characters
             */
            try {
                end = java.net.URLDecoder.decode(end, "UTF-8");
                end = java.net.URLEncoder.encode(end, "UTF-8");
                end = end.replaceAll("\\+", "%20");
            } catch (UnsupportedEncodingException uee) {
                Log.e(k9.LOG_TAG, "UnsupportedEncodingException caught in setUrl");
            } catch (IllegalArgumentException iae) {
                Log.e(k9.LOG_TAG, "IllegalArgumentException caught in setUrl");
            }

            for (int i = 0; i < length - 1; i++) {
                if (i != 0) {
                    url = url + "/" + urlParts[i];
                } else {
                    url = urlParts[i];
                }
            }

            url = url + "/" + end;
            Log.d(k9.LOG_TAG, ">>> Url is: " + url);
            this.mUrl = url;
        }

        public String getUrl() {
            return this.mUrl;
        }
        
        public void setSize(int size) {
            this.mSize = size;
        }

        public void parse(InputStream in) throws IOException, MessagingException {
            Log.d(k9.LOG_TAG, ">>> parse called on webdavmessage");
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
    }
    
    /** 
     * XML Parsing Handler
     * Can handle all XML handling needs
     */
    public class WebDavHandler extends DefaultHandler {
        private ParsedDataSet mDataSet = new ParsedDataSet();
        private Stack<String> mOpenTags = new Stack<String>();
        
        public ParsedDataSet getDataSet() {
            Log.d(k9.LOG_TAG, ">>> WDH.getDataSet called");
            return this.mDataSet;
        }

        @Override
        public void startDocument() throws SAXException {
            Log.d(k9.LOG_TAG, ">>> WDH.startDocument() called");
            this.mDataSet = new ParsedDataSet();
        }

        @Override
        public void endDocument() throws SAXException {
            /* Do nothing */
        }

        @Override
        public void startElement(String namespaceURI, String localName,
                                 String qName, Attributes atts) throws SAXException {
            Log.d(k9.LOG_TAG, ">>> Pushing localName of " + localName + " onto stack");
            mOpenTags.push(localName);
        }

        @Override
        public void endElement(String namespaceURI, String localName, String qName) {
            Log.d(k9.LOG_TAG, ">>> Popping localName of " + localName + " off of stack");
            mOpenTags.pop();

            /** Reset the hash temp variables */
            if (localName.equals("response")) {
                this.mDataSet.clearTempData();
            }
        }

        @Override
        public void characters(char ch[], int start, int length) {
            String value = new String(ch, start, length);
            Log.d(k9.LOG_TAG, ">>> Calling addValue with values of " + value + ", "+mOpenTags.peek());
            mDataSet.addValue(value, mOpenTags.peek());
        }
    }

    /**
     * Data set for handling all XML Parses
     */
    public class ParsedDataSet {
        private ArrayList<String> mHrefs = new ArrayList<String>();
        private ArrayList<String> mUids = new ArrayList<String>();
        private ArrayList<Boolean> mReads = new ArrayList<Boolean>();
        private HashMap<String, String> mUidUrls = new HashMap<String, String>();
        private HashMap<String, Boolean> mUidRead = new HashMap<String, Boolean>();
        private int mMessageCount = 0;
        private String mTempUid = "";
        private String mTempUrl = "";
        private Boolean mTempRead;
        private boolean mRead;

        public void addValue(String value, String tagName) {
            Log.d(k9.LOG_TAG, ">>> addValue called with values of "+value+", "+tagName);
            if (tagName.equals("href")) {
                this.mHrefs.add(value);
                this.mTempUrl = value;
                if (!this.mTempUid.equals("")) {
                    mUidUrls.put(this.mTempUid, this.mTempUrl);
                }
            } else if (tagName.equals("visiblecount")) {
                this.mMessageCount = new Integer(value).intValue();
                Log.d(k9.LOG_TAG, ">>> Weird, value is " + value + " and messagecount is " + this.mMessageCount);
            } else if (tagName.equals("uid")) {
                this.mUids.add(value);
                this.mTempUid = value;
            } else if (tagName.equals("read")) {
                if (value.equals("0")) {
                    this.mReads.add(false);
                    if (!this.mTempUid.equals("")) {
                        this.mUidRead.put(this.mTempUid, false);
                    }
                } else {
                    this.mReads.add(true);
                    if (!this.mTempUid.equals("")) {
                        this.mUidRead.put(this.mTempUid, true);
                    }
                }
            }
            Log.d(k9.LOG_TAG, ">>> mMessageCount is now " + this.mMessageCount);
        }

        /**
         * Clears the temp variables
         */
        public void clearTempData() {
            this.mTempUid = "";
            this.mTempUrl = "";
        }

        /**
         * Returns the Uid to Url hashmap
         */
        public HashMap getUidToUrl() {
            return this.mUidUrls;
        }

        /**
         * Returns the Uid to Read hashmap
         */
        public HashMap getUidToRead() {
            return this.mUidRead;
        }
        
        /**
         * Get all stored Hrefs
         */
        public String[] getHrefs() {
            Log.d(k9.LOG_TAG, ">>> PDS.getHrefs called");
            return this.mHrefs.toArray(new String[] {});
        }

        /**
         * Get the first stored Href
         */
        public String getHref() {
            Log.d(k9.LOG_TAG, ">>> PDS.getHref called");
            String[] hrefs = this.mHrefs.toArray(new String[] {});
            return hrefs[0];
        }
        
        /**
         * Get all stored Uids
         */
        public String[] getUids() {
            Log.d(k9.LOG_TAG, ">>> PDS.getUids called");
            return this.mUids.toArray(new String[] {});
        }

        /**
         * Get the first stored Uid
         */
        public String getUid() {
            Log.d(k9.LOG_TAG, ">>> PDS.getUid called");
            String[] uids = this.mUids.toArray(new String[] {});
            return uids[0];
        }
        
        /**
         * Get message count
         */
        public int getMessageCount() {
            Log.d(k9.LOG_TAG, ">>> PDS.getMessageCount called, returning value " + this.mMessageCount);
            
            return this.mMessageCount;
        }

        /**
         * Get all stored read statuses
         */
        public Boolean[] getReadArray() {
            Log.d(k9.LOG_TAG, ">>> PDS.getReadArray called");
            Boolean[] readStatus = this.mReads.toArray(new Boolean[] {});
            return readStatus;
        }
        
        /**
         * Get the first stored read status
         */
        public boolean getRead() {
            Log.d(k9.LOG_TAG, ">>> PDS.getRead called");
            return this.mRead;
        }
    }
    
    /**
     * New HTTP Method that allows changing of the method and generic handling
     * Needed for WebDAV custom methods such as SEARCH and PROPFIND
     */
    public class HttpGeneric extends HttpEntityEnclosingRequestBase {
        public String METHOD_NAME = "POST";

        public HttpGeneric() {
            super();
        }

        public HttpGeneric(final URI uri) {
            super();
            setURI(uri);
        }

        /**
         * @throws IllegalArgumentException if the uri is invalid. 
         */
        public HttpGeneric(final String uri) {
            super();
            setURI(URI.create(uri));
        }

        @Override
        public String getMethod() {
            return METHOD_NAME;
        }

        public void setMethod(String method) {
            if (method != null) {
                METHOD_NAME = method;
            }
        }
    }
}