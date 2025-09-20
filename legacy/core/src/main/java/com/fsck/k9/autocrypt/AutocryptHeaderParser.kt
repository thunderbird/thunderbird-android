package com.fsck.k9.autocrypt;


import java.util.ArrayList;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.internet.MimeUtility;
import okio.ByteString;
import net.thunderbird.core.logging.legacy.Log;


class AutocryptHeaderParser {
    private static final AutocryptHeaderParser INSTANCE = new AutocryptHeaderParser();


    public static AutocryptHeaderParser getInstance() {
        return INSTANCE;
    }

    private AutocryptHeaderParser() { }


    @Nullable
    AutocryptHeader getValidAutocryptHeader(Message currentMessage) {
        String[] headers = currentMessage.getHeader(AutocryptHeader.AUTOCRYPT_HEADER);
        ArrayList<AutocryptHeader> autocryptHeaders = parseAllAutocryptHeaders(headers);

        boolean isSingleValidHeader = autocryptHeaders.size() == 1;
        return isSingleValidHeader ? autocryptHeaders.get(0) : null;
    }

    @Nullable
    @VisibleForTesting
    AutocryptHeader parseAutocryptHeader(String headerValue) {
        Map<String,String> parameters = MimeUtility.getAllHeaderParameters(headerValue);

        String type = parameters.remove(AutocryptHeader.AUTOCRYPT_PARAM_TYPE);
        if (type != null && !type.equals(AutocryptHeader.AUTOCRYPT_TYPE_1)) {
            Log.e("autocrypt: unsupported type parameter %s", type);
            return null;
        }

        String base64KeyData = parameters.remove(AutocryptHeader.AUTOCRYPT_PARAM_KEY_DATA);
        if (base64KeyData == null) {
            Log.e("autocrypt: missing key parameter");
            return null;
        }

        ByteString byteString = ByteString.decodeBase64(base64KeyData);
        if (byteString == null) {
            Log.e("autocrypt: error parsing base64 data");
            return null;
        }

        String to = parameters.remove(AutocryptHeader.AUTOCRYPT_PARAM_ADDR);
        if (to == null) {
            Log.e("autocrypt: no to header!");
            return null;
        }

        boolean isPreferEncryptMutual = false;
        String preferEncrypt = parameters.remove(AutocryptHeader.AUTOCRYPT_PARAM_PREFER_ENCRYPT);
        if (AutocryptHeader.AUTOCRYPT_PREFER_ENCRYPT_MUTUAL.equalsIgnoreCase(preferEncrypt)) {
            isPreferEncryptMutual = true;
        }

        if (hasCriticalParameters(parameters)) {
            return null;
        }

        return new AutocryptHeader(parameters, to, byteString.toByteArray(), isPreferEncryptMutual);
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
    private ArrayList<AutocryptHeader> parseAllAutocryptHeaders(String[] headers) {
        ArrayList<AutocryptHeader> autocryptHeaders = new ArrayList<>();
        for (String header : headers) {
            AutocryptHeader autocryptHeader = parseAutocryptHeader(header);
            if (autocryptHeader != null) {
                autocryptHeaders.add(autocryptHeader);
            }
        }
        return autocryptHeaders;
    }
}
