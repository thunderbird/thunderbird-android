package com.fsck.k9.message;


import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import android.net.Uri;
import timber.log.Timber;

import com.fsck.k9.K9;
import com.fsck.k9.mail.filter.Base64;


public class IdentityHeaderParser {
    /**
     * Parse an identity string.  Handles both legacy and new (!) style identities.
     *
     * @param identityString
     *         The encoded identity string that was saved in a drafts header.
     *
     * @return A map containing the value for each {@link IdentityField} in the identity string.
     */
    public static Map<IdentityField, String> parse(final String identityString) {
        Map<IdentityField, String> identity = new HashMap<IdentityField, String>();

        Timber.d("Decoding identity: %s", identityString);

        if (identityString == null || identityString.length() < 1) {
            return identity;
        }

        // Check to see if this is a "next gen" identity.
        if (identityString.charAt(0) == IdentityField.IDENTITY_VERSION_1.charAt(0) && identityString.length() > 2) {
            Uri.Builder builder = new Uri.Builder();
            builder.encodedQuery(identityString.substring(1));  // Need to cut off the ! at the beginning.
            Uri uri = builder.build();
            for (IdentityField key : IdentityField.values()) {
                String value = uri.getQueryParameter(key.value());
                if (value != null) {
                    identity.put(key, value);
                }
            }

            Timber.d("Decoded identity: %s", identity);

            // Sanity check our Integers so that recipients of this result don't have to.
            for (IdentityField key : IdentityField.getIntegerFields()) {
                if (identity.get(key) != null) {
                    try {
                        Integer.parseInt(identity.get(key));
                    } catch (NumberFormatException e) {
                        Timber.e("Invalid %s field in identity: %s", key.name(), identity.get(key));
                    }
                }
            }
        } else {
            // Legacy identity

            Timber.d("Got a saved legacy identity: %s", identityString);

            StringTokenizer tokenizer = new StringTokenizer(identityString, ":", false);

            // First item is the body length. We use this to separate the composed reply from the quoted text.
            if (tokenizer.hasMoreTokens()) {
                String bodyLengthS = Base64.decode(tokenizer.nextToken());
                try {
                    identity.put(IdentityField.LENGTH, Integer.valueOf(bodyLengthS).toString());
                } catch (Exception e) {
                    Timber.e("Unable to parse bodyLength '%s'", bodyLengthS);
                }
            }
            if (tokenizer.hasMoreTokens()) {
                identity.put(IdentityField.SIGNATURE, Base64.decode(tokenizer.nextToken()));
            }
            if (tokenizer.hasMoreTokens()) {
                identity.put(IdentityField.NAME, Base64.decode(tokenizer.nextToken()));
            }
            if (tokenizer.hasMoreTokens()) {
                identity.put(IdentityField.EMAIL, Base64.decode(tokenizer.nextToken()));
            }
            if (tokenizer.hasMoreTokens()) {
                identity.put(IdentityField.QUOTED_TEXT_MODE, Base64.decode(tokenizer.nextToken()));
            }
        }

        return identity;
    }
}
