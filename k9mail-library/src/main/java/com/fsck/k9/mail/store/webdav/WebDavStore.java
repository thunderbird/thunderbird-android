package com.fsck.k9.mail.store.webdav;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import android.support.annotation.NonNull;

import com.fsck.k9.mail.CertificateValidationException;
import com.fsck.k9.mail.ConnectionSecurity;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.K9MailLib;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.ServerSettings;
import com.fsck.k9.mail.filter.Base64;
import com.fsck.k9.mail.store.RemoteStore;
import com.fsck.k9.mail.store.StoreConfig;
import javax.net.ssl.SSLException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
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
import timber.log.Timber;

import static com.fsck.k9.mail.K9MailLib.DEBUG_PROTOCOL_WEBDAV;
import static com.fsck.k9.mail.helper.UrlEncodingHelper.decodeUtf8;


/**
 * <pre>
 * Uses WebDAV formatted HTTP calls to an MS Exchange server to fetch email
 * and email information.
 * </pre>
 */
@SuppressWarnings("deprecation")
public class WebDavStore extends RemoteStore {

    public static WebDavStoreSettings decodeUri(String uri) {
        return WebDavStoreUriDecoder.decode(uri);
    }

    public static String createUri(ServerSettings server) {
        return WebDavStoreUriCreator.create(server);
    }

    private ConnectionSecurity mConnectionSecurity;
    private String username;
    private String alias;
    private String password;
    private String baseUrl;
    private String hostname;
    private int port;
    private String path;
    private String formBasedAuthPath;
    private String mailboxPath;

    private final WebDavHttpClient.WebDavHttpClientFactory httpClientFactory;
    private WebDavHttpClient httpClient = null;
    private HttpContext httpContext = null;
    private String authString;
    private CookieStore authCookies = null;
    private short authenticationType = WebDavConstants.AUTH_TYPE_NONE;
    private String cachedLoginUrl;

    private Folder sendFolder = null;
    private Map<String, WebDavFolder> folderList = new HashMap<>();

    public WebDavStore(StoreConfig storeConfig, WebDavHttpClient.WebDavHttpClientFactory clientFactory)
            throws MessagingException {
        super(storeConfig, null);
        httpClientFactory = clientFactory;

        WebDavStoreSettings settings;
        try {
            settings = WebDavStore.decodeUri(storeConfig.getStoreUri());
        } catch (IllegalArgumentException e) {
            throw new MessagingException("Error while decoding store URI", e);
        }

        hostname = settings.host;
        port = settings.port;

        mConnectionSecurity = settings.connectionSecurity;

        username = settings.username;
        password = settings.password;
        alias = settings.alias;

        path = settings.path;
        formBasedAuthPath = settings.authPath;
        mailboxPath = settings.mailboxPath;


        if (path == null || path.equals("")) {
            path = "/Exchange";
        } else if (!path.startsWith("/")) {
            path = "/" + path;
        }

        if (mailboxPath == null || mailboxPath.equals("")) {
            mailboxPath = "/" + alias;
        } else if (!mailboxPath.startsWith("/")) {
            mailboxPath = "/" + mailboxPath;
        }

        if (formBasedAuthPath != null &&
                !formBasedAuthPath.equals("") &&
                !formBasedAuthPath.startsWith("/")) {
            formBasedAuthPath = "/" + formBasedAuthPath;
        }

        // The URL typically looks like the following: "https://mail.domain.com/Exchange/alias".
        // The inbox path would look like: "https://mail.domain.com/Exchange/alias/Inbox".
        baseUrl = getRoot() + path + mailboxPath;

        authString = "Basic " + Base64.encode(username + ":" + password);
    }

    private String getRoot() {
        String root;
        if (mConnectionSecurity == ConnectionSecurity.SSL_TLS_REQUIRED) {
            root = "https";
        } else {
            root = "http";
        }
        root += "://" + hostname + ":" + port;
        return root;
    }

    HttpContext getHttpContext() {
        return httpContext;
    }

    short getAuthentication() {
        return authenticationType;
    }

    StoreConfig getStoreConfig() {
        return mStoreConfig;
    }

    @Override
    public void checkSettings() throws MessagingException {
        authenticate();
    }

    @Override
    @NonNull
    public List<? extends Folder> getFolders(boolean forceListAll) throws MessagingException {
        List<Folder> folderList = new LinkedList<>();
        /*
         * We have to check authentication here so we have the proper URL stored
         */
        getHttpClient();

        /*
         *  Firstly we get the "special" folders list (inbox, outbox, etc)
         *  and setup the account accordingly
         */
        Map<String, String> headers = new HashMap<>();
        headers.put("Depth", "0");
        headers.put("Brief", "t");
        DataSet dataset = processRequest(this.baseUrl, "PROPFIND", getSpecialFoldersList(), headers);

        Map<String, String> specialFoldersMap = dataset.getSpecialFolderToUrl();
        String folderId = getFolderId(specialFoldersMap.get(WebDavConstants.DAV_MAIL_INBOX_FOLDER));
        if (folderId != null) {
            mStoreConfig.setAutoExpandFolderId(folderId);
            mStoreConfig.setInboxFolderId(folderId);
        }

        folderId = getFolderId(specialFoldersMap.get(WebDavConstants.DAV_MAIL_DRAFTS_FOLDER));
        if (folderId != null) {
            mStoreConfig.setDraftsFolderId(folderId);
        }

        folderId = getFolderId(specialFoldersMap.get(WebDavConstants.DAV_MAIL_TRASH_FOLDER));
        if (folderId != null) {
            mStoreConfig.setTrashFolderId(folderId);
        }

        folderId = getFolderId(specialFoldersMap.get(WebDavConstants.DAV_MAIL_SPAM_FOLDER));
        if (folderId != null) {
            mStoreConfig.setSpamFolderId(folderId);
        }

        // K-9 Mail's outbox is a special local folder and different from Exchange/WebDAV's outbox.
        /*
        folderId = getFolderId(specialFoldersMap.get(DAV_MAIL_OUTBOX_FOLDER));
        if (folderId != null)
            mAccount.setOutboxFolderName(folderId);
        */

        folderId = getFolderId(specialFoldersMap.get(WebDavConstants.DAV_MAIL_SENT_FOLDER));
        if (folderId != null) {
            mStoreConfig.setSentFolderId(folderId);
        }

        /*
         * Next we get all the folders (including "special" ones)
         */
        headers = new HashMap<>();
        headers.put("Brief", "t");
        dataset = processRequest(this.baseUrl, "SEARCH", getFolderListXml(), headers);
        String[] folderUrls = dataset.getHrefs();

        for (String tempUrl : folderUrls) {
            WebDavFolder folder = createFolder(tempUrl);
            if (folder != null) {
                folderList.add(folder);
            }
        }

        return folderList;
    }

    @NonNull
    @Override
    public List<? extends Folder> getSubFolders(String parentFolderId, boolean forceListAll) throws MessagingException {
        throw new UnsupportedOperationException();
    }

    /**
     * Creates a folder using the URL passed as parameter (only if it has not been
     * already created) and adds this to our store folder map.
     *
     * @param folderUrl
     *         URL
     *
     * @return WebDAV remote folder
     */
    private WebDavFolder createFolder(String folderUrl) {
        if (folderUrl == null) {
            return null;
        }

        WebDavFolder wdFolder = null;
        String folderId = getFolderId(folderUrl);
        if (folderId != null) {
            wdFolder = getFolder(folderId);
            if (wdFolder != null) {
                wdFolder.setUrl(folderUrl);
            }
        }
        // else: Unknown URL format => NO Folder created

        return wdFolder;
    }

    private String getFolderId(String folderUrl) {
        if (folderUrl == null) {
            return null;
        }

        // Here we extract the folder name starting from the complete url.
        // folderUrl is in the form http://mail.domain.com/exchange/username/foldername
        // so we need "foldername" which is the string after the fifth slash
        int folderSlash = -1;
        for (int j = 0; j < 5; j++) {
            folderSlash = folderUrl.indexOf('/', folderSlash + 1);
            if (folderSlash < 0) {
                break;
            }
        }

        if (folderSlash > 0) {
            String fullPathName;

            // Removes the final slash if present
            if (folderUrl.charAt(folderUrl.length() - 1) == '/') {
                fullPathName = folderUrl.substring(folderSlash + 1, folderUrl.length() - 1);
            } else {
                fullPathName = folderUrl.substring(folderSlash + 1);
            }

            // Decodes the url-encoded folder name (i.e. "My%20folder" => "My Folder"

            return decodeUtf8(fullPathName);
        }

        return null;
    }

    @Override
    @NonNull public WebDavFolder getFolder(String name) {
        WebDavFolder folder = this.folderList.get(name);

        if (folder == null) {
            folder = new WebDavFolder(this, name);
            folderList.put(name, folder);
        }

        return folder;
    }

    private Folder getSendSpoolFolder() throws MessagingException {
        if (sendFolder == null) {
            sendFolder = getFolder(WebDavConstants.DAV_MAIL_SEND_FOLDER);
        }

        return sendFolder;
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
        return "<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"no\"?>" +
                "<propfind xmlns=\"DAV:\">" +
                "<prop>" +
                "<" + WebDavConstants.DAV_MAIL_INBOX_FOLDER + " xmlns=\"urn:schemas:httpmail:\"/>" +
                "<" + WebDavConstants.DAV_MAIL_DRAFTS_FOLDER + " xmlns=\"urn:schemas:httpmail:\"/>" +
                "<" + WebDavConstants.DAV_MAIL_OUTBOX_FOLDER + " xmlns=\"urn:schemas:httpmail:\"/>" +
                "<" + WebDavConstants.DAV_MAIL_SENT_FOLDER + " xmlns=\"urn:schemas:httpmail:\"/>" +
                "<" + WebDavConstants.DAV_MAIL_TRASH_FOLDER + " xmlns=\"urn:schemas:httpmail:\"/>" +
                "<" + WebDavConstants.DAV_MAIL_SPAM_FOLDER + " xmlns=\"urn:schemas:httpmail:\"/>" +
                // This should always be ##DavMailSubmissionURI## for which we already have a constant
                // "<sendmsg xmlns=\"urn:schemas:httpmail:\"/>" +
                "</prop>" +
                "</propfind>";
    }

    /***************************************************************
     * WebDAV XML Request body retrieval functions
     */
    private String getFolderListXml() {
        return "<?xml version='1.0' ?>" +
                "<a:searchrequest xmlns:a='DAV:'><a:sql>\r\n" +
                "SELECT \"DAV:uid\", \"DAV:ishidden\"\r\n" +
                " FROM SCOPE('deep traversal of \"" + this.baseUrl + "\"')\r\n" +
                " WHERE \"DAV:ishidden\"=False AND \"DAV:isfolder\"=True\r\n" +
                "</a:sql></a:searchrequest>\r\n";
    }

    String getMessageCountXml(String messageState) {
        return "<?xml version='1.0' ?>" +
                "<a:searchrequest xmlns:a='DAV:'><a:sql>\r\n" +
                "SELECT \"DAV:visiblecount\"\r\n" +
                " FROM \"\"\r\n" +
                " WHERE \"DAV:ishidden\"=False AND \"DAV:isfolder\"=False AND \"urn:schemas:httpmail:read\"=" +
                messageState + "\r\n" +
                " GROUP BY \"DAV:ishidden\"\r\n" +
                "</a:sql></a:searchrequest>\r\n";
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
        return "<?xml version='1.0' ?>" +
                "<a:searchrequest xmlns:a='DAV:'><a:sql>\r\n" +
                "SELECT \"DAV:uid\"\r\n" +
                " FROM \"\"\r\n" +
                " WHERE \"DAV:ishidden\"=False AND \"DAV:isfolder\"=False\r\n" +
                "</a:sql></a:searchrequest>\r\n";
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

    private boolean authenticate()
            throws MessagingException {
        try {
            if (authenticationType == WebDavConstants.AUTH_TYPE_NONE) {
                ConnectionInfo info = doInitialConnection();

                if (info.requiredAuthType == WebDavConstants.AUTH_TYPE_BASIC) {
                    HttpGeneric request = new HttpGeneric(baseUrl);
                    request.setMethod("GET");
                    request.setHeader("Authorization", authString);

                    WebDavHttpClient httpClient = getHttpClient();
                    HttpResponse response = httpClient.executeOverride(request, httpContext);

                    int statusCode = response.getStatusLine().getStatusCode();
                    if (statusCode >= 200 && statusCode < 300) {
                        authenticationType = WebDavConstants.AUTH_TYPE_BASIC;
                    } else if (statusCode == 401) {
                        throw new MessagingException("Invalid username or password for authentication.");
                    } else {
                        throw new MessagingException("Error with code " + response.getStatusLine().getStatusCode() +
                                " during request processing: " + response.getStatusLine().toString());
                    }
                } else if (info.requiredAuthType == WebDavConstants.AUTH_TYPE_FORM_BASED) {
                    performFormBasedAuthentication(info);
                }
            } else if (authenticationType == WebDavConstants.AUTH_TYPE_BASIC) {
                // Nothing to do, we authenticate with every request when
                // using basic authentication.
            } else if (authenticationType == WebDavConstants.AUTH_TYPE_FORM_BASED) {
                // Our cookie expired, re-authenticate.
                performFormBasedAuthentication(null);
            }
        } catch (IOException ioe) {
            Timber.e(ioe, "Error during authentication");
            throw new MessagingException("Error during authentication", ioe);
        }

        return authenticationType != WebDavConstants.AUTH_TYPE_NONE;
    }

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

        HttpGeneric request = new HttpGeneric(baseUrl);
        request.setMethod("GET");

        try {
            HttpResponse response = httpClient.executeOverride(request, httpContext);
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

                if (formBasedAuthPath != null && !formBasedAuthPath.equals("")) {
                    // The user specified their own authentication path, use that.
                    info.guessedAuthUrl = getRoot() + formBasedAuthPath;
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
            Timber.e(ioe, "IOException during initial connection");
            throw new MessagingException("IOException", ioe);
        }

        return info;
    }

    private void performFormBasedAuthentication(ConnectionInfo info)
            throws IOException, MessagingException {
        // Clear out cookies from any previous authentication.
        if (authCookies != null) {
            authCookies.clear();
        }

        WebDavHttpClient httpClient = getHttpClient();

        String loginUrl;
        if (info != null) {
            loginUrl = info.guessedAuthUrl;
        } else if (cachedLoginUrl != null && !cachedLoginUrl.equals("")) {
            loginUrl = cachedLoginUrl;
        } else {
            throw new MessagingException("No valid login URL available for form-based authentication.");
        }

        HttpGeneric request = new HttpGeneric(loginUrl);
        request.setMethod("POST");

        // Build the POST data.
        List<BasicNameValuePair> pairs = new ArrayList<>();
        pairs.add(new BasicNameValuePair("destination", baseUrl));
        pairs.add(new BasicNameValuePair("username", username));
        pairs.add(new BasicNameValuePair("password", password));
        pairs.add(new BasicNameValuePair("flags", "0"));
        pairs.add(new BasicNameValuePair("SubmitCreds", "Log+On"));
        pairs.add(new BasicNameValuePair("forcedownlevel", "0"));
        pairs.add(new BasicNameValuePair("trusted", "0"));

        UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(pairs);
        request.setEntity(formEntity);

        HttpResponse response = httpClient.executeOverride(request, httpContext);
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

                    response = httpClient.executeOverride(request, httpContext);
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

                    response = httpClient.executeOverride(request, httpContext);
                    authenticated = testAuthenticationResponse(response);
                } catch (URISyntaxException e) {
                    Timber.e(e, "URISyntaxException caught");
                    throw new MessagingException("URISyntaxException caught", e);
                }
            } else {
                throw new MessagingException("A valid URL for Exchange authentication could not be found.");
            }
        }

        if (authenticated) {
            authenticationType = WebDavConstants.AUTH_TYPE_FORM_BASED;
            cachedLoginUrl = loginUrl;
        } else {
            throw new MessagingException("Invalid credentials provided for authentication.");
        }
    }

    private String findFormAction(InputStream istream)
            throws IOException {
        String formAction = null;

        BufferedReader reader = new BufferedReader(new InputStreamReader(istream), 4096);
        String tempText;

        //TODO: Use proper HTML parsing for this
        // Read line by line until we find something like: <form action="owaauth.dll"...>.
        tempText = reader.readLine();
        while (formAction == null) {
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
            tempText = reader.readLine();
        }

        return formAction;
    }

    private boolean testAuthenticationResponse(HttpResponse response)
            throws MessagingException {
        boolean authenticated = false;
        int statusCode = response.getStatusLine().getStatusCode();
        // Exchange 2007 will return a 302 status code no matter what.
        if (((statusCode >= 200 && statusCode < 300) || statusCode == 302) &&
                authCookies != null && !authCookies.getCookies().isEmpty()) {
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
                    String thisPath = new URI(baseUrl).getPath();
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
                    Timber.e(e, "URISyntaxException");
                    throw new MessagingException("URISyntaxException caught", e);
                }
            }
        }

        return authenticated;
    }

    public CookieStore getAuthCookies() {
        return authCookies;
    }

    public String getAlias() {
        return alias;
    }

    public String getUrl() {
        return baseUrl;
    }

    public WebDavHttpClient getHttpClient() throws MessagingException {
        if (httpClient == null) {
            httpClient = httpClientFactory.create();
            // Disable automatic redirects on the http client.
            httpClient.getParams().setBooleanParameter("http.protocol.handle-redirects", false);

            // Setup a cookie store for forms-based authentication.
            httpContext = new BasicHttpContext();
            authCookies = new BasicCookieStore();
            httpContext.setAttribute(ClientContext.COOKIE_STORE, authCookies);

            SchemeRegistry reg = httpClient.getConnectionManager().getSchemeRegistry();
            try {
                Scheme s = new Scheme("https", new WebDavSocketFactory(hostname, 443), 443);
                reg.register(s);
            } catch (NoSuchAlgorithmException nsa) {
                Timber.e(nsa, "NoSuchAlgorithmException in getHttpClient");
                throw new MessagingException("NoSuchAlgorithmException in getHttpClient: ", nsa);
            } catch (KeyManagementException kme) {
                Timber.e(kme, "KeyManagementException in getHttpClient");
                throw new MessagingException("KeyManagementException in getHttpClient: ", kme);
            }
        }
        return httpClient;
    }

    protected InputStream sendRequest(String url, String method, StringEntity messageBody,
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

            if (authenticationType == WebDavConstants.AUTH_TYPE_NONE) {
                if (!tryAuth || !authenticate()) {
                    throw new MessagingException("Unable to authenticate in sendRequest().");
                }
            } else if (authenticationType == WebDavConstants.AUTH_TYPE_BASIC) {
                httpMethod.setHeader("Authorization", authString);
            }

            httpMethod.setMethod(method);
            response = httpClient.executeOverride(httpMethod, httpContext);
            statusCode = response.getStatusLine().getStatusCode();

            entity = response.getEntity();

            if (statusCode == 401) {
                throw new MessagingException("Invalid username or password for Basic authentication.");
            } else if (statusCode == 440) {
                if (tryAuth && authenticationType == WebDavConstants.AUTH_TYPE_FORM_BASED) {
                    // Our cookie expired, re-authenticate.
                    performFormBasedAuthentication(null);
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
            Timber.e(uee, "UnsupportedEncodingException: ");
            throw new MessagingException("UnsupportedEncodingException", uee);
        } catch (IOException ioe) {
            Timber.e(ioe, "IOException: ");
            throw new MessagingException("IOException", ioe);
        }

        return null;
    }

    private void handleUnexpectedRedirect(HttpResponse response, String url) throws IOException {
        if (response.getFirstHeader("Location") != null) {
            // TODO: This may indicate lack of authentication or may alternatively be something we should follow
            throw new IOException("Unexpected redirect during request processing. " +
                    "Expected response from: " + url + " but told to redirect to:" +
                    response.getFirstHeader("Location").getValue());
        } else {
            throw new IOException("Unexpected redirect during request processing. " +
                    "Expected response from: " + url + " but not told where to redirect to");
        }
    }

    public String getAuthString() {
        return authString;
    }

    /**
     * Performs an HttpRequest to the supplied url using the supplied method. messageBody and headers are optional as
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
            Timber.v("processRequest url = '%s', method = '%s', messageBody = '%s'", url, method, messageBody);
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
                    Timber.e(se, "SAXException in processRequest()");
                    throw new MessagingException("SAXException in processRequest() ", se);
                } catch (ParserConfigurationException pce) {
                    Timber.e(pce, "ParserConfigurationException in processRequest()");
                    throw new MessagingException("ParserConfigurationException in processRequest() ", pce);
                }

                istream.close();
            }
        } catch (UnsupportedEncodingException uee) {
            Timber.e(uee, "UnsupportedEncodingException: ");
            throw new MessagingException("UnsupportedEncodingException in processRequest() ", uee);
        } catch (IOException ioe) {
            Timber.e(ioe, "IOException: ");
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
        WebDavFolder tmpFolder = getFolder(mStoreConfig.getDraftsFolderId());
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
