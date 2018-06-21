package com.fsck.k9.helper;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

/**
 * Wraps the java.net.URLDecoder to avoid unhelpful checked exceptions.
 */
public class UrlEncodingHelper {

    public static String decodeUtf8(String s) {
        try {
            return URLDecoder.decode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            /*
             * This is impossible, UTF-8 is always supported
             */
            throw new RuntimeException("UTF-8 not found");
        }
    }

    public static String encodeUtf8(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            /*
             * This is impossible, UTF-8 is always supported
             */
            throw new RuntimeException("UTF-8 not found");
        }
    }
}
