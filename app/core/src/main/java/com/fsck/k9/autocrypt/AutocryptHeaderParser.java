package com.fsck.k9.autocrypt;


import java.util.ArrayList;
import java.util.Map;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.internet.MimeUtility;
import okio.ByteString;
import timber.log.Timber;


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

        String to = parameters.remove(AutocryptHeader.AUTOCRYPT_PARAM_ADDR);
        if (to == null) {
            Timber.e("autocrypt: no to header!");
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
