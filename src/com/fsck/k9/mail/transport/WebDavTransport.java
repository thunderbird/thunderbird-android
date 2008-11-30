
package com.fsck.k9.mail.transport;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.SSLException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import android.util.Config;
import android.util.Log;

import com.fsck.k9.k9;
import com.fsck.k9.PeekableInputStream;
import com.fsck.k9.codec.binary.Base64;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.AuthenticationFailedException;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Transport;
import com.fsck.k9.mail.CertificateValidationException;
import com.fsck.k9.mail.Message.RecipientType;
import com.fsck.k9.mail.store.TrustManagerFactory;
import com.fsck.k9.mail.store.WebDavStore;
import com.fsck.k9.mail.store.WebDavStore.HttpGeneric;
import com.fsck.k9.mail.store.WebDavStore.ParsedDataSet;
import com.fsck.k9.mail.store.WebDavStore.WebDavHandler;

public class WebDavTransport extends Transport {
    public static final int CONNECTION_SECURITY_NONE = 0;
    public static final int CONNECTION_SECURITY_TLS_OPTIONAL = 1;
    public static final int CONNECTION_SECURITY_TLS_REQUIRED = 2;
    public static final int CONNECTION_SECURITY_SSL_REQUIRED = 3;
    public static final int CONNECTION_SECURITY_SSL_OPTIONAL = 4;

    String host;
    int mPort;
    private int mConnectionSecurity;
    private String mUsername; /* Stores the username for authentications */
    private String mPassword; /* Stores the password for authentications */
    private String mUrl;      /* Stores the base URL for the server */

    boolean mSecure;
    Socket mSocket;
    PeekableInputStream mIn;
    OutputStream mOut;
	private WebDavStore store;

    /**
     * webdav://user:password@server:port CONNECTION_SECURITY_NONE
     * webdav+tls://user:password@server:port CONNECTION_SECURITY_TLS_OPTIONAL
     * webdav+tls+://user:password@server:port CONNECTION_SECURITY_TLS_REQUIRED
     * webdav+ssl+://user:password@server:port CONNECTION_SECURITY_SSL_REQUIRED
     * webdav+ssl://user:password@server:port CONNECTION_SECURITY_SSL_OPTIONAL
     *
     * @param _uri
     */
    public WebDavTransport(String _uri) throws MessagingException {
    	store = new WebDavStore(_uri);
        Log.d(k9.LOG_TAG, ">>> New WebDavTransport creation complete");
    }

    public void open() throws MessagingException {
        Log.d(k9.LOG_TAG, ">>> open called on WebDavTransport ");
        if (store.needAuth()) {
            store.authenticate();
        }

        if (store.getAuthCookies() == null) {
            return;
        }
    }

//    public void sendMessage(Message message) throws MessagingException {
//        Address[] from = message.getFrom();
//        
//    }
    
    public void close() {
    }
    
    public String generateTempURI(String subject) {
    	String encodedSubject = URLEncoder.encode(subject);
    	return store.getUrl() + "/Exchange/" + store.getAlias() + "/drafts/" + encodedSubject + ".eml";
    }
    public String generateSendURI() {
    	return store.getUrl() + "/Exchange/" + store.getAlias() + "/##DavMailSubmissionURI##/";
    }
    
    public void sendMessage(Message message) throws MessagingException {
        Log.d(k9.LOG_TAG, ">>> sendMessage called.");

        DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpGeneric httpmethod;
        HttpResponse response;
        HttpEntity responseEntity;
        StringEntity bodyEntity;
        int statusCode;
        String subject;
        ByteArrayOutputStream out;
        try {
        	try {
        		subject = message.getSubject();
        	} catch (MessagingException e) {
        		Log.e(k9.LOG_TAG, "MessagingException while retrieving Subject: " + e);
        		subject = "";
        	}
        	try {
        		out = new ByteArrayOutputStream(message.getSize());
        	} catch (MessagingException e) {
        		Log.e(k9.LOG_TAG, "MessagingException while getting size of message: " + e);
        		out = new ByteArrayOutputStream();
        	}
        	open();
        	message.writeTo(
        			new EOLConvertingOutputStream(
        					new BufferedOutputStream(out, 1024)));
        	httpclient.setCookieStore(store.getAuthCookies());

        	bodyEntity = new StringEntity(out.toString(), "UTF-8");
        	bodyEntity.setContentType("message/rfc822");

        	httpmethod = store.new HttpGeneric(generateTempURI(subject));
        	httpmethod.setMethod("PUT");
        	httpmethod.setEntity(bodyEntity);

        	response = httpclient.execute(httpmethod);
        	statusCode = response.getStatusLine().getStatusCode();

        	if (statusCode < 200 ||
        			statusCode > 300) {
        		throw new IOException("Error sending message, status code was " + statusCode);
        	}
        	
        	//responseEntity = response.getEntity();
            //DefaultHttpClient movehttpclient = new DefaultHttpClient();
            //HttpGeneric movehttpmethod;
            //HttpResponse moveresponse;
            //HttpEntity moveresponseEntity;
            httpmethod = store.new HttpGeneric(generateTempURI(subject));
        	httpmethod.setMethod("MOVE");
        	httpmethod.setHeader("Destination", generateSendURI());

        	response = httpclient.execute(httpmethod);
        	statusCode = response.getStatusLine().getStatusCode();

        	if (statusCode < 200 ||
        			statusCode > 300) {
        		throw new IOException("Error sending message, status code was " + statusCode);
        	}

        } catch (UnsupportedEncodingException uee) {
        	Log.e(k9.LOG_TAG, "UnsupportedEncodingException in getMessageCount() " + uee);
        } catch (IOException ioe) {
        	Log.e(k9.LOG_TAG, "IOException in getMessageCount() " + ioe);
        	throw new MessagingException("Unable to send message", ioe);
        }
        Log.d(k9.LOG_TAG, ">>> getMessageCount finished");
    }

}
