package com.fsck.k9.mail;

import java.security.Principal;

/**
 * This exception is thrown when, during an SSL handshake, a client certificate 
 * is requested and user didn't provide one
 * 
 * @author Konrad Gadzinowski
 *
 */
public class ClientCertificateRequiredException extends RuntimeException {
    public static final long serialVersionUID = -1;

 
	
	public ClientCertificateRequiredException(Exception e) {
		super("Client certificate wasn't set - it's required to authenticate", e);
		
	}

	
	
}