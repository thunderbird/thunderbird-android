package com.fsck.k9.mail.helper;


import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;


public final class UrlEncodingHelper {
    private UrlEncodingHelper() {
    }
    public static Map<String, String> splitQuery(String query) {
        Map<String, String> queryParams = new HashMap<>();
        if (query == null) {
            return queryParams;
        }
        String[] params = query.split("&");
        for(String param: params) {
            String[] parts = param.split("=");
            queryParams.put(parts[0], parts[1]);
        }
        return queryParams;
    }

    public static String buildQuery(Map<String, String> params) {
        StringBuffer query = new StringBuffer("");
        for (String param: params.keySet()) {
            query.append(param);
            query.append('=');
            query.append(params.get(param));
            query.append('&');
        }
        query.deleteCharAt(query.length()-1);
        return query.toString();
    }



    public static String decodeUtf8(String s) {
        try {
            return URLDecoder.decode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("UTF-8 not found");
        }
    }

    public static String encodeUtf8(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("UTF-8 not found");
        }
    }
}
