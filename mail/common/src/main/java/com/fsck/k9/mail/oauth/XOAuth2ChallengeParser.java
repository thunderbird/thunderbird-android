package com.fsck.k9.mail.oauth;


import java.io.IOException;

import com.fsck.k9.logging.Timber;
import com.fsck.k9.mail.K9MailLib;
import com.fsck.k9.mail.filter.Base64;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonDataException;
import com.squareup.moshi.Moshi;


/**
 * Parses Google's Error/Challenge responses
 * See: https://developers.google.com/gmail/xoauth2_protocol#error_response
 */
public class XOAuth2ChallengeParser {
    public static final String BAD_RESPONSE = "400";


    public static boolean shouldRetry(String response, String host) {
        String decodedResponse = Base64.decode(response);

        if (K9MailLib.isDebug()) {
            Timber.v("Challenge response: %s", decodedResponse);
        }

        try {
            Moshi moshi = new Moshi.Builder().build();
            JsonAdapter<XOAuth2Response> adapter = moshi.adapter(XOAuth2Response.class);
            XOAuth2Response responseObject = adapter.fromJson(decodedResponse);
            if (responseObject != null && responseObject.status != null &&
                    !BAD_RESPONSE.equals(responseObject.status)) {
                return false;
            }
        } catch (IOException | JsonDataException e) {
            Timber.e(e, "Error decoding JSON response from: %s. Response was: %s", host, decodedResponse);
        }

        return true;
    }
}
