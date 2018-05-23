package com.fsck.k9.mail.helpers;


import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.X509Certificate;


public class KeyStoreProvider {
    private static final String KEYSTORE_PASSWORD = "password";
    private static final String KEYSTORE_RESOURCE = "/keystore.jks";
    private static final String SERVER_CERTIFICATE_ALIAS = "mockimapserver";


    private final KeyStore keyStore;


    public static KeyStoreProvider getInstance() {
        KeyStore keyStore = loadKeyStore();
        return new KeyStoreProvider(keyStore);
    }

    private static KeyStore loadKeyStore() {
        try {
            KeyStore keyStore = KeyStore.getInstance("JKS");

            InputStream keyStoreInputStream = KeyStoreProvider.class.getResourceAsStream(KEYSTORE_RESOURCE);
            try {
                keyStore.load(keyStoreInputStream, KEYSTORE_PASSWORD.toCharArray());
            } finally {
                keyStoreInputStream.close();
            }

            return keyStore;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private KeyStoreProvider(KeyStore keyStore) {
        this.keyStore = keyStore;
    }

    public KeyStore getKeyStore() {
        return keyStore;
    }

    public char[] getPassword() {
        return KEYSTORE_PASSWORD.toCharArray();
    }

    public X509Certificate getServerCertificate() {
        try {
            KeyStore keyStore = loadKeyStore();
            return (X509Certificate) keyStore.getCertificate(SERVER_CERTIFICATE_ALIAS);
        } catch (KeyStoreException e) {
            throw new RuntimeException(e);
        }
    }
}
