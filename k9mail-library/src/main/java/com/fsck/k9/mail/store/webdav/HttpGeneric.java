package com.fsck.k9.mail.store.webdav;

import android.util.Log;

import com.fsck.k9.mail.K9MailLib;

import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;

import java.net.URI;

import static com.fsck.k9.mail.K9MailLib.DEBUG_PROTOCOL_WEBDAV;
import static com.fsck.k9.mail.K9MailLib.LOG_TAG;
import static com.fsck.k9.mail.helper.UrlEncodingHelper.decodeUtf8;
import static com.fsck.k9.mail.helper.UrlEncodingHelper.encodeUtf8;

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

        if (K9MailLib.isDebug()) {
            Log.v(LOG_TAG, "Starting uri = '" + uri + "'");
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
                end = decodeUtf8(end);
                end = encodeUtf8(end);
                end = end.replaceAll("\\+", "%20");
            }
        } catch (IllegalArgumentException iae) {
            Log.e(LOG_TAG, "IllegalArgumentException caught in HttpGeneric(String uri): " + iae + "\nTrace: "
                    + WebDavUtils.processException(iae));
        }

        for (int i = 0; i < length - 1; i++) {
            if (i != 0) {
                url = url + "/" + urlParts[i];
            } else {
                url = urlParts[i];
            }
        }
        if (K9MailLib.isDebug() && DEBUG_PROTOCOL_WEBDAV) {
            Log.v(LOG_TAG, "url = '" + url + "' length = " + url.length()
                    + ", end = '" + end + "' length = " + end.length());
        }
        url = url + "/" + end;

        Log.i(LOG_TAG, "url = " + url);
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