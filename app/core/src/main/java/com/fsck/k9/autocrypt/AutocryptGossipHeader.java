package com.fsck.k9.autocrypt;


import java.util.Arrays;

import android.support.annotation.NonNull;


class AutocryptGossipHeader {
    static final String AUTOCRYPT_GOSSIP_HEADER = "Autocrypt-Gossip";

    private static final String AUTOCRYPT_PARAM_ADDR = "addr";
    private static final String AUTOCRYPT_PARAM_KEY_DATA = "keydata";


    @NonNull
    final byte[] keyData;
    @NonNull
    final String addr;

    AutocryptGossipHeader(@NonNull String addr, @NonNull byte[] keyData) {
        this.addr = addr;
        this.keyData = keyData;
    }

    String toRawHeaderString() {
        String builder = AutocryptGossipHeader.AUTOCRYPT_GOSSIP_HEADER + ": " +
                AutocryptGossipHeader.AUTOCRYPT_PARAM_ADDR + '=' + addr + "; " +
                AutocryptGossipHeader.AUTOCRYPT_PARAM_KEY_DATA + '=' +
                AutocryptHeader.createFoldedBase64KeyData(keyData);

        return builder;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AutocryptGossipHeader that = (AutocryptGossipHeader) o;

        return Arrays.equals(keyData, that.keyData) && addr.equals(that.addr);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(keyData);
        result = 31 * result + addr.hashCode();
        return result;
    }
}
