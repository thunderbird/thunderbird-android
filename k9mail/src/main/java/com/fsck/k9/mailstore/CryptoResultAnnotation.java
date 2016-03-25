package com.fsck.k9.mailstore;

public class CryptoResultAnnotation {
    public enum CryptoError {
        NONE,
        CRYPTO_API_RETURNED_ERROR,
        SIGNED_BUT_INCOMPLETE,
        ENCRYPTED_BUT_INCOMPLETE
    }
}
