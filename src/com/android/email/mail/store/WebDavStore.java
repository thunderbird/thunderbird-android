package com.android.email.mail.store;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
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
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
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
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
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
    private String mPath;     /* Stores the path for the server */
    private String mAuthPath; /* Stores the path off of the server to post data to for form based authentication */
    private String mMailboxPath; /* Stores the user specified path to the mailbox */
    private URI mUri;         /* Stores the Uniform Resource Indicator with all connection info */
    private String mRedirectUrl;

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

        String[] pathParts = mUri.getPath().split("\\|");

        for (int i = 0, count = pathParts.length; i < count; i++) {
            if (i == 0) {
                if (pathParts[0] != null &&
                    pathParts[0].length() > 1) {
                    if (!pathParts[0].substring(1).equals("")) {
                        mPath = pathParts[0].substring(1);
                    } else {
                        mPath = "";
                    }
                } else {
                    mPath = "";
                }
            } else if (i == 1) {
                if (pathParts[1] != null &&
                    pathParts[1].length() > 1) {
                    mAuthPath = "/" + pathParts[1];
                }
            } else if (i == 2) {
                if (pathParts[2] != null &&
                    pathParts[2].length() > 1) {
                    mMailboxPath = "/" + pathParts[2];
                    if (mPath == null ||
                        mPath.equals("")) {
                        mPath = mMailboxPath;
                    }
                }
            }
        }
        
        if (mConnectionSecurity == CONNECTION_SECURITY_TLS_REQUIRED ||
            mConnectionSecurity == CONNECTION_SECURITY_SSL_REQUIRED ||
            mConnectionSecurity == CONNECTION_SECURITY_TLS_OPTIONAL ||
            mConnectionSecurity == CONNECTION_SECURITY_SSL_OPTIONAL) {
            this.mUrl = "https://" + mHost + ":" + mUri.getPort() + mPath;
        } else {
            this.mUrl = "http://" + mHost + ":" + mUri.getPort() + mPath;
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
        DataSet dataset = new DataSet();
        String messageBody;
        String[] folderUrls;
        int urlLength;

        /**
         * We have to check authentication here so we have the proper URL stored
         */
        getHttpClient();
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
        buffer.append("SELECT \"DAV:uid\", \"DAV:ishidden\"\r\n");
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
            Log.e(Email.LOG_TAG, "Error during authentication: " + ioe + "\nStack: " + processException(ioe));
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
        String authPath;
        CookieStore cookies = null;
        String[] urlParts = url.split("/");
        String finalUrl = "";
        String loginUrl = new String();
        String destinationUrl = new String();

        if (this.mAuthPath != null &&
            !this.mAuthPath.equals("") &&
            !this.mAuthPath.equals("/")) {
            authPath = this.mAuthPath;
        } else {
            authPath = "/exchweb/bin/auth/owaauth.dll";
        }

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
            DefaultHttpClient httpclient = mHttpClient;

            /**
             * This is in a separate block because I really don't like how it's done.
             * This basically scrapes the OWA login page for the form submission URL.
             * UGLY!
             * Added an if-check to see if there's a user supplied authentication path for FBA
             */
            if (this.mAuthPath == null ||
                this.mAuthPath.equals("") ||
                this.mAuthPath.equals("/")) {
                HttpGet httpget = new HttpGet(finalUrl);

                httpclient.addRequestInterceptor(new HttpRequestInterceptor() {
                        public void process(HttpRequest request, HttpContext context)
                                                    throws HttpException, IOException {
                            mRedirectUrl = ((HttpHost) context.getAttribute(ExecutionContext.HTTP_TARGET_HOST)).toURI() + request.getRequestLine().getUri();
                        }
                    });
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
                            if (tagParts[1].lastIndexOf('/') < 0 &&
                                mRedirectUrl != null &&
                                !mRedirectUrl.equals("")) {
                                /* We have to do a multi-stage substring here because of potential GET parameters */
                                mRedirectUrl = mRedirectUrl.substring(0, mRedirectUrl.lastIndexOf('?'));
                                mRedirectUrl = mRedirectUrl.substring(0, mRedirectUrl.lastIndexOf('/'));
                                loginUrl = mRedirectUrl + "/" + tagParts[1];
                            } else {
                                loginUrl = finalUrl + tagParts[1];
                            }
                        }

                        if (tempText.indexOf("destination") >= 0) {
                            String[] tagParts = tempText.split("value");
                            if (tagParts[1] != null) {
                                String[] valueParts = tagParts[1].split("\"");
                                destinationUrl = valueParts[1];
                                matched = true;
                            }
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
            if (this.mMailboxPath != null &&
                !this.mMailboxPath.equals("")) {
                pairs.add(new BasicNameValuePair("destination", finalUrl + this.mMailboxPath));
            } else if (destinationUrl != null &&
                       !destinationUrl.equals("")) {
                pairs.add(new BasicNameValuePair("destination", destinationUrl));
            } else {
                pairs.add(new BasicNameValuePair("destination", "/"));
            }
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

                if (this.mMailboxPath != null &&
                    !this.mMailboxPath.equals("")) {
                    this.mUrl = finalUrl + "/" + this.mMailboxPath + "/";
                } else if (tempUrl.equals("")) {
                    this.mUrl = finalUrl + "/Exchange/" + this.alias + "/";
                } else {
                    this.mUrl = tempUrl;
                }

            } catch (UnsupportedEncodingException uee) {
                Log.e(Email.LOG_TAG, "Error encoding POST data for authentication: " + uee + "\nTrace: " + processException(uee));
            }
        } catch (SSLException e) {
            throw new CertificateValidationException(e.getMessage(), e);
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

    public DefaultHttpClient getHttpClient() throws MessagingException {
        SchemeRegistry reg;
        Scheme s;
        
        if (mHttpClient == null) {
            mHttpClient = new DefaultHttpClient();
        }

        reg = mHttpClient.getConnectionManager().getSchemeRegistry();
        try {
            s = new Scheme("https", new TrustedSocketFactory(mHost, mSecure), 443);
        } catch (NoSuchAlgorithmException nsa) {
            Log.e(Email.LOG_TAG, "NoSuchAlgorithmException in getHttpClient: " + nsa);
            throw new MessagingException("NoSuchAlgorithmException in getHttpClient: " + nsa);
        } catch (KeyManagementException kme) {
            Log.e(Email.LOG_TAG, "KeyManagementException in getHttpClient: " + kme);
            throw new MessagingException("KeyManagementException in getHttpClient: " + kme);
        }
        reg.register(s);

        if (needAuth()) {
            if (!checkAuth()) {
                try {
                    CookieStore cookies = mHttpClient.getCookieStore();
                    cookies.clear();
                    mHttpClient.setCookieStore(cookies);
                    cookies = doAuthentication(this.mUsername, this.mPassword, this.mUrl);
                    if (cookies != null) {
                        this.mAuthenticated = true;
                        this.mLastAuth = System.currentTimeMillis()/1000;
                    }
                    mHttpClient.setCookieStore(cookies);
                } catch (IOException ioe) {
                    Log.e(Email.LOG_TAG, "IOException: " + ioe + "\nTrace: " + processException(ioe));
                }
            } else {
                Credentials creds = new UsernamePasswordCredentials(mUsername, mPassword);
                CredentialsProvider credsProvider = mHttpClient.getCredentialsProvider();
                credsProvider.setCredentials(new AuthScope(mHost, 80, AuthScope.ANY_REALM), creds);
                credsProvider.setCredentials(new AuthScope(mHost, 443, AuthScope.ANY_REALM), creds);
                credsProvider.setCredentials(new AuthScope(mHost, mUri.getPort(), AuthScope.ANY_REALM), creds);
                mHttpClient.setCredentialsProvider(credsProvider);
                // Assume we're authenticated and ok here since the checkAuth() was 401 and we've now set the credentials
                this.mAuthenticated = true;
                this.mLastAuth = System.currentTimeMillis()/1000;
            }
        }

        return mHttpClient;
    }

    private boolean checkAuth() {
        DefaultHttpClient httpclient = mHttpClient;
        HttpResponse response;
        HttpGet httpget = new HttpGet(mUrl);
        try {
            response = httpclient.execute(httpget);
        } catch (IOException ioe) {
            Log.e(Email.LOG_TAG, "Error checking authentication status");
            return false;
        }
        
        HttpEntity entity = response.getEntity();
        int statusCode = response.getStatusLine().getStatusCode();

        if (statusCode == 401) {
            return true;
        }

        return false;
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
    private DataSet processRequest(String url, String method, String messageBody, HashMap<String, String> headers) {
        return processRequest(url, method, messageBody, headers, true);
    }

    private DataSet processRequest(String url, String method, String messageBody, HashMap<String, String> headers, boolean needsParsing) {
        DataSet dataset = new DataSet();
        DefaultHttpClient httpclient;

        if (url == null ||
            method == null) {
            return dataset;
        }

        try {
            httpclient = getHttpClient();
        } catch (MessagingException me) {
            Log.e(Email.LOG_TAG, "Generated MessagingException getting HttpClient: " + me);
            return dataset;
        }

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
                    Log.e(Email.LOG_TAG, "SAXException in processRequest() " + se + "\nTrace: " + processException(se));
                } catch (ParserConfigurationException pce) {
                    Log.e(Email.LOG_TAG, "ParserConfigurationException in processRequest() " + pce + "\nTrace: " + processException(pce));
                }
            }
        } catch (UnsupportedEncodingException uee) {
            Log.e(Email.LOG_TAG, "UnsupportedEncodingException: " + uee + "\nTrace: " + processException(uee));
        } catch (IOException ioe) {
            Log.e(Email.LOG_TAG, "IOException: " + ioe + "\nTrace: " + processException(ioe));
        }

        return dataset;
    }

    /**
     * Returns a string of the stacktrace for a Throwable to allow for easy inline printing of errors.
     */
    private String processException(Throwable t) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);

        t.printStackTrace(ps);
        ps.close();

        return baos.toString();
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
            try {
                getHttpClient();
            } catch (MessagingException me) {
                Log.e(Email.LOG_TAG, "MessagingException during authentication for WebDavFolder: " + me);
                return;
            }

            if (encodedName.equals("INBOX")) {
                encodedName = "Inbox";
            }
            
            this.mFolderUrl = WebDavStore.this.mUrl + encodedName;
        }

        public void setUrl(String url) {
            if (url != null) {
                this.mFolderUrl = url;
            }
        }

        @Override
        public void open(OpenMode mode) throws MessagingException {
            getHttpClient();

            this.mIsOpen = true;
        }

        private int getMessageCount(boolean read, CookieStore authCookies) {
            String isRead;
            int messageCount = 0;
            DataSet dataset = new DataSet();
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
            DataSet dataset = new DataSet();
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

            if (start == 0 && end < 10) {
                end = 10;
            }
            
            /** Verify authentication */
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
            DataSet dataset = new DataSet();
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
            httpclient = getHttpClient();
            
            /**
             * We can't hand off to processRequest() since we need the stream to parse.
             */
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
                    Log.e(Email.LOG_TAG, "IllegalArgumentException caught " + iae + "\nTrace: " + processException(iae));
                } catch (URISyntaxException use) {
                    Log.e(Email.LOG_TAG, "URISyntaxException caught " + use + "\nTrace: " + processException(use));
                } catch (IOException ioe) {
                    Log.e(Email.LOG_TAG, "Non-success response code loading message, response code was " + statusCode + "\nURL: " + wdMessage.getUrl() + "\nError: " + ioe.getMessage() + "\nTrace: " + processException(ioe));
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
            DataSet dataset = new DataSet();
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

            if (dataset == null) {
                throw new MessagingException("Data Set from request was null");
            }
            
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
            DataSet dataset = new DataSet();
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
            DataSet dataset = new DataSet();
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
                Log.e(Email.LOG_TAG, "UnsupportedEncodingException caught in setUrl: " + uee + "\nTrace: " + processException(uee));
            } catch (IllegalArgumentException iae) {
                Log.e(Email.LOG_TAG, "IllegalArgumentException caught in setUrl: " + iae + "\nTrace: " + processException(iae));
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
                String headerValue = messageHeaders.get(headers[i]);
                if (headers[i].equals("Content-Length")) {
                    int size = new Integer(messageHeaders.get(headers[i])).intValue();
                    this.setSize(size);
                }

                if (headerValue != null &&
                    !headerValue.equals("")) {
                    this.addHeader(headers[i], headerValue);
                }
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
        private DataSet mDataSet = new DataSet();
        private Stack<String> mOpenTags = new Stack<String>();
        
        public DataSet getDataSet() {
            return this.mDataSet;
        }

        @Override
        public void startDocument() throws SAXException {
            this.mDataSet = new DataSet();
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
                this.mDataSet.finish();
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
        /**
         * Holds the mappings from the name returned from Exchange to the MIME format header name
         */
        private final HashMap<String, String> mHeaderMappings = new HashMap<String, String>() {
        {
            put("mime-version", "MIME-Version");
            put("content-type", "Content-Type");
            put("subject", "Subject");
            put("date", "Date");
            put("thread-topic", "Thread-Topic");
            put("thread-index", "Thread-Index");
            put("from", "From");
            put("to", "To");
            put("in-reply-to", "In-Reply-To");
            put("cc", "Cc");
            put("getcontentlength", "Content-Length");
        }
        };
        
        private boolean mReadStatus = false;
        private String mUid = new String();
        private HashMap<String, String> mMessageHeaders = new HashMap<String, String>();
        private ArrayList<String> mHeaders = new ArrayList<String>();
        
        public void addHeader(String field, String value) {
            String headerName = mHeaderMappings.get(field);

            if (headerName != null) {
                this.mMessageHeaders.put(mHeaderMappings.get(field), value);
                this.mHeaders.add(mHeaderMappings.get(field));
            }
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
     * Dataset for all XML parses.
     * Data is stored in a single format inside the class and is formatted appropriately depending on the accessor calls made.
     */
    public class DataSet {
        private HashMap<String, HashMap> mData = new HashMap<String, HashMap>();
        private HashMap<String, String> mLostData = new HashMap<String, String>();
        private String mUid = new String();
        private HashMap<String, String> mTempData = new HashMap<String, String>();

        public void addValue(String value, String tagName) {
            if (tagName.equals("uid")) {
                mUid = value;
            }

            if (mTempData.containsKey(tagName)) {
                mTempData.put(tagName, mTempData.get(tagName) + value);
            } else {
                mTempData.put(tagName, value);
            }
        }

        public void finish() {
            if (mUid != null &&
                mTempData != null) {
                mData.put(mUid, mTempData);
            } else if (mTempData != null) {
                /* Lost Data are for requests that don't include a message UID.
                 * These requests should only have a depth of one for the response so it will never get stomped over.
                 */
                mLostData = mTempData;
                String visibleCount = mLostData.get("visiblecount");
            }

            mUid = new String();
            mTempData = new HashMap<String, String>();
        }

        /**
         * Returns a hashmap of Message UID => Message Url
         */
        public HashMap<String, String> getUidToUrl() {
            HashMap<String, String> uidToUrl = new HashMap<String, String>();

            for (String uid : mData.keySet()) {
                HashMap<String, String> data = mData.get(uid);
                String value = data.get("href");
                if (value != null &&
                    !value.equals("")) {
                    uidToUrl.put(uid, value);
                }
            }

            return uidToUrl;
        }

        /**
         * Returns a hashmap of Message UID => Read Status
         */
        public HashMap<String, Boolean> getUidToRead() {
            HashMap<String, Boolean> uidToRead = new HashMap<String, Boolean>();

            for (String uid : mData.keySet()) {
                HashMap<String, String> data = mData.get(uid);
                String readStatus = data.get("read");
                if (readStatus != null &&
                    !readStatus.equals("")) {
                    Boolean value = readStatus.equals("0") ? false : true;
                    uidToRead.put(uid, value);
                }
            }

            return uidToRead;
        }

        /**
         * Returns an array of all hrefs (urls) that were received
         */
        public String[] getHrefs() {
            ArrayList<String> hrefs = new ArrayList<String>();

            for (String uid : mData.keySet()) {
                HashMap<String, String> data = mData.get(uid);
                String href = data.get("href");
                hrefs.add(href);
            }

            return hrefs.toArray(new String[] {});
        }

        /**
         * Return an array of all Message UIDs that were received
         */
        public String[] getUids() {
            ArrayList<String> uids = new ArrayList<String>();

            for (String uid : mData.keySet()) {
                uids.add(uid);
            }

            return uids.toArray(new String[] {});
        }

        /**
         * Returns the message count as it was retrieved
         */
        public int getMessageCount() {
            int messageCount = -1;

            for (String uid : mData.keySet()) {
                HashMap<String, String> data = mData.get(uid);
                String count = data.get("visiblecount");

                if (count != null &&
                    !count.equals("")) {
                    messageCount = new Integer(count).intValue();
                }
                
            }

            return messageCount;
        }

        /**
         * Returns a HashMap of message UID => ParsedMessageEnvelope
         */
        public HashMap<String, ParsedMessageEnvelope> getMessageEnvelopes() {
            HashMap<String, ParsedMessageEnvelope> envelopes = new HashMap<String, ParsedMessageEnvelope>();

            for (String uid : mData.keySet()) {
                ParsedMessageEnvelope envelope = new ParsedMessageEnvelope();
                HashMap<String, String> data = mData.get(uid);

                if (data != null) {
                    for (String header : data.keySet()) {
                        if (header.equals("read")) {
                            String read = data.get(header);
                            Boolean readStatus = read.equals("0") ? false : true;

                            envelope.setReadStatus(readStatus);
                        } else if (header.equals("date")) {
                            /**
                             * Exchange doesn't give us rfc822 dates like it claims.  The date is in the format:
                             * yyyy-MM-dd'T'HH:mm:ss.SSS<Single digit representation of timezone, so far, all instances are Z>
                             */
                            String date = data.get(header);
                            date = date.substring(0, date.length() - 1);
                            
                            DateFormat dfInput = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
                            DateFormat dfOutput = new SimpleDateFormat("EEE, d MMM yy HH:mm:ss Z");
                            String tempDate = "";

                            try {
                                Date parsedDate = dfInput.parse(date);
                                tempDate = dfOutput.format(parsedDate);
                            } catch (java.text.ParseException pe) {
                                Log.e(Email.LOG_TAG, "Error parsing date: "+ pe + "\nTrace: " + processException(pe));
                            }
                            envelope.addHeader(header, tempDate);
                        } else {
                            envelope.addHeader(header, data.get(header));
                        }
                    }
                } 

                if (envelope != null) {
                    envelopes.put(uid, envelope);
                }
            }

            return envelopes;
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
                Log.e(Email.LOG_TAG, "UnsupportedEncodingException caught in HttpGeneric(String uri): " + uee + "\nTrace: " + processException(uee));
            } catch (IllegalArgumentException iae) {
                Log.e(Email.LOG_TAG, "IllegalArgumentException caught in HttpGeneric(String uri): " + iae + "\nTrace: " + processException(iae));
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
