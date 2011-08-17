package com.fsck.k9.mail.store;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.content.Context;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.controller.MessageRetrievalListener;
import com.fsck.k9.helper.Utility;
import com.fsck.k9.helper.power.TracingPowerManager;
import com.fsck.k9.helper.power.TracingPowerManager.TracingWakeLock;
import com.fsck.k9.mail.AuthenticationFailedException;
import com.fsck.k9.mail.FetchProfile;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.PushReceiver;
import com.fsck.k9.mail.Pusher;
import com.fsck.k9.mail.Store;
import com.fsck.k9.mail.filter.EOLConvertingOutputStream;
import com.fsck.k9.mail.internet.MimeMessage;
import com.fsck.k9.mail.store.exchange.Eas;
import com.fsck.k9.mail.store.exchange.adapter.EasEmailSyncParser;
import com.fsck.k9.mail.store.exchange.adapter.FolderSyncParser;
import com.fsck.k9.mail.store.exchange.adapter.PingParser;
import com.fsck.k9.mail.store.exchange.adapter.ProvisionParser;
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

    private static final Flag[] PERMANENT_FLAGS = { Flag.DELETED, Flag.SEEN, Flag.ANSWERED };

    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    private static final Message[] EMPTY_MESSAGE_ARRAY = new Message[0];

    static private final String PING_COMMAND = "Ping";
    // Command timeout is the the time allowed for reading data from an open connection before an
    // IOException is thrown.  After a small added allowance, our watchdog alarm goes off (allowing
    // us to detect a silently dropped connection).  The allowance is defined below.
    static private final int COMMAND_TIMEOUT = 30*1000;
    // Connection timeout is the time given to connect to the server before reporting an IOException
    static private final int CONNECTION_TIMEOUT = 20*1000;

    // This needs to be long enough to send the longest reasonable message, without being so long
    // as to effectively "hang" sending of mail.  The standard 30 second timeout isn't long enough
    // for pictures and the like.  For now, we'll use 15 minutes, in the knowledge that any socket
    // failure would probably generate an Exception before timing out anyway
    public static final int SEND_MAIL_TIMEOUT = 15*60*1000;

    // MSFT's custom HTTP result code indicating the need to provision
    static private final int HTTP_NEED_PROVISIONING = 449;

    // The EAS protocol Provision status for "we implement all of the policies"
    static private final String PROVISION_STATUS_OK = "1";
    // The EAS protocol Provision status meaning "we partially implement the policies"
    static private final String PROVISION_STATUS_PARTIAL = "2";

    static public final String EAS_12_POLICY_TYPE = "MS-EAS-Provisioning-WBXML";
    static public final String EAS_2_POLICY_TYPE = "MS-WAP-Provisioning-XML";
    
    private static final int IDLE_READ_TIMEOUT_INCREMENT = 5 * 60 * 1000;
    private static final int IDLE_FAILURE_COUNT_LIMIT = 10;
    private static int MAX_DELAY_TIME = 5 * 60 * 1000; // 5 minutes
    private static int NORMAL_DELAY_TIME = 5000;

    private short mConnectionSecurity;
    private String mUsername; /* Stores the username for authentications */
    private String mAlias; /* Stores the alias for the user's mailbox */
    private String mPassword; /* Stores the password for authentications */
    private String mUrl; /* Stores the base URL for the server */

    // Reasonable default
    public String mProtocolVersion;
    public Double mProtocolVersionDouble;
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
    private String mAuthString;

    private Folder mSendFolder = null;
    private HashMap<String, EasFolder> mFolderList = new HashMap<String, EasFolder>();
    
    private String mStoreSyncKey = "0";
    private String mStoreSecuritySyncKey = "0";

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
    
	public String getStoreSyncKey() {
		return mStoreSyncKey;
	}

	public void setStoreSyncKey(String mStoreSyncKey) {
		this.mStoreSyncKey = mStoreSyncKey;
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
        HttpConnectionParams.setSoTimeout(method.getParams(), timeout);
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
        
        HttpConnectionParams.setSoTimeout(method.getParams(), COMMAND_TIMEOUT);
        
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
                String accountKey = mStoreSecuritySyncKey;
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
    
    private void init() throws IOException, MessagingException {
        // Determine our protocol version, if we haven't already and save it in the Account
        // Also re-check protocol version at least once a day (in case of upgrade)
    	boolean lastSyncTimeDayDue = false;
    	//lastSyncTimeDayDue = ((System.currentTimeMillis() - mMailbox.mSyncTime) > DAYS);
        if (mProtocolVersion == null || lastSyncTimeDayDue) {
        	Log.d(K9.LOG_TAG, "Determine EAS protocol version");
            HttpResponse resp = sendHttpClientOptions();
            int code = resp.getStatusLine().getStatusCode();
            Log.d(K9.LOG_TAG, "OPTIONS response: " + code);
            if (code == HttpStatus.SC_OK) {
                Header header = resp.getFirstHeader("MS-ASProtocolCommands");
                Log.d(K9.LOG_TAG, header.getValue());
                header = resp.getFirstHeader("ms-asprotocolversions");
                try {
                    setupProtocolVersion(this, header);
                } catch (MessagingException e) {
                    // Since we've already validated, this can't really happen
                    // But if it does, we'll rethrow this...
                    throw new IOException();
                }
             } else {
                Log.e(K9.LOG_TAG, "OPTIONS command failed; throwing IOException");
                throw new IOException();
            }
        }
    }
    
    public List <? extends Folder > getInitialFolderList() throws MessagingException {
        LinkedList<Folder> folderList = new LinkedList<Folder>();
        
    	try {
    		init();
    		
	        Serializer s = new Serializer();
	        s.start(Tags.FOLDER_FOLDER_SYNC).start(Tags.FOLDER_SYNC_KEY)
	            .text(getStoreSyncKey()).end().end().done();
	        HttpResponse resp = sendHttpClientPost("FolderSync", s.toByteArray());
	        int code = resp.getStatusLine().getStatusCode();
	        if (code == HttpStatus.SC_OK) {
	            HttpEntity entity = resp.getEntity();
	            int len = (int)entity.getContentLength();
	            if (len != 0) {
	                InputStream is = entity.getContent();
	                // Returns true if we need to sync again
	                if (new FolderSyncParser(is, this, folderList)
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
    	
    	for (Folder folder : folderList) {
    		if (((EasFolder) folder).mType == FolderSyncParser.INBOX_TYPE) {
    			String inboxFolderName = folder.getName();
    	    	this.mAccount.setAutoExpandFolderName(inboxFolderName);
    	    	this.mAccount.setInboxFolderName(inboxFolderName);
    		}
    	}
    	
    	return folderList;

    }

    private boolean tryProvision() throws IOException, MessagingException {
        // First, see if provisioning is even possible, i.e. do we support the policies required
        // by the server
        ProvisionParser pp = canProvision();
        if (pp != null) {
        	String policyKey = acknowledgeProvision(pp.getPolicyKey(), PROVISION_STATUS_OK);
        	mStoreSecuritySyncKey = policyKey;
        	return true;
        } else {
        	return false;
        }
	}

    @Override
    public Folder getFolder(String name) {
    	return mFolderList.get(name);
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
     * Authentication related methods
     */

    public String getAlias() {
        return mAlias;
    }

    public String getUrl() {
        return mUrl;
    }

    public HttpClient getHttpClient() throws MessagingException {
        if (mHttpClient == null) {
            mHttpClient = new DefaultHttpClient();
            
            HttpParams params = mHttpClient.getParams();
            
            // Disable automatDic redirects on the http client.
			params.setBooleanParameter("http.protocol.handle-redirects", false);

            HttpConnectionParams.setConnectionTimeout(params, CONNECTION_TIMEOUT);
            HttpConnectionParams.setSocketBufferSize(params, 8192);

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

    public String getAuthString() {
        return mAuthString;
    }

    @Override
    public boolean isSendCapable() {
        return true;
    }

    @Override
    public void sendMessages(Message[] messages) throws MessagingException {
    	for (int i = 0; i < messages.length; i++) {
    		Message message = messages[i];
    		
    		try {
    			ByteArrayOutputStream out;

                out = new ByteArrayOutputStream(message.getSize());

                EOLConvertingOutputStream msgOut = new EOLConvertingOutputStream(
                    new BufferedOutputStream(out, 1024));
                message.writeTo(msgOut);
                msgOut.flush();

                StringEntity bodyEntity = new StringEntity(out.toString(), "UTF-8");
//                bodyEntity.setContentType("message/rfc822");


                // Create the appropriate command and POST it to the server
                String cmd = "SendMail&SaveInSent=T";
//                if (smartSend) {
//                    cmd = reply ? "SmartReply" : "SmartForward";
//                    cmd += "&ItemId=" + itemId + "&CollectionId=" + collectionId + "&SaveInSent=T";
//                }
                Log.d(K9.LOG_TAG, "Send cmd: " + cmd);

				HttpResponse resp = sendHttpClientPost(cmd, bodyEntity, SEND_MAIL_TIMEOUT);
				
	            int code = resp.getStatusLine().getStatusCode();
	            if (code == HttpStatus.SC_OK) {
	            	Log.d(K9.LOG_TAG, "Message sent successfully");
	            } else {
	            	Log.e(K9.LOG_TAG, "Message sending failed, code: " + code);
	            }
			} catch (IOException e) {
	            Log.e(K9.LOG_TAG, "Send failed: " + message.getUid());
			}
    	}
    }

    /*************************************************************************
     * Helper and Inner classes
     */

    /**
     * A EAS Folder
     */
    public class EasFolder extends Folder {
        private String mName;
        private String mServerId;
        private int mType;
        private boolean mIsOpen = false;
        private int mMessageCount = 0;
        private String mSyncKey;

        protected EasStore getStore() {
            return EasStore.this;
        }

        public EasFolder(String name, String serverId, int type) {
            super(EasStore.this.getAccount());
            this.mName = name;
            this.mServerId = serverId;
            this.mType = type;
        }
        
		public String getSyncKeyUnmodified() {
			return mSyncKey;
		}
		
		public String getSyncKey() {
	        if (mSyncKey == null) {
	            Log.d(K9.LOG_TAG, "Reset SyncKey to 0");
	            mSyncKey = "0";
	        }
	        return mSyncKey;
		}

		public void setSyncKey(String mSyncKey) {
			this.mSyncKey = mSyncKey;
		}
		
		@Override
        public boolean isSyncMode() {
        	return true;
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
        	String[] uids = new String[msgs.length];

            for (int i = 0, count = msgs.length; i < count; i++) {
                uids[i] = msgs[i].getUid();
            }
            
            deleteServerMessages(uids);
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

        @Override
        public int getMessageCount() throws MessagingException {
            open(OpenMode.READ_WRITE);
            return -1;
//            this.mMessageCount = getMessageCount(true);
//            return this.mMessageCount;
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
        	try {
        		EasEmailSyncParser syncParser = getMessagesInternal(null, null, null, start, end);
            
            	List<EasMessage> messages = syncParser.getMessages();
            	
            	return messages.toArray(EMPTY_MESSAGE_ARRAY);
        	} catch (IOException e) {
				throw new MessagingException("getMessages call failed", e);
			}
        }

		private EasEmailSyncParser getMessagesInternal(Message[] messages, FetchProfile fp, MessageRetrievalListener listener,
				int start, int end) throws IOException,
				MessagingException {

        	Serializer s = new Serializer();
        	EasEmailSyncParser syncParser = null;
//			EmailSyncAdapter target = new EmailSyncAdapter(this, mAccount);
			
			String className = "Email";
			String syncKey = getSyncKey();
//            	userLog("sync, sending ", className, " syncKey: ", syncKey);
			s.start(Tags.SYNC_SYNC)
			    .start(Tags.SYNC_COLLECTIONS)
			    .start(Tags.SYNC_COLLECTION)
			    .data(Tags.SYNC_CLASS, className)
			    .data(Tags.SYNC_SYNC_KEY, syncKey)
			    .data(Tags.SYNC_COLLECTION_ID, mServerId);

			// Start with the default timeout
			int timeout = COMMAND_TIMEOUT;
			
		    // EAS doesn't allow GetChanges in an initial sync; sending other options
		    // appears to cause the server to delay its response in some cases, and this delay
		    // can be long enough to result in an IOException and total failure to sync.
		    // Therefore, we don't send any options with the initial sync.
			if (!syncKey.equals("0")) {
	            boolean fetchBodySane = (fp != null) && fp.contains(FetchProfile.Item.BODY_SANE);
	            boolean fetchBody = (fp != null) && fp.contains(FetchProfile.Item.BODY);
	            
			    s.tag(Tags.SYNC_DELETES_AS_MOVES);
			    
			    if (messages == null) {
			    	s.tag(Tags.SYNC_GET_CHANGES);
//			    	s.data(Tags.SYNC_WINDOW_SIZE, Integer.toString(end - start + 1));
			    }
			    // Handle options
			    s.start(Tags.SYNC_OPTIONS);
			    if (messages == null) {
			    	// Set the lookback appropriately (EAS calls this a "filter") for all but Contacts
			    	s.data(Tags.SYNC_FILTER_TYPE, getEmailFilter());
			    }
			    // Enable MimeSupport
			    s.data(Tags.SYNC_MIME_SUPPORT, "2");
			    // Set the truncation amount for all classes
			    if (mProtocolVersionDouble >= Eas.SUPPORTED_PROTOCOL_EX2007_DOUBLE) {
			        s.start(Tags.BASE_BODY_PREFERENCE)
			        // HTML for email; plain text for everything else
			        .data(Tags.BASE_TYPE, Eas.BODY_PREFERENCE_MIME);
			        
			        if (!fetchBody) {
			        	String truncationSize = "0";
			        	if (fetchBodySane) {
			        		truncationSize = Eas.EAS12_TRUNCATION_SIZE;
			        	}
			        	s.data(Tags.BASE_TRUNCATION_SIZE, truncationSize);
			        }
			        
			        s.end();
			    } else {
			    	String syncTruncation = "0";
			        if (fetchBody) {
			        	syncTruncation = "8";
			        } else if (fetchBodySane) {
			        	syncTruncation = "7";
			        }
			        s.data(Tags.SYNC_MIME_TRUNCATION, syncTruncation);
			    }
			    s.end();
			} else {
			    // Use enormous timeout for initial sync, which empirically can take a while longer
			    timeout = 120 * 1000;
			}
			
		    if (messages != null) {
		    	s.start(Tags.SYNC_COMMANDS);
		    	for (Message msg : messages) {
		    		String serverId = msg.getUid();
		    		s.start(Tags.SYNC_FETCH);
		    			s.data(Tags.SYNC_SERVER_ID, serverId);
		    		s.end();
		    	}
		    	s.end();
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
		        	syncParser = new EasEmailSyncParser(is, this, mAccount);
			        
		        	boolean moreAvailable = syncParser.parse();
		        	
			        if (moreAvailable && syncKey.equals("0")) {
			        	return getMessagesInternal(messages, fp, listener, start, end);
			        }
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
				throw new MessagingException("not ok status");
			}
			return syncParser;
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

        @Override
        public void fetch(Message[] messages, FetchProfile fp, MessageRetrievalListener listener)
        throws MessagingException {
            if (messages == null ||
                    messages.length == 0) {
                return;
            }
            
            for (int i = 0, count = messages.length; i < count; i++) {
                if (!(messages[i] instanceof EasMessage)) {
                    throw new MessagingException("EasStore fetch called with non-EasMessage");
                }
            }
            
            boolean fetchBodySane = fp.contains(FetchProfile.Item.BODY_SANE);
            boolean fetchBody = fp.contains(FetchProfile.Item.BODY);
			if (fetchBodySane || fetchBody) {
	            try {
	            	EasEmailSyncParser syncParser = getMessagesInternal(messages, fp, listener, -1, -1);
					messages = syncParser.getMessages().toArray(EMPTY_MESSAGE_ARRAY);
	            } catch (IOException e) {
	            	throw new MessagingException("io exception while fetching messages", e);
	            }
            }
			
            for (int i = 0, count = messages.length; i < count; i++) {
            	EasMessage easMessage = (EasMessage) messages[i];
                
	            if (listener != null) {
	                listener.messageStarted(easMessage.getUid(), i, count);
	            }
	            
	            if (listener != null) {
	            	listener.messageFinished(easMessage, i, count);
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

        private void markServerMessagesRead(final String[] uids, final boolean read) throws MessagingException {
			new SyncCommand() {
				@Override
				void prepareCommand(Serializer s) throws IOException {
			    	s.start(Tags.SYNC_COMMANDS);
			    	for (String serverId : uids) {
			    		s.start(Tags.SYNC_CHANGE)
			    			.data(Tags.SYNC_SERVER_ID, serverId)
			    			.start(Tags.SYNC_APPLICATION_DATA)
			    				.data(Tags.EMAIL_READ, read ? "1" : "0")
			    			.end()
			    		.end();
			    	}
			    	s.end();
				}
			}.send(this);
        }

        private void deleteServerMessages(final String[] uids) throws MessagingException {
			new SyncCommand() {
				@Override
				void prepareCommand(Serializer s) throws IOException {
					s.tag(Tags.SYNC_DELETES_AS_MOVES);
					
			    	s.start(Tags.SYNC_COMMANDS);
			    	for (String serverId : uids) {
			    		s.start(Tags.SYNC_DELETE)
			    			.data(Tags.SYNC_SERVER_ID, serverId)
			    		.end();
			    	}
			    	s.end();
				}
			}.send(this);
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
    public static class EasMessage extends MimeMessage {
        public EasMessage(String uid, Folder folder) {
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

        @Override
        public void delete(String trashFolderName) throws MessagingException {
//            Log.i(K9.LOG_TAG, "Deleting message by moving to " + trashFolderName);
//            mFolder.moveMessages(new Message[] { this }, mFolder.getStore().getFolder(trashFolderName));
            Log.e(K9.LOG_TAG,
            	"Unimplemented method delete(String trashFolderName) legacy api, should not be in use");
        }

        @Override
        public void setFlag(Flag flag, boolean set) throws MessagingException {
            super.setFlag(flag, set);
            mFolder.setFlags(new Message[] { this }, new Flag[] { flag }, set);
        }
    }
    
    @Override
    public boolean isPushCapable() {
    	return true;
    }
    
    @Override
    public Pusher getPusher(PushReceiver receiver) {
    	return new EasPusher(this, receiver);
    }
    
    public class EasPusher implements Pusher {
    	
    	private static final int IDLE_READ_TIMEOUT_INCREMENT = 5 * 60 * 1000;
    	
        final EasStore mStore;
        final PushReceiver receiver;
        private long lastRefresh = -1;

        Thread listeningThread = null;
        final AtomicBoolean stop = new AtomicBoolean(false);
        final AtomicInteger delayTime = new AtomicInteger(NORMAL_DELAY_TIME);
        final AtomicInteger idleFailureCount = new AtomicInteger(0);
        TracingWakeLock wakeLock = null;

        public EasPusher(EasStore store, PushReceiver receiver) {
            mStore = store;
            this.receiver = receiver;
            
            TracingPowerManager pm = TracingPowerManager.getPowerManager(receiver.getContext());
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "EasPusher " + store.getAccount().getDescription());
            wakeLock.setReferenceCounted(false);
        }
        
        private String getLogId() {
            String id = getAccount().getDescription() + "/" + Thread.currentThread().getName();
            return id;
        }

        public void start(final List<String> folderNames) {
            stop();
            
            Runnable runner = new Runnable() {
                public void run() {
                    wakeLock.acquire(K9.PUSH_WAKE_LOCK_TIMEOUT);
                    if (K9.DEBUG)
                        Log.i(K9.LOG_TAG, "Pusher starting for " + getLogId());

                    while (!stop.get()) {
                        try {
                        	Serializer s = new Serializer();
                        	
                        	int responseTimeout = getAccount().getIdleRefreshMinutes() * 60 + (IDLE_READ_TIMEOUT_INCREMENT / 1000);
							s.start(Tags.PING_PING)
                        		.data(Tags.PING_HEARTBEAT_INTERVAL, String.valueOf(responseTimeout))
                        		.start(Tags.PING_FOLDERS);
							
							for (String folderName : folderNames) {
									s.start(Tags.PING_FOLDER)
										.data(Tags.PING_ID, mFolderList.get(folderName).mServerId)
										.data(Tags.PING_CLASS, "Email")
									.end();
							}
							
								s.end()
							.end()
							.done();
                        	
                        	int timeout = responseTimeout * 1000 + IDLE_READ_TIMEOUT_INCREMENT;
							HttpResponse resp = sendHttpClientPost(PING_COMMAND, new ByteArrayEntity(s.toByteArray()),
                			        timeout);
							int code = resp.getStatusLine().getStatusCode();
							if (code == HttpStatus.SC_OK) {
							    InputStream is = resp.getEntity().getContent();
							    if (is != null) {
									PingParser pingParser = new PingParser(is);
							    	if (!pingParser.parse()) {
								    	for (String folderServerId : pingParser.getFolderList()) {
								    		for (EasFolder folder : mFolderList.values()) {
								    			if (folderServerId.equals(folder.mServerId)) {
								    				receiver.syncFolder(folder);
								    				break;
								    			}
								    		}
								    	}
							    	} else {
							    		throw new MessagingException("Parsing of Ping response failed");
							    	}
							    } else {
							    	Log.d(K9.LOG_TAG, "Empty input stream in sync command response");
							    }
							} else {
								throw new MessagingException("not ok status");
							}

                        } catch (Exception e) {
                            wakeLock.acquire(K9.PUSH_WAKE_LOCK_TIMEOUT);
//                            receiver.setPushActive(getName(), false);

                            if (stop.get()) {
                                Log.i(K9.LOG_TAG, "Got exception while idling, but stop is set for " + getLogId());
                            } else {
                                receiver.pushError("Push error for " + getLogId(), e);
                                Log.e(K9.LOG_TAG, "Got exception while idling for " + getLogId(), e);
                                int delayTimeInt = delayTime.get();
                                receiver.sleep(wakeLock, delayTimeInt);
                                delayTimeInt *= 2;
                                if (delayTimeInt > MAX_DELAY_TIME) {
                                    delayTimeInt = MAX_DELAY_TIME;
                                }
                                delayTime.set(delayTimeInt);
                                if (idleFailureCount.incrementAndGet() > IDLE_FAILURE_COUNT_LIMIT) {
                                    Log.e(K9.LOG_TAG, "Disabling pusher for " + getLogId() + " after " + idleFailureCount.get() + " consecutive errors");
                                    receiver.pushError("Push disabled for " + getLogId() + " after " + idleFailureCount.get() + " consecutive errors", e);
                                    stop.set(true);
                                }

                            }
                        }
                    }
                    
                    for (String folderName : folderNames) {
                    	receiver.setPushActive(folderName, false);
                    }
                    
                    try {
                        if (K9.DEBUG)
                            Log.i(K9.LOG_TAG, "Pusher for " + getLogId() + " is exiting");
                    } catch (Exception me) {
                        Log.e(K9.LOG_TAG, "Got exception while closing for " + getLogId(), me);
                    } finally {
                        wakeLock.release();
                    }
                }
            };
            listeningThread = new Thread(runner);
            listeningThread.start();
        }

        public void refresh() {
        }

        public void stop() {
            if (K9.DEBUG)
                Log.i(K9.LOG_TAG, "Requested stop of EAS pusher");
        }

        public int getRefreshInterval() {
            return (getAccount().getIdleRefreshMinutes() * 60 * 1000);
        }

        public long getLastRefresh() {
            return lastRefresh;
        }

        public void setLastRefresh(long lastRefresh) {
            this.lastRefresh = lastRefresh;
        }

    }
    
    private abstract class SyncCommand {
    	
    	public void send(EasFolder folder) throws MessagingException {
    		try {
    			int timeout = COMMAND_TIMEOUT;
    			
				byte[] byteArr = prepare(folder);
				
				HttpResponse resp = sendHttpClientPost("Sync", new ByteArrayEntity(byteArr),
				        timeout);
				int code = resp.getStatusLine().getStatusCode();
				if (code == HttpStatus.SC_OK) {
				    InputStream is = resp.getEntity().getContent();
				    if (is != null) {
						EasEmailSyncParser syncParser = new EasEmailSyncParser(is, folder, folder.getAccount());
				    	parseResponse(syncParser, is);
				    } else {
				    	Log.d(K9.LOG_TAG, "Empty input stream in sync command response");
				    }
				} else {
					throw new MessagingException("not ok status");
				}
			} catch (IOException e) {
				throw new MessagingException("could not send command");
			}
    	}
    	
		byte[] prepare(EasFolder folder) throws IOException {
        	Serializer s = new Serializer();
        	
			String className = "Email";
			String syncKey = folder.getSyncKey();
			String folderServerId = folder.mServerId;
			s.start(Tags.SYNC_SYNC)
			    .start(Tags.SYNC_COLLECTIONS)
			    .start(Tags.SYNC_COLLECTION)
			    .data(Tags.SYNC_CLASS, className)
			    .data(Tags.SYNC_SYNC_KEY, syncKey)
			    .data(Tags.SYNC_COLLECTION_ID, folderServerId );

			prepareCommand(s);

			s.end().end().end().done();
			
			return s.toByteArray();
    	}

		abstract void prepareCommand(Serializer s) throws IOException;
    	
    	void parseResponse(EasEmailSyncParser syncParser, InputStream is) throws IOException, MessagingException {
    		syncParser.parse();
    	}

    }

}
