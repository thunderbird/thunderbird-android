package com.fsck.k9.mail.store;

import android.util.Log;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.controller.MessageRetrievalListener;
import com.fsck.k9.helper.Utility;
import com.fsck.k9.mail.*;
import com.fsck.k9.mail.Folder.OpenMode;
import com.fsck.k9.mail.filter.EOLConvertingOutputStream;
import com.fsck.k9.mail.internet.MimeMessage;
import com.fsck.k9.mail.transport.TrustedSocketFactory;
import org.apache.http.*;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CookieStore;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
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

import javax.net.ssl.SSLException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;
import java.util.zip.GZIPInputStream;

/**
 * <pre>
 * Uses WebDAV formatted HTTP calls to an MS Exchange server to fetch emails
 * and email information.  This has only been tested on an MS Exchange
 * Server 2003.  It uses Form-Based authentication and requires that
 * Outlook Web Access be enabled on the server.
 * </pre>
 */
public class WebDavStore extends Store
{
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
    private String mAuthString;
    private static String DAV_MAIL_SEND_FOLDER = "##DavMailSubmissionURI##";
    private static String DAV_MAIL_TMP_FOLDER = "drafts";


    private CookieStore mAuthCookies; /* Stores cookies from authentication */
    private boolean mAuthenticated = false; /* Stores authentication state */
    private long mLastAuth = -1; /* Stores the timestamp of last auth */
    private long mAuthTimeout = 5 * 60;

    private HashMap<String, WebDavFolder> mFolderList = new HashMap<String, WebDavFolder>();
    private boolean mSecure;
    private WebDavHttpClient mHttpClient = null;

    /**
     * webdav://user:password@server:port CONNECTION_SECURITY_NONE
     * webdav+tls://user:password@server:port CONNECTION_SECURITY_TLS_OPTIONAL
     * webdav+tls+://user:password@server:port CONNECTION_SECURITY_TLS_REQUIRED
     * webdav+ssl+://user:password@server:port CONNECTION_SECURITY_SSL_REQUIRED
     * webdav+ssl://user:password@server:port CONNECTION_SECURITY_SSL_OPTIONAL
     */
    public WebDavStore(Account account) throws MessagingException
    {
        super(account);

        try
        {
            mUri = new URI(mAccount.getStoreUri());
        }
        catch (URISyntaxException use)
        {
            throw new MessagingException("Invalid WebDavStore URI", use);
        }
        String scheme = mUri.getScheme();
        if (scheme.equals("webdav"))
        {
            mConnectionSecurity = CONNECTION_SECURITY_NONE;
        }
        else if (scheme.equals("webdav+ssl"))
        {
            mConnectionSecurity = CONNECTION_SECURITY_SSL_OPTIONAL;
        }
        else if (scheme.equals("webdav+ssl+"))
        {
            mConnectionSecurity = CONNECTION_SECURITY_SSL_REQUIRED;
        }
        else if (scheme.equals("webdav+tls"))
        {
            mConnectionSecurity = CONNECTION_SECURITY_TLS_OPTIONAL;
        }
        else if (scheme.equals("webdav+tls+"))
        {
            mConnectionSecurity = CONNECTION_SECURITY_TLS_REQUIRED;
        }
        else
        {
            throw new MessagingException("Unsupported protocol");
        }

        mHost = mUri.getHost();
        if (mHost.startsWith("http"))
        {
            String[] hostParts = mHost.split("://", 2);
            if (hostParts.length > 1)
            {
                mHost = hostParts[1];
            }
        }

        String[] pathParts = mUri.getPath().split("\\|");

        for (int i = 0, count = pathParts.length; i < count; i++)
        {
            if (i == 0)
            {
                if (pathParts[0] != null &&
                        pathParts[0].length() > 1)
                {
                    if (!pathParts[0].substring(1).equals(""))
                    {
                        mPath = pathParts[0].substring(1);
                    }
                    else
                    {
                        mPath = "";
                    }
                }
                else
                {
                    mPath = "";
                }
            }
            else if (i == 1)
            {
                if (pathParts[1] != null &&
                        pathParts[1].length() > 1)
                {
                    mAuthPath = "/" + pathParts[1];
                }
            }
            else if (i == 2)
            {
                if (pathParts[2] != null &&
                        pathParts[2].length() > 1)
                {
                    mMailboxPath = "/" + pathParts[2];
                    if (mPath == null ||
                            mPath.equals(""))
                    {
                        mPath = mMailboxPath;
                    }
                }
            }
        }
        String path = mPath;
        if (path.length() > 0 && path.startsWith("/") == false)
        {
            path = "/" + mPath;
        }

        this.mUrl = getRoot() + path;

        if (mUri.getUserInfo() != null)
        {
            try
            {
                String[] userInfoParts = mUri.getUserInfo().split(":");
                mUsername = URLDecoder.decode(userInfoParts[0], "UTF-8");
                String userParts[] = mUsername.split("/", 2);

                if (userParts.length > 1)
                {
                    alias = userParts[1];
                }
                else
                {
                    alias = mUsername;
                }
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
        mSecure = mConnectionSecurity == CONNECTION_SECURITY_SSL_REQUIRED;
        mAuthString = "Basic " + Utility.base64Encode(mUsername + ":" + mPassword);
    }

    private String getRoot()
    {
        String root;
        if (mConnectionSecurity == CONNECTION_SECURITY_TLS_REQUIRED ||
                mConnectionSecurity == CONNECTION_SECURITY_SSL_REQUIRED ||
                mConnectionSecurity == CONNECTION_SECURITY_TLS_OPTIONAL ||
                mConnectionSecurity == CONNECTION_SECURITY_SSL_OPTIONAL)
        {
            root = "https";
        }
        else
        {
            root = "http";
        }
        root += "://" + mHost + ":" + mUri.getPort();
        return root;
    }


    @Override
    public void checkSettings() throws MessagingException
    {
        Log.e(K9.LOG_TAG, "WebDavStore.checkSettings() not implemented");
    }

    @Override
    public List<? extends Folder> getPersonalNamespaces(boolean forceListAll) throws MessagingException
    {
        LinkedList<Folder> folderList = new LinkedList<Folder>();
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

        for (int i = 0; i < urlLength; i++)
        {
//            Log.i(K9.LOG_TAG, "folderUrls[" + i + "] = '" + folderUrls[i]);
            String[] urlParts = folderUrls[i].split("/");
//            Log.i(K9.LOG_TAG, "urlParts = " + urlParts);
            String folderName = urlParts[urlParts.length - 1];
            String fullPathName = "";
            WebDavFolder wdFolder;

            if (folderName.equalsIgnoreCase(K9.INBOX))
            {
                folderName = "INBOX";
            }
            else
            {
                for (int j = 5, count = urlParts.length; j < count; j++)
                {
                    if (j != 5)
                    {
                        fullPathName = fullPathName + "/" + urlParts[j];
                    }
                    else
                    {
                        fullPathName = urlParts[j];
                    }
                }
                try
                {
                    folderName = java.net.URLDecoder.decode(fullPathName, "UTF-8");
                }
                catch (UnsupportedEncodingException uee)
                {
                    /** If we don't support UTF-8 there's a problem, don't decode it then */
                    folderName = fullPathName;
                }
            }

            wdFolder = new WebDavFolder(this, folderName);
            wdFolder.setUrl(folderUrls[i]);
            folderList.add(wdFolder);
            this.mFolderList.put(folderName, wdFolder);
        }

        return folderList;
    }

    @Override
    public Folder getFolder(String name) throws MessagingException
    {
        WebDavFolder folder;

        if ((folder = this.mFolderList.get(name)) == null)
        {
            folder = new WebDavFolder(this, name);
        }

        return folder;
    }

    public Folder getSendSpoolFolder() throws MessagingException
    {
        return getFolder(DAV_MAIL_SEND_FOLDER);
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

    /***************************************************************
     * WebDAV XML Request body retrieval functions
     */

    private String getFolderListXml()
    {
        StringBuffer buffer = new StringBuffer(200);
        buffer.append("<?xml version='1.0' ?>");
        buffer.append("<a:searchrequest xmlns:a='DAV:'><a:sql>\r\n");
        buffer.append("SELECT \"DAV:uid\", \"DAV:ishidden\"\r\n");
        buffer.append(" FROM SCOPE('deep traversal of \""+this.mUrl+"\"')\r\n");
        buffer.append(" WHERE \"DAV:ishidden\"=False AND \"DAV:isfolder\"=True\r\n");
        buffer.append("</a:sql></a:searchrequest>\r\n");
        return buffer.toString();
    }

    private String getMessageCountXml(String messageState)
    {
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

    private String getMessageEnvelopeXml(String[] uids)
    {
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
        for (int i = 0, count = uids.length; i < count; i++)
        {
            if (i != 0)
            {
                buffer.append("  OR ");
            }
            buffer.append(" \"DAV:uid\"='"+uids[i]+"' ");
        }
        buffer.append("\r\n");
        buffer.append("</a:sql></a:searchrequest>\r\n");
        return buffer.toString();
    }

    private String getMessagesXml()
    {
        StringBuffer buffer = new StringBuffer(200);
        buffer.append("<?xml version='1.0' ?>");
        buffer.append("<a:searchrequest xmlns:a='DAV:'><a:sql>\r\n");
        buffer.append("SELECT \"DAV:uid\"\r\n");
        buffer.append(" FROM \"\"\r\n");
        buffer.append(" WHERE \"DAV:ishidden\"=False AND \"DAV:isfolder\"=False\r\n");
        buffer.append("</a:sql></a:searchrequest>\r\n");
        return buffer.toString();
    }

    private String getMessageUrlsXml(String[] uids)
    {
        StringBuffer buffer = new StringBuffer(600);
        buffer.append("<?xml version='1.0' ?>");
        buffer.append("<a:searchrequest xmlns:a='DAV:'><a:sql>\r\n");
        buffer.append("SELECT \"urn:schemas:httpmail:read\", \"DAV:uid\"\r\n");
        buffer.append(" FROM \"\"\r\n");
        buffer.append(" WHERE \"DAV:ishidden\"=False AND \"DAV:isfolder\"=False AND ");
        for (int i = 0, count = uids.length; i < count; i++)
        {
            if (i != 0)
            {
                buffer.append("  OR ");
            }

            buffer.append(" \"DAV:uid\"='"+uids[i]+"' ");

        }
        buffer.append("\r\n");
        buffer.append("</a:sql></a:searchrequest>\r\n");
        return buffer.toString();
    }

    private String getMessageFlagsXml(String[] uids) throws MessagingException
    {
        if (uids.length == 0)
        {
            throw new MessagingException("Attempt to get flags on 0 length array for uids");
        }

        StringBuffer buffer = new StringBuffer(200);
        buffer.append("<?xml version='1.0' ?>");
        buffer.append("<a:searchrequest xmlns:a='DAV:'><a:sql>\r\n");
        buffer.append("SELECT \"urn:schemas:httpmail:read\", \"DAV:uid\"\r\n");
        buffer.append(" FROM \"\"\r\n");
        buffer.append(" WHERE \"DAV:ishidden\"=False AND \"DAV:isfolder\"=False AND ");

        for (int i = 0, count = uids.length; i < count; i++)
        {
            if (i != 0)
            {
                buffer.append(" OR ");
            }
            buffer.append(" \"DAV:uid\"='"+uids[i]+"' ");
        }
        buffer.append("\r\n");
        buffer.append("</a:sql></a:searchrequest>\r\n");
        return buffer.toString();
    }

    private String getMarkMessagesReadXml(String[] urls, boolean read)
    {
        StringBuffer buffer = new StringBuffer(600);
        buffer.append("<?xml version='1.0' ?>\r\n");
        buffer.append("<a:propertyupdate xmlns:a='DAV:' xmlns:b='urn:schemas:httpmail:'>\r\n");
        buffer.append("<a:target>\r\n");
        for (int i = 0, count = urls.length; i < count; i++)
        {
            buffer.append(" <a:href>"+urls[i]+"</a:href>\r\n");
        }
        buffer.append("</a:target>\r\n");
        buffer.append("<a:set>\r\n");
        buffer.append(" <a:prop>\r\n");
        buffer.append("  <b:read>" + (read ? "1" : "0") + "</b:read>\r\n");
        buffer.append(" </a:prop>\r\n");
        buffer.append("</a:set>\r\n");
        buffer.append("</a:propertyupdate>\r\n");
        return buffer.toString();
    }

    // For flag:
//    http://www.devnewsgroups.net/group/microsoft.public.exchange.development/topic27175.aspx
    //"<m:0x10900003>1</m:0x10900003>" & _

    private String getMoveOrCopyMessagesReadXml(String[] urls, boolean isMove)
    {

        String action = (isMove ? "move" : "copy");
        StringBuffer buffer = new StringBuffer(600);
        buffer.append("<?xml version='1.0' ?>\r\n");
        buffer.append("<a:" + action + " xmlns:a='DAV:' xmlns:b='urn:schemas:httpmail:'>\r\n");
        buffer.append("<a:target>\r\n");
        for (int i = 0, count = urls.length; i < count; i++)
        {
            buffer.append(" <a:href>"+urls[i]+"</a:href>\r\n");
        }
        buffer.append("</a:target>\r\n");

        buffer.append("</a:" + action + ">\r\n");
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
    public void authenticate() throws MessagingException
    {
        try
        {
            doFBA();
            //this.mAuthCookies = doAuthentication(this.mUsername, this.mPassword, this.mUrl);
        }
        catch (IOException ioe)
        {
            Log.e(K9.LOG_TAG, "Error during authentication: " + ioe + "\nStack: " + processException(ioe));
            throw new MessagingException("Error during authentication", ioe);
        }

        if (this.mAuthCookies == null)
        {
            this.mAuthenticated = false;
        }
        else
        {
            this.mAuthenticated = true;
            this.mLastAuth = System.currentTimeMillis()/1000;
        }
    }

    /**
     * Determines if a new authentication is needed.
     * Returns true if new authentication is needed.
     */
    public boolean needAuth()
    {
        boolean status = false;
        long currentTime = -1;
        if (this.mAuthenticated == false)
        {
            status = true;
        }

        currentTime = System.currentTimeMillis()/1000;
        if ((currentTime - this.mLastAuth) > (this.mAuthTimeout))
        {
            status = true;
        }
        return status;
    }

    public static String getHttpRequestResponse(HttpEntity request, HttpEntity response) throws IllegalStateException, IOException
    {
        String responseText = "";
        String requestText = "";
        if (response != null)
        {
            BufferedReader reader = new BufferedReader(new InputStreamReader(WebDavHttpClient.getUngzippedContent(response)), 8192);
            String tempText = "";

            while ((tempText = reader.readLine()) != null)
            {
                responseText += tempText;
            }
        }
        if (request != null)
        {
            BufferedReader reader = new BufferedReader(new InputStreamReader(WebDavHttpClient.getUngzippedContent(response)), 8192);
            String tempText = "";

            while ((tempText = reader.readLine()) != null)
            {
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
    public void doFBA() throws IOException, MessagingException
    {
        /*    public CookieStore doAuthentication(String username, String password,
              String url) throws IOException, MessagingException {*/
        String authPath;
        String url = this.mUrl;
        String username = this.mUsername;
        String password = this.mPassword;
        String[] urlParts = url.split("/");
        String finalUrl = "";
        String loginUrl = "";
        String destinationUrl = "";

        if (this.mAuthPath != null &&
                !this.mAuthPath.equals("") &&
                !this.mAuthPath.equals("/"))
        {
            authPath = this.mAuthPath;
        }
        else
        {
            authPath = "/exchweb/bin/auth/owaauth.dll";
        }

        for (int i = 0; i <= 2; i++)
        {
            if (i != 0)
            {
                finalUrl = finalUrl + "/" + urlParts[i];
            }
            else
            {
                finalUrl = urlParts[i];
            }
        }

        if (finalUrl.equals(""))
        {
            throw new MessagingException("doFBA failed, unable to construct URL to post login credentials to.");
        }

        loginUrl = finalUrl + authPath;

        try
        {
            /* Browser Client */
            WebDavHttpClient httpclient = mHttpClient;

            /**
             * This is in a separate block because I really don't like how it's done.
             * This basically scrapes the OWA login page for the form submission URL.
             * UGLY!WebDavHttpClient
             * Added an if-check to see if there's a user supplied authentication path for FBA
             */
            if (this.mAuthPath == null ||
                    this.mAuthPath.equals("") ||
                    this.mAuthPath.equals("/"))
            {

                httpclient.addRequestInterceptor(new HttpRequestInterceptor()
                {
                    public void process(HttpRequest request, HttpContext context)
                    throws HttpException, IOException
                    {
                        mRedirectUrl = ((HttpHost) context.getAttribute(ExecutionContext.HTTP_TARGET_HOST)).toURI() + request.getRequestLine().getUri();
                    }
                });
                HashMap<String, String> headers = new HashMap<String, String>();
                InputStream istream = sendRequest(finalUrl, "GET", null, headers, false);

                if (istream != null)
                {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(istream), 4096);
                    String tempText = "";
                    boolean matched = false;

                    while ((tempText = reader.readLine()) != null &&
                            !matched)
                    {
                        if (tempText.indexOf(" action") >= 0)
                        {
                            String[] tagParts = tempText.split("\"");
                            if (tagParts[1].lastIndexOf('/') < 0 &&
                                    mRedirectUrl != null &&
                                    !mRedirectUrl.equals(""))
                            {
                                /* We have to do a multi-stage substring here because of potential GET parameters */
                                mRedirectUrl = mRedirectUrl.substring(0, mRedirectUrl.lastIndexOf('?'));
                                mRedirectUrl = mRedirectUrl.substring(0, mRedirectUrl.lastIndexOf('/'));
                                loginUrl = mRedirectUrl + "/" + tagParts[1];
                                this.mAuthPath = "/" + tagParts[1];
                            }
                            else
                            {
                                loginUrl = finalUrl + tagParts[1];
                                this.mAuthPath = "/" + tagParts[1];
                            }
                        }

                        if (tempText.indexOf("destination") >= 0)
                        {
                            String[] tagParts = tempText.split("value");
                            if (tagParts[1] != null)
                            {
                                String[] valueParts = tagParts[1].split("\"");
                                destinationUrl = valueParts[1];
                                matched = true;
                            }
                        }
                    }
                    istream.close();
                }
            }


            /** Build the POST data to use */
            ArrayList<BasicNameValuePair> pairs = new ArrayList<BasicNameValuePair>();
            pairs.add(new BasicNameValuePair("username", username));
            pairs.add(new BasicNameValuePair("password", password));
            if (this.mMailboxPath != null &&
                    !this.mMailboxPath.equals(""))
            {
                pairs.add(new BasicNameValuePair("destination", finalUrl + this.mMailboxPath));
            }
            else if (destinationUrl != null &&
                     !destinationUrl.equals(""))
            {
                pairs.add(new BasicNameValuePair("destination", destinationUrl));
            }
            else
            {
                pairs.add(new BasicNameValuePair("destination", "/"));
            }
            pairs.add(new BasicNameValuePair("flags", "0"));
            pairs.add(new BasicNameValuePair("SubmitCreds", "Log+On"));
            pairs.add(new BasicNameValuePair("forcedownlevel", "0"));
            pairs.add(new BasicNameValuePair("trusted", "0"));

            try
            {
                UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(pairs);
                HashMap<String, String> headers = new HashMap<String, String>();
                String tempUrl = "";
                InputStream istream = sendRequest(loginUrl, "POST", formEntity, headers, false);

                /** Get the URL for the mailbox and set it for the store */
                if (istream != null)
                {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(istream), 8192);
                    String tempText = "";

                    while ((tempText = reader.readLine()) != null)
                    {
                        if (tempText.indexOf("BASE href") >= 0)
                        {
                            String[] tagParts = tempText.split("\"");
                            tempUrl = tagParts[1];
                        }
                    }
                }

                if (this.mMailboxPath != null &&
                        !this.mMailboxPath.equals(""))
                {
                    this.mUrl = finalUrl + "/" + this.mMailboxPath + "/";
                }
                else if (tempUrl.equals(""))
                {
                    this.mUrl = finalUrl + "/Exchange/" + this.alias + "/";
                }
                else
                {
                    this.mUrl = tempUrl;
                }

            }
            catch (UnsupportedEncodingException uee)
            {
                Log.e(K9.LOG_TAG, "Error encoding POST data for authentication: " + uee + "\nTrace: " + processException(uee));
                throw new MessagingException("Error encoding POST data for authentication", uee);
            }
        }
        catch (SSLException e)
        {
            throw new CertificateValidationException(e.getMessage(), e);
        }

        this.mAuthenticated = true;
    }

    public CookieStore getAuthCookies()
    {
        return mAuthCookies;
    }

    public String getAlias()
    {
        return alias;
    }

    public String getUrl()
    {
        return mUrl;
    }

    public WebDavHttpClient getHttpClient() throws MessagingException
    {
        SchemeRegistry reg;
        Scheme s;
        boolean needAuth = false;

        if (mHttpClient == null)
        {
            mHttpClient = new WebDavHttpClient();
            needAuth = true;
        }

        reg = mHttpClient.getConnectionManager().getSchemeRegistry();
        try
        {
            // Log.i(K9.LOG_TAG, "getHttpClient mHost = " + mHost);
            s = new Scheme("https", new TrustedSocketFactory(mHost, mSecure), 443);
        }
        catch (NoSuchAlgorithmException nsa)
        {
            Log.e(K9.LOG_TAG, "NoSuchAlgorithmException in getHttpClient: " + nsa);
            throw new MessagingException("NoSuchAlgorithmException in getHttpClient: " + nsa);
        }
        catch (KeyManagementException kme)
        {
            Log.e(K9.LOG_TAG, "KeyManagementException in getHttpClient: " + kme);
            throw new MessagingException("KeyManagementException in getHttpClient: " + kme);
        }
        reg.register(s);

        if (needAuth)
        {
            HashMap<String, String> headers = new HashMap<String, String>();
            processRequest(this.mUrl, "GET", null, headers, false);
        }

        /*
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
                    Log.e(K9.LOG_TAG, "IOException: " + ioe + "\nTrace: " + processException(ioe));
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
        */
        return mHttpClient;
    }

    public WebDavHttpClient getTrustedHttpClient() throws KeyManagementException, NoSuchAlgorithmException
    {
        if (mHttpClient == null)
        {
            mHttpClient = new WebDavHttpClient();
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

    private InputStream sendRequest(String url, String method, StringEntity messageBody, HashMap<String, String> headers, boolean tryAuth)
    throws MessagingException
    {
        WebDavHttpClient httpclient;
        InputStream istream = null;

        if (url == null ||
                method == null)
        {
            return istream;
        }

        httpclient = getHttpClient();

        try
        {
            int statusCode = -1;
            HttpGeneric httpmethod = new HttpGeneric(url);
            HttpResponse response;
            HttpEntity entity;

            if (messageBody != null)
            {
                httpmethod.setEntity(messageBody);
            }

            for (String headerName : headers.keySet())
            {
                httpmethod.setHeader(headerName, headers.get(headerName));
            }

            if (mAuthString != null && mAuthenticated)
            {
                httpmethod.setHeader("Authorization", mAuthString);
            }

            httpmethod.setMethod(method);
            response = httpclient.executeOverride(httpmethod);
            statusCode = response.getStatusLine().getStatusCode();

            entity = response.getEntity();

            if (statusCode == 401)
            {
                if (tryAuth)
                {
                    mAuthenticated = true;
                    sendRequest(url, method, messageBody, headers, false);
                }
                else
                {
                    throw new MessagingException("Invalid username or password for Basic authentication");
                }
            }
            else if (statusCode == 440)
            {
                if (tryAuth)
                {
                    doFBA();
                    sendRequest(url, method, messageBody, headers, false);
                }
                else
                {
                    throw new MessagingException("Authentication failure in sendRequest");
                }
            }
            else if (statusCode < 200 ||
                     statusCode >= 300)
            {
                throw new IOException("Error with code " + statusCode + " during request processing: "+
                                      response.getStatusLine().toString());
            }
            else
            {
                if (tryAuth &&
                        mAuthenticated == false)
                {
                    doFBA();
                    sendRequest(url, method, messageBody, headers, false);
                }
            }

            if (entity != null)
            {
                istream = WebDavHttpClient.getUngzippedContent(entity);
            }
        }
        catch (UnsupportedEncodingException uee)
        {
            Log.e(K9.LOG_TAG, "UnsupportedEncodingException: " + uee + "\nTrace: " + processException(uee));
            throw new MessagingException("UnsupportedEncodingException", uee);
        }
        catch (IOException ioe)
        {
            Log.e(K9.LOG_TAG, "IOException: " + ioe + "\nTrace: " + processException(ioe));
            throw new MessagingException("IOException", ioe);
        }

        return istream;
    }

    public String getAuthString()
    {
        return mAuthString;
    }

    /**
     * Performs an httprequest to the supplied url using the supplied method.
     * messageBody and headers are optional as not all requests will need them.
     * There are two signatures to support calls that don't require parsing of the response.
     */
    private DataSet processRequest(String url, String method, String messageBody, HashMap<String, String> headers)
    throws MessagingException
    {
        return processRequest(url, method, messageBody, headers, true);
    }

    private DataSet processRequest(String url, String method, String messageBody, HashMap<String, String> headers, boolean needsParsing)
    throws MessagingException
    {
        DataSet dataset = new DataSet();
        if (K9.DEBUG)
        {
            Log.v(K9.LOG_TAG, "processRequest url = '" + url + "', method = '" + method + "', messageBody = '" + messageBody + "'");
        }

        if (url == null ||
                method == null)
        {
            return dataset;
        }

        getHttpClient();

        try
        {
            StringEntity messageEntity = null;
            if (messageBody != null)
            {
                messageEntity = new StringEntity(messageBody);
                messageEntity.setContentType("text/xml");
                //                httpmethod.setEntity(messageEntity);
            }
            InputStream istream = sendRequest(url, method, messageEntity, headers, true);
            if (istream != null &&
                    needsParsing)
            {
                try
                {
                    SAXParserFactory spf = SAXParserFactory.newInstance();
                    SAXParser sp = spf.newSAXParser();
                    XMLReader xr = sp.getXMLReader();
                    WebDavHandler myHandler = new WebDavHandler();

                    xr.setContentHandler(myHandler);

                    xr.parse(new InputSource(istream));

                    dataset = myHandler.getDataSet();
                }
                catch (SAXException se)
                {
                    Log.e(K9.LOG_TAG, "SAXException in processRequest() " + se + "\nTrace: " + processException(se));
                    throw new MessagingException("SAXException in processRequest() ", se);
                }
                catch (ParserConfigurationException pce)
                {
                    Log.e(K9.LOG_TAG, "ParserConfigurationException in processRequest() " + pce + "\nTrace: " + processException(pce));
                    throw new MessagingException("ParserConfigurationException in processRequest() ", pce);
                }

                istream.close();
            }
        }
        catch (UnsupportedEncodingException uee)
        {
            Log.e(K9.LOG_TAG, "UnsupportedEncodingException: " + uee + "\nTrace: " + processException(uee));
            throw new MessagingException("UnsupportedEncodingException in processRequest() ", uee);
        }
        catch (IOException ioe)
        {
            Log.e(K9.LOG_TAG, "IOException: " + ioe + "\nTrace: " + processException(ioe));
            throw new MessagingException("IOException in processRequest() ", ioe);
        }

        return dataset;
    }

    /**
     * Returns a string of the stacktrace for a Throwable to allow for easy inline printing of errors.
     */
    private String processException(Throwable t)
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);

        t.printStackTrace(ps);
        ps.close();

        return baos.toString();
    }

    @Override
    public boolean isSendCapable()
    {
        return true;
    }

    @Override
    public void sendMessages(Message[] messages) throws MessagingException
    {
        WebDavFolder tmpFolder = (WebDavStore.WebDavFolder)getFolder(DAV_MAIL_TMP_FOLDER);
        try
        {
            tmpFolder.open(OpenMode.READ_WRITE);
            Message[] retMessages = tmpFolder.appendWebDavMessages(messages);

            tmpFolder.moveMessages(retMessages, getSendSpoolFolder());
        }
        finally
        {
            if (tmpFolder != null)
            {
                tmpFolder.close();
            }
        }
    }

    /*************************************************************************
     * Helper and Inner classes
     */

    /**
     * A WebDav Folder
     */
    class WebDavFolder extends Folder
    {
        private String mName;
        private String mFolderUrl;
        private boolean mIsOpen = false;
        private int mMessageCount = 0;
        private int mUnreadMessageCount = 0;
        private WebDavStore store;

        protected WebDavStore getStore()
        {
            return store;
        }


        public WebDavFolder(WebDavStore nStore, String name)
        {
            super(nStore.getAccount());
            store = nStore;
            this.mName = name;


            if (DAV_MAIL_SEND_FOLDER.equals(name))
            {
                this.mFolderUrl = getUrl() + "/" + name +"/";
            }
            else
            {
                String encodedName = "";
                try
                {
                    String[] urlParts = name.split("/");
                    String url = "";
                    for (int i = 0, count = urlParts.length; i < count; i++)
                    {
                        if (i != 0)
                        {
                            url = url + "/" + java.net.URLEncoder.encode(urlParts[i], "UTF-8");
                        }
                        else
                        {
                            url = java.net.URLEncoder.encode(urlParts[i], "UTF-8");
                        }
                    }
                    encodedName = url;
                }
                catch (UnsupportedEncodingException uee)
                {
                    Log.e(K9.LOG_TAG, "UnsupportedEncodingException URLEncoding folder name, skipping encoded");
                    encodedName = name;
                }

                encodedName = encodedName.replaceAll("\\+", "%20");

                /**
                 * In some instances, it is possible that our folder objects have been collected,
                 * but getPersonalNamespaces() isn't called again (ex. Android destroys the email client).
                 * Perform an authentication to get the appropriate URLs in place again
                 */
                // TODO: danapple0 - huh?
                //getHttpClient();

                if (encodedName.equals("INBOX"))
                {
                    encodedName = "Inbox";
                }
                this.mFolderUrl = WebDavStore.this.mUrl;
                if (WebDavStore.this.mUrl.endsWith("/") == false)
                {
                    this.mFolderUrl += "/";
                }
                this.mFolderUrl += encodedName;
            }
        }

        public void setUrl(String url)
        {
            if (url != null)
            {
                this.mFolderUrl = url;
            }
        }

        @Override
        public void open(OpenMode mode) throws MessagingException
        {
            getHttpClient();

            this.mIsOpen = true;
        }

        @Override
        public void copyMessages(Message[] messages, Folder folder) throws MessagingException
        {
            moveOrCopyMessages(messages, folder.getName(), false);
        }

        @Override
        public void moveMessages(Message[] messages, Folder folder) throws MessagingException
        {
            moveOrCopyMessages(messages, folder.getName(), true);
        }

        @Override
        public void delete(Message[] msgs, String trashFolderName) throws MessagingException
        {
            moveOrCopyMessages(msgs, trashFolderName, true);
        }
        private void moveOrCopyMessages(Message[] messages, String folderName, boolean isMove) throws MessagingException
        {
            String[] uids = new String[messages.length];

            for (int i = 0, count = messages.length; i < count; i++)
            {
                uids[i] = messages[i].getUid();
            }
            String messageBody = "";
            HashMap<String, String> headers = new HashMap<String, String>();
            HashMap<String, String> uidToUrl = getMessageUrls(uids);
            String[] urls = new String[uids.length];

            for (int i = 0, count = uids.length; i < count; i++)
            {
                urls[i] = uidToUrl.get(uids[i]);
                if (urls[i] == null && messages[i] instanceof WebDavMessage)
                {
                    WebDavMessage wdMessage = (WebDavMessage)messages[i];
                    urls[i] = wdMessage.getUrl();
                }
            }

            messageBody = getMoveOrCopyMessagesReadXml(urls, isMove);
            WebDavFolder destFolder = (WebDavFolder)store.getFolder(folderName);
            headers.put("Destination", destFolder.mFolderUrl);
            headers.put("Brief", "t");
            headers.put("If-Match", "*");
            String action = (isMove ? "BMOVE" : "BCOPY");
            Log.i(K9.LOG_TAG, "Moving " + messages.length + " messages to " + destFolder.mFolderUrl);

            processRequest(mFolderUrl, action, messageBody, headers, false);

        }

        private int getMessageCount(boolean read, CookieStore authCookies) throws MessagingException
        {
            String isRead;
            int messageCount = 0;
            DataSet dataset = new DataSet();
            HashMap<String, String> headers = new HashMap<String, String>();
            String messageBody;

            if (read)
            {
                isRead = "True";
            }
            else
            {
                isRead = "False";
            }

            messageBody = getMessageCountXml(isRead);
            headers.put("Brief", "t");
            dataset = processRequest(this.mFolderUrl, "SEARCH", messageBody, headers);
            if (dataset != null)
            {
                messageCount = dataset.getMessageCount();
            }


            return messageCount;
        }

        @Override
        public int getMessageCount() throws MessagingException
        {
            open(OpenMode.READ_WRITE);
            this.mMessageCount = getMessageCount(true, WebDavStore.this.mAuthCookies);

            return this.mMessageCount;
        }

        @Override
        public int getUnreadMessageCount() throws MessagingException
        {
            open(OpenMode.READ_WRITE);
            this.mUnreadMessageCount = getMessageCount(false, WebDavStore.this.mAuthCookies);

            return this.mUnreadMessageCount;
        }
        @Override
        public int getFlaggedMessageCount() throws MessagingException
        {
            return -1;
        }

        @Override
        public boolean isOpen()
        {
            return this.mIsOpen;
        }

        @Override
        public OpenMode getMode() throws MessagingException
        {
            return OpenMode.READ_WRITE;
        }

        @Override
        public String getName()
        {
            return this.mName;
        }

        @Override
        public boolean exists()
        {
            return true;
        }

        @Override
        public void close()
        {
            this.mMessageCount = 0;
            this.mUnreadMessageCount = 0;

            this.mIsOpen = false;
        }

        @Override
        public boolean create(FolderType type) throws MessagingException
        {
            return true;
        }

        @Override
        public void delete(boolean recursive) throws MessagingException
        {
            throw new Error("WebDavFolder.delete() not implemeneted");
        }

        @Override
        public Message getMessage(String uid) throws MessagingException
        {
            return new WebDavMessage(uid, this);
        }

        @Override
        public Message[] getMessages(int start, int end, MessageRetrievalListener listener)
        throws MessagingException
        {
            ArrayList<Message> messages = new ArrayList<Message>();
            String[] uids;
            DataSet dataset = new DataSet();
            HashMap<String, String> headers = new HashMap<String, String>();
            int uidsLength = -1;

            String messageBody;
            int prevStart = start;

            /** Reverse the message range since 0 index is newest */
            start = this.mMessageCount - end;
            end = start + (end - prevStart);

            //end = this.mMessageCount - prevStart;

            if (start < 0 || end < 0 || end < start)
            {
                throw new MessagingException(String.format("Invalid message set %d %d", start, end));
            }

            if (start == 0 && end < 10)
            {
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

            for (int i = 0; i < uidsLength; i++)
            {
                if (listener != null)
                {
                    listener.messageStarted(uids[i], i, uidsLength);
                }
                WebDavMessage message = new WebDavMessage(uids[i], this);
                message.setUrl(uidToUrl.get(uids[i]));
                messages.add(message);

                if (listener != null)
                {
                    listener.messageFinished(message, i, uidsLength);
                }
            }

            return messages.toArray(new Message[] {});
        }


        @Override
        public Message[] getMessages(MessageRetrievalListener listener) throws MessagingException
        {
            return getMessages(null, listener);
        }

        @Override
        public Message[] getMessages(String[] uids, MessageRetrievalListener listener) throws MessagingException
        {
            ArrayList<Message> messageList = new ArrayList<Message>();
            Message[] messages;

            if (uids == null ||
                    uids.length == 0)
            {
                return messageList.toArray(new Message[] {});
            }

            for (int i = 0, count = uids.length; i < count; i++)
            {
                if (listener != null)
                {
                    listener.messageStarted(uids[i], i, count);
                }

                WebDavMessage message = new WebDavMessage(uids[i], this);
                messageList.add(message);

                if (listener != null)
                {
                    listener.messageFinished(message, i, count);
                }
            }
            messages = messageList.toArray(new Message[] {});

            return messages;
        }

        private HashMap<String, String> getMessageUrls(String[] uids) throws MessagingException
        {
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
        throws MessagingException
        {

            if (messages == null ||
                    messages.length == 0)
            {
                return;
            }

            /**
             * Fetch message envelope information for the array
             */
            if (fp.contains(FetchProfile.Item.ENVELOPE))
            {
                fetchEnvelope(messages, listener);
            }
            /**
             * Fetch message flag info for the array
             */
            if (fp.contains(FetchProfile.Item.FLAGS))
            {
                fetchFlags(messages, listener);
            }



            if (fp.contains(FetchProfile.Item.BODY_SANE))
            {
                fetchMessages(messages, listener, FETCH_BODY_SANE_SUGGESTED_SIZE / 76);
            }

            if (fp.contains(FetchProfile.Item.BODY))
            {
                fetchMessages(messages, listener, -1);
            }

//            if (fp.contains(FetchProfile.Item.STRUCTURE)) {
//                for (int i = 0, count = messages.length; i < count; i++) {
//                    if (!(messages[i] instanceof WebDavMessage)) {
//                        throw new MessagingException("WebDavStore fetch called with non-WebDavMessage");
//                    }
//                    WebDavMessage wdMessage = (WebDavMessage) messages[i];
//
//                    if (listener != null) {
//                        listener.messageStarted(wdMessage.getUid(), i, count);
//                    }
//
//                    wdMessage.setBody(null);
//
//                    if (listener != null) {
//                        listener.messageFinished(wdMessage, i, count);
//                    }
//                }
//            }
        }

        /**
         * Fetches the full messages or up to lines lines and passes them to the message parser.
         */
        private void fetchMessages(Message[] messages, MessageRetrievalListener listener, int lines) throws MessagingException
        {
            WebDavHttpClient httpclient;
            httpclient = getHttpClient();

            /**
             * We can't hand off to processRequest() since we need the stream to parse.
             */
            for (int i = 0, count = messages.length; i < count; i++)
            {
                WebDavMessage wdMessage;
                int statusCode = 0;

                if (!(messages[i] instanceof WebDavMessage))
                {
                    throw new MessagingException("WebDavStore fetch called with non-WebDavMessage");
                }

                wdMessage = (WebDavMessage) messages[i];

                if (listener != null)
                {
                    listener.messageStarted(wdMessage.getUid(), i, count);
                }

                /**
                 * If fetch is called outside of the initial list (ie, a locally stored
                 * message), it may not have a URL associated.  Verify and fix that
                 */
                if (wdMessage.getUrl().equals(""))
                {
                    wdMessage.setUrl(getMessageUrls(new String[] {wdMessage.getUid()}).get(wdMessage.getUid()));
                    Log.i(K9.LOG_TAG, "Fetching messages with UID = '" + wdMessage.getUid() + "', URL = '" + wdMessage.getUrl() + "'");
                    if (wdMessage.getUrl().equals(""))
                    {
                        throw new MessagingException("Unable to get URL for message");
                    }
                }

                try
                {
                    Log.i(K9.LOG_TAG, "Fetching message with UID = '" + wdMessage.getUid() + "', URL = '" + wdMessage.getUrl() + "'");
                    HttpGet httpget = new HttpGet(new URI(wdMessage.getUrl()));
                    HttpResponse response;
                    HttpEntity entity;

                    httpget.setHeader("translate", "f");
                    if (mAuthString != null && mAuthenticated)
                    {
                        httpget.setHeader("Authorization", mAuthString);
                    }
                    response = httpclient.executeOverride(httpget);

                    statusCode = response.getStatusLine().getStatusCode();

                    entity = response.getEntity();

                    if (statusCode < 200 ||
                            statusCode > 300)
                    {
                        throw new IOException("Error during with code " + statusCode + " during fetch: "
                                              + response.getStatusLine().toString());
                    }

                    if (entity != null)
                    {
                        InputStream istream = null;
                        StringBuffer buffer = new StringBuffer();
                        String tempText = "";
                        String resultText = "";
                        BufferedReader reader;
                        int currentLines = 0;

                        istream = WebDavHttpClient.getUngzippedContent(entity);

                        if (lines != -1)
                        {
                            reader = new BufferedReader(new InputStreamReader(istream), 8192);

                            while ((tempText = reader.readLine()) != null &&
                                    (currentLines < lines))
                            {
                                buffer.append(tempText+"\r\n");
                                currentLines++;
                            }

                            istream.close();
                            resultText = buffer.toString();
                            istream = new ByteArrayInputStream(resultText.getBytes("UTF-8"));
                        }

                        wdMessage.parse(istream);
                    }

                }
                catch (IllegalArgumentException iae)
                {
                    Log.e(K9.LOG_TAG, "IllegalArgumentException caught " + iae + "\nTrace: " + processException(iae));
                    throw new MessagingException("IllegalArgumentException caught", iae);
                }
                catch (URISyntaxException use)
                {
                    Log.e(K9.LOG_TAG, "URISyntaxException caught " + use + "\nTrace: " + processException(use));
                    throw new MessagingException("URISyntaxException caught", use);
                }
                catch (IOException ioe)
                {
                    Log.e(K9.LOG_TAG, "Non-success response code loading message, response code was " + statusCode + "\nURL: " + wdMessage.getUrl() + "\nError: " + ioe.getMessage() + "\nTrace: " + processException(ioe));
                    throw new MessagingException("Failure code " + statusCode, ioe);
                }

                if (listener != null)
                {
                    listener.messageFinished(wdMessage, i, count);
                }
            }
        }

        /**
         * Fetches and sets the message flags for the supplied messages.
         * The idea is to have this be recursive so that we do a series of medium calls
         * instead of one large massive call or a large number of smaller calls.
         */
        private void fetchFlags(Message[] startMessages, MessageRetrievalListener listener) throws MessagingException
        {
            HashMap<String, Boolean> uidToReadStatus = new HashMap<String, Boolean>();
            HashMap<String, String> headers = new HashMap<String, String>();
            DataSet dataset = new DataSet();
            String messageBody = "";
            Message[] messages = new Message[20];
            String[] uids;


            if (startMessages == null ||
                    startMessages.length == 0)
            {
                return;
            }

            if (startMessages.length > 20)
            {
                Message[] newMessages = new Message[startMessages.length - 20];
                for (int i = 0, count = startMessages.length; i < count; i++)
                {
                    if (i < 20)
                    {
                        messages[i] = startMessages[i];
                    }
                    else
                    {
                        newMessages[i - 20] = startMessages[i];
                    }
                }

                fetchFlags(newMessages, listener);
            }
            else
            {
                messages = startMessages;
            }

            uids = new String[messages.length];

            for (int i = 0, count = messages.length; i < count; i++)
            {
                uids[i] = messages[i].getUid();
            }

            messageBody = getMessageFlagsXml(uids);
            headers.put("Brief", "t");
            dataset = processRequest(this.mFolderUrl, "SEARCH", messageBody, headers);

            if (dataset == null)
            {
                throw new MessagingException("Data Set from request was null");
            }

            uidToReadStatus = dataset.getUidToRead();

            for (int i = 0, count = messages.length; i < count; i++)
            {
                if (!(messages[i] instanceof WebDavMessage))
                {
                    throw new MessagingException("WebDavStore fetch called with non-WebDavMessage");
                }
                WebDavMessage wdMessage = (WebDavMessage) messages[i];

                if (listener != null)
                {
                    listener.messageStarted(messages[i].getUid(), i, count);
                }

                wdMessage.setFlagInternal(Flag.SEEN, uidToReadStatus.get(wdMessage.getUid()));

                if (listener != null)
                {
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
        private void fetchEnvelope(Message[] startMessages, MessageRetrievalListener listener) throws MessagingException
        {
            HashMap<String, ParsedMessageEnvelope> envelopes = new HashMap<String, ParsedMessageEnvelope>();
            HashMap<String, String> headers = new HashMap<String, String>();
            DataSet dataset = new DataSet();
            String messageBody = "";
            String[] uids;
            Message[] messages = new Message[10];

            if (startMessages == null ||
                    startMessages.length == 0)
            {
                return;
            }

            if (startMessages.length > 10)
            {
                Message[] newMessages = new Message[startMessages.length - 10];
                for (int i = 0, count = startMessages.length; i < count; i++)
                {
                    if (i < 10)
                    {
                        messages[i] = startMessages[i];
                    }
                    else
                    {
                        newMessages[i - 10] = startMessages[i];
                    }
                }

                fetchEnvelope(newMessages, listener);
            }
            else
            {
                messages = startMessages;
            }

            uids = new String[messages.length];

            for (int i = 0, count = messages.length; i < count; i++)
            {
                uids[i] = messages[i].getUid();
            }

            messageBody = getMessageEnvelopeXml(uids);
            headers.put("Brief", "t");
            dataset = processRequest(this.mFolderUrl, "SEARCH", messageBody, headers);

            envelopes = dataset.getMessageEnvelopes();

            int count = messages.length;
            for (int i = messages.length - 1; i >= 0; i--)
            {
                if (!(messages[i] instanceof WebDavMessage))
                {
                    throw new MessagingException("WebDavStore fetch called with non-WebDavMessage");
                }
                WebDavMessage wdMessage = (WebDavMessage) messages[i];

                if (listener != null)
                {
                    listener.messageStarted(messages[i].getUid(), i, count);
                }

                wdMessage.setNewHeaders(envelopes.get(wdMessage.getUid()));
                wdMessage.setFlagInternal(Flag.SEEN, envelopes.get(wdMessage.getUid()).getReadStatus());

                if (listener != null)
                {
                    listener.messageFinished(messages[i], i, count);
                }
            }
        }

        @Override
        public Flag[] getPermanentFlags() throws MessagingException
        {
            return PERMANENT_FLAGS;
        }

        @Override
        public void setFlags(Message[] messages, Flag[] flags, boolean value)
        throws MessagingException
        {
            String[] uids = new String[messages.length];

            for (int i = 0, count = messages.length; i < count; i++)
            {
                uids[i] = messages[i].getUid();
            }

            for (int i = 0, count = flags.length; i < count; i++)
            {
                Flag flag = flags[i];

                if (flag == Flag.SEEN)
                {
                    markServerMessagesRead(uids, value);
                }
                else if (flag == Flag.DELETED)
                {
                    deleteServerMessages(uids);
                }
            }
        }

        private void markServerMessagesRead(String[] uids, boolean read) throws MessagingException
        {
            String messageBody = "";
            HashMap<String, String> headers = new HashMap<String, String>();
            HashMap<String, String> uidToUrl = getMessageUrls(uids);
            String[] urls = new String[uids.length];

            for (int i = 0, count = uids.length; i < count; i++)
            {
                urls[i] = uidToUrl.get(uids[i]);
            }

            messageBody = getMarkMessagesReadXml(urls, read);
            headers.put("Brief", "t");
            headers.put("If-Match", "*");

            processRequest(this.mFolderUrl, "BPROPPATCH", messageBody, headers, false);
        }

        private void deleteServerMessages(String[] uids) throws MessagingException
        {
            HashMap<String, String> uidToUrl = getMessageUrls(uids);

            for (int i = 0, count = uids.length; i < count; i++)
            {
                HashMap<String, String> headers = new HashMap<String, String>();
                String uid = uids[i];
                String url = uidToUrl.get(uid);
                String destinationUrl = generateDeleteUrl(url);

                /**
                 * If the destination is the same as the origin, assume delete forever
                 */
                if (destinationUrl.equals(url))
                {
                    headers.put("Brief", "t");
                    processRequest(url, "DELETE", null, headers, false);
                }
                else
                {
                    headers.put("Destination", generateDeleteUrl(url));
                    headers.put("Brief", "t");
                    processRequest(url, "MOVE", null, headers, false);
                }
            }
        }

        private String generateDeleteUrl(String startUrl)
        {
            String[] urlParts = startUrl.split("/");
            String filename = urlParts[urlParts.length - 1];
            String finalUrl = WebDavStore.this.mUrl + "Deleted%20Items/" + filename;

            return finalUrl;
        }

        @Override
        public void appendMessages(Message[] messages) throws MessagingException
        {
            appendWebDavMessages(messages);
        }

        public Message[] appendWebDavMessages(Message[] messages) throws MessagingException
        {

            Message[] retMessages = new Message[messages.length];
            int ind = 0;

            WebDavHttpClient httpclient = getHttpClient();

            for (Message message : messages)
            {
                HttpGeneric httpmethod;
                HttpResponse response;
                StringEntity bodyEntity;
                int statusCode;

                try
                {
                    /*
                    String subject;

                    try
                    {
                        subject = message.getSubject();
                    }
                    catch (MessagingException e)
                    {
                        Log.e(K9.LOG_TAG, "MessagingException while retrieving Subject: " + e);
                        subject = "";
                    }
                    */

                    ByteArrayOutputStream out;
                    try
                    {
                        out = new ByteArrayOutputStream(message.getSize());
                    }
                    catch (MessagingException e)
                    {
                        Log.e(K9.LOG_TAG, "MessagingException while getting size of message: " + e);
                        out = new ByteArrayOutputStream();
                    }
                    open(OpenMode.READ_WRITE);
                    EOLConvertingOutputStream msgOut = new EOLConvertingOutputStream(
                        new BufferedOutputStream(out, 1024));
                    message.writeTo(msgOut);
                    msgOut.flush();

                    bodyEntity = new StringEntity(out.toString(), "UTF-8");
                    bodyEntity.setContentType("message/rfc822");

                    String messageURL = mFolderUrl;
                    if (messageURL.endsWith("/") == false)
                    {
                        messageURL += "/";
                    }
                    messageURL += URLEncoder.encode(message.getUid() + ":" + System.currentTimeMillis() + ".eml");

                    Log.i(K9.LOG_TAG, "Uploading message as " + messageURL);

                    httpmethod = new HttpGeneric(messageURL);
                    httpmethod.setMethod("PUT");
                    httpmethod.setEntity(bodyEntity);

                    String mAuthString = getAuthString();

                    if (mAuthString != null)
                    {
                        httpmethod.setHeader("Authorization", mAuthString);
                    }

                    response = httpclient.executeOverride(httpmethod);
                    statusCode = response.getStatusLine().getStatusCode();

                    if (statusCode < 200 ||
                            statusCode > 300)
                    {
                        throw new IOException("Error with status code " + statusCode
                                              + " while sending/appending message.  Response = "
                                              + response.getStatusLine().toString() + " for message " + messageURL);
                    }
                    WebDavMessage retMessage = new WebDavMessage(message.getUid(), this);

                    retMessage.setUrl(messageURL);
                    retMessages[ind++] = retMessage;
                }
                catch (Exception e)
                {
                    throw new MessagingException("Unable to append", e);
                }

            }
            return retMessages;
        }

        @Override
        public boolean equals(Object o)
        {
            return false;
        }

        @Override
        public int hashCode()
        {
            return super.hashCode();
        }

        @Override
        public String getUidFromMessageId(Message message) throws MessagingException
        {
            Log.e(K9.LOG_TAG, "Unimplemented method getUidFromMessageId in WebDavStore.WebDavFolder could lead to duplicate messages "
                  + " being uploaded to the Sent folder");
            return null;
        }

        @Override
        public void setFlags(Flag[] flags, boolean value) throws MessagingException
        {
            Log.e(K9.LOG_TAG, "Unimplemented method setFlags(Flag[], boolean) breaks markAllMessagesAsRead and EmptyTrash");
            // Try to make this efficient by not retrieving all of the messages
            return;
        }
    }

    /**
     * A WebDav Message
     */
    class WebDavMessage extends MimeMessage
    {
        private String mUrl = "";


        WebDavMessage(String uid, Folder folder) throws MessagingException
        {
            this.mUid = uid;
            this.mFolder = folder;
        }

        public void setUrl(String url)
        {
            //TODO: This is a not as ugly hack (ie, it will actually work)
            //XXX: prevent URLs from getting to us that are broken
            if (!(url.toLowerCase().contains("http")))
            {
                if (!(url.startsWith("/")))
                {
                    url = "/" + url;
                }
                url = WebDavStore.this.mUrl + this.mFolder + url;
            }

            String[] urlParts = url.split("/");
            int length = urlParts.length;
            String end = urlParts[length - 1];

            this.mUrl = "";
            url = "";

            /**
             * We have to decode, then encode the URL because Exchange likes to
             * not properly encode all characters
             */
            try
            {
                end = java.net.URLDecoder.decode(end, "UTF-8");
                end = java.net.URLEncoder.encode(end, "UTF-8");
                end = end.replaceAll("\\+", "%20");
            }
            catch (UnsupportedEncodingException uee)
            {
                Log.e(K9.LOG_TAG, "UnsupportedEncodingException caught in setUrl: " + uee + "\nTrace: " + processException(uee));
            }
            catch (IllegalArgumentException iae)
            {
                Log.e(K9.LOG_TAG, "IllegalArgumentException caught in setUrl: " + iae + "\nTrace: " + processException(iae));
            }

            for (int i = 0; i < length - 1; i++)
            {
                if (i != 0)
                {
                    url = url + "/" + urlParts[i];
                }
                else
                {
                    url = urlParts[i];
                }
            }

            url = url + "/" + end;

            this.mUrl = url;
        }

        public String getUrl()
        {
            return this.mUrl;
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

        public void setNewHeaders(ParsedMessageEnvelope envelope) throws MessagingException
        {
            String[] headers = envelope.getHeaderList();
            HashMap<String, String> messageHeaders = envelope.getMessageHeaders();

            for (int i = 0, count = headers.length; i < count; i++)
            {
                String headerValue = messageHeaders.get(headers[i]);
                if (headers[i].equals("Content-Length"))
                {
                    int size = Integer.parseInt(messageHeaders.get(headers[i]));
                    this.setSize(size);
                }

                if (headerValue != null &&
                        !headerValue.equals(""))
                {
                    this.addHeader(headers[i], headerValue);
                }
            }
        }


        @Override
        public void delete(String trashFolderName) throws MessagingException
        {
            WebDavFolder wdFolder = (WebDavFolder)getFolder();
            Log.i(K9.LOG_TAG, "Deleting message by moving to " + trashFolderName);
            wdFolder.moveMessages(new Message[] { this }, wdFolder.getStore().getFolder(trashFolderName));

        }

        @Override
        public void setFlag(Flag flag, boolean set) throws MessagingException
        {
            super.setFlag(flag, set);
            mFolder.setFlags(new Message[] { this }, new Flag[] { flag }, set);
        }
    }

    /**
     * XML Parsing Handler
     * Can handle all XML handling needs
     */
    public class WebDavHandler extends DefaultHandler
    {
        private DataSet mDataSet = new DataSet();
        private Stack<String> mOpenTags = new Stack<String>();

        public DataSet getDataSet()
        {
            return this.mDataSet;
        }

        @Override
        public void startDocument() throws SAXException
        {
            this.mDataSet = new DataSet();
        }

        @Override
        public void endDocument() throws SAXException
        {
            /* Do nothing */
        }

        @Override
        public void startElement(String namespaceURI, String localName,
                                 String qName, Attributes atts) throws SAXException
        {
            mOpenTags.push(localName);
        }

        @Override
        public void endElement(String namespaceURI, String localName, String qName)
        {
            mOpenTags.pop();

            /** Reset the hash temp variables */
            if (localName.equals("response"))
            {
                this.mDataSet.finish();
            }
        }

        @Override
        public void characters(char ch[], int start, int length)
        {
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
    public class ParsedMessageEnvelope
    {
        /**
         * Holds the mappings from the name returned from Exchange to the MIME format header name
         */
        private final HashMap<String, String> mHeaderMappings = new HashMap<String, String>()
        {
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
        private String mUid = "";
        private HashMap<String, String> mMessageHeaders = new HashMap<String, String>();
        private ArrayList<String> mHeaders = new ArrayList<String>();

        public void addHeader(String field, String value)
        {
            String headerName = mHeaderMappings.get(field);
            //Log.i(K9.LOG_TAG, "header " + headerName + " = '" + value + "'");

            if (headerName != null)
            {
                this.mMessageHeaders.put(mHeaderMappings.get(field), value);
                this.mHeaders.add(mHeaderMappings.get(field));
            }
        }

        public HashMap<String, String> getMessageHeaders()
        {
            return this.mMessageHeaders;
        }

        public String[] getHeaderList()
        {
            return this.mHeaders.toArray(new String[] {});
        }

        public void setReadStatus(boolean status)
        {
            this.mReadStatus = status;
        }

        public boolean getReadStatus()
        {
            return this.mReadStatus;
        }

        public void setUid(String uid)
        {
            if (uid != null)
            {
                this.mUid = uid;
            }
        }

        public String getUid()
        {
            return this.mUid;
        }
    }

    /**
     * Dataset for all XML parses.
     * Data is stored in a single format inside the class and is formatted appropriately depending on the accessor calls made.
     */
    public class DataSet
    {
        private HashMap<String, HashMap<String, String>> mData = new HashMap<String, HashMap<String, String>>();
        //private HashMap<String, String> mLostData = new HashMap<String, String>();
        private String mUid = "";
        private HashMap<String, String> mTempData = new HashMap<String, String>();

        public void addValue(String value, String tagName)
        {
            if (tagName.equals("uid"))
            {
                mUid = value;
            }

            if (mTempData.containsKey(tagName))
            {
                mTempData.put(tagName, mTempData.get(tagName) + value);
            }
            else
            {
                mTempData.put(tagName, value);
            }
        }

        public void finish()
        {
            if (mUid != null &&
                    mTempData != null)
            {
                mData.put(mUid, mTempData);
            }
            else if (mTempData != null)
            {
                /* Lost Data are for requests that don't include a message UID.
                 * These requests should only have a depth of one for the response so it will never get stomped over.
                 */
                //mLostData = mTempData;
                //String visibleCount = mLostData.get("visiblecount");
            }

            mUid = "";
            mTempData = new HashMap<String, String>();
        }

        /**
         * Returns a hashmap of Message UID => Message Url
         */
        public HashMap<String, String> getUidToUrl()
        {
            HashMap<String, String> uidToUrl = new HashMap<String, String>();

            for (String uid : mData.keySet())
            {
                HashMap<String, String> data = mData.get(uid);
                String value = data.get("href");
                if (value != null &&
                        !value.equals(""))
                {
                    uidToUrl.put(uid, value);
                }
            }

            return uidToUrl;
        }

        /**
         * Returns a hashmap of Message UID => Read Status
         */
        public HashMap<String, Boolean> getUidToRead()
        {
            HashMap<String, Boolean> uidToRead = new HashMap<String, Boolean>();

            for (String uid : mData.keySet())
            {
                HashMap<String, String> data = mData.get(uid);
                String readStatus = data.get("read");
                if (readStatus != null &&
                        !readStatus.equals(""))
                {
                    Boolean value = readStatus.equals("0") ? false : true;
                    uidToRead.put(uid, value);
                }
            }

            return uidToRead;
        }

        /**
         * Returns an array of all hrefs (urls) that were received
         */
        public String[] getHrefs()
        {
            ArrayList<String> hrefs = new ArrayList<String>();

            for (String uid : mData.keySet())
            {
                HashMap<String, String> data = mData.get(uid);
                String href = data.get("href");
                hrefs.add(href);
            }

            return hrefs.toArray(new String[] {});
        }

        /**
         * Return an array of all Message UIDs that were received
         */
        public String[] getUids()
        {
            ArrayList<String> uids = new ArrayList<String>();

            for (String uid : mData.keySet())
            {
                uids.add(uid);
            }

            return uids.toArray(new String[] {});
        }

        /**
         * Returns the message count as it was retrieved
         */
        public int getMessageCount()
        {
            int messageCount = -1;

            for (String uid : mData.keySet())
            {
                HashMap<String, String> data = mData.get(uid);
                String count = data.get("visiblecount");

                if (count != null &&
                        !count.equals(""))
                {
                    messageCount = Integer.parseInt(count);
                }

            }

            return messageCount;
        }

        /**
         * Returns a HashMap of message UID => ParsedMessageEnvelope
         */
        public HashMap<String, ParsedMessageEnvelope> getMessageEnvelopes()
        {
            HashMap<String, ParsedMessageEnvelope> envelopes = new HashMap<String, ParsedMessageEnvelope>();

            for (String uid : mData.keySet())
            {
                ParsedMessageEnvelope envelope = new ParsedMessageEnvelope();
                HashMap<String, String> data = mData.get(uid);

                if (data != null)
                {
                    for (String header : data.keySet())
                    {
                        if (header.equals("read"))
                        {
                            String read = data.get(header);
                            Boolean readStatus = read.equals("0") ? false : true;

                            envelope.setReadStatus(readStatus);
                        }
                        else if (header.equals("date"))
                        {
                            /**
                             * Exchange doesn't give us rfc822 dates like it claims.  The date is in the format:
                             * yyyy-MM-dd'T'HH:mm:ss.SSS<Single digit representation of timezone, so far, all instances are Z>
                             */
                            String date = data.get(header);
                            date = date.substring(0, date.length() - 1);

                            DateFormat dfInput = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
                            DateFormat dfOutput = new SimpleDateFormat("EEE, d MMM yy HH:mm:ss Z");
                            String tempDate = "";

                            try
                            {
                                Date parsedDate = dfInput.parse(date);
                                tempDate = dfOutput.format(parsedDate);
                            }
                            catch (java.text.ParseException pe)
                            {
                                Log.e(K9.LOG_TAG, "Error parsing date: "+ pe + "\nTrace: " + processException(pe));
                            }
                            envelope.addHeader(header, tempDate);
                        }
                        else
                        {
                            envelope.addHeader(header, data.get(header));
                        }
                    }
                }

                if (envelope != null)
                {
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
    public class HttpGeneric extends HttpEntityEnclosingRequestBase
    {
        public String METHOD_NAME = "POST";

        public HttpGeneric()
        {
            super();
        }

        public HttpGeneric(final URI uri)
        {
            super();
            setURI(uri);
        }

        /**
         * @throws IllegalArgumentException if the uri is invalid.
         */
        public HttpGeneric(final String uri)
        {
            super();

            if (K9.DEBUG)
            {
                Log.v(K9.LOG_TAG, "Starting uri = '" + uri + "'");
            }

            String[] urlParts = uri.split("/");
            int length = urlParts.length;
            String end = urlParts[length - 1];
            String url = "";

            /**
             * We have to decode, then encode the URL because Exchange likes to
             * not properly encode all characters
             */
            try
            {
                if (length > 3)
                {
                    end = java.net.URLDecoder.decode(end, "UTF-8");
                    end = java.net.URLEncoder.encode(end, "UTF-8");
                    end = end.replaceAll("\\+", "%20");
                }
            }
            catch (UnsupportedEncodingException uee)
            {
                Log.e(K9.LOG_TAG, "UnsupportedEncodingException caught in HttpGeneric(String uri): " + uee + "\nTrace: " + processException(uee));
            }
            catch (IllegalArgumentException iae)
            {
                Log.e(K9.LOG_TAG, "IllegalArgumentException caught in HttpGeneric(String uri): " + iae + "\nTrace: " + processException(iae));
            }

            for (int i = 0; i < length - 1; i++)
            {
                if (i != 0)
                {
                    url = url + "/" + urlParts[i];
                }
                else
                {
                    url = urlParts[i];
                }
            }
            if (K9.DEBUG)
            {
                Log.v(K9.LOG_TAG, "url = '" + url + "' length = " + url.length()
                      + ", end = '" + end + "' length = " + end.length());
            }
            url = url + "/" + end;

            Log.i(K9.LOG_TAG, "url = " + url);
            setURI(URI.create(url));
        }

        @Override
        public String getMethod()
        {
            return METHOD_NAME;
        }

        public void setMethod(String method)
        {
            if (method != null)
            {
                METHOD_NAME = method;
            }
        }
    }
    public static class WebDavHttpClient extends DefaultHttpClient
    {
        /*
        * Copyright (C) 2007 The Android Open Source Project
        *
        * Licensed under the Apache License, Version 2.0 (the "License");
        * you may not use this file except in compliance with the License.
        * You may obtain a copy of the License at
        *
        *      http://www.apache.org/licenses/LICENSE-2.0
        *
        * Unless required by applicable law or agreed to in writing, software
        * distributed under the License is distributed on an "AS IS" BASIS,
        * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
        * See the License for the specific language governing permissions and
        * limitations under the License.
        */
        public static void modifyRequestToAcceptGzipResponse(HttpRequest request)
        {
            Log.i(K9.LOG_TAG, "Requesting gzipped data");
            request.addHeader("Accept-Encoding", "gzip");
        }
        public static InputStream getUngzippedContent(HttpEntity entity)
        throws IOException
        {
            InputStream responseStream = entity.getContent();
            if (responseStream == null) return responseStream;
            Header header = entity.getContentEncoding();
            if (header == null) return responseStream;
            String contentEncoding = header.getValue();
            if (contentEncoding == null) return responseStream;
            if (contentEncoding.contains("gzip"))
            {
                Log.i(K9.LOG_TAG, "Response is gzipped");
                responseStream = new GZIPInputStream(responseStream);
            }
            return responseStream;
        }


        public HttpResponse executeOverride(HttpUriRequest request) throws IOException
        {
            modifyRequestToAcceptGzipResponse(request);
            return super.execute(request);
        }

    }
}
