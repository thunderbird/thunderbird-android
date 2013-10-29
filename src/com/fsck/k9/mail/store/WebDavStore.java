package com.fsck.k9.mail.store;

import android.util.Log;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.controller.MessageRetrievalListener;
import com.fsck.k9.helper.Utility;
import com.fsck.k9.mail.*;

import com.fsck.k9.mail.filter.EOLConvertingOutputStream;
import com.fsck.k9.mail.internet.MimeMessage;
import org.apache.commons.io.IOUtils;
import org.apache.http.*;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
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
import java.util.*;
import java.util.zip.GZIPInputStream;

/**
 * <pre>
 * Uses WebDAV formatted HTTP calls to an MS Exchange server to fetch email
 * and email information.
 * </pre>
 */
public class WebDavStore extends Store {
    public static final String STORE_TYPE = "WebDAV";

    // Security options
    private static final short CONNECTION_SECURITY_NONE = 0;
    private static final short CONNECTION_SECURITY_TLS_OPTIONAL = 1;
    private static final short CONNECTION_SECURITY_TLS_REQUIRED = 2;
    private static final short CONNECTION_SECURITY_SSL_OPTIONAL = 3;
    private static final short CONNECTION_SECURITY_SSL_REQUIRED = 4;

    // Authentication types
    private static final short AUTH_TYPE_NONE = 0;
    private static final short AUTH_TYPE_BASIC = 1;
    private static final short AUTH_TYPE_FORM_BASED = 2;

    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    private static final Message[] EMPTY_MESSAGE_ARRAY = new Message[0];

    // These are the ids used from Exchange server to identify the special folders
    // http://social.technet.microsoft.com/Forums/en/exchangesvrdevelopment/thread/1cd2e98c-8a12-44bd-a3e3-9c5ee9e4e14d
    private static final String DAV_MAIL_INBOX_FOLDER = "inbox";
    private static final String DAV_MAIL_DRAFTS_FOLDER = "drafts";
    private static final String DAV_MAIL_SPAM_FOLDER = "junkemail";
    private static final String DAV_MAIL_SEND_FOLDER = "##DavMailSubmissionURI##";
    private static final String DAV_MAIL_TRASH_FOLDER = "deleteditems";
    private static final String DAV_MAIL_OUTBOX_FOLDER = "outbox";
    private static final String DAV_MAIL_SENT_FOLDER = "sentitems";


    /**
     * Decodes a WebDavStore URI.
     *
     * <p>Possible forms:</p>
     * <pre>
     * webdav://user:password@server:port CONNECTION_SECURITY_NONE
     * webdav+tls://user:password@server:port CONNECTION_SECURITY_TLS_OPTIONAL
     * webdav+tls+://user:password@server:port CONNECTION_SECURITY_TLS_REQUIRED
     * webdav+ssl+://user:password@server:port CONNECTION_SECURITY_SSL_REQUIRED
     * webdav+ssl://user:password@server:port CONNECTION_SECURITY_SSL_OPTIONAL
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
        if (scheme.equals("webdav")) {
            connectionSecurity = ConnectionSecurity.NONE;
        } else if (scheme.equals("webdav+ssl")) {
            connectionSecurity = ConnectionSecurity.SSL_TLS_OPTIONAL;
        } else if (scheme.equals("webdav+ssl+")) {
            connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED;
        } else if (scheme.equals("webdav+tls")) {
            connectionSecurity = ConnectionSecurity.STARTTLS_OPTIONAL;
        } else if (scheme.equals("webdav+tls+")) {
            connectionSecurity = ConnectionSecurity.STARTTLS_REQUIRED;
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
            try {
                String[] userInfoParts = userInfo.split(":");
                username = URLDecoder.decode(userInfoParts[0], "UTF-8");
                String userParts[] = username.split("\\\\", 2);

                if (userParts.length > 1) {
                    alias = userParts[1];
                } else {
                    alias = username;
                }
                if (userInfoParts.length > 1) {
                    password = URLDecoder.decode(userInfoParts[1], "UTF-8");
                }
            } catch (UnsupportedEncodingException enc) {
                // This shouldn't happen since the encoding is hardcoded to UTF-8
                throw new IllegalArgumentException("Couldn't urldecode username or password.", enc);
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
                alias, path, authPath, mailboxPath);
    }

    /**
     * Creates a WebDavStore URI with the supplied settings.
     *
     * @param server
     *         The {@link ServerSettings} object that holds the server settings.
     *
     * @return A WebDavStore URI that holds the same information as the {@code server} parameter.
     *
     * @see Account#getStoreUri()
     * @see WebDavStore#decodeUri(String)
     */
    public static String createUri(ServerSettings server) {
        String userEnc;
        String passwordEnc;
        try {
            userEnc = URLEncoder.encode(server.username, "UTF-8");
            passwordEnc = (server.password != null) ?
                    URLEncoder.encode(server.password, "UTF-8") : "";
        }
        catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("Could not encode username or password", e);
        }

        String scheme;
        switch (server.connectionSecurity) {
            case SSL_TLS_OPTIONAL:
                scheme = "webdav+ssl";
                break;
            case SSL_TLS_REQUIRED:
                scheme = "webdav+ssl+";
                break;
            case STARTTLS_OPTIONAL:
                scheme = "webdav+tls";
                break;
            case STARTTLS_REQUIRED:
                scheme = "webdav+tls+";
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


    /**
     * This class is used to store the decoded contents of an WebDavStore URI.
     *
     * @see WebDavStore#decodeUri(String)
     */
    public static class WebDavStoreSettings extends ServerSettings {
        public static final String ALIAS_KEY = "alias";
        public static final String PATH_KEY = "path";
        public static final String AUTH_PATH_KEY = "authPath";
        public static final String MAILBOX_PATH_KEY = "mailboxPath";

        public final String alias;
        public final String path;
        public final String authPath;
        public final String mailboxPath;

        protected WebDavStoreSettings(String host, int port, ConnectionSecurity connectionSecurity,
                String authenticationType, String username, String password, String alias,
                String path, String authPath, String mailboxPath) {
            super(STORE_TYPE, host, port, connectionSecurity, authenticationType, username,
                    password);
            this.alias = alias;
            this.path = path;
            this.authPath = authPath;
            this.mailboxPath = mailboxPath;
        }

        @Override
        public Map<String, String> getExtra() {
            Map<String, String> extra = new HashMap<String, String>();
            putIfNotNull(extra, ALIAS_KEY, alias);
            putIfNotNull(extra, PATH_KEY, path);
            putIfNotNull(extra, AUTH_PATH_KEY, authPath);
            putIfNotNull(extra, MAILBOX_PATH_KEY, mailboxPath);
            return extra;
        }

        @Override
        public ServerSettings newPassword(String newPassword) {
            return new WebDavStoreSettings(host, port, connectionSecurity, authenticationType,
                    username, newPassword, alias, path, authPath, mailboxPath);
        }
    }


    private short mConnectionSecurity;
    private String mUsername; /* Stores the username for authentications */
    private String mAlias; /* Stores the alias for the user's mailbox */
    private String mPassword; /* Stores the password for authentications */
    private String mUrl; /* Stores the base URL for the server */
    private String mHost; /* Stores the host name for the server */
    private int mPort;
    private String mPath; /* Stores the path for the server */
    private String mAuthPath; /* Stores the path off of the server to post data to for form based authentication */
    private String mMailboxPath; /* Stores the user specified path to the mailbox */

    private boolean mSecure;
    private WebDavHttpClient mHttpClient = null;
    private HttpContext mContext = null;
    private String mAuthString;
    private CookieStore mAuthCookies = null;
    private short mAuthentication = AUTH_TYPE_NONE;
    private String mCachedLoginUrl;

    private Folder mSendFolder = null;
    private HashMap<String, WebDavFolder> mFolderList = new HashMap<String, WebDavFolder>();


    public WebDavStore(Account account) throws MessagingException {
        super(account);

        WebDavStoreSettings settings;
        try {
            settings = decodeUri(mAccount.getStoreUri());
        } catch (IllegalArgumentException e) {
            throw new MessagingException("Error while decoding store URI", e);
        }

        mHost = settings.host;
        mPort = settings.port;

        switch (settings.connectionSecurity) {
        case NONE:
            mConnectionSecurity = CONNECTION_SECURITY_NONE;
            break;
        case STARTTLS_OPTIONAL:
            mConnectionSecurity = CONNECTION_SECURITY_TLS_OPTIONAL;
            break;
        case STARTTLS_REQUIRED:
            mConnectionSecurity = CONNECTION_SECURITY_TLS_REQUIRED;
            break;
        case SSL_TLS_OPTIONAL:
            mConnectionSecurity = CONNECTION_SECURITY_SSL_OPTIONAL;
            break;
        case SSL_TLS_REQUIRED:
            mConnectionSecurity = CONNECTION_SECURITY_SSL_REQUIRED;
            break;
        }

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

        mSecure = mConnectionSecurity == CONNECTION_SECURITY_SSL_REQUIRED;
        mAuthString = "Basic " + Utility.base64Encode(mUsername + ":" + mPassword);
    }

    private String getRoot() {
        String root;
        if (mConnectionSecurity == CONNECTION_SECURITY_TLS_REQUIRED ||
                mConnectionSecurity == CONNECTION_SECURITY_SSL_REQUIRED ||
                mConnectionSecurity == CONNECTION_SECURITY_TLS_OPTIONAL ||
                mConnectionSecurity == CONNECTION_SECURITY_SSL_OPTIONAL) {
            root = "https";
        } else {
            root = "http";
        }
        root += "://" + mHost + ":" + mPort;
        return root;
    }

    @Override
    public void checkSettings() throws MessagingException {
        authenticate();
    }

    @Override
    public List <? extends Folder > getPersonalNamespaces(boolean forceListAll) throws MessagingException {
        LinkedList<Folder> folderList = new LinkedList<Folder>();
        /**
         * We have to check authentication here so we have the proper URL stored
         */
        getHttpClient();

        /**
         *  Firstly we get the "special" folders list (inbox, outbox, etc)
         *  and setup the account accordingly
         */
        HashMap<String, String> headers = new HashMap<String, String>();
        DataSet dataset = new DataSet();
        headers.put("Depth", "0");
        headers.put("Brief", "t");
        dataset = processRequest(this.mUrl, "PROPFIND", getSpecialFoldersList(), headers);

        HashMap<String, String> specialFoldersMap = dataset.getSpecialFolderToUrl();
        String folderName = getFolderName(specialFoldersMap.get(DAV_MAIL_INBOX_FOLDER));
        if (folderName != null) {
            mAccount.setAutoExpandFolderName(folderName);
            mAccount.setInboxFolderName(folderName);
        }

        folderName = getFolderName(specialFoldersMap.get(DAV_MAIL_DRAFTS_FOLDER));
        if (folderName != null)
            mAccount.setDraftsFolderName(folderName);

        folderName = getFolderName(specialFoldersMap.get(DAV_MAIL_TRASH_FOLDER));
        if (folderName != null)
            mAccount.setTrashFolderName(folderName);

        folderName = getFolderName(specialFoldersMap.get(DAV_MAIL_SPAM_FOLDER));
        if (folderName != null)
            mAccount.setSpamFolderName(folderName);

        // K-9 Mail's outbox is a special local folder and different from Exchange/WebDAV's outbox.
        /*
        folderName = getFolderName(specialFoldersMap.get(DAV_MAIL_OUTBOX_FOLDER));
        if (folderName != null)
            mAccount.setOutboxFolderName(folderName);
        */

        folderName = getFolderName(specialFoldersMap.get(DAV_MAIL_SENT_FOLDER));
        if (folderName != null)
            mAccount.setSentFolderName(folderName);

        /**
         * Next we get all the folders (including "special" ones)
         */
        headers = new HashMap<String, String>();
        dataset = new DataSet();
        headers.put("Brief", "t");
        dataset = processRequest(this.mUrl, "SEARCH", getFolderListXml(), headers);
        String[] folderUrls = dataset.getHrefs();

        for (int i = 0; i < folderUrls.length; i++) {
            String tempUrl = folderUrls[i];
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
            if (!this.mFolderList.containsKey(folderName)) {
                wdFolder = new WebDavFolder(this, folderName);
                wdFolder.setUrl(folderUrl);
                mFolderList.put(folderName, wdFolder);
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
            String folderName;
            String fullPathName;

            // Removes the final slash if present
            if (folderUrl.charAt(folderUrl.length() - 1) == '/')
                fullPathName = folderUrl.substring(folderSlash + 1, folderUrl.length() - 1);
            else
                fullPathName = folderUrl.substring(folderSlash + 1);

            // Decodes the url-encoded folder name (i.e. "My%20folder" => "My Folder"
            try {
                folderName = java.net.URLDecoder.decode(fullPathName, "UTF-8");
            } catch (UnsupportedEncodingException uee) {
                /**
                 * If we don't support UTF-8 there's a problem, don't decode
                 * it then
                 */
                folderName = fullPathName;
            }

            return folderName;
        }

        return null;
    }

    @Override
    public Folder getFolder(String name) {
        WebDavFolder folder;

        if ((folder = this.mFolderList.get(name)) == null) {
            folder = new WebDavFolder(this, name);
        }

        return folder;
    }

    public Folder getSendSpoolFolder() throws MessagingException {
        if (mSendFolder == null)
            mSendFolder = getFolder(DAV_MAIL_SEND_FOLDER);

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
        StringBuilder buffer = new StringBuilder(200);
        buffer.append("<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"no\"?>");
        buffer.append("<propfind xmlns=\"DAV:\">");
        buffer.append("<prop>");
        buffer.append("<").append(DAV_MAIL_INBOX_FOLDER).append(" xmlns=\"urn:schemas:httpmail:\"/>");
        buffer.append("<").append(DAV_MAIL_DRAFTS_FOLDER).append(" xmlns=\"urn:schemas:httpmail:\"/>");
        buffer.append("<").append(DAV_MAIL_OUTBOX_FOLDER).append(" xmlns=\"urn:schemas:httpmail:\"/>");
        buffer.append("<").append(DAV_MAIL_SENT_FOLDER).append(" xmlns=\"urn:schemas:httpmail:\"/>");
        buffer.append("<").append(DAV_MAIL_TRASH_FOLDER).append(" xmlns=\"urn:schemas:httpmail:\"/>");
        // This should always be ##DavMailSubmissionURI## for which we already have a constant
        // buffer.append("<sendmsg xmlns=\"urn:schemas:httpmail:\"/>");

        buffer.append("<").append(DAV_MAIL_SPAM_FOLDER).append(" xmlns=\"urn:schemas:httpmail:\"/>");

        buffer.append("</prop>");
        buffer.append("</propfind>");
        return buffer.toString();
    }

    /***************************************************************
     * WebDAV XML Request body retrieval functions
     */
    private String getFolderListXml() {
        StringBuilder buffer = new StringBuilder(200);
        buffer.append("<?xml version='1.0' ?>");
        buffer.append("<a:searchrequest xmlns:a='DAV:'><a:sql>\r\n");
        buffer.append("SELECT \"DAV:uid\", \"DAV:ishidden\"\r\n");
        buffer.append(" FROM SCOPE('hierarchical traversal of \"").append(this.mUrl).append("\"')\r\n");
        buffer.append(" WHERE \"DAV:ishidden\"=False AND \"DAV:isfolder\"=True\r\n");
        buffer.append("</a:sql></a:searchrequest>\r\n");
        return buffer.toString();
    }

    private String getMessageCountXml(String messageState) {
        StringBuilder buffer = new StringBuilder(200);
        buffer.append("<?xml version='1.0' ?>");
        buffer.append("<a:searchrequest xmlns:a='DAV:'><a:sql>\r\n");
        buffer.append("SELECT \"DAV:visiblecount\"\r\n");
        buffer.append(" FROM \"\"\r\n");
        buffer.append(" WHERE \"DAV:ishidden\"=False AND \"DAV:isfolder\"=False AND \"urn:schemas:httpmail:read\"=")
        .append(messageState).append("\r\n");
        buffer.append(" GROUP BY \"DAV:ishidden\"\r\n");
        buffer.append("</a:sql></a:searchrequest>\r\n");
        return buffer.toString();
    }

    private String getMessageEnvelopeXml(String[] uids) {
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

    private String getMessagesXml() {
        StringBuilder buffer = new StringBuilder(200);
        buffer.append("<?xml version='1.0' ?>");
        buffer.append("<a:searchrequest xmlns:a='DAV:'><a:sql>\r\n");
        buffer.append("SELECT \"DAV:uid\"\r\n");
        buffer.append(" FROM \"\"\r\n");
        buffer.append(" WHERE \"DAV:ishidden\"=False AND \"DAV:isfolder\"=False\r\n");
        buffer.append("</a:sql></a:searchrequest>\r\n");
        return buffer.toString();
    }

    private String getMessageUrlsXml(String[] uids) {
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

    private String getMessageFlagsXml(String[] uids) throws MessagingException {
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

    private String getMarkMessagesReadXml(String[] urls, boolean read) {
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

    private String getMoveOrCopyMessagesReadXml(String[] urls, boolean isMove) {

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
            if (mAuthentication == AUTH_TYPE_NONE) {
                ConnectionInfo info = doInitialConnection();

                if (info.requiredAuthType == AUTH_TYPE_BASIC) {
                    HttpGeneric request = new HttpGeneric(mUrl);
                    request.setMethod("GET");
                    request.setHeader("Authorization", mAuthString);

                    WebDavHttpClient httpClient = new WebDavHttpClient();
                    HttpResponse response = httpClient.executeOverride(request, mContext);

                    int statusCode = response.getStatusLine().getStatusCode();
                    if (statusCode >= 200 && statusCode < 300) {
                        mAuthentication = AUTH_TYPE_BASIC;
                    } else if (statusCode == 401) {
                        throw new MessagingException("Invalid username or password for authentication.");
                    } else {
                        throw new MessagingException("Error with code " + response.getStatusLine().getStatusCode() +
                                                     " during request processing: " + response.getStatusLine().toString());
                    }
                } else if (info.requiredAuthType == AUTH_TYPE_FORM_BASED) {
                    doFBA(info);
                }
            } else if (mAuthentication == AUTH_TYPE_BASIC) {
                // Nothing to do, we authenticate with every request when
                // using basic authentication.
            } else if (mAuthentication == AUTH_TYPE_FORM_BASED) {
                // Our cookie expired, re-authenticate.
                doFBA(null);
            }
        } catch (IOException ioe) {
            Log.e(K9.LOG_TAG, "Error during authentication: " + ioe + "\nStack: " + processException(ioe));
            throw new MessagingException("Error during authentication", ioe);
        }

        return mAuthentication != AUTH_TYPE_NONE;
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
                info.requiredAuthType = AUTH_TYPE_BASIC;
            } else if ((info.statusCode >= 200 && info.statusCode < 300) || // Success
                       (info.statusCode >= 300 && info.statusCode < 400) || // Redirect
                       (info.statusCode == 440)) { // Unauthorized
                // We will handle all 3 situations the same. First we take an educated
                // guess at where the authorization DLL is located. If this is this
                // doesn't work, then we'll use the redirection URL for OWA login given
                // to us by exchange. We can use this to scrape the location of the
                // authorization URL.
                info.requiredAuthType = AUTH_TYPE_FORM_BASED;

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
            Log.e(K9.LOG_TAG, "IOException: " + ioe + "\nTrace: " + processException(ioe));
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
        mAuthCookies.clear();

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
        ArrayList<BasicNameValuePair> pairs = new ArrayList<BasicNameValuePair>();
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
                    Log.e(K9.LOG_TAG, "URISyntaxException caught " + e + "\nTrace: " + processException(e));
                    throw new MessagingException("URISyntaxException caught", e);
                }
            } else {
                throw new MessagingException("A valid URL for Exchange authentication could not be found.");
            }
        }

        if (authenticated) {
            mAuthentication = AUTH_TYPE_FORM_BASED;
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
            if (tempText.indexOf(" action=") > -1) {
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
                    Log.e(K9.LOG_TAG, "URISyntaxException caught " + e + "\nTrace: " + processException(e));
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
            mHttpClient = new WebDavHttpClient();
            // Disable automatic redirects on the http client.
            mHttpClient.getParams().setBooleanParameter("http.protocol.handle-redirects", false);

            // Setup a cookie store for forms-based authentication.
            mContext = new BasicHttpContext();
            mAuthCookies = new BasicCookieStore();
            mContext.setAttribute(ClientContext.COOKIE_STORE, mAuthCookies);

            SchemeRegistry reg = mHttpClient.getConnectionManager().getSchemeRegistry();
            try {
                Scheme s = new Scheme("https", new WebDavSocketFactory(mHost, mSecure), 443);
                reg.register(s);
            } catch (NoSuchAlgorithmException nsa) {
                Log.e(K9.LOG_TAG, "NoSuchAlgorithmException in getHttpClient: " + nsa);
                throw new MessagingException("NoSuchAlgorithmException in getHttpClient: " + nsa);
            } catch (KeyManagementException kme) {
                Log.e(K9.LOG_TAG, "KeyManagementException in getHttpClient: " + kme);
                throw new MessagingException("KeyManagementException in getHttpClient: " + kme);
            }
        }
        return mHttpClient;
    }

    private InputStream sendRequest(String url, String method, StringEntity messageBody,
                                    HashMap<String, String> headers, boolean tryAuth)
    throws MessagingException {
        InputStream istream = null;

        if (url == null || method == null) {
            return istream;
        }

        WebDavHttpClient httpclient = getHttpClient();

        try {
            int statusCode = -1;
            HttpGeneric httpmethod = new HttpGeneric(url);
            HttpResponse response;
            HttpEntity entity;

            if (messageBody != null) {
                httpmethod.setEntity(messageBody);
            }

            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    httpmethod.setHeader(entry.getKey(), entry.getValue());
                }
            }

            if (mAuthentication == AUTH_TYPE_NONE) {
                if (!tryAuth || !authenticate()) {
                    throw new MessagingException("Unable to authenticate in sendRequest().");
                }
            } else if (mAuthentication == AUTH_TYPE_BASIC) {
                httpmethod.setHeader("Authorization", mAuthString);
            }

            httpmethod.setMethod(method);
            response = httpclient.executeOverride(httpmethod, mContext);
            statusCode = response.getStatusLine().getStatusCode();

            entity = response.getEntity();

            if (statusCode == 401) {
                throw new MessagingException("Invalid username or password for Basic authentication.");
            } else if (statusCode == 440) {
                if (tryAuth && mAuthentication == AUTH_TYPE_FORM_BASED) {
                    // Our cookie expired, re-authenticate.
                    doFBA(null);
                    sendRequest(url, method, messageBody, headers, false);
                } else {
                    throw new MessagingException("Authentication failure in sendRequest().");
                }
            } else if (statusCode < 200 || statusCode >= 300) {
                throw new IOException("Error with code " + statusCode + " during request processing: " +
                                      response.getStatusLine().toString());
            }

            if (entity != null) {
                istream = WebDavHttpClient.getUngzippedContent(entity);
            }
        } catch (UnsupportedEncodingException uee) {
            Log.e(K9.LOG_TAG, "UnsupportedEncodingException: " + uee + "\nTrace: " + processException(uee));
            throw new MessagingException("UnsupportedEncodingException", uee);
        } catch (IOException ioe) {
            Log.e(K9.LOG_TAG, "IOException: " + ioe + "\nTrace: " + processException(ioe));
            throw new MessagingException("IOException", ioe);
        }

        return istream;
    }

    public String getAuthString() {
        return mAuthString;
    }

    /**
     * Performs an httprequest to the supplied url using the supplied method. messageBody and headers are optional as
     * not all requests will need them. There are two signatures to support calls that don't require parsing of the
     * response.
     */
    private DataSet processRequest(String url, String method, String messageBody, HashMap<String, String> headers)
    throws MessagingException {
        return processRequest(url, method, messageBody, headers, true);
    }

    private DataSet processRequest(String url, String method, String messageBody, HashMap<String, String> headers,
                                   boolean needsParsing)
    throws MessagingException {
        DataSet dataset = new DataSet();
        if (K9.DEBUG && K9.DEBUG_PROTOCOL_WEBDAV) {
            Log.v(K9.LOG_TAG, "processRequest url = '" + url + "', method = '" + method + "', messageBody = '"
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
                    SAXParser sp = spf.newSAXParser();
                    XMLReader xr = sp.getXMLReader();
                    WebDavHandler myHandler = new WebDavHandler();

                    xr.setContentHandler(myHandler);

                    xr.parse(new InputSource(istream));

                    dataset = myHandler.getDataSet();
                } catch (SAXException se) {
                    Log.e(K9.LOG_TAG, "SAXException in processRequest() " + se + "\nTrace: " + processException(se));
                    throw new MessagingException("SAXException in processRequest() ", se);
                } catch (ParserConfigurationException pce) {
                    Log.e(K9.LOG_TAG, "ParserConfigurationException in processRequest() " + pce + "\nTrace: "
                          + processException(pce));
                    throw new MessagingException("ParserConfigurationException in processRequest() ", pce);
                }

                istream.close();
            }
        } catch (UnsupportedEncodingException uee) {
            Log.e(K9.LOG_TAG, "UnsupportedEncodingException: " + uee + "\nTrace: " + processException(uee));
            throw new MessagingException("UnsupportedEncodingException in processRequest() ", uee);
        } catch (IOException ioe) {
            Log.e(K9.LOG_TAG, "IOException: " + ioe + "\nTrace: " + processException(ioe));
            throw new MessagingException("IOException in processRequest() ", ioe);
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

    @Override
    public boolean isSendCapable() {
        return true;
    }

    @Override
    public void sendMessages(Message[] messages) throws MessagingException {
        WebDavFolder tmpFolder = (WebDavStore.WebDavFolder) getFolder(mAccount.getDraftsFolderName());
        try {
            tmpFolder.open(Folder.OPEN_MODE_RW);
            Message[] retMessages = tmpFolder.appendWebDavMessages(messages);

            tmpFolder.moveMessages(retMessages, getSendSpoolFolder());
        } finally {
            if (tmpFolder != null) {
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
    class WebDavFolder extends Folder {
        private String mName;
        private String mFolderUrl;
        private boolean mIsOpen = false;
        private int mMessageCount = 0;
        private int mUnreadMessageCount = 0;
        private WebDavStore store;

        protected WebDavStore getStore() {
            return store;
        }

        public WebDavFolder(WebDavStore nStore, String name) {
            super(nStore.getAccount());
            store = nStore;
            this.mName = name;

            String encodedName = "";
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
                Log.e(K9.LOG_TAG, "UnsupportedEncodingException URLEncoding folder name, skipping encoded");
                encodedName = name;
            }

            encodedName = encodedName.replaceAll("\\+", "%20");

            this.mFolderUrl = WebDavStore.this.mUrl;
            if (!WebDavStore.this.mUrl.endsWith("/")) {
                this.mFolderUrl += "/";
            }
            this.mFolderUrl += encodedName;
        }

        public void setUrl(String url) {
            if (url != null) {
                this.mFolderUrl = url;
            }
        }

        @Override
        public void open(int mode) throws MessagingException {
            getHttpClient();

            this.mIsOpen = true;
        }

        @Override
        public Map<String, String> copyMessages(Message[] messages, Folder folder) throws MessagingException {
            moveOrCopyMessages(messages, folder.getName(), false);
            return null;
        }

        @Override
        public Map<String, String> moveMessages(Message[] messages, Folder folder) throws MessagingException {
            moveOrCopyMessages(messages, folder.getName(), true);
            return null;
        }

        @Override
        public void delete(Message[] msgs, String trashFolderName) throws MessagingException {
            moveOrCopyMessages(msgs, trashFolderName, true);
        }

        private void moveOrCopyMessages(Message[] messages, String folderName, boolean isMove)
        throws MessagingException {
            String[] uids = new String[messages.length];

            for (int i = 0, count = messages.length; i < count; i++) {
                uids[i] = messages[i].getUid();
            }
            String messageBody = "";
            HashMap<String, String> headers = new HashMap<String, String>();
            HashMap<String, String> uidToUrl = getMessageUrls(uids);
            String[] urls = new String[uids.length];

            for (int i = 0, count = uids.length; i < count; i++) {
                urls[i] = uidToUrl.get(uids[i]);
                if (urls[i] == null && messages[i] instanceof WebDavMessage) {
                    WebDavMessage wdMessage = (WebDavMessage) messages[i];
                    urls[i] = wdMessage.getUrl();
                }
            }

            messageBody = getMoveOrCopyMessagesReadXml(urls, isMove);
            WebDavFolder destFolder = (WebDavFolder) store.getFolder(folderName);
            headers.put("Destination", destFolder.mFolderUrl);
            headers.put("Brief", "t");
            headers.put("If-Match", "*");
            String action = (isMove ? "BMOVE" : "BCOPY");
            Log.i(K9.LOG_TAG, "Moving " + messages.length + " messages to " + destFolder.mFolderUrl);

            processRequest(mFolderUrl, action, messageBody, headers, false);
        }

        private int getMessageCount(boolean read) throws MessagingException {
            String isRead;
            int messageCount = 0;
            HashMap<String, String> headers = new HashMap<String, String>();
            String messageBody;

            if (read) {
                isRead = "True";
            } else {
                isRead = "False";
            }

            messageBody = getMessageCountXml(isRead);
            headers.put("Brief", "t");
            DataSet dataset = processRequest(this.mFolderUrl, "SEARCH", messageBody, headers);
            if (dataset != null) {
                messageCount = dataset.getMessageCount();
            }
            if (K9.DEBUG && K9.DEBUG_PROTOCOL_WEBDAV) {
                Log.v(K9.LOG_TAG, "Counted messages and webdav returned: "+messageCount);
            }

            return messageCount;
        }

        @Override
        public int getMessageCount() throws MessagingException {
            open(Folder.OPEN_MODE_RW);
            this.mMessageCount = getMessageCount(true);
            return this.mMessageCount;
        }

        @Override
        public int getUnreadMessageCount() throws MessagingException {
            open(Folder.OPEN_MODE_RW);
            this.mUnreadMessageCount = getMessageCount(false);
            return this.mUnreadMessageCount;
        }

        @Override
        public int getFlaggedMessageCount() throws MessagingException {
            return -1;
        }

        @Override
        public boolean isOpen() {
            return this.mIsOpen;
        }

        @Override
        public int getMode() {
            return Folder.OPEN_MODE_RW;
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
        public void close() {
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
        public Message[] getMessages(int start, int end, Date earliestDate, MessageRetrievalListener listener)
        throws MessagingException {
            ArrayList<Message> messages = new ArrayList<Message>();
            String[] uids;
            HashMap<String, String> headers = new HashMap<String, String>();
            int uidsLength = -1;

            String messageBody;
            int prevStart = start;

            /** Reverse the message range since 0 index is newest */
            start = this.mMessageCount - end;
            end = start + (end - prevStart);

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
            DataSet dataset = processRequest(this.mFolderUrl, "SEARCH", messageBody, headers);

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

            return messages.toArray(EMPTY_MESSAGE_ARRAY);
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
                return messageList.toArray(EMPTY_MESSAGE_ARRAY);
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
            messages = messageList.toArray(EMPTY_MESSAGE_ARRAY);

            return messages;
        }

        private HashMap<String, String> getMessageUrls(String[] uids) throws MessagingException {
            HashMap<String, String> headers = new HashMap<String, String>();
            String messageBody;

            /** Retrieve and parse the XML entity for our messages */
            messageBody = getMessageUrlsXml(uids);
            headers.put("Brief", "t");

            DataSet dataset = processRequest(this.mFolderUrl, "SEARCH", messageBody, headers);
            HashMap<String, String> uidToUrl = dataset.getUidToUrl();

            return uidToUrl;
        }

        @Override
        public void fetch(Message[] messages, FetchProfile fp, MessageRetrievalListener listener)
        throws MessagingException {
            if (messages == null ||
                    messages.length == 0) {
                return;
            }

            /**
             * Fetch message envelope information for the array
             */
            if (fp.contains(FetchProfile.Item.ENVELOPE)) {
                fetchEnvelope(messages, listener);
            }
            /**
             * Fetch message flag info for the array
             */
            if (fp.contains(FetchProfile.Item.FLAGS)) {
                fetchFlags(messages, listener);
            }

            if (fp.contains(FetchProfile.Item.BODY_SANE)) {
                if (mAccount.getMaximumAutoDownloadMessageSize() > 0) {
                    fetchMessages(messages, listener, (mAccount.getMaximumAutoDownloadMessageSize() / 76));
                } else {
                    fetchMessages(messages, listener, -1);
                }
            }
            if (fp.contains(FetchProfile.Item.BODY)) {
                fetchMessages(messages, listener, -1);
            }
        }

        /**
         * Fetches the full messages or up to lines lines and passes them to the message parser.
         */
        private void fetchMessages(Message[] messages, MessageRetrievalListener listener, int lines)
        throws MessagingException {
            WebDavHttpClient httpclient;
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
                 * If fetch is called outside of the initial list (ie, a locally stored message), it may not have a URL
                 * associated. Verify and fix that
                 */
                if (wdMessage.getUrl().equals("")) {
                    wdMessage.setUrl(getMessageUrls(new String[] { wdMessage.getUid() }).get(wdMessage.getUid()));
                    Log.i(K9.LOG_TAG, "Fetching messages with UID = '" + wdMessage.getUid() + "', URL = '"
                          + wdMessage.getUrl() + "'");
                    if (wdMessage.getUrl().equals("")) {
                        throw new MessagingException("Unable to get URL for message");
                    }
                }

                try {
                    Log.i(K9.LOG_TAG, "Fetching message with UID = '" + wdMessage.getUid() + "', URL = '"
                          + wdMessage.getUrl() + "'");
                    HttpGet httpget = new HttpGet(new URI(wdMessage.getUrl()));
                    HttpResponse response;
                    HttpEntity entity;

                    httpget.setHeader("translate", "f");
                    if (mAuthentication == AUTH_TYPE_BASIC) {
                        httpget.setHeader("Authorization", mAuthString);
                    }
                    response = httpclient.executeOverride(httpget, mContext);

                    statusCode = response.getStatusLine().getStatusCode();

                    entity = response.getEntity();

                    if (statusCode < 200 ||
                            statusCode > 300) {
                        throw new IOException("Error during with code " + statusCode + " during fetch: "
                                              + response.getStatusLine().toString());
                    }

                    if (entity != null) {
                        InputStream istream = null;
                        StringBuilder buffer = new StringBuilder();
                        String tempText = "";
                        String resultText = "";
                        BufferedReader reader = null;
                        int currentLines = 0;

                        try {
                            istream = WebDavHttpClient.getUngzippedContent(entity);

                            if (lines != -1) {
                                reader = new BufferedReader(new InputStreamReader(istream), 8192);

                                while ((tempText = reader.readLine()) != null &&
                                        (currentLines < lines)) {
                                    buffer.append(tempText).append("\r\n");
                                    currentLines++;
                                }

                                istream.close();
                                resultText = buffer.toString();
                                istream = new ByteArrayInputStream(resultText.getBytes("UTF-8"));
                            }

                            wdMessage.parse(istream);

                        } finally {
                            IOUtils.closeQuietly(reader);
                            IOUtils.closeQuietly(istream);
                        }
                    }

                } catch (IllegalArgumentException iae) {
                    Log.e(K9.LOG_TAG, "IllegalArgumentException caught " + iae + "\nTrace: " + processException(iae));
                    throw new MessagingException("IllegalArgumentException caught", iae);
                } catch (URISyntaxException use) {
                    Log.e(K9.LOG_TAG, "URISyntaxException caught " + use + "\nTrace: " + processException(use));
                    throw new MessagingException("URISyntaxException caught", use);
                } catch (IOException ioe) {
                    Log.e(K9.LOG_TAG, "Non-success response code loading message, response code was " + statusCode
                          + "\nURL: " + wdMessage.getUrl() + "\nError: " + ioe.getMessage() + "\nTrace: "
                          + processException(ioe));
                    throw new MessagingException("Failure code " + statusCode, ioe);
                }

                if (listener != null) {
                    listener.messageFinished(wdMessage, i, count);
                }
            }
        }

        /**
         * Fetches and sets the message flags for the supplied messages. The idea is to have this be recursive so that
         * we do a series of medium calls instead of one large massive call or a large number of smaller calls.
         */
        private void fetchFlags(Message[] startMessages, MessageRetrievalListener listener) throws MessagingException {
            HashMap<String, String> headers = new HashMap<String, String>();
            String messageBody = "";
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
            DataSet dataset = processRequest(this.mFolderUrl, "SEARCH", messageBody, headers);

            if (dataset == null) {
                throw new MessagingException("Data Set from request was null");
            }

            HashMap<String, Boolean> uidToReadStatus = dataset.getUidToRead();

            for (int i = 0, count = messages.length; i < count; i++) {
                if (!(messages[i] instanceof WebDavMessage)) {
                    throw new MessagingException("WebDavStore fetch called with non-WebDavMessage");
                }
                WebDavMessage wdMessage = (WebDavMessage) messages[i];

                if (listener != null) {
                    listener.messageStarted(wdMessage.getUid(), i, count);
                }

                try {
                    wdMessage.setFlagInternal(Flag.SEEN, uidToReadStatus.get(wdMessage.getUid()));
                } catch (NullPointerException e) {
                    Log.v(K9.LOG_TAG,"Under some weird circumstances, setting the read status when syncing from webdav threw an NPE. Skipping.");
                }

                if (listener != null) {
                    listener.messageFinished(wdMessage, i, count);
                }
            }
        }

        /**
         * Fetches and parses the message envelopes for the supplied messages. The idea is to have this be recursive so
         * that we do a series of medium calls instead of one large massive call or a large number of smaller calls.
         * Call it a happy balance
         */
        private void fetchEnvelope(Message[] startMessages, MessageRetrievalListener listener)
        throws MessagingException {
            HashMap<String, String> headers = new HashMap<String, String>();
            String messageBody = "";
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
            DataSet dataset = processRequest(this.mFolderUrl, "SEARCH", messageBody, headers);

            Map<String, ParsedMessageEnvelope> envelopes = dataset.getMessageEnvelopes();

            int count = messages.length;
            for (int i = messages.length - 1; i >= 0; i--) {
                if (!(messages[i] instanceof WebDavMessage)) {
                    throw new MessagingException("WebDavStore fetch called with non-WebDavMessage");
                }
                WebDavMessage wdMessage = (WebDavMessage) messages[i];

                if (listener != null) {
                    listener.messageStarted(messages[i].getUid(), i, count);
                }

                ParsedMessageEnvelope envelope = envelopes.get(wdMessage.getUid());
                if (envelope != null) {
                    wdMessage.setNewHeaders(envelope);
                    wdMessage.setFlagInternal(Flag.SEEN, envelope.getReadStatus());
                } else {
                    Log.e(K9.LOG_TAG,"Asked to get metadata for a non-existent message: "+wdMessage.getUid());
                }

                if (listener != null) {
                    listener.messageFinished(messages[i], i, count);
                }
            }
        }

        @Override
        public void setFlags(Message[] messages, Flag[] flags, boolean value)
        throws MessagingException {
            String[] uids = new String[messages.length];

            for (int i = 0, count = messages.length; i < count; i++) {
                uids[i] = messages[i].getUid();
            }

            for (Flag flag : flags) {
                if (flag == Flag.SEEN) {
                    markServerMessagesRead(uids, value);
                } else if (flag == Flag.DELETED) {
                    deleteServerMessages(uids);
                }
            }
        }

        private void markServerMessagesRead(String[] uids, boolean read) throws MessagingException {
            String messageBody = "";
            HashMap<String, String> headers = new HashMap<String, String>();
            HashMap<String, String> uidToUrl = getMessageUrls(uids);
            String[] urls = new String[uids.length];

            for (int i = 0, count = uids.length; i < count; i++) {
                urls[i] = uidToUrl.get(uids[i]);
            }

            messageBody = getMarkMessagesReadXml(urls, read);
            headers.put("Brief", "t");
            headers.put("If-Match", "*");

            processRequest(this.mFolderUrl, "BPROPPATCH", messageBody, headers, false);
        }

        private void deleteServerMessages(String[] uids) throws MessagingException {
            HashMap<String, String> uidToUrl = getMessageUrls(uids);

            for (String uid : uids) {
                HashMap<String, String> headers = new HashMap<String, String>();
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
        public Map<String, String> appendMessages(Message[] messages) throws MessagingException {
            appendWebDavMessages(messages);
            return null;
        }

        public Message[] appendWebDavMessages(Message[] messages) throws MessagingException {
            Message[] retMessages = new Message[messages.length];
            int ind = 0;

            WebDavHttpClient httpclient = getHttpClient();

            for (Message message : messages) {
                HttpGeneric httpmethod;
                HttpResponse response;
                StringEntity bodyEntity;
                int statusCode;

                try {
                    ByteArrayOutputStream out;

                    out = new ByteArrayOutputStream(message.getSize());

                    open(Folder.OPEN_MODE_RW);
                    EOLConvertingOutputStream msgOut = new EOLConvertingOutputStream(
                        new BufferedOutputStream(out, 1024));
                    message.writeTo(msgOut);
                    msgOut.flush();

                    bodyEntity = new StringEntity(out.toString(), "UTF-8");
                    bodyEntity.setContentType("message/rfc822");

                    String messageURL = mFolderUrl;
                    if (!messageURL.endsWith("/")) {
                        messageURL += "/";
                    }
                    messageURL += URLEncoder.encode(message.getUid() + ":" + System.currentTimeMillis() + ".eml", "UTF-8");

                    Log.i(K9.LOG_TAG, "Uploading message as " + messageURL);

                    httpmethod = new HttpGeneric(messageURL);
                    httpmethod.setMethod("PUT");
                    httpmethod.setEntity(bodyEntity);

                    String mAuthString = getAuthString();

                    if (mAuthString != null) {
                        httpmethod.setHeader("Authorization", mAuthString);
                    }

                    response = httpclient.executeOverride(httpmethod, mContext);
                    statusCode = response.getStatusLine().getStatusCode();

                    if (statusCode < 200 ||
                            statusCode > 300) {
                        throw new IOException("Error with status code " + statusCode
                                              + " while sending/appending message.  Response = "
                                              + response.getStatusLine().toString() + " for message " + messageURL);
                    }
                    WebDavMessage retMessage = new WebDavMessage(message.getUid(), this);

                    retMessage.setUrl(messageURL);
                    retMessages[ind++] = retMessage;
                } catch (Exception e) {
                    throw new MessagingException("Unable to append", e);
                }
            }
            return retMessages;
        }

        @Override
        public boolean equals(Object o) {
            return false;
        }

        @Override
        public String getUidFromMessageId(Message message) throws MessagingException {
            Log.e(K9.LOG_TAG,
                  "Unimplemented method getUidFromMessageId in WebDavStore.WebDavFolder could lead to duplicate messages "
                  + " being uploaded to the Sent folder");
            return null;
        }

        @Override
        public void setFlags(Flag[] flags, boolean value) throws MessagingException {
            Log.e(K9.LOG_TAG,
                  "Unimplemented method setFlags(Flag[], boolean) breaks markAllMessagesAsRead and EmptyTrash");
            // Try to make this efficient by not retrieving all of the messages
        }
    }

    /**
     * A WebDav Message
     */
    class WebDavMessage extends MimeMessage {
        private String mUrl = "";

        WebDavMessage(String uid, Folder folder) {
            this.mUid = uid;
            this.mFolder = folder;
        }

        public void setUrl(String url) {
            // TODO: This is a not as ugly hack (ie, it will actually work)
            // XXX: prevent URLs from getting to us that are broken
            if (!(url.toLowerCase(Locale.US).contains("http"))) {
                if (!(url.startsWith("/"))) {
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
             * We have to decode, then encode the URL because Exchange likes to not properly encode all characters
             */
            try {
                end = java.net.URLDecoder.decode(end, "UTF-8");
                end = java.net.URLEncoder.encode(end, "UTF-8");
                end = end.replaceAll("\\+", "%20");
            } catch (UnsupportedEncodingException uee) {
                Log.e(K9.LOG_TAG, "UnsupportedEncodingException caught in setUrl: " + uee + "\nTrace: "
                      + processException(uee));
            } catch (IllegalArgumentException iae) {
                Log.e(K9.LOG_TAG, "IllegalArgumentException caught in setUrl: " + iae + "\nTrace: "
                      + processException(iae));
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

        @Override
        public void parse(InputStream in) throws IOException, MessagingException {
            super.parse(in);
        }

        public void setFlagInternal(Flag flag, boolean set) throws MessagingException {
            super.setFlag(flag, set);
        }

        public void setNewHeaders(ParsedMessageEnvelope envelope) throws MessagingException {
            String[] headers = envelope.getHeaderList();
            HashMap<String, String> messageHeaders = envelope.getMessageHeaders();

            for (String header : headers) {
                String headerValue = messageHeaders.get(header);
                if (header.equals("Content-Length")) {
                    int size = Integer.parseInt(messageHeaders.get(header));
                    this.setSize(size);
                }

                if (headerValue != null &&
                        !headerValue.equals("")) {
                    this.addHeader(header, headerValue);
                }
            }
        }

        @Override
        public void delete(String trashFolderName) throws MessagingException {
            WebDavFolder wdFolder = (WebDavFolder) getFolder();
            Log.i(K9.LOG_TAG, "Deleting message by moving to " + trashFolderName);
            wdFolder.moveMessages(new Message[] { this }, wdFolder.getStore().getFolder(trashFolderName));
        }

        @Override
        public void setFlag(Flag flag, boolean set) throws MessagingException {
            super.setFlag(flag, set);
            mFolder.setFlags(new Message[] { this }, new Flag[] { flag }, set);
        }
    }

    /**
     * XML Parsing Handler Can handle all XML handling needs
     */
    public class WebDavHandler extends DefaultHandler {
        private DataSet mDataSet = new DataSet();
        private final LinkedList<String> mOpenTags = new LinkedList<String>();

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
            mOpenTags.addFirst(localName);
        }

        @Override
        public void endElement(String namespaceURI, String localName, String qName) {
            mOpenTags.removeFirst();

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
     * Data set for a single E-Mail message's required headers (the envelope) Only provides accessor methods to the
     * stored data. All processing should be done elsewhere. This is done rather than having multiple hashmaps
     * associating UIDs to values
     */
    public static class ParsedMessageEnvelope {
        /**
         * Holds the mappings from the name returned from Exchange to the MIME format header name
         */
        private static final Map<String, String> HEADER_MAPPINGS;
        static {
            Map<String, String> map = new HashMap<String, String>();
            map.put("mime-version", "MIME-Version");
            map.put("content-type", "Content-Type");
            map.put("subject", "Subject");
            map.put("date", "Date");
            map.put("thread-topic", "Thread-Topic");
            map.put("thread-index", "Thread-Index");
            map.put("from", "From");
            map.put("to", "To");
            map.put("in-reply-to", "In-Reply-To");
            map.put("cc", "Cc");
            map.put("getcontentlength", "Content-Length");
            HEADER_MAPPINGS = Collections.unmodifiableMap(map);
        }

        private boolean mReadStatus = false;
        private String mUid = "";
        private HashMap<String, String> mMessageHeaders = new HashMap<String, String>();
        private ArrayList<String> mHeaders = new ArrayList<String>();

        public void addHeader(String field, String value) {
            String headerName = HEADER_MAPPINGS.get(field);

            if (headerName != null) {
                this.mMessageHeaders.put(HEADER_MAPPINGS.get(field), value);
                this.mHeaders.add(HEADER_MAPPINGS.get(field));
            }
        }

        public HashMap<String, String> getMessageHeaders() {
            return this.mMessageHeaders;
        }

        public String[] getHeaderList() {
            return this.mHeaders.toArray(EMPTY_STRING_ARRAY);
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
     * Dataset for all XML parses. Data is stored in a single format inside the class and is formatted appropriately
     * depending on the accessor calls made.
     */
    public class DataSet {
        private HashMap<String, HashMap<String, String>> mData = new HashMap<String, HashMap<String, String>>();
        private StringBuilder mUid = new StringBuilder();
        private HashMap<String, String> mTempData = new HashMap<String, String>();

        public void addValue(String value, String tagName) {
            if (tagName.equals("uid")) {
                mUid.append(value);
            }

            if (mTempData.containsKey(tagName)) {
                mTempData.put(tagName, mTempData.get(tagName) + value);
            } else {
                mTempData.put(tagName, value);
            }
        }

        public void finish() {
            String uid = mUid.toString();
            if (uid != null && mTempData != null) {
                mData.put(uid, mTempData);
            } else if (mTempData != null) {
                /*
                 * Lost Data are for requests that don't include a message UID. These requests should only have a depth
                 * of one for the response so it will never get stomped over.
                 */
            }

            mUid = new StringBuilder();
            mTempData = new HashMap<String, String>();
        }

        /**
         * Returns a hashmap of special folder name => special folder url
         */
        public HashMap<String, String> getSpecialFolderToUrl() {
            // We return the first (and only) map
            for (HashMap<String, String> folderMap : mData.values()) {
                return folderMap;
            }
            return new HashMap<String, String>();
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
                if (readStatus != null && !readStatus.equals("")) {
                    Boolean value = !readStatus.equals("0");
                    uidToRead.put(uid, value);
                } else {
                    // We don't actually want to have null values in our hashmap,
                    // as it causes the calling code to crash with an NPE as it
                    // does a lookup in the map.
                    uidToRead.put(uid, false);
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

            return hrefs.toArray(EMPTY_STRING_ARRAY);
        }

        /**
         * Return an array of all Message UIDs that were received
         */
        public String[] getUids() {
            ArrayList<String> uids = new ArrayList<String>();

            for (String uid : mData.keySet()) {
                uids.add(uid);
            }

            return uids.toArray(EMPTY_STRING_ARRAY);
        }

        /**
         * Returns the message count as it was retrieved
         */
        public int getMessageCount() {
            // It appears that Exchange is returning responses
            // without a visiblecount element for empty folders
            // Which resulted in this code returning -1 (as that was
            // the previous default.)
            // -1 is an error condition. Now the default is empty
            int messageCount = 0;

            for (String uid : mData.keySet()) {
                HashMap<String, String> data = mData.get(uid);
                String count = data.get("visiblecount");

                if (count != null &&
                        !count.equals("")) {
                    messageCount = Integer.parseInt(count);
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
                    for (Map.Entry<String, String> entry : data.entrySet()) {
                        String header = entry.getKey();
                        if (header.equals("read")) {
                            String read = entry.getValue();
                            boolean readStatus = !read.equals("0");

                            envelope.setReadStatus(readStatus);
                        } else if (header.equals("date")) {
                            /**
                             * Exchange doesn't give us rfc822 dates like it claims. The date is in the format:
                             * yyyy-MM-dd'T'HH:mm:ss.SSS<Single digit representation of timezone, so far, all instances
                             * are Z>
                             */
                            String date = entry.getValue();
                            date = date.substring(0, date.length() - 1);

                            DateFormat dfInput = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US);
                            DateFormat dfOutput = new SimpleDateFormat("EEE, d MMM yy HH:mm:ss Z", Locale.US);
                            String tempDate = "";

                            try {
                                Date parsedDate = dfInput.parse(date);
                                tempDate = dfOutput.format(parsedDate);
                            } catch (java.text.ParseException pe) {
                                Log.e(K9.LOG_TAG, "Error parsing date: " + pe + "\nTrace: " + processException(pe));
                            }
                            envelope.addHeader(header, tempDate);
                        } else {
                            envelope.addHeader(header, entry.getValue());
                        }
                    }
                }

                envelopes.put(uid, envelope);
            }

            return envelopes;
        }
    }

    /**
     * New HTTP Method that allows changing of the method and generic handling Needed for WebDAV custom methods such as
     * SEARCH and PROPFIND
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
         * @throws IllegalArgumentException
         *             if the uri is invalid.
         */
        public HttpGeneric(final String uri) {
            super();

            if (K9.DEBUG) {
                Log.v(K9.LOG_TAG, "Starting uri = '" + uri + "'");
            }

            String[] urlParts = uri.split("/");
            int length = urlParts.length;
            String end = urlParts[length - 1];
            String url = "";

            /**
             * We have to decode, then encode the URL because Exchange likes to not properly encode all characters
             */
            try {
                if (length > 3) {
                    end = java.net.URLDecoder.decode(end, "UTF-8");
                    end = java.net.URLEncoder.encode(end, "UTF-8");
                    end = end.replaceAll("\\+", "%20");
                }
            } catch (UnsupportedEncodingException uee) {
                Log.e(K9.LOG_TAG, "UnsupportedEncodingException caught in HttpGeneric(String uri): " + uee
                      + "\nTrace: " + processException(uee));
            } catch (IllegalArgumentException iae) {
                Log.e(K9.LOG_TAG, "IllegalArgumentException caught in HttpGeneric(String uri): " + iae + "\nTrace: "
                      + processException(iae));
            }

            for (int i = 0; i < length - 1; i++) {
                if (i != 0) {
                    url = url + "/" + urlParts[i];
                } else {
                    url = urlParts[i];
                }
            }
            if (K9.DEBUG && K9.DEBUG_PROTOCOL_WEBDAV) {
                Log.v(K9.LOG_TAG, "url = '" + url + "' length = " + url.length()
                      + ", end = '" + end + "' length = " + end.length());
            }
            url = url + "/" + end;

            Log.i(K9.LOG_TAG, "url = " + url);
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

    public static class WebDavHttpClient extends DefaultHttpClient {
        /*
         * Copyright (C) 2007 The Android Open Source Project
         *
         * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
         * compliance with the License. You may obtain a copy of the License at
         *
         * http://www.apache.org/licenses/LICENSE-2.0
         *
         * Unless required by applicable law or agreed to in writing, software distributed under the License is
         * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
         * the License for the specific language governing permissions and limitations under the License.
         */
        public static void modifyRequestToAcceptGzipResponse(HttpRequest request) {
            Log.i(K9.LOG_TAG, "Requesting gzipped data");
            request.addHeader("Accept-Encoding", "gzip");
        }

        public static InputStream getUngzippedContent(HttpEntity entity)
        throws IOException {
            InputStream responseStream = entity.getContent();
            if (responseStream == null)
                return responseStream;
            Header header = entity.getContentEncoding();
            if (header == null)
                return responseStream;
            String contentEncoding = header.getValue();
            if (contentEncoding == null)
                return responseStream;
            if (contentEncoding.contains("gzip")) {
                Log.i(K9.LOG_TAG, "Response is gzipped");
                responseStream = new GZIPInputStream(responseStream);
            }
            return responseStream;
        }

        public HttpResponse executeOverride(HttpUriRequest request, HttpContext context)
        throws IOException {
            modifyRequestToAcceptGzipResponse(request);
            return super.execute(request, context);
        }
    }

    /**
     * Simple data container for passing connection information.
     */
    private static class ConnectionInfo {
        public int statusCode;
        public short requiredAuthType;
        public String guessedAuthUrl;
        public String redirectUrl;
    }
}
