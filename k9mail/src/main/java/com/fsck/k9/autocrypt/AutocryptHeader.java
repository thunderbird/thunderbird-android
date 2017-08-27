package com.fsck.k9.autocrypt;


import java.util.Arrays;
import java.util.Map;

import android.support.annotation.NonNull;

import okio.ByteString;


class AutocryptHeader {
    static final String AUTOCRYPT_HEADER = "Autocrypt";

    static final String AUTOCRYPT_PARAM_ADDR = "addr";
    static final String AUTOCRYPT_PARAM_KEY_DATA = "keydata";

    static final String AUTOCRYPT_PARAM_TYPE = "type";
    static final String AUTOCRYPT_TYPE_1 = "1";

    static final String AUTOCRYPT_PARAM_PREFER_ENCRYPT = "prefer-encrypt";
    static final String AUTOCRYPT_PREFER_ENCRYPT_MUTUAL = "mutual";

    private static final int HEADER_LINE_LENGTH = 76;


    @NonNull
    final byte[] keyData;
    @NonNull
    final String addr;
    @NonNull
    final Map<String,String> parameters;
    final boolean isPreferEncryptMutual;

    AutocryptHeader(@NonNull Map<String, String> parameters, @NonNull String addr,
            @NonNull byte[] keyData, boolean isPreferEncryptMutual) {
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
        autocryptHeaderString += AutocryptHeader.AUTOCRYPT_PARAM_ADDR + "=" + addr + ";";
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AutocryptHeader that = (AutocryptHeader) o;

        if (isPreferEncryptMutual != that.isPreferEncryptMutual) {
            return false;
        }
        if (!Arrays.equals(keyData, that.keyData)) {
            return false;
        }
        if (!addr.equals(that.addr)) {
            return false;
        }
        if (!parameters.equals(that.parameters)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(keyData);
        result = 31 * result + addr.hashCode();
        result = 31 * result + parameters.hashCode();
        result = 31 * result + (isPreferEncryptMutual ? 1 : 0);
        return result;
    }
}
