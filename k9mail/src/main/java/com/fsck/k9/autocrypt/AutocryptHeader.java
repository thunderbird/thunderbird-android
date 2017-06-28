package com.fsck.k9.autocrypt;


import java.util.Map;

import okio.ByteString;


class AutocryptHeader {
    static final String AUTOCRYPT_HEADER = "Autocrypt";

    static final String AUTOCRYPT_PARAM_TO = "addr";
    static final String AUTOCRYPT_PARAM_KEY_DATA = "keydata";

    static final String AUTOCRYPT_PARAM_TYPE = "type";
    static final String AUTOCRYPT_TYPE_1 = "1";

    static final String AUTOCRYPT_PARAM_PREFER_ENCRYPT = "prefer-encrypt";
    static final String AUTOCRYPT_PREFER_ENCRYPT_MUTUAL = "mutual";

    private static final int HEADER_LINE_LENGTH = 76;


    final byte[] keyData;
    final String addr;
    final Map<String,String> parameters;
    final boolean isPreferEncryptMutual;

    AutocryptHeader(Map<String, String> parameters, String addr, byte[] keyData, boolean isPreferEncryptMutual) {
        this.parameters = parameters;
        this.addr = addr;
        this.keyData = keyData;
        this.isPreferEncryptMutual = isPreferEncryptMutual;
    }

    String toRawHeaderString() {
        if (!parameters.isEmpty()) {
            throw new UnsupportedOperationException("arbitrary parameters not supported");
        }

        String autocryptHeaderString = AutocryptHeader.AUTOCRYPT_HEADER + ": ";
        autocryptHeaderString += AutocryptHeader.AUTOCRYPT_PARAM_TO + "=" + addr + ";";
        if (isPreferEncryptMutual) {
            autocryptHeaderString += AutocryptHeader.AUTOCRYPT_PARAM_PREFER_ENCRYPT + "=" +
                    AutocryptHeader.AUTOCRYPT_PREFER_ENCRYPT_MUTUAL + ";";
        }
        autocryptHeaderString += AutocryptHeader.AUTOCRYPT_PARAM_KEY_DATA + "=" + ByteString.of(keyData).base64();

        StringBuilder headerLines = new StringBuilder();
        int autocryptHeaderLength = autocryptHeaderString.length();
        for (int i = 0; i < autocryptHeaderLength; i += HEADER_LINE_LENGTH) {
            if (i + HEADER_LINE_LENGTH <= autocryptHeaderLength) {
                headerLines.append(autocryptHeaderString, i, i + HEADER_LINE_LENGTH).append("\n ");
            } else {
                headerLines.append(autocryptHeaderString, i, autocryptHeaderLength);
            }
        }

        return headerLines.toString();
    }
}
