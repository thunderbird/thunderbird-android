package com.android.email.mail.internet;

import java.net.URI;

import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;

/**
 * New HTTP method that allows changing the method and generic handling.
 * This allows for the actual knowledge of HTTP methods to be abstracted
 * out as well as custom methods.
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
     * @throws IllegalArgumentException if the uri is invalid. 
     */
    public HttpGeneric(final String uri) {
        super();
        
        setURI(URI.create(uri));
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
