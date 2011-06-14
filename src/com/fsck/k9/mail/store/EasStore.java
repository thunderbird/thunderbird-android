package com.fsck.k9.mail.store;

import java.io.BufferedOutputStream;
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
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import javax.net.ssl.SSLException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import android.content.Context;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.controller.MessageRetrievalListener;
import com.fsck.k9.helper.Utility;
import com.fsck.k9.mail.AuthenticationFailedException;
import com.fsck.k9.mail.CertificateValidationException;
import com.fsck.k9.mail.FetchProfile;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Store;
import com.fsck.k9.mail.FetchProfile.Item;
import com.fsck.k9.mail.Folder.OpenMode;
import com.fsck.k9.mail.filter.EOLConvertingOutputStream;
import com.fsck.k9.mail.internet.MimeMessage;
import com.fsck.k9.mail.store.WebDavStore.WebDavHttpClient;
import com.fsck.k9.mail.store.WebDavStore.WebDavMessage;
import com.fsck.k9.mail.store.exchange.Eas;
import com.fsck.k9.mail.store.exchange.adapter.AbstractSyncAdapter;
import com.fsck.k9.mail.store.exchange.adapter.AccountAdapter;
import com.fsck.k9.mail.store.exchange.adapter.AccountSyncAdapter;
import com.fsck.k9.mail.store.exchange.adapter.EmailSyncAdapter;
import com.fsck.k9.mail.store.exchange.adapter.FolderSyncParser;
import com.fsck.k9.mail.store.exchange.adapter.GetItemEstimateParser;
import com.fsck.k9.mail.store.exchange.adapter.MailboxAdapter;
import com.fsck.k9.mail.store.exchange.adapter.Parser;
import com.fsck.k9.mail.store.exchange.adapter.ProvisionParser;
import com.fsck.k9.mail.store.exchange.adapter.SearchParser;
import com.fsck.k9.mail.store.exchange.adapter.Serializer;
import com.fsck.k9.mail.store.exchange.adapter.Tags;
import com.fsck.k9.mail.transport.TrustedSocketFactory;

/**
 * <pre>
 * Uses WebDAV formatted HTTP calls to an MS Exchange server to fetch email
 * and email information.
 * </pre>
 */
public class EasStore extends Store {
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

    private static final Flag[] PERMANENT_FLAGS = { Flag.DELETED, Flag.SEEN, Flag.ANSWERED };

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

    static private final String PING_COMMAND = "Ping";
    // Command timeout is the the time allowed for reading data from an open connection before an
    // IOException is thrown.  After a small added allowance, our watchdog alarm goes off (allowing
    // us to detect a silently dropped connection).  The allowance is defined below.
    static private final int COMMAND_TIMEOUT = 30*1000;

    // MSFT's custom HTTP result code indicating the need to provision
    static private final int HTTP_NEED_PROVISIONING = 449;

    // The EAS protocol Provision status for "we implement all of the policies"
    static private final String PROVISION_STATUS_OK = "1";
    // The EAS protocol Provision status meaning "we partially implement the policies"
    static private final String PROVISION_STATUS_PARTIAL = "2";

    static public final String EAS_12_POLICY_TYPE = "MS-EAS-Provisioning-WBXML";
    static public final String EAS_2_POLICY_TYPE = "MS-WAP-Provisioning-XML";

    private short mConnectionSecurity;
    private String mUsername; /* Stores the username for authentications */
    private String mAlias; /* Stores the alias for the user's mailbox */
    private String mPassword; /* Stores the password for authentications */
    private String mUrl; /* Stores the base URL for the server */

    // Reasonable default
    public String mProtocolVersion = Eas.DEFAULT_PROTOCOL_VERSION;
    public Double mProtocolVersionDouble = Double.parseDouble(mProtocolVersion);
    protected String mDeviceId = null;
    protected String mDeviceType = "Android";
    private String mCmdString = null;
    
    private String mHost; /* Stores the host name for the server */
    private String mPath; /* Stores the path for the server */
    private String mAuthPath; /* Stores the path off of the server to post data to for form based authentication */
    private String mMailboxPath; /* Stores the user specified path to the mailbox */
    private URI mUri; /* Stores the Uniform Resource Indicator with all connection info */

    private boolean mSecure;
    private HttpClient mHttpClient = null;
    private HttpContext mContext = null;
    private String mAuthString;
    private CookieStore mAuthCookies = null;
    private short mAuthentication = AUTH_TYPE_NONE;
    private String mCachedLoginUrl;

    private Folder mSendFolder = null;
    private HashMap<String, EasFolder> mFolderList = new HashMap<String, EasFolder>();

    /**
     * eas://user:password@server:port CONNECTION_SECURITY_NONE
     * eas+tls://user:password@server:port CONNECTION_SECURITY_TLS_OPTIONAL
     * eas+tls+://user:password@server:port CONNECTION_SECURITY_TLS_REQUIRED
     * eas+ssl+://user:password@server:port CONNECTION_SECURITY_SSL_REQUIRED
     * eas+ssl://user:password@server:port CONNECTION_SECURITY_SSL_OPTIONAL
     */
    public EasStore(Account account) throws MessagingException {
        super(account);

        try {
            mUri = new URI(mAccount.getStoreUri());
        } catch (URISyntaxException use) {
            throw new MessagingException("Invalid WebDavStore URI", use);
        }

        String scheme = mUri.getScheme();
        if (scheme.equals("eas")) {
            mConnectionSecurity = CONNECTION_SECURITY_NONE;
        } else if (scheme.equals("eas+ssl")) {
            mConnectionSecurity = CONNECTION_SECURITY_SSL_OPTIONAL;
        } else if (scheme.equals("eas+ssl+")) {
            mConnectionSecurity = CONNECTION_SECURITY_SSL_REQUIRED;
        } else if (scheme.equals("eas+tls")) {
            mConnectionSecurity = CONNECTION_SECURITY_TLS_OPTIONAL;
        } else if (scheme.equals("eas+tls+")) {
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

        if (mUri.getUserInfo() != null) {
            try {
                String[] userInfoParts = mUri.getUserInfo().split(":");
                mUsername = URLDecoder.decode(userInfoParts[0], "UTF-8");
                String userParts[] = mUsername.split("\\\\", 2);

                if (userParts.length > 1) {
                    mAlias = userParts[1];
                } else {
                    mAlias = mUsername;
                }
                if (userInfoParts.length > 1) {
                    mPassword = URLDecoder.decode(userInfoParts[1], "UTF-8");
                }
            } catch (UnsupportedEncodingException enc) {
                // This shouldn't happen since the encoding is hardcoded to UTF-8
                Log.e(K9.LOG_TAG, "Couldn't urldecode username or password.", enc);
            }
        }

        String[] pathParts = mUri.getPath().split("\\|");

        for (int i = 0, count = pathParts.length; i < count; i++) {
            if (i == 0) {
                if (pathParts[0] != null &&
                        pathParts[0].length() > 1) {
                    mPath = pathParts[0];
                }
            } else if (i == 1) {
                if (pathParts[1] != null &&
                        pathParts[1].length() > 1) {
                    mAuthPath = pathParts[1];
                }
            } else if (i == 2) {
                if (pathParts[2] != null &&
                        pathParts[2].length() > 1) {
                    mMailboxPath = pathParts[2];
                }
            }
        }

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
        
        getInitialFolderList();
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
        root += "://" + mHost + ":" + mUri.getPort();
        return root;
    }

    @Override
    public void checkSettings() throws MessagingException {
        boolean ssl = true;
        
		boolean trustCertificates = true;
		
		validateAccount(
        		mHost,
        		mUsername,
        		mPassword,
        		mUri.getPort(),
        		ssl,
        		trustCertificates,
        		K9.app);
    }
    
    public void validateAccount(String hostAddress, String userName, String password, int port,
            boolean ssl, boolean trustCertificates, Context context) throws MessagingException {
        try {
        	Log.i(K9.LOG_TAG, "Testing EAS: " + hostAddress + ", " + userName + ", ssl = " + (ssl ? "1" : "0"));
            
//        	Account account = Preferences.getPreferences(context).newAccount();
//            account.setName("%TestAccount%");
//            account.setStoreUri(mUri.toString());
			EasStore svc = new EasStore(mAccount);
            svc.mContext = mContext;
            svc.mHost = hostAddress;
            svc.mUsername = userName;
            svc.mPassword = password;
//            svc.mSsl = ssl;
//            svc.mTrustSsl = trustCertificates;
            // We mustn't use the "real" device id or we'll screw up current accounts
            // Any string will do, but we'll go for "validate"
            svc.mDeviceId = "validate";
            HttpResponse resp = svc.sendHttpClientOptions();
            int code = resp.getStatusLine().getStatusCode();
            Log.e(K9.LOG_TAG, "Validation (OPTIONS) response: " + code);
            if (code == HttpStatus.SC_OK) {
                // No exception means successful validation
                Header commands = resp.getFirstHeader("MS-ASProtocolCommands");
                Header versions = resp.getFirstHeader("ms-asprotocolversions");
                if (commands == null || versions == null) {
                	Log.e(K9.LOG_TAG, "OPTIONS response without commands or versions; reporting I/O error");
                    throw new MessagingException("MessagingException.IOERROR");
                }

                // Make sure we've got the right protocol version set up
                setupProtocolVersion(svc, versions);

                // Run second test here for provisioning failures...
                Serializer s = new Serializer();
                Log.e(K9.LOG_TAG, "Validate: try folder sync");
                s.start(Tags.FOLDER_FOLDER_SYNC).start(Tags.FOLDER_SYNC_KEY).text("0")
                    .end().end().done();
                resp = svc.sendHttpClientPost("FolderSync", s.toByteArray());
                code = resp.getStatusLine().getStatusCode();
                // We'll get one of the following responses if policies are required by the server
                if (code == HttpStatus.SC_FORBIDDEN || code == HTTP_NEED_PROVISIONING) {
                    // Get the policies and see if we are able to support them
                	Log.e(K9.LOG_TAG, "Validate: provisioning required");
                    if (svc.canProvision() != null) {
                        // If so, send the advisory Exception (the account may be created later)
                    	Log.e(K9.LOG_TAG, "Validate: provisioning is possible");
                        //throw new MessagingException("MessagingException.SECURITY_POLICIES_REQUIRED");
                    	return;
                    } else
                    	Log.e(K9.LOG_TAG, "Validate: provisioning not possible");
                        // If not, send the unsupported Exception (the account won't be created)
                        throw new MessagingException(
                                "MessagingException.SECURITY_POLICIES_UNSUPPORTED");
                } else if (code == HttpStatus.SC_NOT_FOUND) {
                	Log.e(K9.LOG_TAG, "Wrong address or bad protocol version");
                    // We get a 404 from OWA addresses (which are NOT EAS addresses)
                    throw new MessagingException("MessagingException.PROTOCOL_VERSION_UNSUPPORTED");
                } else if (code != HttpStatus.SC_OK) {
                    // Fail generically with anything other than success
                	Log.e(K9.LOG_TAG, "Unexpected response for FolderSync: " + code);
                    throw new MessagingException("MessagingException.UNSPECIFIED_EXCEPTION");
                }
                Log.e(K9.LOG_TAG, "Validation successful");
                return;
            }
            if (isAuthError(code)) {
            	Log.e(K9.LOG_TAG, "Authentication failed");
                throw new AuthenticationFailedException("Validation failed");
            } else {
                // TODO Need to catch other kinds of errors (e.g. policy) For now, report the code.
            	Log.e(K9.LOG_TAG, "Validation failed, reporting I/O error: " + code);
                throw new MessagingException("MessagingException.IOERROR");
            }
        } catch (IOException e) {
            Throwable cause = e.getCause();
            if (cause != null && cause instanceof CertificateException) {
            	Log.e(K9.LOG_TAG, "CertificateException caught: ", e);
                throw new MessagingException("MessagingException.GENERAL_SECURITY");
            }
            Log.e(K9.LOG_TAG, "IOException caught: ", e);
            throw new MessagingException("MessagingException.IOERROR");
        }

    }
    
    private String getPolicyType() {
        return (mProtocolVersionDouble >=
            Eas.SUPPORTED_PROTOCOL_EX2007_DOUBLE) ? EAS_12_POLICY_TYPE : EAS_2_POLICY_TYPE;
    }

    /**
     * Obtain a set of policies from the server and determine whether those policies are supported
     * by the device.
     * @return the ProvisionParser (holds policies and key) if we receive policies and they are
     * supported by the device; null otherwise
     * @throws IOException
     */
    private ProvisionParser canProvision() throws IOException, MessagingException {
        Serializer s = new Serializer();
        s.start(Tags.PROVISION_PROVISION).start(Tags.PROVISION_POLICIES);
        s.start(Tags.PROVISION_POLICY).data(Tags.PROVISION_POLICY_TYPE, getPolicyType())
            .end().end().end().done();
        HttpResponse resp = sendHttpClientPost("Provision", s.toByteArray());
        int code = resp.getStatusLine().getStatusCode();
        if (code == HttpStatus.SC_OK) {
            InputStream is = resp.getEntity().getContent();
            ProvisionParser pp = new ProvisionParser(is);
            if (pp.parse()) {
                // The PolicySet in the ProvisionParser will have the requirements for all KNOWN
                // policies.  If others are required, hasSupportablePolicySet will be false
                if (pp.hasSupportablePolicySet()) {
                    // If the policies are supportable (in this context, meaning that there are no
                    // completely unimplemented policies required), just return the parser itself
                    return pp;
                } else {
                    // Try to acknowledge using the "partial" status (i.e. we can partially
                    // accommodate the required policies).  The server will agree to this if the
                    // "allow non-provisionable devices" setting is enabled on the server
                    String policyKey = acknowledgeProvision(pp.getPolicyKey(),
                            PROVISION_STATUS_PARTIAL);
                    // Return either the parser (success) or null (failure)
                    return (policyKey != null) ? pp : null;
                }
            }
        }
        // On failures, simply return null
        return null;
    }
    
    private String acknowledgeProvision(String tempKey, String result) throws IOException, MessagingException {
        return acknowledgeProvisionImpl(tempKey, result, false);
     }

    private String acknowledgeProvisionImpl(String tempKey, String status,
            boolean remoteWipe) throws IOException, MessagingException {
        Serializer s = new Serializer();
        s.start(Tags.PROVISION_PROVISION).start(Tags.PROVISION_POLICIES);
        s.start(Tags.PROVISION_POLICY);

        // Use the proper policy type, depending on EAS version
        s.data(Tags.PROVISION_POLICY_TYPE, getPolicyType());

        s.data(Tags.PROVISION_POLICY_KEY, tempKey);
        s.data(Tags.PROVISION_STATUS, status);
        s.end().end(); // PROVISION_POLICY, PROVISION_POLICIES
        if (remoteWipe) {
            s.start(Tags.PROVISION_REMOTE_WIPE);
            s.data(Tags.PROVISION_STATUS, PROVISION_STATUS_OK);
            s.end();
        }
        s.end().done(); // PROVISION_PROVISION
        HttpResponse resp = sendHttpClientPost("Provision", s.toByteArray());
        int code = resp.getStatusLine().getStatusCode();
        if (code == HttpStatus.SC_OK) {
            InputStream is = resp.getEntity().getContent();
            ProvisionParser pp = new ProvisionParser(is);
            if (pp.parse()) {
                // Return the final policy key from the ProvisionParser
                return pp.getPolicyKey();
            }
        }
        // On failures, return null
        return null;
    }



    /**
     * Determine whether an HTTP code represents an authentication error
     * @param code the HTTP code returned by the server
     * @return whether or not the code represents an authentication error
     */
    protected boolean isAuthError(int code) {
        return (code == HttpStatus.SC_UNAUTHORIZED) || (code == HttpStatus.SC_FORBIDDEN);
    }

    /**
     * Determine whether an HTTP code represents a provisioning error
     * @param code the HTTP code returned by the server
     * @return whether or not the code represents an provisioning error
     */
    protected boolean isProvisionError(int code) {
        return (code == HTTP_NEED_PROVISIONING) || (code == HttpStatus.SC_FORBIDDEN);
    }


    
    private void setupProtocolVersion(EasStore service, Header versionHeader)
		    throws MessagingException {
		// The string is a comma separated list of EAS versions in ascending order
		// e.g. 1.0,2.0,2.5,12.0,12.1
		String supportedVersions = versionHeader.getValue();
		Log.i(K9.LOG_TAG, "Server supports versions: " + supportedVersions);
		String[] supportedVersionsArray = supportedVersions.split(",");
		String ourVersion = null;
		// Find the most recent version we support
		for (String version: supportedVersionsArray) {
		    if (version.equals(Eas.SUPPORTED_PROTOCOL_EX2003) ||
		            version.equals(Eas.SUPPORTED_PROTOCOL_EX2007)) {
		        ourVersion = version;
		    }
		}
		// If we don't support any of the servers supported versions, throw an exception here
		// This will cause validation to fail
		if (ourVersion == null) {
		    Log.w(K9.LOG_TAG, "No supported EAS versions: " + supportedVersions);
		    throw new MessagingException("MessagingException.PROTOCOL_VERSION_UNSUPPORTED");
		} else {
		    service.mProtocolVersion = ourVersion;
		    service.mProtocolVersionDouble = Double.parseDouble(ourVersion);
		}
	}


    
    protected HttpResponse sendHttpClientPost(String cmd, byte[] bytes) throws IOException, MessagingException {
        return sendHttpClientPost(cmd, new ByteArrayEntity(bytes), COMMAND_TIMEOUT);
    }

    protected HttpResponse sendHttpClientPost(String cmd, HttpEntity entity) throws IOException, MessagingException {
        return sendHttpClientPost(cmd, entity, COMMAND_TIMEOUT);
    }

    /**
     * Convenience method for executePostWithTimeout for use other than with the Ping command
     */
    protected HttpResponse executePostWithTimeout(HttpClient client, HttpPost method, int timeout)
            throws IOException {
        return executePostWithTimeout(client, method, timeout, false);
    }

    /**
     * Handle executing an HTTP POST command with proper timeout, watchdog, and ping behavior
     * @param client the HttpClient
     * @param method the HttpPost
     * @param timeout the timeout before failure, in ms
     * @param isPingCommand whether the POST is for the Ping command (requires wakelock logic)
     * @return the HttpResponse
     * @throws IOException
     */
    protected HttpResponse executePostWithTimeout(HttpClient client, HttpPost method, int timeout,
            boolean isPingCommand) throws IOException {
//        synchronized(getSynchronizer()) {
//            mPendingPost = method;
//            long alarmTime = timeout + WATCHDOG_TIMEOUT_ALLOWANCE;
//            if (isPingCommand) {
//                SyncManager.runAsleep(mMailboxId, alarmTime);
//            } else {
//                SyncManager.setWatchdogAlarm(mMailboxId, alarmTime);
//            }
//        }
//        try {
            return client.execute(method);
//        } finally {
//            synchronized(getSynchronizer()) {
//                if (isPingCommand) {
//                    SyncManager.runAwake(mMailboxId);
//                } else {
//                    SyncManager.clearWatchdogAlarm(mMailboxId);
//                }
//                mPendingPost = null;
//            }
//        }
    }

    protected HttpResponse sendHttpClientPost(String cmd, HttpEntity entity, int timeout)
		    throws IOException, MessagingException {
		HttpClient client = getHttpClient();
		boolean isPingCommand = cmd.equals(PING_COMMAND);
		
		// Split the mail sending commands
		String extra = null;
		boolean msg = false;
		if (cmd.startsWith("SmartForward&") || cmd.startsWith("SmartReply&")) {
		    int cmdLength = cmd.indexOf('&');
		    extra = cmd.substring(cmdLength);
		    cmd = cmd.substring(0, cmdLength);
		    msg = true;
		} else if (cmd.startsWith("SendMail&")) {
		    msg = true;
		}
		
		String us = makeUriString(cmd, extra);
		HttpPost method = new HttpPost(URI.create(us));
		// Send the proper Content-Type header
		// If entity is null (e.g. for attachments), don't set this header
		if (msg) {
		    method.setHeader("Content-Type", "message/rfc822");
		} else if (entity != null) {
		    method.setHeader("Content-Type", "application/vnd.ms-sync.wbxml");
		}
		setHeaders(method, !cmd.equals(PING_COMMAND));
		method.setEntity(entity);
		return executePostWithTimeout(client, method, timeout, isPingCommand);
	}

    protected HttpResponse sendHttpClientOptions() throws IOException, MessagingException {
        HttpClient client = getHttpClient();
        String us = makeUriString("OPTIONS", null);
        HttpOptions method = new HttpOptions(URI.create(us));
        setHeaders(method, false);
        return client.execute(method);
    }
    
    /**
     * Set standard HTTP headers, using a policy key if required
     * @param method the method we are going to send
     * @param usePolicyKey whether or not a policy key should be sent in the headers
     */
    /*package*/ void setHeaders(HttpRequestBase method, boolean usePolicyKey) {
        method.setHeader("Authorization", mAuthString);
        method.setHeader("MS-ASProtocolVersion", mProtocolVersion);
        method.setHeader("Connection", "keep-alive");
        method.setHeader("User-Agent", mDeviceType + '/' + Eas.VERSION);
        if (usePolicyKey) {
            // If there's an account in existence, use its key; otherwise (we're creating the
            // account), send "0".  The server will respond with code 449 if there are policies
            // to be enforced
            String key = "0";
            if (mAccount != null) {
                String accountKey = AccountAdapter.mSecuritySyncKey;
                if (!TextUtils.isEmpty(accountKey)) {
                    key = accountKey;
                }
            }
            method.setHeader("X-MS-PolicyKey", key);
        }
    }

    private String makeUriString(String cmd, String extra) throws IOException {
        // Cache the authentication string and the command string
       if (mAuthString == null || mCmdString == null) {
           cacheAuthAndCmdString();
       }
       boolean mSsl = true;
       boolean mTrustSsl = false;
       String us = (mSsl ? (mTrustSsl ? "httpts" : "https") : "http") + "://" + mHost +
           "/Microsoft-Server-ActiveSync";
       if (cmd != null) {
           us += "?Cmd=" + cmd + mCmdString;
       }
       if (extra != null) {
           us += extra;
       }
       return us;
   }

    /**
     * Using mUserName and mPassword, create and cache mAuthString and mCacheString, which are used
     * in all HttpPost commands.  This should be called if these strings are null, or if mUserName
     * and/or mPassword are changed
     */
    @SuppressWarnings("deprecation")
    private void cacheAuthAndCmdString() {
        String safeUserName = URLEncoder.encode(mUsername);
        String cs = mUsername + ':' + mPassword;
        mAuthString = "Basic " + Base64.encodeToString(cs.getBytes(), Base64.NO_WRAP);
        mCmdString = "&User=" + safeUserName + "&DeviceId=" + mDeviceId +
            "&DeviceType=" + mDeviceType;
    }
    
    @Override
    public List <? extends Folder > getPersonalNamespaces(boolean forceListAll) throws MessagingException {
    	if (forceListAll) {
    		return getInitialFolderList();
    	} else {
    		return new ArrayList<EasFolder>(mFolderList.values());
    	}
    }
    
    public List <? extends Folder > getInitialFolderList() throws MessagingException {
        LinkedList<Folder> folderList = new LinkedList<Folder>();
        
    	AccountAdapter mAccount = new AccountAdapter();
    	MailboxAdapter mMailbox = new MailboxAdapter();
    	
    	try {
	        Serializer s = new Serializer();
	        s.start(Tags.FOLDER_FOLDER_SYNC).start(Tags.FOLDER_SYNC_KEY)
	            .text(mAccount.mSyncKey).end().end().done();
	        HttpResponse resp = sendHttpClientPost("FolderSync", s.toByteArray());
	        int code = resp.getStatusLine().getStatusCode();
	        if (code == HttpStatus.SC_OK) {
	            HttpEntity entity = resp.getEntity();
	            int len = (int)entity.getContentLength();
	            if (len != 0) {
	                InputStream is = entity.getContent();
	                // Returns true if we need to sync again
	                if (new FolderSyncParser(is, new AccountSyncAdapter(mMailbox, mAccount), this, folderList)
	                        .parse()) {
	                	throw new RuntimeException();
	                }
	            }
	        } else if (isProvisionError(code)) {
	            // If the sync error is a provisioning failure (perhaps the policies changed),
	            // let's try the provisioning procedure
	            // Provisioning must only be attempted for the account mailbox - trying to
	            // provision any other mailbox may result in race conditions and the creation
	            // of multiple policy keys.
	            if (!tryProvision()) {
	                // Set the appropriate failure status
//	                mExitStatus = EXIT_SECURITY_FAILURE;
	                throw new RuntimeException();
	            } else {
	                // If we succeeded, try again...
	                return getInitialFolderList();
	            }
	        } else if (isAuthError(code)) {
	            throw new RuntimeException();
//	            return;
	        } else {
	            Log.w(K9.LOG_TAG, "FolderSync response error: " + code);
	            throw new RuntimeException();
	        }
    	} catch (IOException e) {
    		throw new MessagingException("io", e);
    	}
    	
    	//this.mAccount.setAutoExpandFolderName("Inbox");
    	//this.mAccount.setInboxFolderName("Inbox");
    	for (Folder folder : folderList) {
    		mFolderList.put(folder.getName(), (EasFolder) folder);
    	}
    	
    	return folderList;

    }

    private boolean tryProvision() throws IOException, MessagingException {
        // First, see if provisioning is even possible, i.e. do we support the policies required
        // by the server
        ProvisionParser pp = canProvision();
        if (pp != null) {
        	String policyKey = acknowledgeProvision(pp.getPolicyKey(), PROVISION_STATUS_OK);
        	AccountAdapter.mSecuritySyncKey = policyKey;
        	return true;
        } else {
        	return false;
        }
	}

    @Override
    public Folder getFolder(String name) {
    	return mFolderList.get(name);
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

    /***************************************************************
     * WebDAV XML Request body retrieval functions
     */
    private String getFolderListXml() {
        StringBuffer buffer = new StringBuffer(200);
        buffer.append("<?xml version='1.0' ?>");
        buffer.append("<a:searchrequest xmlns:a='DAV:'><a:sql>\r\n");
        buffer.append("SELECT \"DAV:uid\", \"DAV:ishidden\"\r\n");
        buffer.append(" FROM SCOPE('hierarchical traversal of \"").append(this.mUrl).append("\"')\r\n");
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
        buffer.append(" WHERE \"DAV:ishidden\"=False AND \"DAV:isfolder\"=False AND \"urn:schemas:httpmail:read\"=")
        .append(messageState).append("\r\n");
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
            buffer.append(" \"DAV:uid\"='").append(uids[i]).append("' ");
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
            buffer.append(" \"DAV:uid\"='").append(uids[i]).append("' ");
        }
        buffer.append("\r\n");
        buffer.append("</a:sql></a:searchrequest>\r\n");
        return buffer.toString();
    }

    private String getMarkMessagesReadXml(String[] urls, boolean read) {
        StringBuffer buffer = new StringBuffer(600);
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
        StringBuffer buffer = new StringBuffer(600);
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
                }
            } else if (mAuthentication == AUTH_TYPE_BASIC) {
                // Nothing to do, we authenticate with every request when
                // using basic authentication.
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

        HttpClient httpClient = getHttpClient();

        HttpGeneric request = new HttpGeneric(mUrl);
        request.setMethod("GET");

        try {
            HttpResponse response = httpClient.execute(request, mContext);
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

    public CookieStore getAuthCookies() {
        return mAuthCookies;
    }

    public String getAlias() {
        return mAlias;
    }

    public String getUrl() {
        return mUrl;
    }

    public HttpClient getHttpClient() throws MessagingException {
        if (mHttpClient == null) {
            mHttpClient = new DefaultHttpClient();
            // Disable automatDic redirects on the http client.
            mHttpClient.getParams().setBooleanParameter("http.protocol.handle-redirects", false);

            // Setup a cookie store for forms-based authentication.
            mContext = new BasicHttpContext();
            mAuthCookies = new BasicCookieStore();
            mContext.setAttribute(ClientContext.COOKIE_STORE, mAuthCookies);

            SchemeRegistry reg = mHttpClient.getConnectionManager().getSchemeRegistry();
            try {
                Scheme s = new Scheme("https", new TrustedSocketFactory(mHost, mSecure), 443);
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

        HttpClient httpclient = getHttpClient();

        try {
            int statusCode = -1;
            HttpGeneric httpmethod = new HttpGeneric(url);
            HttpResponse response;
            HttpEntity entity;

            if (messageBody != null) {
                httpmethod.setEntity(messageBody);
            }

            if (headers != null) {
                for (String headerName : headers.keySet()) {
                    httpmethod.setHeader(headerName, headers.get(headerName));
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
            response = httpclient.execute(httpmethod, mContext);
            statusCode = response.getStatusLine().getStatusCode();

            entity = response.getEntity();

            if (statusCode == 401) {
                throw new MessagingException("Invalid username or password for Basic authentication.");
            } else if (statusCode == 440) {
                throw new MessagingException("Authentication failure in sendRequest().");
            } else if (statusCode < 200 || statusCode >= 300) {
                throw new IOException("Error with code " + statusCode + " during request processing: " +
                                      response.getStatusLine().toString());
            }

            if (entity != null) {
                istream = entity.getContent();
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
        EasFolder tmpFolder = (EasStore.EasFolder) getFolder(mAccount.getDraftsFolderName());
        try {
            tmpFolder.open(OpenMode.READ_WRITE);
//            Message[] retMessages = tmpFolder.appendWebDavMessages(messages);
//
//            tmpFolder.moveMessages(retMessages, getSendSpoolFolder());
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
     * A EAS Folder
     */
    class EasFolder extends Folder {
        private String mName;
        private String mServerId;
        private boolean mIsOpen = false;
        private int mMessageCount = 0;
        private EasStore store;

        protected EasStore getStore() {
            return store;
        }

        public EasFolder(EasStore nStore, String name, String serverId) {
            super(nStore.getAccount());
            store = nStore;
            this.mName = name;
            this.mServerId = serverId;
        }

        @Override
        public void open(OpenMode mode) throws MessagingException {
            getHttpClient();

            this.mIsOpen = true;
        }

        @Override
        public void copyMessages(Message[] messages, Folder folder) throws MessagingException {
            moveOrCopyMessages(messages, folder.getName(), false);
        }

        @Override
        public void moveMessages(Message[] messages, Folder folder) throws MessagingException {
            moveOrCopyMessages(messages, folder.getName(), true);
        }

        @Override
        public void delete(Message[] msgs, String trashFolderName) throws MessagingException {
            moveOrCopyMessages(msgs, trashFolderName, true);
        }

        private void moveOrCopyMessages(Message[] messages, String folderName, boolean isMove)
        throws MessagingException {
//            String[] uids = new String[messages.length];
//
//            for (int i = 0, count = messages.length; i < count; i++) {
//                uids[i] = messages[i].getUid();
//            }
//            String messageBody = "";
//            HashMap<String, String> headers = new HashMap<String, String>();
//            HashMap<String, String> uidToUrl = getMessageUrls(uids);
//            String[] urls = new String[uids.length];
//
//            for (int i = 0, count = uids.length; i < count; i++) {
//                urls[i] = uidToUrl.get(uids[i]);
//                if (urls[i] == null && messages[i] instanceof EasMessage) {
//                    EasMessage wdMessage = (EasMessage) messages[i];
//                    urls[i] = wdMessage.getUrl();
//                }
//            }
//
//            messageBody = getMoveOrCopyMessagesReadXml(urls, isMove);
//            EasFolder destFolder = (EasFolder) store.getFolder(folderName);
//            headers.put("Destination", destFolder.mFolderUrl);
//            headers.put("Brief", "t");
//            headers.put("If-Match", "*");
//            String action = (isMove ? "BMOVE" : "BCOPY");
//            Log.i(K9.LOG_TAG, "Moving " + messages.length + " messages to " + destFolder.mFolderUrl);
//
//            processRequest(mFolderUrl, action, messageBody, headers, false);
        }

        private int getMessageCount(boolean read) throws MessagingException {
			Serializer s = new Serializer();
			try {
				s
					.start(Tags.GIE_GET_ITEM_ESTIMATE)
						.start(Tags.GIE_COLLECTIONS)
							.start(Tags.GIE_COLLECTION)
								.data(Tags.SYNC_SYNC_KEY, "0")
								.data(Tags.GIE_COLLECTION_ID, mServerId)
							.end()
						.end()
					.end()
				.done();
				
		        HttpResponse resp = sendHttpClientPost("GetItemEstimate", s.toByteArray());
		        int code = resp.getStatusLine().getStatusCode();
		        if (code == HttpStatus.SC_OK) {
		        	HttpEntity entity = resp.getEntity();
		            int len = (int)entity.getContentLength();
		            if (len != 0) {
		                InputStream is = entity.getContent();
		                GetItemEstimateParser gieParser = new GetItemEstimateParser(is);
		                if (gieParser.parse()) {
		                	return gieParser.getEstimate();
		                }
		            }
		        }
		        // On failures
		        throw new MessagingException("getItemEstimate call returned not OK status");
			} catch (IOException e) {
				throw new MessagingException("getItemEstimate call failed", e);
			}
        }

        @Override
        public int getMessageCount() throws MessagingException {
            open(OpenMode.READ_WRITE);
            this.mMessageCount = getMessageCount(true);
            return this.mMessageCount;
        }

        @Override
        public int getUnreadMessageCount() throws MessagingException {
            return -1;
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
        public OpenMode getMode() {
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
        public void close() {
            this.mMessageCount = 0;
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
            return new EasMessage(uid, this);
        }

        @Override
        public Message[] getMessages(int start, int end, Date earliestDate, MessageRetrievalListener listener)
        throws MessagingException {
        	Serializer s = new Serializer();
        	
        	
//        	try {
//				s
//					.start(Tags.SEARCH_SEARCH)
//						.start(Tags.SEARCH_STORE)
//							.data(Tags.SEARCH_NAME, "Mailbox")
//							.start(Tags.SEARCH_QUERY)
////								.start(Tags.SEARCH_AND)
////									.data(Tags.SYNC_COLLECTION_ID, mServerId);
//									.data(Tags.SEARCH_FREE_TEXT, "gmail");
//				
//				if (earliestDate != null) {
////					s
////									.start(Tags.SEARCH_GREATER_THAN)
////										.data(Tags.EMAIL_DATE_RECEIVED, "")
////									.end();
//				}
//				
//				s
////								.end()
//							.end()
////							.start(Tags.SEARCH_OPTIONS)
////								.tag(Tags.SEARCH_REBUILD_RESULTS)
////								.data(Tags.SEARCH_RANGE, (start-1) + "-" + (end-1))
////								.tag(Tags.SEARCH_DEEP_TRAVERSAL)
////							.end()
//						.end()
//					.end()
//				.done();
//				
//				s = new Serializer();
//				
//				s.start(Tags.SEARCH_SEARCH)
//					.start(Tags.SEARCH_STORE)
//						.data(Tags.SEARCH_NAME, "Mailbox")
//						.start(Tags.SEARCH_QUERY)
//							.start(Tags.SEARCH_AND)
//								.data(Tags.SYNC_COLLECTION_ID, mServerId)
//								.data(Tags.SEARCH_FREE_TEXT, "gmail")
//							.end()
//						.end()
//					.end()
//				.end()
//				.done();
//				
//		        HttpResponse resp = sendHttpClientPost("Search", s.toByteArray());
//		        int code = resp.getStatusLine().getStatusCode();
//		        if (code == HttpStatus.SC_OK) {
//		        	HttpEntity entity = resp.getEntity();
//		            int len = (int)entity.getContentLength();
//		            if (len != 0) {
//		                InputStream is = entity.getContent();
//		                SearchParser searchParser = new SearchParser(is);
//		                if (searchParser.parse()) {
//		                	return searchParser.getMessages();
//		                }
//		            }
//		        }
//		        // On failures
//		        throw new MessagingException("getMessages call returned not OK status");
//			} catch (IOException e) {
//				throw new MessagingException("getMessages call failed", e);
//			}
        	
        	
        	
            // Maximum number of times we'll allow a sync to "loop" with MoreAvailable true before
            // forcing it to stop.  This number has been determined empirically.
            final int MAX_LOOPING_COUNT = 100;

        	try {
	        	EmailSyncAdapter target = new EmailSyncAdapter(new MailboxAdapter(), new AccountAdapter());
	            
	            String className = target.getCollectionName();
	            String syncKey = target.getSyncKey();
//            	userLog("sync, sending ", className, " syncKey: ", syncKey);
	            s.start(Tags.SYNC_SYNC)
	                .start(Tags.SYNC_COLLECTIONS)
	                .start(Tags.SYNC_COLLECTION)
	                .data(Tags.SYNC_CLASS, className)
	                .data(Tags.SYNC_SYNC_KEY, syncKey)
	                .data(Tags.SYNC_COLLECTION_ID, mServerId);
	
	            // Start with the default timeout
	            int timeout = COMMAND_TIMEOUT;
	            if (!syncKey.equals("0")) {
	                // EAS doesn't allow GetChanges in an initial sync; sending other options
	                // appears to cause the server to delay its response in some cases, and this delay
	                // can be long enough to result in an IOException and total failure to sync.
	                // Therefore, we don't send any options with the initial sync.
	                s.tag(Tags.SYNC_DELETES_AS_MOVES);
	                s.tag(Tags.SYNC_GET_CHANGES);
	                s.data(Tags.SYNC_WINDOW_SIZE, Integer.toString(end - start + 1));
	                // Handle options
	                s.start(Tags.SYNC_OPTIONS);
	                // Set the lookback appropriately (EAS calls this a "filter") for all but Contacts
	                s.data(Tags.SYNC_FILTER_TYPE, getEmailFilter());
	                // Set the truncation amount for all classes
	                if (mProtocolVersionDouble >= Eas.SUPPORTED_PROTOCOL_EX2007_DOUBLE) {
	                    s.start(Tags.BASE_BODY_PREFERENCE)
	                    // HTML for email; plain text for everything else
	                    .data(Tags.BASE_TYPE, Eas.BODY_PREFERENCE_HTML)
	                    .data(Tags.BASE_TRUNCATION_SIZE, Eas.EAS12_TRUNCATION_SIZE)
	                    .end();
	                } else {
	                    s.data(Tags.SYNC_TRUNCATION, Eas.EAS2_5_TRUNCATION_SIZE);
	                }
	                s.end();
	            } else {
	                // Use enormous timeout for initial sync, which empirically can take a while longer
	                timeout = 120 * 1000;
	            }
//	            // Send our changes up to the server
//	            target.sendLocalChanges(s);

	            s.end().end().end().done();
	            HttpResponse resp = sendHttpClientPost("Sync", new ByteArrayEntity(s.toByteArray()),
	                    timeout);
	            int code = resp.getStatusLine().getStatusCode();
	            if (code == HttpStatus.SC_OK) {
	                InputStream is = resp.getEntity().getContent();
	                if (is != null) {
	                    boolean moreAvailable = target.parse(is);
	                    int loopingCount = 0;
						if (target.isLooping()) {
	                        loopingCount ++;
	                        Log.d(K9.LOG_TAG, "** Looping: " + loopingCount);
	                        // After the maximum number of loops, we'll set moreAvailable to false and
	                        // allow the sync loop to terminate
	                        if (moreAvailable && (loopingCount > MAX_LOOPING_COUNT)) {
	                        	Log.d(K9.LOG_TAG, "** Looping force stopped");
	                            moreAvailable = false;
	                        }
	                    } else {
	                        loopingCount = 0;
	                    }
	                    target.cleanup();
	                } else {
	                	Log.d(K9.LOG_TAG, "Empty input stream in sync command response");
	                }
	            } else {
//	                userLog("Sync response error: ", code);
//	                if (isProvisionError(code)) {
//	                    mExitStatus = EXIT_SECURITY_FAILURE;
//	                } else if (isAuthError(code)) {
//	                    mExitStatus = EXIT_LOGIN_FAILURE;
//	                } else {
//	                    mExitStatus = EXIT_IO_ERROR;
//	                }
//	                return;
	            	
	            }
            
            	List<Message> messages = target.getMessages();
            	
            	return messages.toArray(EMPTY_MESSAGE_ARRAY);
        	} catch (IOException e) {
				throw new MessagingException("getMessages call failed", e);
			}
        }
        
        private String getEmailFilter() {
            String filter = Eas.FILTER_1_WEEK;
//            switch (mAccount.mSyncLookback) {
//                case com.android.email.Account.SYNC_WINDOW_1_DAY: {
//                    filter = Eas.FILTER_1_DAY;
//                    break;
//                }
//                case com.android.email.Account.SYNC_WINDOW_3_DAYS: {
//                    filter = Eas.FILTER_3_DAYS;
//                    break;
//                }
//                case com.android.email.Account.SYNC_WINDOW_1_WEEK: {
//                    filter = Eas.FILTER_1_WEEK;
//                    break;
//                }
//                case com.android.email.Account.SYNC_WINDOW_2_WEEKS: {
//                    filter = Eas.FILTER_2_WEEKS;
//                    break;
//                }
//                case com.android.email.Account.SYNC_WINDOW_1_MONTH: {
//                    filter = Eas.FILTER_1_MONTH;
//                    break;
//                }
//                case com.android.email.Account.SYNC_WINDOW_ALL: {
                    filter = Eas.FILTER_ALL;
//                    break;
//                }
//            }
            return filter;
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

                EasMessage message = new EasMessage(uids[i], this);
                messageList.add(message);

                if (listener != null) {
                    listener.messageFinished(message, i, count);
                }
            }
            messages = messageList.toArray(EMPTY_MESSAGE_ARRAY);

            return messages;
        }

        private HashMap<String, String> getMessageUrls(String[] uids) throws MessagingException {
            HashMap<String, String> uidToUrl = new HashMap<String, String>();
//            HashMap<String, String> headers = new HashMap<String, String>();
//            DataSet dataset = new DataSet();
//            String messageBody;
//
//            /** Retrieve and parse the XML entity for our messages */
//            messageBody = getMessageUrlsXml(uids);
//            headers.put("Brief", "t");
//
//            dataset = processRequest(this.mFolderUrl, "SEARCH", messageBody, headers);
//            uidToUrl = dataset.getUidToUrl();
//
            return uidToUrl;
        }

        @Override
        public void fetch(Message[] messages, FetchProfile fp, MessageRetrievalListener listener)
        throws MessagingException {
            if (messages == null ||
                    messages.length == 0) {
                return;
            }

            for (int i = 0, count = messages.length; i < count; i++) {
                Message easMessage;

//                if (!(messages[i] instanceof EasMessage)) {
//                    throw new MessagingException("EasStore fetch called with non-EasMessage");
//                }

                easMessage = (Message) messages[i];
                
	            if (listener != null) {
	                listener.messageStarted(easMessage.getUid(), i, count);
	            }
	            
	            if (listener != null) {
	            	listener.messageFinished(easMessage, i, count);
	            }
            }

//            /**
//             * Fetch message envelope information for the array
//             */
//            if (fp.contains(FetchProfile.Item.ENVELOPE)) {
//                fetchEnvelope(messages, listener);
//            }
//            /**
//             * Fetch message flag info for the array
//             */
//            if (fp.contains(FetchProfile.Item.FLAGS)) {
//                fetchFlags(messages, listener);
//            }
//
//            if (fp.contains(FetchProfile.Item.BODY_SANE)) {
//                fetchMessages(messages, listener, (mAccount.getMaximumAutoDownloadMessageSize() / 76));
//            }
//            if (fp.contains(FetchProfile.Item.BODY)) {
//                fetchMessages(messages, listener, -1);
//            }
        }

        /**
         * Fetches the full messages or up to lines lines and passes them to the message parser.
         */
        private void fetchMessages(Message[] messages, MessageRetrievalListener listener, int lines)
        throws MessagingException {
//            HttpClient httpclient;
//            httpclient = getHttpClient();
//
//            /**
//             * We can't hand off to processRequest() since we need the stream to parse.
//             */
//            for (int i = 0, count = messages.length; i < count; i++) {
//                EasMessage wdMessage;
//                int statusCode = 0;
//
//                if (!(messages[i] instanceof EasMessage)) {
//                    throw new MessagingException("WebDavStore fetch called with non-WebDavMessage");
//                }
//
//                wdMessage = (EasMessage) messages[i];
//
//                if (listener != null) {
//                    listener.messageStarted(wdMessage.getUid(), i, count);
//                }
//
//                /**
//                 * If fetch is called outside of the initial list (ie, a locally stored message), it may not have a URL
//                 * associated. Verify and fix that
//                 */
//                if (wdMessage.getUrl().equals("")) {
//                    wdMessage.setUrl(getMessageUrls(new String[] { wdMessage.getUid() }).get(wdMessage.getUid()));
//                    Log.i(K9.LOG_TAG, "Fetching messages with UID = '" + wdMessage.getUid() + "', URL = '"
//                          + wdMessage.getUrl() + "'");
//                    if (wdMessage.getUrl().equals("")) {
//                        throw new MessagingException("Unable to get URL for message");
//                    }
//                }
//
//                try {
//                    Log.i(K9.LOG_TAG, "Fetching message with UID = '" + wdMessage.getUid() + "', URL = '"
//                          + wdMessage.getUrl() + "'");
//                    HttpGet httpget = new HttpGet(new URI(wdMessage.getUrl()));
//                    HttpResponse response;
//                    HttpEntity entity;
//
//                    httpget.setHeader("translate", "f");
//                    if (mAuthentication == AUTH_TYPE_BASIC) {
//                        httpget.setHeader("Authorization", mAuthString);
//                    }
//                    response = httpclient.execute(httpget, mContext);
//
//                    statusCode = response.getStatusLine().getStatusCode();
//
//                    entity = response.getEntity();
//
//                    if (statusCode < 200 ||
//                            statusCode > 300) {
//                        throw new IOException("Error during with code " + statusCode + " during fetch: "
//                                              + response.getStatusLine().toString());
//                    }
//
//                    if (entity != null) {
//                        InputStream istream = null;
//                        StringBuffer buffer = new StringBuffer();
//                        String tempText = "";
//                        String resultText = "";
//                        BufferedReader reader;
//                        int currentLines = 0;
//
//                        istream = WebDavHttpClient.getUngzippedContent(entity);
//
//                        if (lines != -1) {
//                            reader = new BufferedReader(new InputStreamReader(istream), 8192);
//
//                            while ((tempText = reader.readLine()) != null &&
//                                    (currentLines < lines)) {
//                                buffer.append(tempText).append("\r\n");
//                                currentLines++;
//                            }
//
//                            istream.close();
//                            resultText = buffer.toString();
//                            istream = new ByteArrayInputStream(resultText.getBytes("UTF-8"));
//                        }
//
//                        wdMessage.parse(istream);
//                    }
//
//                } catch (IllegalArgumentException iae) {
//                    Log.e(K9.LOG_TAG, "IllegalArgumentException caught " + iae + "\nTrace: " + processException(iae));
//                    throw new MessagingException("IllegalArgumentException caught", iae);
//                } catch (URISyntaxException use) {
//                    Log.e(K9.LOG_TAG, "URISyntaxException caught " + use + "\nTrace: " + processException(use));
//                    throw new MessagingException("URISyntaxException caught", use);
//                } catch (IOException ioe) {
//                    Log.e(K9.LOG_TAG, "Non-success response code loading message, response code was " + statusCode
//                          + "\nURL: " + wdMessage.getUrl() + "\nError: " + ioe.getMessage() + "\nTrace: "
//                          + processException(ioe));
//                    throw new MessagingException("Failure code " + statusCode, ioe);
//                }
//
//                if (listener != null) {
//                    listener.messageFinished(wdMessage, i, count);
//                }
//            }
        }

        /**
         * Fetches and sets the message flags for the supplied messages. The idea is to have this be recursive so that
         * we do a series of medium calls instead of one large massive call or a large number of smaller calls.
         */
        private void fetchFlags(Message[] startMessages, MessageRetrievalListener listener) throws MessagingException {
            HashMap<String, Boolean> uidToReadStatus = new HashMap<String, Boolean>();
            HashMap<String, String> headers = new HashMap<String, String>();
            DataSet dataset = new DataSet();
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
//            dataset = processRequest(this.mFolderUrl, "SEARCH", messageBody, headers);

            if (dataset == null) {
                throw new MessagingException("Data Set from request was null");
            }

            uidToReadStatus = dataset.getUidToRead();

            for (int i = 0, count = messages.length; i < count; i++) {
                if (!(messages[i] instanceof EasMessage)) {
                    throw new MessagingException("WebDavStore fetch called with non-WebDavMessage");
                }
                EasMessage wdMessage = (EasMessage) messages[i];

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
         * Fetches and parses the message envelopes for the supplied messages. The idea is to have this be recursive so
         * that we do a series of medium calls instead of one large massive call or a large number of smaller calls.
         * Call it a happy balance
         */
        private void fetchEnvelope(Message[] startMessages, MessageRetrievalListener listener)
        throws MessagingException {
            HashMap<String, ParsedMessageEnvelope> envelopes = new HashMap<String, ParsedMessageEnvelope>();
            HashMap<String, String> headers = new HashMap<String, String>();
            DataSet dataset = new DataSet();
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
//            dataset = processRequest(this.mFolderUrl, "SEARCH", messageBody, headers);

            envelopes = dataset.getMessageEnvelopes();

            int count = messages.length;
            for (int i = messages.length - 1; i >= 0; i--) {
                if (!(messages[i] instanceof EasMessage)) {
                    throw new MessagingException("EasStore fetch called with non-EasMessage");
                }
                EasMessage wdMessage = (EasMessage) messages[i];

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
        public Flag[] getPermanentFlags() {
            return PERMANENT_FLAGS;
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

//            processRequest(this.mFolderUrl, "BPROPPATCH", messageBody, headers, false);
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
            String finalUrl = EasStore.this.mUrl + "Deleted%20Items/" + filename;

            return finalUrl;
        }

        @Override
        public void appendMessages(Message[] messages) throws MessagingException {
//            appendWebDavMessages(messages);
        }

        @Override
        public boolean equals(Object o) {
            return false;
        }

        @Override
        public int hashCode() {
            return super.hashCode();
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
     * A EAS Message
     */
    class EasMessage extends MimeMessage {
        EasMessage(String uid, Folder folder) {
            this.mUid = uid;
            this.mFolder = folder;
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
//            String[] headers = envelope.getHeaderList();
//            HashMap<String, String> messageHeaders = envelope.getMessageHeaders();
//
//            for (String header : headers) {
//                String headerValue = messageHeaders.get(header);
//                if (header.equals("Content-Length")) {
//                    int size = Integer.parseInt(messageHeaders.get(header));
//                    this.setSize(size);
//                }
//
//                if (headerValue != null &&
//                        !headerValue.equals("")) {
//                    this.addHeader(header, headerValue);
//                }
//            }
        }

        @Override
        public void delete(String trashFolderName) throws MessagingException {
//            EasFolder wdFolder = (EasFolder) getFolder();
//            Log.i(K9.LOG_TAG, "Deleting message by moving to " + trashFolderName);
//            wdFolder.moveMessages(new Message[] { this }, wdFolder.getStore().getFolder(trashFolderName));
        }

        @Override
        public void setFlag(Flag flag, boolean set) throws MessagingException {
//            super.setFlag(flag, set);
//            mFolder.setFlags(new Message[] { this }, new Flag[] { flag }, set);
        }
    }

    /**
     * XML Parsing Handler Can handle all XML handling needs
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
     * Data set for a single E-Mail message's required headers (the envelope) Only provides accessor methods to the
     * stored data. All processing should be done elsewhere. This is done rather than having multiple hashmaps
     * associating UIDs to values
     */
    public static class ParsedMessageEnvelope {
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
        private String mUid = "";
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
        // private HashMap<String, String> mLostData = new HashMap<String, String>();
        private String mUid = "";
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
                /*
                 * Lost Data are for requests that don't include a message UID. These requests should only have a depth
                 * of one for the response so it will never get stomped over.
                 */
            }

            mUid = "";
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
                if (readStatus != null &&
                        !readStatus.equals("")) {
                    Boolean value = !readStatus.equals("0");
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
            int messageCount = -1;

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
                    for (String header : data.keySet()) {
                        if (header.equals("read")) {
                            String read = data.get(header);
                            Boolean readStatus = !read.equals("0");

                            envelope.setReadStatus(readStatus);
                        } else if (header.equals("date")) {
                            /**
                             * Exchange doesn't give us rfc822 dates like it claims. The date is in the format:
                             * yyyy-MM-dd'T'HH:mm:ss.SSS<Single digit representation of timezone, so far, all instances
                             * are Z>
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
                                Log.e(K9.LOG_TAG, "Error parsing date: " + pe + "\nTrace: " + processException(pe));
                            }
                            envelope.addHeader(header, tempDate);
                        } else {
                            envelope.addHeader(header, data.get(header));
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

    /**
     * Simple data container for passing connection information.
     */
    private static class ConnectionInfo {
        public int statusCode;
        public short requiredAuthType;
        public String guessedAuthUrl;
        public String redirectUrl;
    }

	public Folder createFolderInternal(String name, String serverId, int type) {
		EasFolder folder = new EasFolder(this, name, serverId);
		return folder;
	}

	public Message createMessageInternal(String uid, Folder folder) {
		EasMessage message = new EasMessage(uid, folder);
		return message;
	}
}
