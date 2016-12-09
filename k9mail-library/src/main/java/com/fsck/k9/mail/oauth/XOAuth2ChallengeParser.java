package com.fsck.k9.mail.oauth;

import android.util.Log;

import com.fsck.k9.mail.filter.Base64;

import org.json.JSONException;
import org.json.JSONObject;

import static com.fsck.k9.mail.K9MailLib.LOG_TAG;

/**
 * Parses Google's Error/Challenge responses
 * See: https://developers.google.com/gmail/xoauth2_protocol#error_response
 */
public class XOAuth2ChallengeParser {
    public static final String BAD_RESPONSE = "400";

    public static boolean shouldRetry(String response, String host) {
        String decodedResponse = Base64.decode(response);

        Log.v(LOG_TAG, "Challenge response: "+ decodedResponse);

        try {
            JSONObject json = new JSONObject(decodedResponse);
            if(!json.getString("status").equals(BAD_RESPONSE)) {
                return false;
            }
        } catch (JSONException jsonException) {
            Log.i(LOG_TAG, "Error decoding JSON response from:"
                    + host + ". Response was:" + decodedResponse);
        }
        return true;
    }
}
