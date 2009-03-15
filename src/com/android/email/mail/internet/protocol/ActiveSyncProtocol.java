package com.android.email.mail.internet.protocol;

import android.util.Log;

import com.android.email.Email;
import com.android.email.Utility;
import com.android.email.mail.ProtocolException;
import com.android.email.mail.internet.HttpGeneric;
import com.android.email.mail.internet.protocol.Protocol;
import com.android.email.mail.transport.TrustedSocketFactory;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

/**
 * A class for handling all the protocol level communication and
 * data for the Microsoft Exchange ActiveSync protocol.
 *
 * @version .1
 * @author Matthew Brace
 */
public class ActiveSyncProtocol extends Protocol {
    private URI mUri;            /* Stores the URI with connection information */
    private String mUsername;    /* Stores the user supplied username for authentication */
    private String mAlias;       /* Stores just the username if the user included realm/username */
    private String mPassword;    /* Stores the user supplied password for authentication */
    private String mHost;        /* Stores the user supplied server for connection */
    private String mAuthString;  /* Stores the base64 encoded string used in headers for authentication */

    private DefaultHttpClient mHttpClient = null; /* Stores the HttpClient to be used for requests */

    private static enum supportedCommands { /* Stores the supported commands */
        checkSettings;
    };
    
    /**
     * Public constructor.  Initializes default data and settings based on the supplied URI
     */
    public ActiveSyncProtocol(String uri) throws ProtocolException {
        /** Generate the information from the URI */
        try {
            mUri = new URI(uri);
        } catch (URISyntaxException use) {
            throw new ProtocolException("Invalid URI supplied to ActiveSyncProtocol", use);
        }

        /**
         * All requests will occur over https.  According to the spec, this is
         * the only way it can occur.
         */
        mHost = mUri.getHost();
        if (mHost.startsWith("http")) {
            String[] hostParts = mHost.split("://", 2);
            if (hostParts.length > 1) {
                mHost = hostParts[1];
            }
        }
        mHost = "https://" + mHost;

        if (mUri.getUserInfo() != null) {
            String[] userInfoParts = mUri.getUserInfo().split(":", 2);
            mUsername = userInfoParts[0];
            String userParts[] = mUsername.split("/", 2);

            if (userParts.length > 1) {
                mAlias = userParts[1];
            } else {
                mAlias = mUsername;
            }
            if (userInfoParts.length > 1) {
                mPassword = userInfoParts[1];
            }
        }

        /**
         * DefaultHttpClient managed to fail basic authentication with a result of "Bad Request (Invalid Verb)"
         * every single time in testing.  Manually generating the authentication header succeeded.
         * ActiveSync uses Basic Authentication over SSL, so generate our header based on the supplied
         * information here
         */
        mAuthString = "Basic " + Utility.base64Encode(mUsername + ":" + mPassword);
    }

    /**
     * Determines if the supplied string is a command that the protocol supports.  Command is
     * only a loose-fitting term as it is used to directly corellate to functions.
     */
    public boolean isCommandSupported(String command) {
        boolean result = false;
        try {
            supportedCommands.valueOf(command);
            result = true;
        } catch (IllegalArgumentException iae) {
            result = false;
        }
        
        return result;
    }

    /**
     * Verifies that all the supplied settings result in a successful authentication.
     * Realistically the entire object should be rebuilt if this fails, but could
     * fail due to network problems, etc.
     */
    public boolean checkSettings() {
        boolean result = false;
        try {
            HashMap<String, String> headers = new HashMap<String, String>();
            /* No explicit headers needed */
            processRequest(mHost + "/Microsoft-Server-ActiveSync",
                           "OPTIONS",
                           null,
                           null,
                           headers,
                           false);
            result = true;
        } catch (ProtocolException pe) {
            result = false;
        }
        
        return result;
    }

    /**
     * Retrieves an HttpClient that is ready for requests.
     */
    public DefaultHttpClient getHttpClient() throws ProtocolException {
        SchemeRegistry reg;
        Scheme s;
        
        if (mHttpClient == null) {
            mHttpClient = new DefaultHttpClient();
        }

        /** Assign everything for supporting self-signed certificates */
        reg = mHttpClient.getConnectionManager().getSchemeRegistry();
        try {
            String host = mHost.replaceAll("https://", "");
            s = new Scheme("https", new TrustedSocketFactory(host, true), 443);
        } catch (NoSuchAlgorithmException nsa) {
            Log.e(Email.LOG_TAG, "NoSuchAlgorithmException in getHttpClient: " + nsa);
            throw new ProtocolException("NoSuchAlgorithmException in getHttpClient: " + nsa);
        } catch (KeyManagementException kme) {
            Log.e(Email.LOG_TAG, "KeyManagementException in getHttpClient: " + kme);
            throw new ProtocolException("KeyManagementException in getHttpClient: " + kme);
        }
        reg.register(s);

        /**
         * Return the mHttpClient.  It's the responsibility of the user of the client to
         * assign the authentication headers to the entity request object.
         */
        return mHttpClient;
    }

    /**
     * Processes the supplied request.  Throws ProtocolException if any error condition
     * is reached.  If needsParsing is false, DataSet is guaranteed to be empty.  However,
     * if needsParsing is true, DataSet is not guaranteed to be non-empty.
     */
    private DataSet processRequest(String url,
                                   String method,
                                   String messageBody,
                                   String contentType,
                                   HashMap<String, String> headers,
                                   boolean needsParsing) throws ProtocolException {
        DataSet result = new DataSet();
        DefaultHttpClient httpclient = getHttpClient();

        if (url == null) {
            throw new ProtocolException("Invalid URL supplied to processRequest");
        }
        if (method == null) {
            throw new ProtocolException("Invalid method supplied to processRequest");
        }

        try {
            int statusCode = -1;
            StringEntity messageEntity = null;
            HttpGeneric httpmethod = new HttpGeneric(url);
            HttpResponse response;
            HttpEntity entity;

            if (messageBody != null) {
                messageEntity = new StringEntity(messageBody);
                messageEntity.setContentType(contentType);
                httpmethod.setEntity(messageEntity);
            }

            for (String headerName : headers.keySet()) {
                httpmethod.setHeader(headerName, headers.get(headerName));
            }

            /** The caller doesn't need to explicitly know anything about authentication. */
            httpmethod.setHeader("Authorization", mAuthString);

            httpmethod.setMethod(method);
            response = httpclient.execute(httpmethod);
            statusCode = response.getStatusLine().getStatusCode();

            entity = response.getEntity();

            /** 401 gets special handling to show the authentication failure */
            if (statusCode == 401) {
                throw new IOException("Authentication failed processing request");
            } else if (statusCode < 200 ||
                       statusCode > 300) {
                throw new IOException("Error during request processing: "+
                                      response.getStatusLine().toString()+ "\n\n"+
                                      getHttpRequestResponse(messageEntity, entity));
            }

            if (entity != null &&
                needsParsing) {
                /* Unsupported at this time */
            }
        } catch (UnsupportedEncodingException uee) {
            Log.e(Email.LOG_TAG,
                  "UnsupportedEncodingException in processRequest: " + uee + "\nTrace: " + processException(uee));
            throw new ProtocolException("UnsupportedEncodingException in processRequest");
        } catch (IOException ioe) {
            Log.e(Email.LOG_TAG, "IOException in processRequest: " + ioe + "\nTrace: " + processException(ioe));
            throw new ProtocolException("IOException in processRequest");
        }

        return result;
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

    /**
     * Retrieves details about an HTTP Request response.  Used to provide more detailed log information.
     */
    public static String getHttpRequestResponse(HttpEntity request, HttpEntity response) throws IllegalStateException, IOException{
        String responseText = "";
        String requestText = "";
        if (response != null) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(response.getContent()), 8192);
            String tempText = "";

            while ((tempText = reader.readLine()) != null) {
                responseText += tempText;
            }
        }
        if (request != null) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(request.getContent()), 8192);
            String tempText = "";

            while ((tempText = reader.readLine()) != null) {
                requestText += tempText;
            }
            requestText = requestText.replaceAll("Basic .*", "Basic (omitted)&");
        }
        return "Request: " + requestText +
            "\n\nResponse: " + responseText;

    }
    
    /**
     * Dataset for all XML parses.
     * Data is stored in a single format inside the class and is formatted appropriately
     * depending on the accessor calls made.
     */
    public class DataSet {
        public DataSet() {

        }
    }
}
