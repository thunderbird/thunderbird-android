package com.fsck.k9.autocrypt;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.fsck.k9.mail.Part;
import com.fsck.k9.mail.internet.MimeUtility;
import okio.ByteString;
import timber.log.Timber;


class AutocryptGossipHeaderParser {
    private static final AutocryptGossipHeaderParser INSTANCE = new AutocryptGossipHeaderParser();


    public static AutocryptGossipHeaderParser getInstance() {
        return INSTANCE;
    }

    private AutocryptGossipHeaderParser() { }


    List<AutocryptGossipHeader> getAllAutocryptGossipHeaders(Part part) {
        String[] headers = part.getHeader(AutocryptGossipHeader.AUTOCRYPT_GOSSIP_HEADER);
        List<AutocryptGossipHeader> autocryptHeaders = parseAllAutocryptGossipHeaders(headers);

        return Collections.unmodifiableList(autocryptHeaders);
    }

    @Nullable
    @VisibleForTesting
    AutocryptGossipHeader parseAutocryptGossipHeader(String headerValue) {
        Map<String,String> parameters = MimeUtility.getAllHeaderParameters(headerValue);

        String type = parameters.remove(AutocryptHeader.AUTOCRYPT_PARAM_TYPE);
        if (type != null && !type.equals(AutocryptHeader.AUTOCRYPT_TYPE_1)) {
            Timber.e("autocrypt: unsupported type parameter %s", type);
            return null;
        }

        String base64KeyData = parameters.remove(AutocryptHeader.AUTOCRYPT_PARAM_KEY_DATA);
        if (base64KeyData == null) {
            Timber.e("autocrypt: missing key parameter");
            return null;
        }

        ByteString byteString = ByteString.decodeBase64(base64KeyData);
        if (byteString == null) {
            Timber.e("autocrypt: error parsing base64 data");
            return null;
        }

        String addr = parameters.remove(AutocryptHeader.AUTOCRYPT_PARAM_ADDR);
        if (addr == null) {
            Timber.e("autocrypt: no to header!");
            return null;
        }

        if (hasCriticalParameters(parameters)) {
            return null;
        }

        return new AutocryptGossipHeader(addr, byteString.toByteArray());
    }

    private boolean hasCriticalParameters(Map<String, String> parameters) {
        for (String parameterName : parameters.keySet()) {
            if (parameterName != null && !parameterName.startsWith("_")) {
                return true;
            }
        }
        return false;
    }

    @NonNull
    private List<AutocryptGossipHeader> parseAllAutocryptGossipHeaders(String[] headers) {
        ArrayList<AutocryptGossipHeader> autocryptHeaders = new ArrayList<>();
        for (String header : headers) {
            AutocryptGossipHeader autocryptHeader = parseAutocryptGossipHeader(header);
            if (autocryptHeader == null) {
                Timber.e("Encountered malformed autocrypt-gossip header - skipping!");
                continue;
            }
            autocryptHeaders.add(autocryptHeader);
        }
        return autocryptHeaders;
    }
}
