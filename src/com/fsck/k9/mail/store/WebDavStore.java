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
import java.util.ArrayList;

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

public class WebDavStore extends Store {
    public static final int CONNECTION_SECURITY_NONE = 0;
    public static final int CONNECTION_SECURITY_TLS_OPTIONAL = 1;
    public static final int CONNECTION_SECURITY_TLS_REQUIRED = 2;
    public static final int CONNECTION_SECURITY_SSL_REQUIRED = 3;
    public static final int CONNECTION_SECURITY_SSL_OPTIONAL = 4;
    
    private static final Flag[] PERMANENT_FLAGS = { Flag.DELETED, Flag.SEEN };
    private int mConnectionSecurity;
    private String mUsername;
    private String mPassword;
    private String mUrl;
    private CookieStore authCookies;

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
            if (userInfoParts.length > 1) {
                mPassword = userInfoParts[1];
            }
        }
        Log.d(k9.LOG_TAG, ">>> New WebDavStore creation complete");
    }
    
    @Override
    public void checkSettings() throws MessagingException {
        Log.d(k9.LOG_TAG, ">>> checkSettings() called");
    }

    @Override
    public Folder[] getPersonalNamespaces() throws MessagingException {
        ArrayList<Folder> folderList = new ArrayList<Folder>();
        Log.d(k9.LOG_TAG, ">>> getPersonalNamespaces() called");
        try {
            /** Populate the authentication cookies so we can poll for our list of folders */
            WebDavConnection connection = new WebDavConnection(this.mUsername, mPassword, this.mUrl);
            this.authCookies = connection.getAuthCookies();

            /** Retrieve the list of folder names */
            DefaultHttpClient httpclient = new DefaultHttpClient();
            httpclient.setCookieStore(this.authCookies);

            StringBuffer strBuf = new StringBuffer(200);
            strBuf.append("<?xml version='1.0' ?>");
            strBuf.append("<a:searchrequest xmlns:a='DAV:'><a:sql>\r\n");
            strBuf.append("SELECT \"DAV:ishidden\"\r\n");
            strBuf.append(" FROM \"\"\r\n");
            strBuf.append(" WHERE \"DAV:ishidden\"=False AND \"DAV:isfolder\"=True\r\n");
            strBuf.append("</a:sql></a:searchrequest>\r\n");
            String body = strBuf.toString();
                    
            StringEntity b_entity = new StringEntity(body);
            b_entity.setContentType("text/xml");

            /** Get our proper username for the path */
            String[] userParts = this.mUsername.split(":", 2);
            String localUsername;
            
            if (userParts.length > 1) {
                localUsername = userParts[1];
            } else {
                localUsername = this.mUsername;
            }

            HttpGeneric httpmethod = new HttpGeneric(this.mUrl + "/Exchange/" + localUsername);
            httpmethod.setMethod("SEARCH");
            httpmethod.setEntity(b_entity);
            httpmethod.setHeader("Brief", "t");

            HttpResponse response = httpclient.execute(httpmethod);
            int status_code = response.getStatusLine().getStatusCode();

            if (status_code < 200 ||
                status_code > 300) {
                throw new IOException("Error getting folder listing, error during authentication");
            }
            
            HttpEntity entity = response.getEntity();
            
            if (entity != null) {
                try {
                    InputStream istream = entity.getContent();
                    SAXParserFactory spf = SAXParserFactory.newInstance();
                    SAXParser sp = spf.newSAXParser();
                    
                    XMLReader xr = sp.getXMLReader();
                    
                    FolderListingHandler myHandler = new FolderListingHandler();
                    xr.setContentHandler(myHandler);
                    
                    xr.parse(new InputSource(istream));
                    
                    ParsedFolderListingSet dataset = myHandler.getDataSet();

                    String[] folderUrls = dataset.getHrefs();
                    int urlLength = folderUrls.length;
                    
                    for (int i = 0; i < urlLength; i++) {
                        String[] urlParts = folderUrls[i].split("/");
                        folderList.add(getFolder(urlParts[urlParts.length - 1]));
                    }
                    /** And hack it up, get the base Inbox folder */
                    folderList.add(getFolder(""));
                } catch (SAXException se) {
                    Log.d(k9.LOG_TAG, ">>> SAXException caught");
                } catch (ParserConfigurationException pce) {
                    Log.d(k9.LOG_TAG, ">>> ParserConfigurationException caught");
                }
            }
        } catch (UnsupportedEncodingException uee) {
            Log.d(k9.LOG_TAG, ">>> UnsupportedEncodingException caught");
        } catch (IOException ioe) {
            Log.d(k9.LOG_TAG, ">>> IOException caught");
        }

        return folderList.toArray(new WebDavFolder[] {});
    }

    @Override
    public Folder getFolder(String name) throws MessagingException {
        WebDavFolder folder;
        Log.d(k9.LOG_TAG, ">>> getFolder called on " + name);
        folder = new WebDavFolder(name);
        return folder;
    }

    public void delete() {
        Log.d(k9.LOG_TAG, ">>> delete() called but not implemented");
    }

    class WebDavFolder extends Folder {
        private String mName;
        private String mFolderUrl;
        private String mLocalUsername;
        private int mMessageCount = -1;
        private int mUnreadMessageCount = 0;
        private boolean mExists;
        private CookieStore authCookies;
        private boolean isOpen = false;

        public WebDavFolder(String name) {
            Log.d(k9.LOG_TAG, ">>> New WebDavFolder created");
            this.mName = name;
            String[] userParts = WebDavStore.this.mUsername.split("/", 2);
            if (userParts.length > 1) {
                this.mLocalUsername = userParts[1];
            } else {
                this.mLocalUsername = WebDavStore.this.mUsername;
            }
            this.mFolderUrl = WebDavStore.this.mUrl + "/Exchange/" + this.mLocalUsername + "/" + this.mName;
        }

        @Override
        public void open(OpenMode mode) throws MessagingException {
            Log.d(k9.LOG_TAG, ">>> open called on folder " + this.mName);
            try {
                /** Get our authentication cookies */
                WebDavConnection connection = new WebDavConnection(WebDavStore.this.mUsername, WebDavStore.this.mPassword, WebDavStore.this.mUrl);
                this.authCookies = connection.getAuthCookies();

                this.mMessageCount = getMessageCount(true, authCookies);
                this.mUnreadMessageCount = getMessageCount(false, authCookies);
                
            } catch (IOException ioe) {
                Log.d(k9.LOG_TAG, ">>> IOException caught in open()");
                throw new MessagingException("IOException in WebDavConnection");
            }
        }

        private int getMessageCount(boolean read, CookieStore authCookies) throws IOException {
            String isRead;
            int messageCount = 0;
            Log.d(k9.LOG_TAG, ">>> getMessageCount() called for folder " + this.mName);
            if (read) {
                isRead = new String("True");
            } else {
                isRead = new String("False");
            }
            
            /** Generate a new HTTP Client and assign the authentication cookies */
            DefaultHttpClient httpclient = new DefaultHttpClient();
            httpclient.setCookieStore(authCookies);

            /** Generate our XML request body to get message counts */
            StringBuffer strBuf = new StringBuffer(600);
            strBuf.append("<?xml version='1.0' ?>");
            strBuf.append("<a:searchrequest xmlns:a='DAV:'><a:sql>\r\n");
            strBuf.append("SELECT \"DAV:visiblecount\"\r\n");
            strBuf.append(" FROM \"\"\r\n");
            strBuf.append(" WHERE \"DAV:ishidden\"=False AND \"DAV:isfolder\"=False AND \"urn:schemas:httpmail:read\"="+isRead+"\r\n");
            strBuf.append(" GROUP BY \"DAV:ishidden\"\r\n");
            strBuf.append("</a:sql></a:searchrequest>\r\n");
            String body = strBuf.toString();

            /** Generate the request entity from the body */
            StringEntity b_entity = new StringEntity(body);
            b_entity.setContentType("text/xml");

            /** Try and get the listing from the inbox */
            HttpGeneric httpmethod = new HttpGeneric(this.mFolderUrl);
            Log.d(k9.LOG_TAG, ">>> Created HttpGeneric with url " + this.mFolderUrl);
            httpmethod.setMethod("SEARCH");
            httpmethod.setEntity(b_entity);
            httpmethod.setHeader("Brief", "t");

            /** Execute the request */
            HttpResponse response = httpclient.execute(httpmethod);
            int status_code = response.getStatusLine().getStatusCode();

            /** Verify we have success */
            if (status_code < 200 ||
                status_code > 300) {
                Log.d(k9.LOG_TAG, ">>> Status code was bad, value was: " + status_code);
                throw new IOException("Error getting folder listing, error during authentication");
            }

            /** Generate our response entity */
            HttpEntity entity = response.getEntity();
                
            if (entity != null) {
                try {
                    /** Initialize the parser for our expected response format */
                    InputStream istream = entity.getContent();
                    SAXParserFactory spf = SAXParserFactory.newInstance();
                    SAXParser sp = spf.newSAXParser();
                    
                    XMLReader xr = sp.getXMLReader();
                    MessageCountHandler myHandler = new MessageCountHandler();
                    xr.setContentHandler(myHandler);

                    /** Assign the input stream and parse it*/
                    xr.parse(new InputSource(istream));
                    
                    /** Get the results of parsing */
                    ParsedCountDataSet dataset = myHandler.getDataSet();
                    messageCount = dataset.getMessageCount();

                    istream.close();
                    
                } catch (SAXException se) {
                    
                } catch (ParserConfigurationException pce) {
                        
                }
            }

            Log.d(k9.LOG_TAG, ">>> Returning message count " + messageCount);
            return messageCount;
        }
        
        @Override
        public boolean isOpen() {
            Log.d(k9.LOG_TAG, ">>> isOpen called on " + this.mName);
            return this.isOpen;
        }

        @Override
        public OpenMode getMode() throws MessagingException {
            Log.d(k9.LOG_TAG, ">>> getMode() called on " + this.mName);
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
            this.mMessageCount = -1;
            this.isOpen = false;
            Log.d(k9.LOG_TAG, ">>> close() called on " + this.mName);

        }
        
        @Override
        public boolean create(FolderType type) throws MessagingException {
            //            return false;
            return true;
        }

        @Override
        public int getMessageCount() throws MessagingException {
            open(OpenMode.READ_WRITE);
            Log.d(k9.LOG_TAG, ">>> getMessageCount called on " + this.mName + ".  Returning " + this.mMessageCount);
            return this.mMessageCount;
        }

        @Override
        public int getUnreadMessageCount() throws MessagingException {
            open(OpenMode.READ_WRITE);
            Log.d(k9.LOG_TAG, ">>> getUnreadMessageCount called on " + this.mName);
            int messageCount = 0;
            try {
                messageCount = getMessageCount(false, authCookies);
            } catch (IOException ioe) {
                messageCount = -1;
            }

            Log.d(k9.LOG_TAG, ">>> Returning " + messageCount);
            return messageCount;
            /**            return this.mUnreadMessageCount;*/
        }

        @Override
        public void delete(boolean recursive) throws MessagingException {
            throw new Error("WebDavStore.delete() not yet implemented");
        }

        @Override
        public Message getMessage(String uid) throws MessagingException {
            Log.d(k9.LOG_TAG, ">>> getMessage called for uid: " + uid);
            return new WebDavMessage(uid, this);
        }
        
        @Override
        public Message[] getMessages(int start, int end, MessageRetrievalListener listener)
                throws MessagingException {
            Log.d(k9.LOG_TAG, ">>> getMessages called for range " + start + "-" + end);
            /** Reverse the range since they're numbered backwards */
            int prevStart = start;
            start = this.mMessageCount - end;
            end = this.mMessageCount - prevStart;
            
            Log.d(k9.LOG_TAG, ">>> reversed message range is " + start + "-" + end);
            if (start < 0 || end < 1 || end < start) {
                throw new MessagingException(String.format("Invalid message set %d %d", start, end));
            }

            ArrayList<Message> messages = new ArrayList<Message>();
            ArrayList<String> uids = new ArrayList<String>();
            uids = getMessages(start, end);
                
            for (int i = 0, count = uids.size(); i < count; i++) {
                if (listener != null) {
                    listener.messageStarted(uids.get(i), i, count);
                }
                WebDavMessage message = new WebDavMessage(uids.get(i), this);
                messages.add(message);
                if (listener != null) {
                    listener.messageFinished(message, i, count);
                }
            }
            
            return messages.toArray(new Message[] {});
        }

        public Message[] getMessages(MessageRetrievalListener listener) throws MessagingException {
            return getMessages(null, listener);
        }

        public Message[] getMessages(String[] uids, MessageRetrievalListener listener)
                throws MessagingException {
            Log.d(k9.LOG_TAG, ">>> getMessages called on array of uids");
            ArrayList<Message> messages = new ArrayList<Message>();
            ArrayList<String> newUids = new ArrayList<String>();
            if (uids == null) {
                newUids = getMessages(0, k9.DEFAULT_VISIBLE_LIMIT);
                uids = newUids.toArray(new String[] {});
            }
            
            for (int i = 0, count = uids.length; i < count; i++) {
                if (listener != null) {
                    listener.messageStarted(uids[i], i, count);
                }
                WebDavMessage message = new WebDavMessage(uids[i], this);
                messages.add(message);
                if (listener != null) {
                    listener.messageFinished(message, i, count);
                }
            }

            return messages.toArray(new Message[] {});
        }

        public ArrayList<String> getMessages(int start, int end) throws MessagingException {
            ArrayList<String> uids = new ArrayList<String>();

            open(OpenMode.READ_WRITE);
            DefaultHttpClient httpclient = new DefaultHttpClient();
            httpclient.setCookieStore(this.authCookies);
            
            StringBuffer strBuf = new StringBuffer(200);
            strBuf.append("<?xml version='1.0' ?>");
            strBuf.append("<a:searchrequest xmlns:a='DAV:'><a:sql>\r\n");
            strBuf.append("SELECT \"DAV:uid\"\r\n");
            strBuf.append(" FROM \"\"\r\n");
            strBuf.append(" WHERE \"DAV:ishidden\"=False AND \"DAV:isfolder\"=False\r\n");
            strBuf.append("</a:sql></a:searchrequest>\r\n");
            String body = strBuf.toString();

            try {
                StringEntity b_entity = new StringEntity(body);
                b_entity.setContentType("text/xml");

                /** Try and get a listing from the inbox */
                HttpGeneric httpmethod = new HttpGeneric(this.mFolderUrl);
                httpmethod.setMethod("SEARCH");
                httpmethod.setEntity(b_entity);
                httpmethod.setHeader("Brief", "t");
                httpmethod.setHeader("Range", "rows=" + start + "-" + end);

                HttpResponse response = httpclient.execute(httpmethod);
                int status_code = response.getStatusLine().getStatusCode();

                if (status_code < 200 ||
                    status_code > 300) {
                    throw new IOException("Error getting folder listing, error during authentication");
                }
                
                HttpEntity entity = response.getEntity();
            
                if (entity != null) {
                    try {
                        InputStream istream = entity.getContent();
                        SAXParserFactory spf = SAXParserFactory.newInstance();
                        SAXParser sp = spf.newSAXParser();
                        
                        XMLReader xr = sp.getXMLReader();
                        MessageUidHandler myHandler = new MessageUidHandler();
                        xr.setContentHandler(myHandler);
                    
                        xr.parse(new InputSource(istream));
                    
                        ParsedMessageUidSet dataset = myHandler.getDataSet();
                    
                        uids = dataset.getUids();
                    } catch (SAXException se) {
                    
                    } catch (ParserConfigurationException pce) {
                        
                    }
                }
            } catch (UnsupportedEncodingException uee) {

            } catch (IOException ioe) {

            }
            
            return uids;
        }
        
        @Override
        public void fetch(Message[] messages, FetchProfile fp, MessageRetrievalListener listener)
                throws MessagingException {
            Log.d(k9.LOG_TAG, ">>> fetch called on array of messages");
            Log.e(k9.LOG_TAG, "Starting fetch");
            if (messages == null ||
                messages.length == 0) {
                return;
            }

            /** Initialize the folder */
            open(OpenMode.READ_WRITE);
            ArrayList<String> uids = new ArrayList<String>();

            for (Message message : messages) {
                uids.add(message.getUid());
            }

            for (int i = 0, count = messages.length; i < count; i++) {
                Message message = messages[i];
                Log.d(k9.LOG_TAG, ">>> Working on message number " + i);
                if (!(message instanceof WebDavMessage)) {
                    Log.d(k9.LOG_TAG, ">>> Not a WebDavMessage");
                    throw new MessagingException("WebDavStore.fetch called with non-WebDav Message");
                }

                WebDavMessage wdMessage = (WebDavMessage) message;

                try {
                    if (listener != null) {
                        Log.d(k9.LOG_TAG, ">>> Starting listener");
                        listener.messageStarted(wdMessage.getUid(), i, count);
                    }

                    /** Get our href */
                    /** Build our request for the appropriate message attributes */
                    DefaultHttpClient httpclient = new DefaultHttpClient();
                    httpclient.setCookieStore(this.authCookies);

                    StringBuffer strBuf = new StringBuffer(600);
                    strBuf.append("<?xml version='1.0' ?>");
                    strBuf.append("<a:searchrequest xmlns:a='DAV:'><a:sql>\r\n");
                    strBuf.append("SELECT \"DAV:uid\"\r\n");
                    strBuf.append(" FROM \"\"\r\n");
                    strBuf.append(" WHERE \"DAV:ishidden\"=False AND \"DAV:isfolder\"=False AND \"DAV:uid\"=\'"+wdMessage.getUid()+"\'\r\n");
                    strBuf.append("</a:sql></a:searchrequest>\r\n");
                    String body = strBuf.toString();
                    Log.d(k9.LOG_TAG, ">>> Request body is " + body);
                    
                    try {
                        StringEntity b_entity = new StringEntity(body);
                        b_entity.setContentType("text/xml");

                        /** Try and get a listing from the inbox */
                        Log.d(k9.LOG_TAG, ">>> Attempting post against url: " + this.mFolderUrl);
                        HttpGeneric httpmethod = new HttpGeneric(this.mFolderUrl);
                        httpmethod.setMethod("SEARCH");
                        httpmethod.setEntity(b_entity);
                        httpmethod.setHeader("Brief", "t");
                        /**                        httpmethod.setHeader("Range", "0-0");*/
                        
                        HttpResponse response = httpclient.execute(httpmethod);
                        
                        int status_code = response.getStatusLine().getStatusCode();

                        if (status_code < 200 ||
                            status_code > 300) {
                            Log.d(k9.LOG_TAG, ">>> Response code from fetching href details was " + status_code);
                            throw new IOException("Error getting folder listing, error during authentication");
                        }
                
                        HttpEntity entity = response.getEntity();
                        Log.d(k9.LOG_TAG, ">>> Got response entity");
                        if (entity != null) {
                            try {
                                Log.d(k9.LOG_TAG, ">>> Parsing href");
                                InputStream istream = entity.getContent();
                                SAXParserFactory spf = SAXParserFactory.newInstance();
                                SAXParser sp = spf.newSAXParser();
                        
                                XMLReader xr = sp.getXMLReader();
                                MessageHrefHandler myHandler = new MessageHrefHandler();
                                xr.setContentHandler(myHandler);
                    
                                xr.parse(new InputSource(istream));
                    
                                String href = myHandler.getHref();
                                Log.d(k9.LOG_TAG, ">>> Fetching email at url: " + href);

                                /** Assign the stuff to the message here */
                                HttpGet httpget = new HttpGet(href);
                                httpget.setHeader("translate", "f");
                                Log.d(k9.LOG_TAG, ">>> HttpGet set");
                                
                                response = httpclient.execute(httpget);
                                Log.d(k9.LOG_TAG, ">>> Httpclient executed");
                                status_code = response.getStatusLine().getStatusCode();
                                Log.d(k9.LOG_TAG, ">>> Response code was " + status_code);
                                if (status_code < 200 ||
                                    status_code > 300) {
                                    Log.d(k9.LOG_TAG, ">>> Response code for fetching email was " + status_code);
                                    throw new IOException("Error getting folder listing, error during authentication");
                                }
                                Log.d(k9.LOG_TAG, ">>> Getting entity");
                                entity = response.getEntity();
                                if (entity != null) {
                                    Log.d(k9.LOG_TAG, ">>> Using full input stream");
                                    istream = entity.getContent();
                                    Log.d(k9.LOG_TAG, ">>> Parsing item");
                                    wdMessage.parse(istream);
                                }
                            } catch (SAXException se) {
                                Log.d(k9.LOG_TAG, ">>> SAXException");
                            } catch (ParserConfigurationException pce) {
                                Log.d(k9.LOG_TAG, ">>> ParserConfigurationException");
                            }
                        }
                    } catch (UnsupportedEncodingException uee) {
                        Log.d(k9.LOG_TAG, ">>> UnsupportedEncodingException");
                    } catch (IOException ioe) {
                        Log.d(k9.LOG_TAG, ">>> IOException");
                    }
                
                    if (listener != null) {
                        Log.e(k9.LOG_TAG, "Messages fetched and parsed, setting finished for this one");
                        listener.messageFinished(wdMessage, i, count);
                    }
                } finally {

                }
            }
        }

        @Override
        public Flag[] getPermanentFlags() throws MessagingException {
            return PERMANENT_FLAGS;
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
        public void setFlags(Message[] messages, Flag[] flags, boolean value)
                throws MessagingException {

        }

        @Override
        public boolean equals(Object o) {
            return false;
        }

        class MessageHrefHandler extends DefaultHandler {
            private String mHref = new String();
            private boolean inHref = false;
            
            public String getHref() {
                return this.mHref;
            }

            @Override
            public void startDocument() throws SAXException {
                this.mHref = new String();
            }

            @Override
            public void endDocument() throws SAXException {
                /** Do nothing */
            }

            @Override
            public void startElement(String namespaceURI, String localName,
                                     String qName, Attributes atts) throws SAXException {
                if (localName.equals("href")) {
                    this.inHref = true;
                }
            }

            @Override
            public void endElement(String namespaceURI, String localName, String qName) {
                if (localName.equals("href")) {
                    this.inHref = false;
                }
            }

            @Override
            public void characters(char ch[], int start, int length) {
                if (this.inHref) {
                    String value = new String(ch, start, length);
                    this.mHref = value;
                }

            }
        }
        
        class MessageUidHandler extends DefaultHandler {
            private ParsedMessageUidSet parsedSet = new ParsedMessageUidSet();
            private boolean inUid = false;

            public ParsedMessageUidSet getDataSet() {
                return this.parsedSet;
            }

            @Override
            public void startDocument() throws SAXException {
                this.parsedSet = new ParsedMessageUidSet();
            }

            @Override
            public void endDocument() throws SAXException {
                /** Do nothing */
            }

            @Override
            public void startElement(String namespaceURI, String localName,
                                     String qName, Attributes atts) throws SAXException {
                if (localName.equals("uid")) {
                    this.inUid = true;
                }
            }

            @Override
            public void endElement(String namespaceURI, String localName, String qName) {
                if (localName.equals("uid")) {
                    this.inUid = false;
                }
            }

            @Override
            public void characters(char ch[], int start, int length) {
                if (this.inUid) {
                    String value = new String(ch, start, length);
                    this.parsedSet.addUid(value);
                }
            }
        }

        class MessageCountHandler extends DefaultHandler {
            private ParsedCountDataSet parsedSet = new ParsedCountDataSet();
            private boolean inVisibleCount = false;
        
            public ParsedCountDataSet getDataSet() {
                return this.parsedSet;
            }

            @Override
            public void startDocument() throws SAXException {
                this.parsedSet = new ParsedCountDataSet();
            }

            @Override
            public void endDocument() throws SAXException {
                /** Do nothing */
            }

            @Override
            public void startElement(String namespaceURI, String localName,
                                     String qName, Attributes atts) throws SAXException {
                if (localName.equals("visiblecount")) {
                    this.inVisibleCount = true;
                } 
            }

            @Override
            public void endElement(String namespaceURI, String localName, String qName) {
                if (localName.equals("visiblecount")) {
                    this.inVisibleCount = false;
                } 
            }
            
            @Override
            public void characters(char ch[], int start, int length) {
                if (this.inVisibleCount) {
                    String value = new String(ch, start, length);
                    this.parsedSet.setMessageCount(Integer.parseInt(value));
                }
            }
        }

        class ParsedMessageEnvelopeSet {
            private String mSubject = new String();
            private String mFrom = new String();
            private String mContentType = new String();
            private String mTo = new String();
            private String mCc = new String();
            private String mContentLength = new String();

            public void addValue(String value, String valname) {
                if (valname.equals("subject")) {
                    this.mSubject = value;
                } else if (valname.equals("from")) {
                    this.mFrom = value;
                } else if (valname.equals("content-type")) {
                    this.mContentType = value;
                } else if (valname.equals("to")) {
                    this.mTo = value;
                } else if (valname.equals("cc")) {
                    this.mCc = value;
                } else if (valname.equals("getcontentlength")) {
                    this.mContentLength = value;
                }
            }

            public String getValue(String valname) {
                if (valname.equals("subject")) {
                    return this.mSubject;
                } else if (valname.equals("from")) {
                    return this.mFrom;
                } else if (valname.equals("content-type")) {
                    return this.mContentType;
                } else if (valname.equals("to")) {
                    return this.mTo;
                } else if (valname.equals("cc")) {
                    return this.mCc;
                } else if (valname.equals("getcontentlength")) {
                    return this.mContentLength;
                }

                return new String();
            }
        }
        
        class ParsedMessageUidSet {
            private ArrayList<String> uids = new ArrayList<String>();

            public void addUid(String uid) {
                this.uids.add(uid);
            }

            public ArrayList<String> getUids() {
                return this.uids;
            }
        }
        
        /** Class for holding count of messages from XML Parser */
        class ParsedCountDataSet {
            private int mMessageCount = 0;
            private int mUnreadMessageCount = 0;
            
            public int getMessageCount() {
                return this.mMessageCount;
            }

            public int getUnreadMessageCount() {
                return this.mUnreadMessageCount;
            }

            public void setUnreadMessageCount(int count) {
                this.mUnreadMessageCount = count;
            }
            
            public void setMessageCount(int count) {
                this.mMessageCount = count;
            }

            public void addOneUnreadMessage() {
                this.mUnreadMessageCount++;
            }
            
            public void addOneMessage() {
                this.mMessageCount++;
            }
        }
    }

    class WebDavMessage extends MimeMessage {
        WebDavMessage(String uid, Folder folder) throws MessagingException {
            Log.e(k9.LOG_TAG, "Created WebDavMessage, uid is " + uid + " Folder is " + folder);
            this.mUid = uid;
            this.mFolder = folder;
        }

        public void setSize(int size) {
            this.mSize = size;
        }

        public void parse(InputStream in) throws IOException, MessagingException {
            Log.d(k9.LOG_TAG, ">>> parse called on WebDavMessage " + this.mUid);
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

    class WebDavBodyPart extends MimeBodyPart {
        public WebDavBodyPart() throws MessagingException {
            super();
        }

        public void setSize(int size) {
            this.mSize = size;
        }
    }

    class WebDavException extends MessagingException {
        String mAlertText;

        public WebDavException(String message, String alertText, Throwable throwable) {
            super(message, throwable);
            this.mAlertText = alertText;
        }

        public WebDavException(String message, String alertText) {
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

    class WebDavConnection {
        private String username = null;
        private String password = null;
        private String url = null;
        private String authPath = "/exchweb/bin/auth/owaauth.dll";
        private CookieStore cookies = null;

        public WebDavConnection(String username, String password, String url) throws IOException, MessagingException {
            Log.d(k9.LOG_TAG, ">>> New WebDavConnection created for url: " + url);
            /** Assign our private info */
            this.username = username;
            this.password = password;
            this.url = url;

            doAuth();
        }

        public void doAuth() throws IOException, MessagingException {
            /** Perform the first authentication */
            DefaultHttpClient httpclient = new DefaultHttpClient();

            /** Method wrapper for the form based authentication */
            HttpPost httppost = new HttpPost(this.url + this.authPath);
            Log.d(k9.LOG_TAG, ">>> Performing logon post to " + this.url + this.authPath);
            /** Build the POST data */
            ArrayList<BasicNameValuePair> pairs = new ArrayList();
            pairs.add(new BasicNameValuePair("username", this.username));
            pairs.add(new BasicNameValuePair("password", this.password));
            pairs.add(new BasicNameValuePair("destination", this.url + "/Exchange/"));
            pairs.add(new BasicNameValuePair("flags", "0"));
            pairs.add(new BasicNameValuePair("SubmitCreds", "Log+On"));
            pairs.add(new BasicNameValuePair("forcedownlevel", "0"));
            pairs.add(new BasicNameValuePair("trusted", "0"));

            try {
                Log.d(k9.LOG_TAG, ">>> In try block");
                UrlEncodedFormEntity p_entity = new UrlEncodedFormEntity(pairs);
        
                /** Assign the POST data to the entity */
                httppost.setEntity(p_entity);
                Log.d(k9.LOG_TAG, ">>> setEntity called");
                /** Perform the actual HTTP POST */
                HttpResponse response = httpclient.execute(httppost);
                Log.d(k9.LOG_TAG, ">>> POST executed");
                HttpEntity entity = response.getEntity();
                Log.d(k9.LOG_TAG, ">>> getEntity() called");
                int status_code = response.getStatusLine().getStatusCode();
                Log.d(k9.LOG_TAG, ">>> getStatusCode() called");

                /** Check the response */
                if (status_code > 300 ||
                    status_code < 200) {
                    Log.d(k9.LOG_TAG, ">>> status_code out of range, was " + status_code);
                    throw new MessagingException("Error in WebDAV authentication");
                }
                
                this.cookies = httpclient.getCookieStore();
                if (this.cookies == null) {
                    Log.d(k9.LOG_TAG, ">>> cookies were null");
                    throw new MessagingException("Error in WebDAV authentication");
                }
                
            } catch (UnsupportedEncodingException uee) {
                Log.d(k9.LOG_TAG, ">>> UnsupportedEncodingException caught");
                throw new MessagingException("Error encoding POST entity");
            }                 
        }

        public CookieStore getAuthCookies() {
            return this.cookies;
        }
    }

    class FolderListingHandler extends DefaultHandler {
        private ParsedFolderListingSet parsedSet = new ParsedFolderListingSet();
            private boolean inHref = false;

        public ParsedFolderListingSet getDataSet() {
                return this.parsedSet;
            }

        @Override
        public void startDocument() throws SAXException {
            this.parsedSet = new ParsedFolderListingSet();
        }

        @Override
        public void endDocument() throws SAXException {
            /** Do nothing */
        }

        @Override
        public void startElement(String namespaceURI, String localName,
                                 String qName, Attributes atts) throws SAXException {
            if (localName.equals("href")) {
                this.inHref = true;
            }
        }

        @Override
        public void endElement(String namespaceURI, String localName, String qName) {
            if (localName.equals("href")) {
                this.inHref = false;
            }
        }

        @Override
        public void characters(char ch[], int start, int length) {
            if (this.inHref) {
                String value = new String(ch, start, length);
                this.parsedSet.addHref(value);
            }
        }
    }

    class ParsedFolderListingSet {
        private ArrayList<String> mFolderNames = new ArrayList<String>();

        public void addHref(String href) {
            this.mFolderNames.add(href);
        }

        public String[] getHrefs() {
            return this.mFolderNames.toArray(new String[] {});
        }
    }
    
    class HttpGeneric extends HttpEntityEnclosingRequestBase {
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