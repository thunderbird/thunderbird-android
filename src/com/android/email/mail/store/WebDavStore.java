package com.android.email.mail.store;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Stack;

import javax.net.ssl.SSLException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CookieStore;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import android.util.Log;

import com.android.email.Email;
import com.android.email.mail.CertificateValidationException;
import com.android.email.mail.FetchProfile;
import com.android.email.mail.Flag;
import com.android.email.mail.Folder;
import com.android.email.mail.Message;
import com.android.email.mail.MessageRetrievalListener;
import com.android.email.mail.MessagingException;
import com.android.email.mail.Store;
import com.android.email.mail.internet.MimeMessage;
import com.android.email.mail.transport.TrustedSocketFactory;

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
    private String mHost;     /* Stores the host name for the server */
    private URI mUri;         /* Stores the Uniform Resource Indicator with all connection info */

    private CookieStore mAuthCookies; /* Stores cookies from authentication */
    private boolean mAuthenticated = false; /* Stores authentication state */
    private long mLastAuth = -1; /* Stores the timestamp of last auth */
    private long mAuthTimeout = 5 * 60;

    private HashMap<String, WebDavFolder> mFolderList = new HashMap<String, WebDavFolder>();
    private boolean mSecure;
    private DefaultHttpClient mHttpClient = null;
	
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
        try {
            mUri = new URI(_uri);
        } catch (URISyntaxException use) {
            throw new MessagingException("Invalid WebDavStore URI", use);
        }
        String scheme = mUri.getScheme();
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

        mHost = mUri.getHost();
        if (mHost.startsWith("http")) {
            String[] hostParts = mHost.split("://", 2);
            if (hostParts.length > 1) {
                mHost = hostParts[1];
            }
        }

        if (mConnectionSecurity == CONNECTION_SECURITY_TLS_REQUIRED ||
            mConnectionSecurity == CONNECTION_SECURITY_SSL_REQUIRED ||
            mConnectionSecurity == CONNECTION_SECURITY_TLS_OPTIONAL ||
            mConnectionSecurity == CONNECTION_SECURITY_SSL_OPTIONAL) {
            this.mUrl = "https://" + mHost + ":" + mUri.getPort();
        } else {
            this.mUrl = "http://" + mHost + ":" + mUri.getPort();
        }
        
        if (mUri.getUserInfo() != null) {
            String[] userInfoParts = mUri.getUserInfo().split(":", 2);
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
        mSecure = mConnectionSecurity == CONNECTION_SECURITY_SSL_REQUIRED;
    }


    @Override
    public void checkSettings() throws MessagingException {
        Log.e(Email.LOG_TAG, "WebDavStore.checkSettings() not implemented");
    }

    @Override
    public Folder[] getPersonalNamespaces() throws MessagingException {
        ArrayList<Folder> folderList = new ArrayList<Folder>();
        HashMap<String, String> headers = new HashMap<String, String>();
        ParsedDataSet dataset = new ParsedDataSet();
        String messageBody;
        String[] folderUrls;
        int urlLength;

        /**
         * We have to check authentication here so we have the proper URL stored
         */
        if (needAuth()) {
            authenticate();
        }
        
        messageBody = getFolderListXml();
        headers.put("Brief", "t");
        dataset = processRequest(this.mUrl, "SEARCH", messageBody, headers);

        folderUrls = dataset.getHrefs();
        urlLength = folderUrls.length;

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
                try {
                    folderName = java.net.URLDecoder.decode(fullPathName, "UTF-8");
                } catch (UnsupportedEncodingException uee) {
                    /** If we don't support UTF-8 there's a problem, don't decode it then */
                    folderName = fullPathName;
                }
            }

            wdFolder = new WebDavFolder(folderName);
            wdFolder.setUrl(folderUrls[i]);
            folderList.add(wdFolder);
            this.mFolderList.put(folderName, wdFolder);
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
        buffer.append(" \"urn:schemas:mailheader:mime-version\",");
        buffer.append(" \"urn:schemas:mailheader:content-type\",");
        buffer.append(" \"urn:schemas:mailheader:subject\",");
        buffer.append(" \"urn:schemas:mailheader:date\",");
        buffer.append(" \"urn:schemas:mailheader:thread-topic\",");
        buffer.append(" \"urn:schemas:mailheader:thread-index\",");
        buffer.append(" \"urn:schemas:mailheader:from\",");
        buffer.append(" \"urn:schemas:mailheader:to\",");
        buffer.append(" \"urn:schemas:mailheader:in-reply-to\",");
        buffer.append(" \"urn:schemas:mailheader:cc\",");
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
        buffer.append("SELECT \"urn:schemas:httpmail:read\", \"DAV:uid\", \"DAV:href\"\r\n");
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
            buffer.append(" <a:href>"+urls[i]+"</a:href>\r\n");
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
     * @throws MessagingException 
     */
    public void authenticate() throws MessagingException {
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

    public static String getHttpRequestResponse(HttpEntity request, HttpEntity response) throws IllegalStateException, IOException{
        String responseText = "";
        String requestText = "";
        if (response != null) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(response.getContent()), 8192);
            String tempText = "";

            while ((tempText = reader.readLine()) != null) {
                responseText += tempText;
            }
        }
        if (request != null) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(request.getContent()), 8192);
            String tempText = "";

            while ((tempText = reader.readLine()) != null) {
                requestText += tempText;
            }
            requestText = requestText.replaceAll("password=.*?&", "password=(omitted)&");
        }
        return "Request: " + requestText +
            "\n\nResponse: " + responseText;

    }
    
    /**
     * Performs the Form Based Authentication
     * Returns the CookieStore object for later use or null
     * @throws MessagingException 
     */
    public CookieStore doAuthentication(String username, String password,
                                        String url) throws IOException, MessagingException {
        String authPath = "/exchweb/bin/auth/owaauth.dll";
        CookieStore cookies = null;
        String[] urlParts = url.split("/");
        String finalUrl = "";
        String loginUrl = new String();

        for (int i = 0; i <= 2; i++) {
            if (i != 0) {
                finalUrl = finalUrl + "/" + urlParts[i];
            } else {
                finalUrl = urlParts[i];
            }
        }

        if (finalUrl.equals("")) {
            throw new MessagingException("doAuthentication failed, unable to construct URL to post login credentials to.");
        }
        
        loginUrl = finalUrl + authPath;
        
        try {
            /* Browser Client */
            DefaultHttpClient httpclient = getTrustedHttpClient();

            /* Verb Fix issue */
            /**
             * This is in a separate block because I really don't like how it's done.
             * This basically scrapes the OWA login page for the form submission URL.
             * UGLY!
             */
            {
                HttpGet httpget = new HttpGet(finalUrl);
                HttpResponse response = httpclient.execute(httpget);
                HttpEntity entity = response.getEntity();
                int statusCode = response.getStatusLine().getStatusCode();

                if (statusCode > 300 ||
                    statusCode < 200) {
                    throw new MessagingException("Error during authentication: "+
                                                 response.getStatusLine().toString()+"\n\n");
                }
                
                if (entity != null) {
                    InputStream istream = entity.getContent();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(istream), 4096);
                    String tempText = new String();
                    boolean matched = false;

                    while ((tempText = reader.readLine()) != null &&
                           !matched) {
                        if (tempText.indexOf(" action") >= 0) {
                            String[] tagParts = tempText.split("\"");
                            loginUrl = finalUrl + tagParts[1];
                            matched = true;
                        }
                    }
                    istream.close();
                }
            }

            /* Post Method */
            HttpPost httppost = new HttpPost(loginUrl);

            /** Build the POST data to use */
            ArrayList<BasicNameValuePair> pairs = new ArrayList();
            pairs.add(new BasicNameValuePair("username", username));
            pairs.add(new BasicNameValuePair("password", password));
            pairs.add(new BasicNameValuePair("destination", finalUrl + "/exchange/" +username+"/"));
            pairs.add(new BasicNameValuePair("flags", "0"));
            pairs.add(new BasicNameValuePair("SubmitCreds", "Log+On"));
            pairs.add(new BasicNameValuePair("forcedownlevel", "0"));
            pairs.add(new BasicNameValuePair("trusted", "0"));

            try {
                UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(pairs);
                String tempUrl = "";
                
                httppost.setEntity(formEntity);

                /** Perform the actual POST */
                HttpResponse response = httpclient.execute(httppost);
                HttpEntity entity = response.getEntity();
                int status_code = response.getStatusLine().getStatusCode();
        		
                /** Verify success */
                if (status_code > 300 ||
                    status_code < 200) {
                    throw new MessagingException("Error during authentication: "+
                                                 response.getStatusLine().toString()+ "\n\n"+
                                                 getHttpRequestResponse(formEntity, entity));
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
                            tempUrl = tagParts[1];
                        }
                    }
                }

                if (tempUrl.equals("")) {
                    this.mUrl = finalUrl + "/Exchange/" + this.alias + "/";
                } else {
                    this.mUrl = tempUrl;
                }

            } catch (UnsupportedEncodingException uee) {
                Log.e(Email.LOG_TAG, "Error encoding POST data for authencation");
            }
        } catch (SSLException e) {
            throw new CertificateValidationException(e.getMessage(), e);
        } catch (GeneralSecurityException gse) {
            throw new MessagingException(
                                         "Unable to open connection to SMTP server due to security error.", gse);
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

    public DefaultHttpClient getTrustedHttpClient() throws KeyManagementException, NoSuchAlgorithmException{
    	if (mHttpClient == null) {
    		mHttpClient = new DefaultHttpClient();
    		SchemeRegistry reg = mHttpClient.getConnectionManager().getSchemeRegistry();
    		Scheme s = new Scheme("https",new TrustedSocketFactory(mHost,mSecure),443);
    		reg.register(s);


    		//Add credentials for NTLM/Digest/Basic Auth
    		Credentials creds = new UsernamePasswordCredentials(mUsername, mPassword);
    		CredentialsProvider credsProvider  = mHttpClient.getCredentialsProvider();
    		// setting AuthScope for 80 and 443, in case we end up getting redirected
    		// from 80 to 443.
    		credsProvider.setCredentials(new AuthScope(mHost, 80, AuthScope.ANY_REALM), creds);
    		credsProvider.setCredentials(new AuthScope(mHost, 443, AuthScope.ANY_REALM), creds);
    		credsProvider.setCredentials(new AuthScope(mHost, mUri.getPort(), AuthScope.ANY_REALM), creds);
    		mHttpClient.setCredentialsProvider(credsProvider);
    	} 

        return mHttpClient;
    }
    
    /**
     * Performs an httprequest to the supplied url using the supplied method.
     * messageBody and headers are optional as not all requests will need them.
     * There are two signatures to support calls that don't require parsing of the response.
     */
    private ParsedDataSet processRequest(String url, String method, String messageBody, HashMap<String, String> headers) {
        return processRequest(url, method, messageBody, headers, true);
    }
    
    private ParsedDataSet processRequest(String url, String method, String messageBody, HashMap<String, String> headers, boolean needsParsing) {
        ParsedDataSet dataset = new ParsedDataSet();
        DefaultHttpClient httpclient;
        
        if (url == null ||
            method == null) {
            return dataset;
        }
        try {
        	httpclient = getTrustedHttpClient();
        } catch (KeyManagementException e) {
        	Log.e(Email.LOG_TAG, "Generated KeyManagementException during authentication" + e.getStackTrace());
        	return dataset;
        } catch (NoSuchAlgorithmException e) {
        	Log.e(Email.LOG_TAG, "Generated NoSuchAlgorithmException during authentication" + e.getStackTrace());
        	return dataset;
        }
        if (needAuth()) {
            try {
                authenticate();
            } catch (MessagingException e) {
                Log.e(Email.LOG_TAG, "Generated MessagingException during authentication" + e.getStackTrace());
            }
        }

        if (this.mAuthenticated == false ||
            this.mAuthCookies == null) {
            Log.e(Email.LOG_TAG, "Error during authentication");
            return dataset;
        }

        httpclient.setCookieStore(this.mAuthCookies);
        try {
            int statusCode = -1;
            StringEntity messageEntity = null;
            HttpGeneric httpmethod = new HttpGeneric(url);
            HttpResponse response;
            HttpEntity entity;

            if (messageBody != null) {
                messageEntity = new StringEntity(messageBody);
                messageEntity.setContentType("text/xml");
                httpmethod.setEntity(messageEntity);
            }

            for (String headerName : headers.keySet()) {
                httpmethod.setHeader(headerName, headers.get(headerName));
            }

            httpmethod.setMethod(method);

            response = httpclient.execute(httpmethod);
            statusCode = response.getStatusLine().getStatusCode();

            entity = response.getEntity();

            if (statusCode < 200 ||
                statusCode > 300) {
    			throw new IOException("Error during request processing: "+
    					response.getStatusLine().toString()+ "\n\n"+
    					getHttpRequestResponse(messageEntity, entity));
            }

            if (entity != null &&
                needsParsing) {
                try {
                    InputStream istream = entity.getContent();
                    SAXParserFactory spf = SAXParserFactory.newInstance();
                    SAXParser sp = spf.newSAXParser();
                    XMLReader xr = sp.getXMLReader();
                    WebDavHandler myHandler = new WebDavHandler();
                        
                    xr.setContentHandler(myHandler);
                    xr.parse(new InputSource(istream));

                    dataset = myHandler.getDataSet();
                } catch (SAXException se) {
                    Log.e(Email.LOG_TAG, "SAXException in processRequest() " + se);
                } catch (ParserConfigurationException pce) {
                    Log.e(Email.LOG_TAG, "ParserConfigurationException in processRequest() " + pce);
                }
            }
        } catch (UnsupportedEncodingException uee) {
            Log.e(Email.LOG_TAG, "UnsupportedEncodingException: " + uee);
        } catch (IOException ioe) {
            Log.e(Email.LOG_TAG, "IOException: " + ioe);
        }

        return dataset;
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
                String[] urlParts = name.split("/");
                String url = "";
                for (int i = 0, count = urlParts.length; i < count; i++) {
                    if (i != 0) {
                        url = url + "/" + java.net.URLEncoder.encode(urlParts[i], "UTF-8");
                    } else {
                        url = java.net.URLEncoder.encode(urlParts[i], "UTF-8");
                    }
                }
                encodedName = url;
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

            /**
             * In some instances, it is possible that our folder objects have been collected,
             * but getPersonalNamespaces() isn't called again (ex. Android destroys the email client).
             * Perform an authentication to get the appropriate URLs in place again
             */
            if (needAuth()) {
                try {
                    authenticate();
                } catch (MessagingException e) {
                    Log.e(Email.LOG_TAG, "Generated MessagingException during authentication" + e.getStackTrace());
                }
            }

            if (encodedName.equals("INBOX")) {
                encodedName = "Inbox";
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
            ParsedDataSet dataset = new ParsedDataSet();
            HashMap<String, String> headers = new HashMap<String, String>();
            String messageBody;
            
            if (read) {
                isRead = new String("True");
            } else {
                isRead = new String("False");
            }

            messageBody = getMessageCountXml(isRead);
            headers.put("Brief", "t");
            dataset = processRequest(this.mFolderUrl, "SEARCH", messageBody, headers);
            if (dataset != null) {
                messageCount = dataset.getMessageCount();
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
            ArrayList<Message> messages = new ArrayList<Message>();
            String[] uids;
            ParsedDataSet dataset = new ParsedDataSet();
            HashMap<String, String> headers = new HashMap<String, String>();
            int uidsLength = -1;
            
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
            
            messageBody = getMessagesXml();

            headers.put("Brief", "t");
            headers.put("Range", "rows=" + start + "-" + end);
            dataset = processRequest(this.mFolderUrl, "SEARCH", messageBody, headers);

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
            HashMap<String, String> headers = new HashMap<String, String>();
            ParsedDataSet dataset = new ParsedDataSet();
            String messageBody;

            /** Retrieve and parse the XML entity for our messages */
            messageBody = getMessageUrlsXml(uids);
            headers.put("Brief", "t");

            dataset = processRequest(this.mFolderUrl, "SEARCH", messageBody, headers);
            uidToUrl = dataset.getUidToUrl();

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

            if (fp.contains(FetchProfile.Item.BODY_SANE)) {
                fetchMessages(messages, listener, FETCH_BODY_SANE_SUGGESTED_SIZE / 76);
            }

            if (fp.contains(FetchProfile.Item.BODY)) {
                fetchMessages(messages, listener, -1);
            }

            if (fp.contains(FetchProfile.Item.STRUCTURE)) {
                for (int i = 0, count = messages.length; i < count; i++) {
                    if (!(messages[i] instanceof WebDavMessage)) {
                        throw new MessagingException("WebDavStore fetch called with non-WebDavMessage");
                    }
                    WebDavMessage wdMessage = (WebDavMessage) messages[i];

                    if (listener != null) {
                        listener.messageStarted(wdMessage.getUid(), i, count);
                    }

                    wdMessage.setBody(null);

                    if (listener != null) {
                        listener.messageFinished(wdMessage, i, count);
                    }
                }
            }
        }

        /**
         * Fetches the full messages or up to lines lines and passes them to the message parser.
         */
        private void fetchMessages(Message[] messages, MessageRetrievalListener listener, int lines) throws MessagingException {
            DefaultHttpClient httpclient;
			try {
				httpclient = getTrustedHttpClient();
			} catch (KeyManagementException e) {
                throw new MessagingException("KeyManagement Exception in fetchMessages()."+ e.getStackTrace());
			} catch (NoSuchAlgorithmException e) {
                throw new MessagingException("NoSuchAlgorithm Exception in fetchMessages():" + e.getStackTrace());
			}

            /**
             * We can't hand off to processRequest() since we need the stream to parse.
             */
            if (needAuth()) {
                authenticate();
            }

            if (WebDavStore.this.mAuthenticated == false ||
                WebDavStore.this.mAuthCookies == null) {
                throw new MessagingException("Error during authentication in fetchMessages().");
            }

            httpclient.setCookieStore(WebDavStore.this.mAuthCookies);
            
            for (int i = 0, count = messages.length; i < count; i++) {
                WebDavMessage wdMessage;
                int statusCode = 0;
                
                if (!(messages[i] instanceof WebDavMessage)) {
                    throw new MessagingException("WebDavStore fetch called with non-WebDavMessage");
                }

                wdMessage = (WebDavMessage) messages[i];

                if (listener != null) {
                    listener.messageStarted(wdMessage.getUid(), i, count);
                }

                /**
                 * If fetch is called outside of the initial list (ie, a locally stored
                 * message), it may not have a URL associated.  Verify and fix that
                 */
                if (wdMessage.getUrl().equals("")) {
                    wdMessage.setUrl(getMessageUrls(new String[] {wdMessage.getUid()}).get(wdMessage.getUid()));
                    if (wdMessage.getUrl().equals("")) {
                        throw new MessagingException("Unable to get URL for message");
                    }
                }

                try {
                    HttpGet httpget = new HttpGet(new URI(wdMessage.getUrl()));
                    HttpResponse response;
                    HttpEntity entity;
                    
                    httpget.setHeader("translate", "f");

                    response = httpclient.execute(httpget);
                    
                    statusCode = response.getStatusLine().getStatusCode();

                    entity = response.getEntity();

                    if (statusCode < 200 ||
                        statusCode > 300) {
            			throw new IOException("Error during fetch: "+
            					response.getStatusLine().toString()+ "\n\n"+
            					getHttpRequestResponse(null, entity));
                    }

                    if (entity != null) {
                        InputStream istream = null;
                        StringBuffer buffer = new StringBuffer();
                        String tempText = new String();
                        String resultText = new String();
                        String bodyBoundary = "";
                        BufferedReader reader;
                        int currentLines = 0;
                            
                        istream = entity.getContent();
                            
                        if (lines != -1) {
                            reader = new BufferedReader(new InputStreamReader(istream), 8192);

                            while ((tempText = reader.readLine()) != null &&
                                   (currentLines < lines)) {
                                buffer.append(tempText+"\r\n");
                                currentLines++;
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
                    Log.e(Email.LOG_TAG, "Non-success response code loading message, response code was: " + statusCode + " Error: "+ioe.getMessage());
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
            HashMap<String, String> headers = new HashMap<String, String>();
            ParsedDataSet dataset = new ParsedDataSet();
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

            messageBody = getMessageFlagsXml(uids);
            headers.put("Brief", "t");
            dataset = processRequest(this.mFolderUrl, "SEARCH", messageBody, headers);

            uidToReadStatus = dataset.getUidToRead();

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
            HashMap<String, String> headers = new HashMap<String, String>();
            ParsedDataSet dataset = new ParsedDataSet();
            String messageBody = new String();
            String[] uids;
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

                fetchEnvelope(newMessages, listener);
            } else {
                messages = startMessages;
            }

            uids = new String[messages.length];

            for (int i = 0, count = messages.length; i < count; i++) {
                uids[i] = messages[i].getUid();
            }

            messageBody = getMessageEnvelopeXml(uids);
            headers.put("Brief", "t");
            dataset = processRequest(this.mFolderUrl, "SEARCH", messageBody, headers);

            envelopes = dataset.getMessageEnvelopes();

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
            String messageBody = new String();
            HashMap<String, String> headers = new HashMap<String, String>();
            HashMap<String, String> uidToUrl = getMessageUrls(uids);
            ParsedDataSet dataset = new ParsedDataSet();
            String[] urls = new String[uids.length];

            for (int i = 0, count = uids.length; i < count; i++) {
                urls[i] = uidToUrl.get(uids[i]);
            }
            
            messageBody = getMarkMessagesReadXml(urls);
            headers.put("Brief", "t");
            headers.put("If-Match", "*");

            processRequest(this.mFolderUrl, "BPROPPATCH", messageBody, headers, false);
        }

        private void deleteServerMessages(String[] uids) throws MessagingException {
            HashMap<String, String> uidToUrl = getMessageUrls(uids);
            String[] urls = new String[uids.length];

            for (int i = 0, count = uids.length; i < count; i++) {
                HashMap<String, String> headers = new HashMap<String, String>();
                String uid = uids[i];
                String url = uidToUrl.get(uid);
                String destinationUrl = generateDeleteUrl(url);

                /**
                 * If the destination is the same as the origin, assume delete forever
                 */
                if (destinationUrl.equals(url)) {
                    headers.put("Brief", "t");
                    processRequest(url, "DELETE", null, headers, false);
                } else {
                    headers.put("Destination", generateDeleteUrl(url));
                    headers.put("Brief", "t");
                    processRequest(url, "MOVE", null, headers, false);
                }
            }
        }

        private String generateDeleteUrl(String startUrl) {
            String[] urlParts = startUrl.split("/");
            String filename = urlParts[urlParts.length - 1];
            String finalUrl = WebDavStore.this.mUrl + "Deleted%20Items/" + filename;

            return finalUrl;
        }

        @Override
        public void appendMessages(Message[] messages) throws MessagingException {
            Log.e(Email.LOG_TAG, "appendMessages() not implmented");
        }

        
        @Override
        public void copyMessages(Message[] msgs, Folder folder) throws MessagingException {
            Log.e(Email.LOG_TAG, "copyMessages() not implemented");
        }

        @Override
        public Message[] expunge() throws MessagingException {
            /** Do nothing, deletes occur as soon as the call is made rather than flags on the message */
            return null;
        }

        @Override
        public boolean equals(Object o) {
            return false;
        }

        public String getUidFromMessageId(Message message) throws MessagingException {
            Log.e(Email.LOG_TAG, "Unimplemented method getUidFromMessageId in WebDavStore.WebDavFolder could lead to duplicate messages "
                  + " being uploaded to the Sent folder");
            return null;
        }

        public void setFlags(Flag[] flags, boolean value) throws MessagingException {
            Log.e(Email.LOG_TAG, "Unimplemented method setFlags(Flag[], boolean) breaks markAllMessagesAsRead and EmptyTrash");
            // Try to make this efficient by not retrieving all of the messages
            return;
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
            //TODO: This is a not as ugly hack (ie, it will actually work)
            //XXX: prevent URLs from getting to us that are broken
            if (!(url.toLowerCase().contains("http"))) {
                if (!(url.startsWith("/"))){
                    url = "/" + url;
                }
                url = WebDavStore.this.mUrl + this.mFolder + url;
            }

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
            } else if (tagName.equals("mime-version")) {
                this.mEnvelope.addHeader("MIME-Version", value);
            } else if (tagName.equals("content-type")) {
                this.mEnvelope.addHeader("Content-Type", value);
            } else if (tagName.equals("subject")) {
                this.mEnvelope.addHeader("Subject", value);
            } else if (tagName.equals("date")) {
                /**
                 * Exchange doesn't give us rfc822 dates like it claims.  The date is in the format:
                 * yyyy-MM-dd'T'HH:mm:ss.SSS<Single digit representation of timezone, so far, all instances are Z>
                 */
                value = value.substring(0, value.length() - 1);

                DateFormat dfInput = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
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
            } else if (tagName.equals("cc")) {
                this.mCc = this.mCc + value;
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
