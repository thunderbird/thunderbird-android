package com.android.email.mail.store;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Stack;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.ParserConfigurationException;

import com.android.email.Email;
import com.android.email.mail.FetchProfile;
import com.android.email.mail.Flag;
import com.android.email.mail.Folder;
import com.android.email.mail.Message;
import com.android.email.mail.MessageRetrievalListener;
import com.android.email.mail.MessagingException;
import com.android.email.mail.Store;
import com.android.email.mail.internet.MimeBodyPart;
import com.android.email.mail.internet.MimeMessage;
import com.android.email.mail.internet.TextBody;

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

    private HashMap<String, WebDavFolder> mFolderList = new HashMap<String, WebDavFolder>();
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
            uri = new URI(_uri);
        } catch (URISyntaxException use) {
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
    }


    @Override
    public void checkSettings() throws MessagingException {
        Log.e(Email.LOG_TAG, "WebDavStore.checkSettings() not implemented");
    }

    @Override
    public Folder[] getPersonalNamespaces() throws MessagingException {
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
            
            httpmethod = new HttpGeneric(this.mUrl);// + "/Exchange/" + this.mUsername);
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
                        String folderName = urlParts[urlParts.length - 1];
                        String fullPathName = "";
                        WebDavFolder wdFolder;
                        
                        if (folderName.equalsIgnoreCase(Email.INBOX)) {
                            folderName = "INBOX";
                        } else {
                            for (int j = 5, count = urlParts.length; j < count; j++) {
                                if (j != 5) {
                                    fullPathName = fullPathName + "/" + urlParts[j];
                                } else {
                                    fullPathName = urlParts[j];
                                }
                            }
                            folderName = java.net.URLDecoder.decode(fullPathName, "UTF-8");
                        }

                        wdFolder = new WebDavFolder(folderName);
                        wdFolder.setUrl(folderUrls[i]);
                        folderList.add(wdFolder);
                        this.mFolderList.put(folderName, wdFolder);
                        //folderList.add(getFolder(java.net.URLDecoder.decode(folderName, "UTF-8")));
                    }
                } catch (SAXException se) {
                    Log.e(Email.LOG_TAG, "Error with SAXParser " + se);
                } catch (ParserConfigurationException pce) {
                    Log.e(Email.LOG_TAG, "Error with SAXParser " + pce);
                }
            }
        } catch (UnsupportedEncodingException uee) {
            Log.e(Email.LOG_TAG, "Error with encoding " + uee);
        } catch (IOException ioe) {
            Log.e(Email.LOG_TAG, "IOException " + ioe);
        } 

        return folderList.toArray(new WebDavFolder[] {});
    }

    @Override
    public Folder getFolder(String name) throws MessagingException {
        WebDavFolder folder;

        if ((folder = this.mFolderList.get(name)) == null) {
            folder = new WebDavFolder(name);
        }

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
        //        buffer.append(" FROM \"\"\r\n");
        buffer.append(" FROM SCOPE('deep traversal of \""+this.mUrl+"\"')\r\n");
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

    private String getMessageEnvelopeXml(String[] uids) {
        StringBuffer buffer = new StringBuffer(200);
        buffer.append("<?xml version='1.0' ?>");
        buffer.append("<a:searchrequest xmlns:a='DAV:'><a:sql>\r\n");
        buffer.append("SELECT \"DAV:uid\", \"DAV:getcontentlength\",");
        buffer.append(" \"urn:schemas:mailheader:received\",");
        buffer.append(" \"urn:schemas:mailheader:mime-version\",");
        buffer.append(" \"urn:schemas:mailheader:content-type\",");
        buffer.append(" \"urn:schemas:mailheader:subject\",");
        buffer.append(" \"urn:schemas:mailheader:date\",");
        buffer.append(" \"urn:schemas:mailheader:thread-topic\",");
        buffer.append(" \"urn:schemas:mailheader:thread-index\",");
        buffer.append(" \"urn:schemas:mailheader:from\",");
        buffer.append(" \"urn:schemas:mailheader:to\",");
        buffer.append(" \"urn:schemas:mailheader:in-reply-to\",");
        buffer.append(" \"urn:schemas:mailheader:return-path\",");
        buffer.append(" \"urn:schemas:mailheader:cc\",");
        buffer.append(" \"urn:schemas:mailheader:references\",");
        buffer.append(" \"urn:schemas:httpmail:read\"");
        buffer.append(" \r\n");
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
        buffer.append("SELECT \"urn:schemas:httpmail:read\", \"DAV:uid\"\r\n");
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
            throw new MessagingException("Attempt to get flags on 0 length array for uids");
        }
        
        StringBuffer buffer = new StringBuffer(200);
        buffer.append("<?xml version='1.0' ?>");
        buffer.append("<a:searchrequest xmlns:a='DAV:'><a:sql>\r\n");
        buffer.append("SELECT \"urn:schemas:httpmail:read\", \"DAV:uid\"\r\n");
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
        try {
            this.mAuthCookies = doAuthentication(this.mUsername, this.mPassword, this.mUrl);
        } catch (IOException ioe) {
            Log.e(Email.LOG_TAG, "Error during authentication: " + ioe);
            this.mAuthCookies = null;
        }
        
        if (this.mAuthCookies == null) {
            this.mAuthenticated = false;
        } else {
            this.mAuthenticated = true;
            this.mLastAuth = System.currentTimeMillis()/1000;
        }
    }

    /**
     * Determines if a new authentication is needed.
     * Returns true if new authentication is needed.
     */
    public boolean needAuth() {
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
        String authPath = "/exchweb/bin/auth/owaauth.dll";
        CookieStore cookies = null;
        String[] urlParts = url.split("/");
        String finalUrl = "";

        for (int i = 0; i <= 2; i++) {
            if (i != 0) {
                finalUrl = finalUrl + "/" + urlParts[i];
            } else {
                finalUrl = urlParts[i];
            }
        }
            
        /* Browser Client */
        DefaultHttpClient httpclient = new DefaultHttpClient();

        /* Post Method */
        HttpPost httppost = new HttpPost(finalUrl + authPath);

        /** Build the POST data to use */
        ArrayList<BasicNameValuePair> pairs = new ArrayList();
        pairs.add(new BasicNameValuePair("username", username));
        pairs.add(new BasicNameValuePair("password", password));
        pairs.add(new BasicNameValuePair("destination", finalUrl + "/Exchange/"));
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

            /** Get the URL for the mailbox and set it for the store */
            if (entity != null) {
                InputStream istream = entity.getContent();

                BufferedReader reader = new BufferedReader(new InputStreamReader(istream), 8192);
                String tempText = "";

                while ((tempText = reader.readLine()) != null) {
                    if (tempText.indexOf("BASE href") >= 0) {
                        String[] tagParts = tempText.split("\"");
                        this.mUrl = tagParts[1];
                    }
                }
            }
            
        } catch (UnsupportedEncodingException uee) {
            Log.e(Email.LOG_TAG, "Error encoding POST data for authencation");
        }
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
                Log.e(Email.LOG_TAG, "UnsupportedEncodingException URLEncoding folder name, skipping encoded");
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

            
            //this.mFolderUrl = WebDavStore.this.mUrl + "/Exchange/" + this.mLocalUsername + "/" + encodedName;
            this.mFolderUrl = WebDavStore.this.mUrl + encodedName;
        }

        public void setUrl(String url) {
            if (url != null) {
                this.mFolderUrl = url;
            }
        }

        @Override
        public void open(OpenMode mode) throws MessagingException {
            if (needAuth()) {
                authenticate();
            }

            if (WebDavStore.this.mAuthCookies == null) {
                return;
            }

            this.mIsOpen = true;
        }

        private int getMessageCount(boolean read, CookieStore authCookies) {
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
                        Log.e(Email.LOG_TAG, "SAXException in getMessageCount " + se);
                    } catch (ParserConfigurationException pce) {
                        Log.e(Email.LOG_TAG, "ParserConfigurationException in getMessageCount " + pce);
                    }
                }
            } catch (UnsupportedEncodingException uee) {
                Log.e(Email.LOG_TAG, "UnsupportedEncodingException in getMessageCount() " + uee);
            } catch (IOException ioe) {
                Log.e(Email.LOG_TAG, "IOException in getMessageCount() " + ioe);
            }
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
            DefaultHttpClient httpclient = new DefaultHttpClient();
            ArrayList<Message> messages = new ArrayList<Message>();
            String[] uids;

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
                        HashMap<String, String> uidToUrl = dataset.getUidToUrl();
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
                    } catch (SAXException se) {
                        Log.e(Email.LOG_TAG, "SAXException in getMessages() " + se);
                    } catch (ParserConfigurationException pce) {
                        Log.e(Email.LOG_TAG, "ParserConfigurationException in getMessages() " + pce);
                    }
                }
            } catch (UnsupportedEncodingException uee) {
                Log.e(Email.LOG_TAG, "UnsupportedEncodingException: " + uee);
            } catch (IOException ioe) {
                Log.e(Email.LOG_TAG, "IOException: " + ioe);
            }

            return messages.toArray(new Message[] {});
        }

        @Override
        public Message[] getMessages(MessageRetrievalListener listener) throws MessagingException {
            return getMessages(null, listener);
        }

        @Override
        public Message[] getMessages(String[] uids, MessageRetrievalListener listener) throws MessagingException {
            ArrayList<Message> messageList = new ArrayList<Message>();
            Message[] messages;

            if (uids == null ||
                uids.length == 0) {
                return messageList.toArray(new Message[] {});
            }
            
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

            return messages;
        }

        private HashMap<String, String> getMessageUrls(String[] uids) {
            HashMap<String, String> uidToUrl = new HashMap<String, String>();
            DefaultHttpClient httpclient = new DefaultHttpClient();
            String messageBody;

            /** Verify authentication */
            if (needAuth()) {
                authenticate();
            }

            if (WebDavStore.this.mAuthenticated == false ||
                WebDavStore.this.mAuthCookies == null) {
                return uidToUrl;
            }

            /** Retrieve and parse the XML entity for our messages */
            httpclient.setCookieStore(WebDavStore.this.mAuthCookies);
            messageBody = getMessageUrlsXml(uids);

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
                        uidToUrl = dataset.getUidToUrl();
                    } catch (SAXException se) {
                        Log.e(Email.LOG_TAG, "SAXException in getMessages() " + se);
                    } catch (ParserConfigurationException pce) {
                        Log.e(Email.LOG_TAG, "ParserConfigurationException in getMessages() " + pce);
                    }
                }
            } catch (UnsupportedEncodingException uee) {
                Log.e(Email.LOG_TAG, "UnsupportedEncodingException: " + uee);
            } catch (IOException ioe) {
                Log.e(Email.LOG_TAG, "IOException: " + ioe);
            }

            return uidToUrl;
        }
        
        @Override
        public void fetch(Message[] messages, FetchProfile fp, MessageRetrievalListener listener)
                throws MessagingException {
            HashMap<String, Boolean> uidToReadStatus = new HashMap<String, Boolean>();

            if (messages == null ||
                messages.length == 0) {
                return;
            }

            if (needAuth()) {
                authenticate();
            }

            if (WebDavStore.this.mAuthenticated == false ||
                WebDavStore.this.mAuthCookies == null) {
                return;
            }

            /**
             * Fetch message flag info for the array
             */
            if (fp.contains(FetchProfile.Item.FLAGS)) {
                fetchFlags(messages, listener);
            }

            /**
             * Fetch message envelope information for the array
             */
            if (fp.contains(FetchProfile.Item.ENVELOPE)) {
                fetchEnvelope(messages, listener);
            }

            for (int i = 0, count = messages.length; i < count; i++) {
                if (!(messages[i] instanceof WebDavMessage)) {
                    throw new MessagingException("WebDavStore fetch called with non-WebDavMessage");
                }
                WebDavMessage wdMessage = (WebDavMessage) messages[i];

                if (listener != null) {
                    listener.messageStarted(wdMessage.getUid(), i, count);
                }

                /**
                 * Set the body to null if it's asking for the structure because
                 * we don't support it yet.
                 */
                if (fp.contains(FetchProfile.Item.STRUCTURE)) {
                    wdMessage.setBody(null);
                }

                /**
                 * Message fetching that we can pull as a stream
                 */
                if (fp.contains(FetchProfile.Item.BODY) ||
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

                        /**
                         * If fetch is called outside of the initial list (ie, a locally stored
                         * stored message), it may not have a URL associated.  Verify and fix that
                         */
                        if (wdMessage.getUrl().equals("")) {
                            wdMessage.setUrl(getMessageUrls(new String[] {wdMessage.getUid()}).get(wdMessage.getUid()));
                            if (wdMessage.getUrl().equals("")) {
                                throw new MessagingException("Unable to get URL for message");
                            }
                        }

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
                            StringBuffer buffer = new StringBuffer();
                            String tempText = new String();
                            String resultText = new String();
                            String bodyBoundary = "";
                            BufferedReader reader;
                            int totalLines = FETCH_BODY_SANE_SUGGESTED_SIZE / 76;
                            int lines = 0;
                            
                            istream = entity.getContent();

                            if (fp.contains(FetchProfile.Item.BODY_SANE)) {
                                reader = new BufferedReader(new InputStreamReader(istream), 8192);

                                while ((tempText = reader.readLine()) != null &&
                                       (lines < totalLines)) {
                                    buffer.append(tempText+"\r\n");
                                    lines++;
                                }

                                istream.close();
                                resultText = buffer.toString();
                                istream = new ByteArrayInputStream(resultText.getBytes("UTF-8"));
                            }

                            wdMessage.parse(istream);

                        }
                
                    } catch (IllegalArgumentException iae) {
                        Log.e(Email.LOG_TAG, "IllegalArgumentException caught " + iae);
                    } catch (URISyntaxException use) {
                        Log.e(Email.LOG_TAG, "URISyntaxException caught " + use);
                    } catch (IOException ioe) {
                        Log.e(Email.LOG_TAG, "Non-success response code loading message, response code was " + statusCode);
                    }
                }

                if (listener != null) {
                    listener.messageFinished(wdMessage, i, count);
                }
            }
        }

        /**
         * Fetches and sets the message flags for the supplied messages.
         * The idea is to have this be recursive so that we do a series of medium calls
         * instead of one large massive call or a large number of smaller calls.
         */
        private void fetchFlags(Message[] startMessages, MessageRetrievalListener listener) throws MessagingException {
            HashMap<String, Boolean> uidToReadStatus = new HashMap<String, Boolean>();
            DefaultHttpClient httpclient = new DefaultHttpClient();
            String messageBody = new String();
            Message[] messages = new Message[20];
            String[] uids;
            

            if (startMessages == null ||
                startMessages.length == 0) {
                return;
            }

            if (startMessages.length > 20) {
                Message[] newMessages = new Message[startMessages.length - 20];
                for (int i = 0, count = startMessages.length; i < count; i++) {
                    if (i < 20) {
                        messages[i] = startMessages[i];
                    } else {
                        newMessages[i - 20] = startMessages[i];
                    }
                }

                fetchFlags(newMessages, listener);
            } else {
                messages = startMessages;
            }

            uids = new String[messages.length];

            for (int i = 0, count = messages.length; i < count; i++) {
                uids[i] = messages[i].getUid();
            }

            httpclient.setCookieStore(WebDavStore.this.mAuthCookies);
            messageBody = getMessageFlagsXml(uids);

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
                        uidToReadStatus = dataset.getUidToRead();
                    } catch (SAXException se) {
                        Log.e(Email.LOG_TAG, "SAXException in fetch() " + se);
                    } catch (ParserConfigurationException pce) {
                        Log.e(Email.LOG_TAG, "ParserConfigurationException in fetch() " + pce);
                    }
                }
            } catch (UnsupportedEncodingException uee) {
                Log.e(Email.LOG_TAG, "UnsupportedEncodingException: " + uee);
            } catch (IOException ioe) {
                Log.e(Email.LOG_TAG, "IOException: " + ioe);
            }

            for (int i = 0, count = messages.length; i < count; i++) {
                if (!(messages[i] instanceof WebDavMessage)) {
                    throw new MessagingException("WebDavStore fetch called with non-WebDavMessage");
                }
                WebDavMessage wdMessage = (WebDavMessage) messages[i];
                
                if (listener != null) {
                    listener.messageStarted(messages[i].getUid(), i, count);
                }

                wdMessage.setFlagInternal(Flag.SEEN, uidToReadStatus.get(wdMessage.getUid()));

                if (listener != null) {
                    listener.messageFinished(messages[i], i, count);
                }
            }
        }
        
        /**
         * Fetches and parses the message envelopes for the supplied messages.
         * The idea is to have this be recursive so that we do a series of medium calls
         * instead of one large massive call or a large number of smaller calls.
         * Call it a happy balance
         */
        private void fetchEnvelope(Message[] startMessages, MessageRetrievalListener listener) throws MessagingException {
            HashMap<String, ParsedMessageEnvelope> envelopes = new HashMap<String, ParsedMessageEnvelope>();
            Message[] messages = new Message[10];
            if (startMessages == null ||
                startMessages.length == 0) {
                return;
            }

            if (startMessages.length > 10) {
                Message[] newMessages = new Message[startMessages.length - 10];
                for (int i = 0, count = startMessages.length; i < count; i++) {
                    if (i < 10) {
                        messages[i] = startMessages[i];
                    } else {
                        newMessages[i - 10] = startMessages[i];
                    }
                }
                /**                System.arraycopy(startMessages, 0, messages, 0, 10);
                                   System.arraycopy(startMessages, 10, newMessages, 0, startMessages.length - 10);*/
                fetchEnvelope(newMessages, listener);
            } else {
                messages = startMessages;
            }

            DefaultHttpClient httpclient = new DefaultHttpClient();
            String messageBody = new String();
            String[] uids = new String[messages.length];

            for (int i = 0, count = messages.length; i < count; i++) {
                uids[i] = messages[i].getUid();
            }

            httpclient.setCookieStore(WebDavStore.this.mAuthCookies);
            messageBody = getMessageEnvelopeXml(uids);

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
                        envelopes = dataset.getMessageEnvelopes();
                    } catch (SAXException se) {
                        Log.e(Email.LOG_TAG, "SAXException in fetch() " + se);
                    } catch (ParserConfigurationException pce) {
                        Log.e(Email.LOG_TAG, "ParserConfigurationException in fetch() " + pce);
                    }
                }
            } catch (UnsupportedEncodingException uee) {
                Log.e(Email.LOG_TAG, "UnsupportedEncodingException: " + uee);
            } catch (IOException ioe) {
                Log.e(Email.LOG_TAG, "IOException: " + ioe);
            }

            int count = messages.length;
            for (int i = messages.length - 1; i >= 0; i--) {
                /*            for (int i = 0, count = messages.length; i < count; i++) {*/
                if (!(messages[i] instanceof WebDavMessage)) {
                    throw new MessagingException("WebDavStore fetch called with non-WebDavMessage");
                }
                WebDavMessage wdMessage = (WebDavMessage) messages[i];
                
                if (listener != null) {
                    listener.messageStarted(messages[i].getUid(), i, count);
                }

                wdMessage.setNewHeaders(envelopes.get(wdMessage.getUid()));
                wdMessage.setFlagInternal(Flag.SEEN, envelopes.get(wdMessage.getUid()).getReadStatus());

                if (listener != null) {
                    listener.messageFinished(messages[i], i, count);
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
            String[] uids = new String[messages.length];
            
            if (needAuth()) {
                authenticate();
            }

            if (WebDavStore.this.mAuthenticated == false ||
                WebDavStore.this.mAuthCookies == null) {
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
            HashMap<String, String> uidToUrl = getMessageUrls(uids);
            String[] urls = new String[uids.length];

            for (int i = 0, count = uids.length; i < count; i++) {
                urls[i] = uidToUrl.get(uids[i]);
            }
            
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

                entity = response.getEntity();
            } catch (UnsupportedEncodingException uee) {
                Log.e(Email.LOG_TAG, "UnsupportedEncodingException: " + uee);
            } catch (IOException ioe) {
                Log.e(Email.LOG_TAG, "IOException: " + ioe);
            }
        }

        private void deleteServerMessages(String[] uids) throws MessagingException {
            DefaultHttpClient httpclient = new DefaultHttpClient();
            HashMap<String, String> uidToUrl = getMessageUrls(uids);
            String[] urls = new String[uids.length];

            httpclient.setCookieStore(WebDavStore.this.mAuthCookies);
            
            for (int i = 0, count = uids.length; i < count; i++) {
                try {
                    int status_code = -1;
                    HttpGeneric httpmethod = new HttpGeneric(uidToUrl.get(uids[i]));
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
                    Log.e(Email.LOG_TAG, "UnsupportedEncodingException: " + uee);
                } catch (IOException ioe) {
                    Log.e(Email.LOG_TAG, "IOException: " + ioe);
                }
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
        private String mUrl = new String();
        
        WebDavMessage(String uid, Folder folder) throws MessagingException {
            this.mUid = uid;
            this.mFolder = folder;
        }

        public void setUrl(String url) {
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
                Log.e(Email.LOG_TAG, "UnsupportedEncodingException caught in setUrl");
            } catch (IllegalArgumentException iae) {
                Log.e(Email.LOG_TAG, "IllegalArgumentException caught in setUrl");
            }

            for (int i = 0; i < length - 1; i++) {
                if (i != 0) {
                    url = url + "/" + urlParts[i];
                } else {
                    url = urlParts[i];
                }
            }

            url = url + "/" + end;

            this.mUrl = url;
        }

        public String getUrl() {
            return this.mUrl;
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

        public void setNewHeaders(ParsedMessageEnvelope envelope) throws MessagingException {
            String[] headers = envelope.getHeaderList();
            HashMap<String, String> messageHeaders = envelope.getMessageHeaders();
            
            for (int i = 0, count = headers.length; i < count; i++) {
                if (headers[i].equals("Content-Length")) {
                    this.setSize(new Integer(messageHeaders.get(headers[i])).intValue());
                }
                this.addHeader(headers[i], messageHeaders.get(headers[i]));
            }
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
            return this.mDataSet;
        }

        @Override
        public void startDocument() throws SAXException {
            this.mDataSet = new ParsedDataSet();
        }

        @Override
        public void endDocument() throws SAXException {
            /* Do nothing */
        }

        @Override
        public void startElement(String namespaceURI, String localName,
                                 String qName, Attributes atts) throws SAXException {
            mOpenTags.push(localName);
        }

        @Override
        public void endElement(String namespaceURI, String localName, String qName) {
            mOpenTags.pop();

            /** Reset the hash temp variables */
            if (localName.equals("response")) {
                this.mDataSet.addEnvelope();
                this.mDataSet.clearTempData();
            }
        }

        @Override
        public void characters(char ch[], int start, int length) {
            String value = new String(ch, start, length);
            mDataSet.addValue(value, mOpenTags.peek());
        }
    }

    /**
     * Data set for a single E-Mail message's required headers (the envelope)
     * Only provides accessor methods to the stored data.  All processing should be
     * done elsewhere.  This is done rather than having multiple hashmaps 
     * associating UIDs to values
     */
    public class ParsedMessageEnvelope {
        private boolean mReadStatus = false;
        private String mUid = new String();
        private HashMap<String, String> mMessageHeaders = new HashMap<String, String>();
        private ArrayList<String> mHeaders = new ArrayList<String>();
        
        public void addHeader(String field, String value) {
            this.mMessageHeaders.put(field, value);
            this.mHeaders.add(field);
        }

        public HashMap<String, String> getMessageHeaders() {
            return this.mMessageHeaders;
        }

        public String[] getHeaderList() {
            return this.mHeaders.toArray(new String[] {});
        }
        
        public void setReadStatus(boolean status) {
            this.mReadStatus = status;
        }

        public boolean getReadStatus() {
            return this.mReadStatus;
        }

        public void setUid(String uid) {
            if (uid != null) {
                this.mUid = uid;
            }
        }

        public String getUid() {
            return this.mUid;
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
        private HashMap<String, ParsedMessageEnvelope> mEnvelopes = new HashMap<String, ParsedMessageEnvelope>();
        private int mMessageCount = 0;
        private String mTempUid = "";
        private String mTempUrl = "";
        private String mFrom = "";
        private String mTo = "";
        private String mCc = "";
        private String mReceived = "";
        private Boolean mTempRead;
        private ParsedMessageEnvelope mEnvelope = new ParsedMessageEnvelope();
        private boolean mRead;

        public void addValue(String value, String tagName) {
            if (tagName.equals("href")) {
                this.mHrefs.add(value);
                this.mTempUrl = value;
            } else if (tagName.equals("visiblecount")) {
                this.mMessageCount = new Integer(value).intValue();
            } else if (tagName.equals("uid")) {
                this.mUids.add(value);
                this.mEnvelope.setUid(value);
                this.mTempUid = value;
            } else if (tagName.equals("read")) {
                if (value.equals("0")) {
                    this.mReads.add(false);
                    this.mEnvelope.setReadStatus(false);
                    this.mTempRead = false;
                } else {
                    this.mReads.add(true);
                    this.mEnvelope.setReadStatus(true);
                    this.mTempRead = true;
                }
            } else if (tagName.equals("received")) {
                this.mReceived = this.mReceived + value;
            } else if (tagName.equals("mime-version")) {
                this.mEnvelope.addHeader("MIME-Version", value);
            } else if (tagName.equals("content-type")) {
                this.mEnvelope.addHeader("Content-Type", value);
            } else if (tagName.equals("subject")) {
                this.mEnvelope.addHeader("Subject", value);
            } else if (tagName.equals("date")) {
                value = value.replaceAll("T", " ");
                String[] valueBreak = value.split("\\.");
                value = valueBreak[0];

                DateFormat dfInput = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                DateFormat dfOutput = new SimpleDateFormat("EEE, d MMM yy HH:mm:ss Z");
                String tempDate = "";

                try {
                    Date parsedDate = dfInput.parse(value);
                    tempDate = dfOutput.format(parsedDate);
                } catch (java.text.ParseException pe) {
                    Log.e(Email.LOG_TAG, "Error parsing date: "+ pe);
                }

                this.mEnvelope.addHeader("Date", tempDate);
            } else if (tagName.equals("thread-topic")) {
                this.mEnvelope.addHeader("Thread-Topic", value);
            } else if (tagName.equals("thread-index")) {
                this.mEnvelope.addHeader("Thread-Index", value);
            } else if (tagName.equals("from")) {
                this.mFrom = this.mFrom + value;
            } else if (tagName.equals("to")) {
                this.mTo = this.mTo + value;
            } else if (tagName.equals("in-reply-to")) {
                this.mEnvelope.addHeader("In-Reply-To", value);
            } else if (tagName.equals("return-path")) {
                this.mEnvelope.addHeader("Return-Path", value);
            } else if (tagName.equals("cc")) {
                this.mCc = this.mCc + value;
            } else if (tagName.equals("references")) {
                this.mEnvelope.addHeader("References", value);
            } else if (tagName.equals("getcontentlength")) {
                this.mEnvelope.addHeader("Content-Length", value);
            } 


            if (!this.mTempUid.equals("") &&
                this.mTempRead != null) {
                if (this.mTempRead) {
                    this.mUidRead.put(this.mTempUid, true);
                } else {
                    this.mUidRead.put(this.mTempUid, false);
                }
            }

            if (!this.mTempUid.equals("") &&
                !this.mTempUrl.equals("")) {
                this.mUidUrls.put(this.mTempUid, this.mTempUrl);
            }
        }

        /**
         * Clears the temp variables
         */
        public void clearTempData() {
            this.mTempUid = "";
            this.mTempUrl = "";
            this.mFrom = "";
            this.mEnvelope = new ParsedMessageEnvelope();
        }

        public void addEnvelope() {
            this.mEnvelope.addHeader("From", this.mFrom);
            this.mEnvelope.addHeader("To", this.mTo);
            this.mEnvelope.addHeader("Cc", this.mCc);
            this.mEnvelope.addHeader("Received", this.mReceived);
            this.mEnvelopes.put(this.mEnvelope.getUid(), this.mEnvelope);
        }

        /**
         * Returns an array of the set of message envelope objects
         */
        public HashMap<String, ParsedMessageEnvelope> getMessageEnvelopes() {
            return this.mEnvelopes;
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
            return this.mHrefs.toArray(new String[] {});
        }

        /**
         * Get the first stored Href
         */
        public String getHref() {
            String[] hrefs = this.mHrefs.toArray(new String[] {});
            return hrefs[0];
        }
        
        /**
         * Get all stored Uids
         */
        public String[] getUids() {
            return this.mUids.toArray(new String[] {});
        }

        /**
         * Get the first stored Uid
         */
        public String getUid() {
            String[] uids = this.mUids.toArray(new String[] {});
            return uids[0];
        }
        
        /**
         * Get message count
         */
        public int getMessageCount() {
            return this.mMessageCount;
        }

        /**
         * Get all stored read statuses
         */
        public Boolean[] getReadArray() {
            Boolean[] readStatus = this.mReads.toArray(new Boolean[] {});
            return readStatus;
        }
        
        /**
         * Get the first stored read status
         */
        public boolean getRead() {
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

            String[] urlParts = uri.split("/");
            int length = urlParts.length;
            String end = urlParts[length - 1];
            String url = new String();
            
            /**
             * We have to decode, then encode the URL because Exchange likes to
             * not properly encode all characters
             */
            try {
                end = java.net.URLDecoder.decode(end, "UTF-8");
                end = java.net.URLEncoder.encode(end, "UTF-8");
                end = end.replaceAll("\\+", "%20");
            } catch (UnsupportedEncodingException uee) {
                Log.e(Email.LOG_TAG, "UnsupportedEncodingException caught in HttpGeneric(String uri)");
            } catch (IllegalArgumentException iae) {
                Log.e(Email.LOG_TAG, "IllegalArgumentException caught in HttpGeneric(String uri)");
            }

            for (int i = 0; i < length - 1; i++) {
                if (i != 0) {
                    url = url + "/" + urlParts[i];
                } else {
                    url = urlParts[i];
                }
            }

            url = url + "/" + end;

            setURI(URI.create(url));
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
