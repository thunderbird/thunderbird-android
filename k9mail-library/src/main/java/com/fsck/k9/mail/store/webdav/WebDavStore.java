package com.fsck.k9.mail.store.webdav;

import android.util.Log;

import com.fsck.k9.mail.*;
import com.fsck.k9.mail.filter.Base64;
import com.fsck.k9.mail.CertificateValidationException;
import com.fsck.k9.mail.store.RemoteStore;
import com.fsck.k9.mail.store.StoreConfig;

import org.apache.http.*;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.net.ssl.SSLException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import static com.fsck.k9.mail.K9MailLib.DEBUG_PROTOCOL_WEBDAV;
import static com.fsck.k9.mail.K9MailLib.LOG_TAG;
import static com.fsck.k9.mail.helper.UrlEncodingHelper.decodeUtf8;
import static com.fsck.k9.mail.helper.UrlEncodingHelper.encodeUtf8;


/**
 * <pre>
 * Uses WebDAV formatted HTTP calls to an MS Exchange server to fetch email
 * and email information.
 * </pre>
 */
public class WebDavStore extends RemoteStore {

    /**
     * Decodes a WebDavStore URI.
     * <p/>
     * <p>Possible forms:</p>
     * <pre>
     * webdav://user:password@server:port ConnectionSecurity.NONE
     * webdav+ssl+://user:password@server:port ConnectionSecurity.SSL_TLS_REQUIRED
     * </pre>
     */
    public static WebDavStoreSettings decodeUri(String uri) {
        String host;
        int port;
        ConnectionSecurity connectionSecurity;
        String username = null;
        String password = null;
        String alias = null;
        String path = null;
        String authPath = null;
        String mailboxPath = null;


        URI webDavUri;
        try {
            webDavUri = new URI(uri);
        } catch (URISyntaxException use) {
            throw new IllegalArgumentException("Invalid WebDavStore URI", use);
        }

        String scheme = webDavUri.getScheme();
        /*
         * Currently available schemes are:
         * webdav
         * webdav+ssl+
         *
         * The following are obsolete schemes that may be found in pre-existing
         * settings from earlier versions or that may be found when imported. We
         * continue to recognize them and re-map them appropriately:
         * webdav+tls
         * webdav+tls+
         * webdav+ssl
         */
        if (scheme.equals("webdav")) {
            connectionSecurity = ConnectionSecurity.NONE;
        } else if (scheme.startsWith("webdav+")) {
            connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED;
        } else {
            throw new IllegalArgumentException("Unsupported protocol (" + scheme + ")");
        }

        host = webDavUri.getHost();
        if (host.startsWith("http")) {
            String[] hostParts = host.split("://", 2);
            if (hostParts.length > 1) {
                host = hostParts[1];
            }
        }

        port = webDavUri.getPort();

        String userInfo = webDavUri.getUserInfo();
        if (userInfo != null) {
            String[] userInfoParts = userInfo.split(":");
            username = decodeUtf8(userInfoParts[0]);
            String userParts[] = username.split("\\\\", 2);

            if (userParts.length > 1) {
                alias = userParts[1];
            } else {
                alias = username;
            }
            if (userInfoParts.length > 1) {
                password = decodeUtf8(userInfoParts[1]);
            }
        }

        String[] pathParts = webDavUri.getPath().split("\\|");
        for (int i = 0, count = pathParts.length; i < count; i++) {
            if (i == 0) {
                if (pathParts[0] != null &&
                        pathParts[0].length() > 1) {
                    path = pathParts[0];
                }
            } else if (i == 1) {
                if (pathParts[1] != null &&
                        pathParts[1].length() > 1) {
                    authPath = pathParts[1];
                }
            } else if (i == 2) {
                if (pathParts[2] != null &&
                        pathParts[2].length() > 1) {
                    mailboxPath = pathParts[2];
                }
            }
        }

        return new WebDavStoreSettings(host, port, connectionSecurity, null, username, password,
                null, alias, path, authPath, mailboxPath);
    }


    /**
     * Creates a WebDavStore URI with the supplied settings.
     *
     * @param server The {@link ServerSettings} object that holds the server settings.
     * @return A WebDavStore URI that holds the same information as the {@code server} parameter.
     * @see StoreConfig#getStoreUri()
     * @see WebDavStore#decodeUri(String)
     */
    public static String createUri(ServerSettings server) {
        String userEnc = encodeUtf8(server.username);
        String passwordEnc = (server.password != null) ?
                encodeUtf8(server.password) : "";

        String scheme;
        switch (server.connectionSecurity) {
            case SSL_TLS_REQUIRED:
                scheme = "webdav+ssl+";
                break;
            default:
            case NONE:
                scheme = "webdav";
                break;
        }

        String userInfo = userEnc + ":" + passwordEnc;

        String uriPath;
        Map<String, String> extra = server.getExtra();
        if (extra != null) {
            String path = extra.get(WebDavStoreSettings.PATH_KEY);
            path = (path != null) ? path : "";
            String authPath = extra.get(WebDavStoreSettings.AUTH_PATH_KEY);
            authPath = (authPath != null) ? authPath : "";
            String mailboxPath = extra.get(WebDavStoreSettings.MAILBOX_PATH_KEY);
            mailboxPath = (mailboxPath != null) ? mailboxPath : "";
            uriPath = "/" + path + "|" + authPath + "|" + mailboxPath;
        } else {
            uriPath = "/||";
        }

        try {
            return new URI(scheme, userInfo, server.host, server.port, uriPath,
                    null, null).toString();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Can't create WebDavStore URI", e);
        }
    }

    private ConnectionSecurity mConnectionSecurity;
    private String mUsername; /* Stores the username for authentications */
    private String mAlias; /* Stores the alias for the user's mailbox */
    private String mPassword; /* Stores the password for authentications */
    private String mUrl; /* Stores the base URL for the server */
    private String mHost; /* Stores the host name for the server */
    private int mPort;
    private String mPath; /* Stores the path for the server */
    private String mAuthPath; /* Stores the path off of the server to post data to for form based authentication */
    private String mMailboxPath; /* Stores the user specified path to the mailbox */

    private final WebDavHttpClient.WebDavHttpClientFactory mHttpClientFactory;
    private WebDavHttpClient mHttpClient = null;
    private HttpContext mContext = null;
    private String mAuthString;
    private CookieStore mAuthCookies = null;
    private short mAuthentication = WebDavConstants.AUTH_TYPE_NONE;
    private String mCachedLoginUrl;

    private Folder mSendFolder = null;
    private Map<String, WebDavFolder> mFolderList = new HashMap<String, WebDavFolder>();

    public WebDavStore(StoreConfig storeConfig, WebDavHttpClient.WebDavHttpClientFactory clientFactory)
            throws MessagingException {
        super(storeConfig, null);
        mHttpClientFactory = clientFactory;

        WebDavStoreSettings settings;
        try {
            settings = WebDavStore.decodeUri(storeConfig.getStoreUri());
        } catch (IllegalArgumentException e) {
            throw new MessagingException("Error while decoding store URI", e);
        }

        mHost = settings.host;
        mPort = settings.port;

        mConnectionSecurity = settings.connectionSecurity;

        mUsername = settings.username;
        mPassword = settings.password;
        mAlias = settings.alias;

        mPath = settings.path;
        mAuthPath = settings.authPath;
        mMailboxPath = settings.mailboxPath;


        if (mPath == null || mPath.equals("")) {
            mPath = "/Exchange";
        } else if (!mPath.startsWith("/")) {
            mPath = "/" + mPath;
        }

        if (mMailboxPath == null || mMailboxPath.equals("")) {
            mMailboxPath = "/" + mAlias;
        } else if (!mMailboxPath.startsWith("/")) {
            mMailboxPath = "/" + mMailboxPath;
        }

        if (mAuthPath != null &&
                !mAuthPath.equals("") &&
                !mAuthPath.startsWith("/")) {
            mAuthPath = "/" + mAuthPath;
        }

        // The URL typically looks like the following: "https://mail.domain.com/Exchange/alias".
        // The inbox path would look like: "https://mail.domain.com/Exchange/alias/Inbox".
        mUrl = getRoot() + mPath + mMailboxPath;

        mAuthString = "Basic " + Base64.encode(mUsername + ":" + mPassword);
    }

    private String getRoot() {
        String root;
        if (mConnectionSecurity == ConnectionSecurity.SSL_TLS_REQUIRED) {
            root = "https";
        } else {
            root = "http";
        }
        root += "://" + mHost + ":" + mPort;
        return root;
    }

    HttpContext getContext() {
        return mContext;
    }

    short getAuthentication() {
        return mAuthentication;
    }

    StoreConfig getStoreConfig() {
        return mStoreConfig;
    }

    @Override
    public void checkSettings() throws MessagingException {
        authenticate();
    }

    @Override
    public List<? extends Folder> getPersonalNamespaces(boolean forceListAll) throws MessagingException {
        List<Folder> folderList = new LinkedList<Folder>();
        /**
         * We have to check authentication here so we have the proper URL stored
         */
        getHttpClient();

        /**
         *  Firstly we get the "special" folders list (inbox, outbox, etc)
         *  and setup the account accordingly
         */
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Depth", "0");
        headers.put("Brief", "t");
        DataSet dataset = processRequest(this.mUrl, "PROPFIND", getSpecialFoldersList(), headers);

        Map<String, String> specialFoldersMap = dataset.getSpecialFolderToUrl();
        String folderName = getFolderName(specialFoldersMap.get(WebDavConstants.DAV_MAIL_INBOX_FOLDER));
        if (folderName != null) {
            mStoreConfig.setAutoExpandFolderName(folderName);
            mStoreConfig.setInboxFolderName(folderName);
        }

        folderName = getFolderName(specialFoldersMap.get(WebDavConstants.DAV_MAIL_DRAFTS_FOLDER));
        if (folderName != null)
            mStoreConfig.setDraftsFolderName(folderName);

        folderName = getFolderName(specialFoldersMap.get(WebDavConstants.DAV_MAIL_TRASH_FOLDER));
        if (folderName != null)
            mStoreConfig.setTrashFolderName(folderName);

        folderName = getFolderName(specialFoldersMap.get(WebDavConstants.DAV_MAIL_SPAM_FOLDER));
        if (folderName != null)
            mStoreConfig.setSpamFolderName(folderName);

        // K-9 Mail's outbox is a special local folder and different from Exchange/WebDAV's outbox.
        /*
        folderName = getFolderName(specialFoldersMap.get(DAV_MAIL_OUTBOX_FOLDER));
        if (folderName != null)
            mAccount.setOutboxFolderName(folderName);
        */

        folderName = getFolderName(specialFoldersMap.get(WebDavConstants.DAV_MAIL_SENT_FOLDER));
        if (folderName != null)
            mStoreConfig.setSentFolderName(folderName);

        /**
         * Next we get all the folders (including "special" ones)
         */
        headers = new HashMap<String, String>();
        headers.put("Brief", "t");
        dataset = processRequest(this.mUrl, "SEARCH", getFolderListXml(), headers);
        String[] folderUrls = dataset.getHrefs();

        for (String tempUrl : folderUrls) {
            WebDavFolder folder = createFolder(tempUrl);
            if (folder != null)
                folderList.add(folder);
        }

        return folderList;
    }

    /**
     * Creates a folder using the URL passed as parameter (only if it has not been
     * already created) and adds this to our store folder map.
     *
     * @param folderUrl
     * @return
     */
    private WebDavFolder createFolder(String folderUrl) {
        if (folderUrl == null)
            return null;

        WebDavFolder wdFolder = null;
        String folderName = getFolderName(folderUrl);
        if (folderName != null) {
            wdFolder = getFolder(folderName);
            if (wdFolder != null) {
                wdFolder.setUrl(folderUrl);
            }
        }
        // else: Unknown URL format => NO Folder created

        return wdFolder;
    }

    private String getFolderName(String folderUrl) {
        if (folderUrl == null)
            return null;

        // Here we extract the folder name starting from the complete url.
        // folderUrl is in the form http://mail.domain.com/exchange/username/foldername
        // so we need "foldername" which is the string after the fifth slash
        int folderSlash = -1;
        for (int j = 0; j < 5; j++) {
            folderSlash = folderUrl.indexOf('/', folderSlash + 1);
            if (folderSlash < 0)
                break;
        }

        if (folderSlash > 0) {
            String fullPathName;

            // Removes the final slash if present
            if (folderUrl.charAt(folderUrl.length() - 1) == '/')
                fullPathName = folderUrl.substring(folderSlash + 1, folderUrl.length() - 1);
            else
                fullPathName = folderUrl.substring(folderSlash + 1);

            // Decodes the url-encoded folder name (i.e. "My%20folder" => "My Folder"

            return decodeUtf8(fullPathName);
        }

        return null;
    }

    @Override
    public WebDavFolder getFolder(String name) {
        WebDavFolder folder;

        if ((folder = this.mFolderList.get(name)) == null) {
            folder = new WebDavFolder(this, name);
            mFolderList.put(name, folder);
        }

        return folder;
    }

    public Folder getSendSpoolFolder() throws MessagingException {
        if (mSendFolder == null)
            mSendFolder = getFolder(WebDavConstants.DAV_MAIL_SEND_FOLDER);

        return mSendFolder;
    }

    @Override
    public boolean isMoveCapable() {
        return true;
    }

    @Override
    public boolean isCopyCapable() {
        return true;
    }

    private String getSpecialFoldersList() {
        StringBuilder builder = new StringBuilder(200);
        builder.append("<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"no\"?>");
        builder.append("<propfind xmlns=\"DAV:\">");
        builder.append("<prop>");
        builder.append("<").append(WebDavConstants.DAV_MAIL_INBOX_FOLDER).append(" xmlns=\"urn:schemas:httpmail:\"/>");
        builder.append("<").append(WebDavConstants.DAV_MAIL_DRAFTS_FOLDER).append(" xmlns=\"urn:schemas:httpmail:\"/>");
        builder.append("<").append(WebDavConstants.DAV_MAIL_OUTBOX_FOLDER).append(" xmlns=\"urn:schemas:httpmail:\"/>");
        builder.append("<").append(WebDavConstants.DAV_MAIL_SENT_FOLDER).append(" xmlns=\"urn:schemas:httpmail:\"/>");
        builder.append("<").append(WebDavConstants.DAV_MAIL_TRASH_FOLDER).append(" xmlns=\"urn:schemas:httpmail:\"/>");
        // This should always be ##DavMailSubmissionURI## for which we already have a constant
        // buffer.append("<sendmsg xmlns=\"urn:schemas:httpmail:\"/>");

        builder.append("<").append(WebDavConstants.DAV_MAIL_SPAM_FOLDER).append(" xmlns=\"urn:schemas:httpmail:\"/>");

        builder.append("</prop>");
        builder.append("</propfind>");
        return builder.toString();
    }

    /***************************************************************
     * WebDAV XML Request body retrieval functions
     */
    private String getFolderListXml() {
        StringBuilder builder = new StringBuilder(200);
        builder.append("<?xml version='1.0' ?>");
        builder.append("<a:searchrequest xmlns:a='DAV:'><a:sql>\r\n");
        builder.append("SELECT \"DAV:uid\", \"DAV:ishidden\"\r\n");
        builder.append(" FROM SCOPE('deep traversal of \"").append(this.mUrl).append("\"')\r\n");
        builder.append(" WHERE \"DAV:ishidden\"=False AND \"DAV:isfolder\"=True\r\n");
        builder.append("</a:sql></a:searchrequest>\r\n");
        return builder.toString();
    }

    String getMessageCountXml(String messageState) {
        StringBuilder builder = new StringBuilder(200);
        builder.append("<?xml version='1.0' ?>");
        builder.append("<a:searchrequest xmlns:a='DAV:'><a:sql>\r\n");
        builder.append("SELECT \"DAV:visiblecount\"\r\n");
        builder.append(" FROM \"\"\r\n");
        builder.append(" WHERE \"DAV:ishidden\"=False AND \"DAV:isfolder\"=False AND \"urn:schemas:httpmail:read\"=")
                .append(messageState).append("\r\n");
        builder.append(" GROUP BY \"DAV:ishidden\"\r\n");
        builder.append("</a:sql></a:searchrequest>\r\n");
        return builder.toString();
    }

    String getMessageEnvelopeXml(String[] uids) {
        StringBuilder buffer = new StringBuilder(200);
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
            buffer.append(" \"DAV:uid\"='").append(uids[i]).append("' ");
        }
        buffer.append("\r\n");
        buffer.append("</a:sql></a:searchrequest>\r\n");
        return buffer.toString();
    }

    String getMessagesXml() {
        StringBuilder builder = new StringBuilder(200);
        builder.append("<?xml version='1.0' ?>");
        builder.append("<a:searchrequest xmlns:a='DAV:'><a:sql>\r\n");
        builder.append("SELECT \"DAV:uid\"\r\n");
        builder.append(" FROM \"\"\r\n");
        builder.append(" WHERE \"DAV:ishidden\"=False AND \"DAV:isfolder\"=False\r\n");
        builder.append("</a:sql></a:searchrequest>\r\n");
        return builder.toString();
    }

    String getMessageUrlsXml(String[] uids) {
        StringBuilder buffer = new StringBuilder(600);
        buffer.append("<?xml version='1.0' ?>");
        buffer.append("<a:searchrequest xmlns:a='DAV:'><a:sql>\r\n");
        buffer.append("SELECT \"urn:schemas:httpmail:read\", \"DAV:uid\"\r\n");
        buffer.append(" FROM \"\"\r\n");
        buffer.append(" WHERE \"DAV:ishidden\"=False AND \"DAV:isfolder\"=False AND ");
        for (int i = 0, count = uids.length; i < count; i++) {
            if (i != 0) {
                buffer.append("  OR ");
            }

            buffer.append(" \"DAV:uid\"='").append(uids[i]).append("' ");

        }
        buffer.append("\r\n");
        buffer.append("</a:sql></a:searchrequest>\r\n");
        return buffer.toString();
    }

    String getMessageFlagsXml(String[] uids) throws MessagingException {
        if (uids.length == 0) {
            throw new MessagingException("Attempt to get flags on 0 length array for uids");
        }

        StringBuilder buffer = new StringBuilder(200);
        buffer.append("<?xml version='1.0' ?>");
        buffer.append("<a:searchrequest xmlns:a='DAV:'><a:sql>\r\n");
        buffer.append("SELECT \"urn:schemas:httpmail:read\", \"DAV:uid\"\r\n");
        buffer.append(" FROM \"\"\r\n");
        buffer.append(" WHERE \"DAV:ishidden\"=False AND \"DAV:isfolder\"=False AND ");

        for (int i = 0, count = uids.length; i < count; i++) {
            if (i != 0) {
                buffer.append(" OR ");
            }
            buffer.append(" \"DAV:uid\"='").append(uids[i]).append("' ");
        }
        buffer.append("\r\n");
        buffer.append("</a:sql></a:searchrequest>\r\n");
        return buffer.toString();
    }

    String getMarkMessagesReadXml(String[] urls, boolean read) {
        StringBuilder buffer = new StringBuilder(600);
        buffer.append("<?xml version='1.0' ?>\r\n");
        buffer.append("<a:propertyupdate xmlns:a='DAV:' xmlns:b='urn:schemas:httpmail:'>\r\n");
        buffer.append("<a:target>\r\n");
        for (String url : urls) {
            buffer.append(" <a:href>").append(url).append("</a:href>\r\n");
        }
        buffer.append("</a:target>\r\n");
        buffer.append("<a:set>\r\n");
        buffer.append(" <a:prop>\r\n");
        buffer.append("  <b:read>").append(read ? "1" : "0").append("</b:read>\r\n");
        buffer.append(" </a:prop>\r\n");
        buffer.append("</a:set>\r\n");
        buffer.append("</a:propertyupdate>\r\n");
        return buffer.toString();
    }

    // For flag:
    // http://www.devnewsgroups.net/group/microsoft.public.exchange.development/topic27175.aspx
    // "<m:0x10900003>1</m:0x10900003>" & _

    String getMoveOrCopyMessagesReadXml(String[] urls, boolean isMove) {

        String action = (isMove ? "move" : "copy");
        StringBuilder buffer = new StringBuilder(600);
        buffer.append("<?xml version='1.0' ?>\r\n");
        buffer.append("<a:").append(action).append(" xmlns:a='DAV:' xmlns:b='urn:schemas:httpmail:'>\r\n");
        buffer.append("<a:target>\r\n");
        for (String url : urls) {
            buffer.append(" <a:href>").append(url).append("</a:href>\r\n");
        }
        buffer.append("</a:target>\r\n");

        buffer.append("</a:").append(action).append(">\r\n");
        return buffer.toString();
    }

    /***************************************************************
     * Authentication related methods
     */

    /**
     * Determines which type of authentication Exchange is using and authenticates appropriately.
     *
     * @throws MessagingException
     */
    public boolean authenticate()
            throws MessagingException {
        try {
            if (mAuthentication == WebDavConstants.AUTH_TYPE_NONE) {
                ConnectionInfo info = doInitialConnection();

                if (info.requiredAuthType == WebDavConstants.AUTH_TYPE_BASIC) {
                    HttpGeneric request = new HttpGeneric(mUrl);
                    request.setMethod("GET");
                    request.setHeader("Authorization", mAuthString);

                    WebDavHttpClient httpClient = getHttpClient();
                    HttpResponse response = httpClient.executeOverride(request, mContext);

                    int statusCode = response.getStatusLine().getStatusCode();
                    if (statusCode >= 200 && statusCode < 300) {
                        mAuthentication = WebDavConstants.AUTH_TYPE_BASIC;
                    } else if (statusCode == 401) {
                        throw new MessagingException("Invalid username or password for authentication.");
                    } else {
                        throw new MessagingException("Error with code " + response.getStatusLine().getStatusCode() +
                                " during request processing: " + response.getStatusLine().toString());
                    }
                } else if (info.requiredAuthType == WebDavConstants.AUTH_TYPE_FORM_BASED) {
                    doFBA(info);
                }
            } else if (mAuthentication == WebDavConstants.AUTH_TYPE_BASIC) {
                // Nothing to do, we authenticate with every request when
                // using basic authentication.
            } else if (mAuthentication == WebDavConstants.AUTH_TYPE_FORM_BASED) {
                // Our cookie expired, re-authenticate.
                doFBA(null);
            }
        } catch (IOException ioe) {
            Log.e(LOG_TAG, "Error during authentication: " + ioe + "\nStack: " + WebDavUtils.processException(ioe));
            throw new MessagingException("Error during authentication", ioe);
        }

        return mAuthentication != WebDavConstants.AUTH_TYPE_NONE;
    }

    /**
     * Makes the initial connection to Exchange for authentication. Determines the type of authentication necessary for
     * the server.
     *
     * @throws MessagingException
     */
    private ConnectionInfo doInitialConnection()
            throws MessagingException {
        // For our initial connection we are sending an empty GET request to
        // the configured URL, which should be in the following form:
        // https://mail.server.com/Exchange/alias
        //
        // Possible status codes include:
        // 401 - the server uses basic authentication
        // 30x - the server is trying to redirect us to an OWA login
        // 20x - success
        //
        // The latter two indicate form-based authentication.
        ConnectionInfo info = new ConnectionInfo();

        WebDavHttpClient httpClient = getHttpClient();

        HttpGeneric request = new HttpGeneric(mUrl);
        request.setMethod("GET");

        try {
            HttpResponse response = httpClient.executeOverride(request, mContext);
            info.statusCode = response.getStatusLine().getStatusCode();

            if (info.statusCode == 401) {
                // 401 is the "Unauthorized" status code, meaning the server wants
                // an authentication header for basic authentication.
                info.requiredAuthType = WebDavConstants.AUTH_TYPE_BASIC;
            } else if ((info.statusCode >= 200 && info.statusCode < 300) || // Success
                    (info.statusCode >= 300 && info.statusCode < 400) || // Redirect
                    (info.statusCode == 440)) { // Unauthorized
                // We will handle all 3 situations the same. First we take an educated
                // guess at where the authorization DLL is located. If this is this
                // doesn't work, then we'll use the redirection URL for OWA login given
                // to us by exchange. We can use this to scrape the location of the
                // authorization URL.
                info.requiredAuthType = WebDavConstants.AUTH_TYPE_FORM_BASED;

                if (mAuthPath != null && !mAuthPath.equals("")) {
                    // The user specified their own authentication path, use that.
                    info.guessedAuthUrl = getRoot() + mAuthPath;
                } else {
                    // Use the default path to the authentication dll.
                    info.guessedAuthUrl = getRoot() + "/exchweb/bin/auth/owaauth.dll";
                }

                // Determine where the server is trying to redirect us.
                Header location = response.getFirstHeader("Location");
                if (location != null) {
                    info.redirectUrl = location.getValue();
                }
            } else {
                throw new IOException("Error with code " + info.statusCode + " during request processing: " +
                        response.getStatusLine().toString());
            }
        } catch (SSLException e) {
            throw new CertificateValidationException(e.getMessage(), e);
        } catch (IOException ioe) {
            Log.e(LOG_TAG, "IOException: " + ioe + "\nTrace: " + WebDavUtils.processException(ioe));
            throw new MessagingException("IOException", ioe);
        }

        return info;
    }

    /**
     * Performs form-based authentication.
     *
     * @throws MessagingException
     */
    public void doFBA(ConnectionInfo info)
            throws IOException, MessagingException {
        // Clear out cookies from any previous authentication.
        if (mAuthCookies != null) mAuthCookies.clear();

        WebDavHttpClient httpClient = getHttpClient();

        String loginUrl;
        if (info != null) {
            loginUrl = info.guessedAuthUrl;
        } else if (mCachedLoginUrl != null && !mCachedLoginUrl.equals("")) {
            loginUrl = mCachedLoginUrl;
        } else {
            throw new MessagingException("No valid login URL available for form-based authentication.");
        }

        HttpGeneric request = new HttpGeneric(loginUrl);
        request.setMethod("POST");

        // Build the POST data.
        List<BasicNameValuePair> pairs = new ArrayList<BasicNameValuePair>();
        pairs.add(new BasicNameValuePair("destination", mUrl));
        pairs.add(new BasicNameValuePair("username", mUsername));
        pairs.add(new BasicNameValuePair("password", mPassword));
        pairs.add(new BasicNameValuePair("flags", "0"));
        pairs.add(new BasicNameValuePair("SubmitCreds", "Log+On"));
        pairs.add(new BasicNameValuePair("forcedownlevel", "0"));
        pairs.add(new BasicNameValuePair("trusted", "0"));

        UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(pairs);
        request.setEntity(formEntity);

        HttpResponse response = httpClient.executeOverride(request, mContext);
        boolean authenticated = testAuthenticationResponse(response);
        if (!authenticated) {
            // Check the response from the authentication request above for a form action.
            String formAction = findFormAction(WebDavHttpClient.getUngzippedContent(response.getEntity()));
            if (formAction == null) {
                // If there is no form action, try using our redirect URL from the initial connection.
                if (info != null && info.redirectUrl != null && !info.redirectUrl.equals("")) {
                    loginUrl = info.redirectUrl;

                    request = new HttpGeneric(loginUrl);
                    request.setMethod("GET");

                    response = httpClient.executeOverride(request, mContext);
                    formAction = findFormAction(WebDavHttpClient.getUngzippedContent(response.getEntity()));
                }
            }
            if (formAction != null) {
                try {
                    URI formActionUri = new URI(formAction);
                    URI loginUri = new URI(loginUrl);

                    if (formActionUri.isAbsolute()) {
                        // The form action is an absolute URL, just use it.
                        loginUrl = formAction;
                    } else {
                        // Append the form action to our current URL, minus the file name.
                        String urlPath;
                        if (formAction.startsWith("/")) {
                            urlPath = formAction;
                        } else {
                            urlPath = loginUri.getPath();
                            int lastPathPos = urlPath.lastIndexOf('/');
                            if (lastPathPos > -1) {
                                urlPath = urlPath.substring(0, lastPathPos + 1);
                                urlPath = urlPath.concat(formAction);
                            }
                        }

                        // Reconstruct the login URL based on the original login URL and the form action.
                        URI finalUri = new URI(loginUri.getScheme(),
                                loginUri.getUserInfo(),
                                loginUri.getHost(),
                                loginUri.getPort(),
                                urlPath,
                                null,
                                null);
                        loginUrl = finalUri.toString();
                    }

                    // Retry the login using our new URL.
                    request = new HttpGeneric(loginUrl);
                    request.setMethod("POST");
                    request.setEntity(formEntity);

                    response = httpClient.executeOverride(request, mContext);
                    authenticated = testAuthenticationResponse(response);
                } catch (URISyntaxException e) {
                    Log.e(LOG_TAG, "URISyntaxException caught " + e + "\nTrace: " + WebDavUtils.processException(e));
                    throw new MessagingException("URISyntaxException caught", e);
                }
            } else {
                throw new MessagingException("A valid URL for Exchange authentication could not be found.");
            }
        }

        if (authenticated) {
            mAuthentication = WebDavConstants.AUTH_TYPE_FORM_BASED;
            mCachedLoginUrl = loginUrl;
        } else {
            throw new MessagingException("Invalid credentials provided for authentication.");
        }
    }

    /**
     * Searches the specified stream for an HTML form and returns the form's action target.
     *
     * @throws IOException
     */
    private String findFormAction(InputStream istream)
            throws IOException {
        String formAction = null;

        BufferedReader reader = new BufferedReader(new InputStreamReader(istream), 4096);
        String tempText;

        // Read line by line until we find something like: <form action="owaauth.dll"...>.
        while ((tempText = reader.readLine()) != null &&
                formAction == null) {
            if (tempText.contains(" action=")) {
                String[] actionParts = tempText.split(" action=");
                if (actionParts.length > 1 && actionParts[1].length() > 1) {
                    char openQuote = actionParts[1].charAt(0);
                    int closePos = actionParts[1].indexOf(openQuote, 1);
                    if (closePos > 1) {
                        formAction = actionParts[1].substring(1, closePos);
                        // Remove any GET parameters.
                        int quesPos = formAction.indexOf('?');
                        if (quesPos != -1) {
                            formAction = formAction.substring(0, quesPos);
                        }
                    }
                }
            }
        }

        return formAction;
    }

    private boolean testAuthenticationResponse(HttpResponse response)
            throws MessagingException {
        boolean authenticated = false;
        int statusCode = response.getStatusLine().getStatusCode();
        // Exchange 2007 will return a 302 status code no matter what.
        if (((statusCode >= 200 && statusCode < 300) || statusCode == 302) &&
                mAuthCookies != null && !mAuthCookies.getCookies().isEmpty()) {
            // We may be authenticated, we need to send a test request to know for sure.
            // Exchange 2007 adds the same cookies whether the username and password were valid or not.
            ConnectionInfo info = doInitialConnection();
            if (info.statusCode >= 200 && info.statusCode < 300) {
                authenticated = true;
            } else if (info.statusCode == 302) {
                // If we are successfully authenticated, Exchange will try to redirect us to our OWA inbox.
                // Otherwise, it will redirect us to a logon page.
                // Our URL is in the form: https://hostname:port/Exchange/alias.
                // The redirect is in the form: https://hostname:port/owa/alias.
                // Do a simple replace and compare the resulting strings.
                try {
                    String thisPath = new URI(mUrl).getPath();
                    String redirectPath = new URI(info.redirectUrl).getPath();

                    if (!thisPath.endsWith("/")) {
                        thisPath = thisPath.concat("/");
                    }
                    if (!redirectPath.endsWith("/")) {
                        redirectPath = redirectPath.concat("/");
                    }

                    if (redirectPath.equalsIgnoreCase(thisPath)) {
                        authenticated = true;
                    } else {
                        int found = thisPath.indexOf('/', 1);
                        if (found != -1) {
                            String replace = thisPath.substring(0, found + 1);
                            redirectPath = redirectPath.replace("/owa/", replace);
                            if (redirectPath.equalsIgnoreCase(thisPath)) {
                                authenticated = true;
                            }
                        }
                    }
                } catch (URISyntaxException e) {
                    Log.e(LOG_TAG, "URISyntaxException caught " + e + "\nTrace: " + WebDavUtils.processException(e));
                    throw new MessagingException("URISyntaxException caught", e);
                }
            }
        }

        return authenticated;
    }

    public CookieStore getAuthCookies() {
        return mAuthCookies;
    }

    public String getAlias() {
        return mAlias;
    }

    public String getUrl() {
        return mUrl;
    }

    public WebDavHttpClient getHttpClient() throws MessagingException {
        if (mHttpClient == null) {
            mHttpClient = mHttpClientFactory.create();
            // Disable automatic redirects on the http client.
            mHttpClient.getParams().setBooleanParameter("http.protocol.handle-redirects", false);

            // Setup a cookie store for forms-based authentication.
            mContext = new BasicHttpContext();
            mAuthCookies = new BasicCookieStore();
            mContext.setAttribute(ClientContext.COOKIE_STORE, mAuthCookies);

            SchemeRegistry reg = mHttpClient.getConnectionManager().getSchemeRegistry();
            try {
                Scheme s = new Scheme("https", new WebDavSocketFactory(mHost, 443), 443);
                reg.register(s);
            } catch (NoSuchAlgorithmException nsa) {
                Log.e(LOG_TAG, "NoSuchAlgorithmException in getHttpClient: " + nsa);
                throw new MessagingException("NoSuchAlgorithmException in getHttpClient: " + nsa);
            } catch (KeyManagementException kme) {
                Log.e(LOG_TAG, "KeyManagementException in getHttpClient: " + kme);
                throw new MessagingException("KeyManagementException in getHttpClient: " + kme);
            }
        }
        return mHttpClient;
    }

    private InputStream sendRequest(String url, String method, StringEntity messageBody,
                                    Map<String, String> headers, boolean tryAuth)
            throws MessagingException {
        if (url == null || method == null) {
            return null;
        }

        WebDavHttpClient httpClient = getHttpClient();

        try {
            int statusCode;
            HttpGeneric httpMethod = new HttpGeneric(url);
            HttpResponse response;
            HttpEntity entity;

            if (messageBody != null) {
                httpMethod.setEntity(messageBody);
            }

            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    httpMethod.setHeader(entry.getKey(), entry.getValue());
                }
            }

            if (mAuthentication == WebDavConstants.AUTH_TYPE_NONE) {
                if (!tryAuth || !authenticate()) {
                    throw new MessagingException("Unable to authenticate in sendRequest().");
                }
            } else if (mAuthentication == WebDavConstants.AUTH_TYPE_BASIC) {
                httpMethod.setHeader("Authorization", mAuthString);
            }

            httpMethod.setMethod(method);
            response = httpClient.executeOverride(httpMethod, mContext);
            statusCode = response.getStatusLine().getStatusCode();

            entity = response.getEntity();

            if (statusCode == 401) {
                throw new MessagingException("Invalid username or password for Basic authentication.");
            } else if (statusCode == 440) {
                if (tryAuth && mAuthentication == WebDavConstants.AUTH_TYPE_FORM_BASED) {
                    // Our cookie expired, re-authenticate.
                    doFBA(null);
                    sendRequest(url, method, messageBody, headers, false);
                } else {
                    throw new MessagingException("Authentication failure in sendRequest().");
                }
            } else if (statusCode == 302) {
                handleUnexpectedRedirect(response, url);
            } else if (statusCode < 200 || statusCode >= 300) {
                throw new IOException("Error with code " + statusCode + " during request processing: " +
                        response.getStatusLine().toString());
            }

            if (entity != null) {
                return WebDavHttpClient.getUngzippedContent(entity);
            }
        } catch (UnsupportedEncodingException uee) {
            Log.e(LOG_TAG, "UnsupportedEncodingException: " + uee + "\nTrace: " + WebDavUtils.processException(uee));
            throw new MessagingException("UnsupportedEncodingException", uee);
        } catch (IOException ioe) {
            Log.e(LOG_TAG, "IOException: " + ioe + "\nTrace: " + WebDavUtils.processException(ioe));
            throw new MessagingException("IOException", ioe);
        }

        return null;
    }

    private void handleUnexpectedRedirect(HttpResponse response, String url) throws IOException {
        if (response.getFirstHeader("Location") != null) {
            // TODO: This may indicate lack of authentication or may alternatively be something we should follow
            throw new IOException("Unexpected redirect during request processing. " +
                    "Expected response from: "+url+" but told to redirect to:" +
                    response.getFirstHeader("Location").getValue());
        } else {
            throw new IOException("Unexpected redirect during request processing. " +
                    "Expected response from: " + url + " but not told where to redirect to");
        }
    }

    public String getAuthString() {
        return mAuthString;
    }

    /**
     * Performs an httprequest to the supplied url using the supplied method. messageBody and headers are optional as
     * not all requests will need them. There are two signatures to support calls that don't require parsing of the
     * response.
     */
    DataSet processRequest(String url, String method, String messageBody, Map<String, String> headers)
            throws MessagingException {
        return processRequest(url, method, messageBody, headers, true);
    }

    DataSet processRequest(String url, String method, String messageBody, Map<String, String> headers,
                           boolean needsParsing)
            throws MessagingException {
        DataSet dataset = new DataSet();
        if (K9MailLib.isDebug() && DEBUG_PROTOCOL_WEBDAV) {
            Log.v(LOG_TAG, "processRequest url = '" + url + "', method = '" + method + "', messageBody = '"
                    + messageBody + "'");
        }

        if (url == null ||
                method == null) {
            return dataset;
        }

        getHttpClient();

        try {
            StringEntity messageEntity = null;
            if (messageBody != null) {
                messageEntity = new StringEntity(messageBody);
                messageEntity.setContentType("text/xml");
            }
            InputStream istream = sendRequest(url, method, messageEntity, headers, true);
            if (istream != null &&
                    needsParsing) {
                try {
                    SAXParserFactory spf = SAXParserFactory.newInstance();
                    spf.setNamespaceAware(true); //This should be a no-op on Android, but makes the tests work
                    SAXParser sp = spf.newSAXParser();
                    XMLReader xr = sp.getXMLReader();
                    WebDavHandler myHandler = new WebDavHandler();

                    xr.setContentHandler(myHandler);

                    xr.parse(new InputSource(istream));

                    dataset = myHandler.getDataSet();
                } catch (SAXException se) {
                    Log.e(LOG_TAG, "SAXException in processRequest() " + se + "\nTrace: " + WebDavUtils.processException(se));
                    throw new MessagingException("SAXException in processRequest() ", se);
                } catch (ParserConfigurationException pce) {
                    Log.e(LOG_TAG, "ParserConfigurationException in processRequest() " + pce + "\nTrace: "
                            + WebDavUtils.processException(pce));
                    throw new MessagingException("ParserConfigurationException in processRequest() ", pce);
                }

                istream.close();
            }
        } catch (UnsupportedEncodingException uee) {
            Log.e(LOG_TAG, "UnsupportedEncodingException: " + uee + "\nTrace: " + WebDavUtils.processException(uee));
            throw new MessagingException("UnsupportedEncodingException in processRequest() ", uee);
        } catch (IOException ioe) {
            Log.e(LOG_TAG, "IOException: " + ioe + "\nTrace: " + WebDavUtils.processException(ioe));
            throw new MessagingException("IOException in processRequest() ", ioe);
        }

        return dataset;
    }

    @Override
    public boolean isSendCapable() {
        return true;
    }

    @Override
    public void sendMessages(List<? extends Message> messages) throws MessagingException {
        WebDavFolder tmpFolder = (WebDavFolder) getFolder(mStoreConfig.getDraftsFolderName());
        try {
            tmpFolder.open(Folder.OPEN_MODE_RW);
            List<? extends Message> retMessages = tmpFolder.appendWebDavMessages(messages);

            tmpFolder.moveMessages(retMessages, getSendSpoolFolder());
        } finally {
            if (tmpFolder != null) {
                tmpFolder.close();
            }
        }
    }
}
