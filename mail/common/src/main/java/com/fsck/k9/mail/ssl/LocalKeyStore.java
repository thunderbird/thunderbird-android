package com.fsck.k9.mail.ssl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import android.content.Context;

import org.apache.commons.io.IOUtils;
import timber.log.Timber;


public class LocalKeyStore {
    private static final int KEY_STORE_FILE_VERSION = 1;

    public static LocalKeyStore createInstance(Context context) {
        String keyStoreLocation = context.getDir("KeyStore", Context.MODE_PRIVATE).toString();
        LocalKeyStore localKeyStore = new LocalKeyStore(keyStoreLocation);
        localKeyStore.initializeKeyStore();
        return localKeyStore;
    }


    private final String keyStoreLocation;
    private File keyStoreFile;
    private KeyStore keyStore;


    private LocalKeyStore(String keyStoreLocation) {
        this.keyStoreLocation = keyStoreLocation;
    }

    /** Reinitialize the local key store with stored certificates */
    private synchronized void initializeKeyStore() {
        upgradeKeyStoreFile();

        File file = new File(getKeyStoreFilePath(KEY_STORE_FILE_VERSION));
        if (file.length() == 0) {
            /*
             * The file may be empty (e.g., if it was created with
             * File.createTempFile). We can't pass an empty file to
             * Keystore.load. Instead, we let it be created anew.
             */
            if (file.exists() && !file.delete()) {
                Timber.d("Failed to delete empty keystore file: %s", file.getAbsolutePath());
            }
        }

        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            // If the file doesn't exist, that's fine, too
        }

        try {
            KeyStore store = KeyStore.getInstance(KeyStore.getDefaultType());
            store.load(fis, "".toCharArray());
            keyStore = store;
            keyStoreFile = file;
        } catch (Exception e) {
            Timber.e(e, "Failed to initialize local key store");
            // Use of the local key store is effectively disabled.
            keyStore = null;
            keyStoreFile = null;
        } finally {
            IOUtils.closeQuietly(fis);
        }
    }

    private void upgradeKeyStoreFile() {
        if (KEY_STORE_FILE_VERSION > 0) {
            // Blow away version "0" because certificate aliases have changed.
            File versionZeroFile = new File(getKeyStoreFilePath(0));
            if (versionZeroFile.exists() && !versionZeroFile.delete()) {
                Timber.d("Failed to delete old key-store file: %s", versionZeroFile.getAbsolutePath());
            }
        }
    }

    public synchronized void addCertificate(String host, int port,
            X509Certificate certificate) throws CertificateException {
        if (keyStore == null) {
            throw new CertificateException(
                    "Certificate not added because key store not initialized");
        }
        try {
            keyStore.setCertificateEntry(getCertKey(host, port), certificate);
        } catch (KeyStoreException e) {
            throw new CertificateException(
                    "Failed to add certificate to local key store", e);
        }
        writeCertificateFile();
    }

    private void writeCertificateFile() throws CertificateException {
        java.io.OutputStream keyStoreStream = null;
        try {
            keyStoreStream = new java.io.FileOutputStream(keyStoreFile);
            keyStore.store(keyStoreStream, "".toCharArray());
        } catch (FileNotFoundException e) {
            throw new CertificateException("Unable to write KeyStore: "
                    + e.getMessage());
        } catch (CertificateException e) {
            throw new CertificateException("Unable to write KeyStore: "
                    + e.getMessage());
        } catch (IOException e) {
            throw new CertificateException("Unable to write KeyStore: "
                    + e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            throw new CertificateException("Unable to write KeyStore: "
                    + e.getMessage());
        } catch (KeyStoreException e) {
            throw new CertificateException("Unable to write KeyStore: "
                    + e.getMessage());
        } finally {
            IOUtils.closeQuietly(keyStoreStream);
        }
    }

    public synchronized boolean isValidCertificate(Certificate certificate,
            String host, int port) {
        if (keyStore == null) {
            return false;
        }
        try {
            Certificate storedCert = keyStore.getCertificate(getCertKey(host, port));
            return (storedCert != null && storedCert.equals(certificate));
        } catch (KeyStoreException e) {
            return false;
        }
    }

    private static String getCertKey(String host, int port) {
        return host + ":" + port;
    }

    public synchronized void deleteCertificate(String oldHost, int oldPort) {
        if (keyStore == null) {
            return;
        }
        try {
            keyStore.deleteEntry(getCertKey(oldHost, oldPort));
            writeCertificateFile();
        } catch (KeyStoreException e) {
            // Ignore: most likely there was no cert. found
        } catch (CertificateException e) {
            Timber.e(e, "Error updating the local key store file");
        }
    }

    private String getKeyStoreFilePath(int version) {
        if (version < 1) {
            return keyStoreLocation + File.separator + "KeyStore.bks";
        } else {
            return keyStoreLocation + File.separator + "KeyStore_v" + version + ".bks";
        }
    }
}
