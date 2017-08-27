package com.fsck.k9.autocrypt;


import java.util.Map;


class AutocryptHeader {
    static final String AUTOCRYPT_HEADER = "Autocrypt";

    static final String AUTOCRYPT_PARAM_TO = "addr";
    static final String AUTOCRYPT_PARAM_KEY_DATA = "keydata";

    static final String AUTOCRYPT_PARAM_TYPE = "type";
    static final String AUTOCRYPT_TYPE_1 = "1";

    static final String AUTOCRYPT_PARAM_PREFER_ENCRYPT = "prefer-encrypt";
    static final String AUTOCRYPT_PREFER_ENCRYPT_MUTUAL = "mutual";


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
}
