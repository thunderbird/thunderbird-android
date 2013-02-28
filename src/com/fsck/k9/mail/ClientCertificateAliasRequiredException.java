package com.fsck.k9.mail;

import java.security.Principal;

/**
 * this exception is thrown when, during an ssl handshake, a client certificate
 * alias is requested but we want the user to pick one instead of using the 
 * previously selected one silently.  
 * 
 * this must be a RuntimeException because the implemented interface of X509ExtendedKeyManager
 * (where it is thrown) does not allow anything else
 * @author David Mansfield
 *
 */
public class ClientCertificateAliasRequiredException extends RuntimeException {
    public static final long serialVersionUID = -1;

    String[] mKeyTypes;
	Principal[] mIssuers;
	String mHostName;
	int mPort;
	
	public ClientCertificateAliasRequiredException(String[] keyTypes,
			Principal[] issuers,
			String hostName,
			int port) {
		super("interactive client certificate alias choice required");
		this.mKeyTypes = keyTypes;
		this.mIssuers = issuers;
		this.mHostName = hostName;
		this.mPort = port;
	}

	public String[] getKeyTypes() {
		return mKeyTypes;
	}

	public Principal[] getIssuers() {
		return mIssuers;
	}

	public String getHostName() {
		return mHostName;
	}

	public int getPort() {
		return mPort;
	}
	
	
}