package com.android.email.mail.transport;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.scheme.SocketFactory;
import org.apache.http.params.HttpParams;

import com.android.email.mail.store.TrustManagerFactory;

public class TrustedSocketFactory implements SocketFactory {
	private SSLSocketFactory mSocketFactory;
	private org.apache.http.conn.ssl.SSLSocketFactory mSchemeSocketFactory;
	
	public TrustedSocketFactory(String host, boolean secure) throws NoSuchAlgorithmException, KeyManagementException{
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, new TrustManager[] {
                TrustManagerFactory.get(host, secure)
        }, new SecureRandom());
        mSocketFactory = sslContext.getSocketFactory();
        mSchemeSocketFactory = org.apache.http.conn.ssl.SSLSocketFactory.getSocketFactory();
        mSchemeSocketFactory.setHostnameVerifier(
        		org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
	}

	public Socket connectSocket(Socket sock, String host, int port,
			InetAddress localAddress, int localPort, HttpParams params)
			throws IOException, UnknownHostException, ConnectTimeoutException {
		return mSchemeSocketFactory.connectSocket(sock, host, port, localAddress, localPort, params);
	}

	public Socket createSocket() throws IOException {
		return mSocketFactory.createSocket();
	}

	public boolean isSecure(Socket sock) throws IllegalArgumentException {
		return mSchemeSocketFactory.isSecure(sock);
	}
	
}
