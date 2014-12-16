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

import org.apache.commons.io.IOUtils;

import android.util.Log;

import static com.fsck.k9.mail.K9MailLib.LOG_TAG;

public class LocalKeyStore {
    private static final int KEY_STORE_FILE_VERSION = 1;

    private static String sKeyStoreLocation;

    public static void setKeyStoreLocation(String directory) {
        sKeyStoreLocation = directory;
    }

    private static class LocalKeyStoreHolder {
        static final LocalKeyStore INSTANCE = new LocalKeyStore();
    }

    public static LocalKeyStore getInstance() {
        return LocalKeyStoreHolder.INSTANCE;
    }


    private File mKeyStoreFile;
    private KeyStore mKeyStore;


    private LocalKeyStore() {
        try {
            upgradeKeyStoreFile();
            setKeyStoreFile(null);
        } catch (CertificateException e) {
            /*
             * Can happen if setKeyStoreLocation(String directory) has not been
             * called before the first call to getInstance(). Not necessarily an
             * error, presuming setKeyStoreFile(File) is called next with a
             * non-null File.
             */
            Log.w(LOG_TAG, "Local key store has not been initialized");
        }
    }

    /**
     * Reinitialize the local key store with certificates contained in
     * {@code file}
     *
     * @param file
     *            {@link File} containing locally saved certificates. May be 0
     *            length, in which case it is deleted and recreated. May be
     *            {@code null}, in which case a default file location is used.
     * @throws CertificateException
     *            Occurs if {@code file == null} and
     *            {@code setKeyStoreLocation(directory)} was not called previously.
     */
    public synchronized void setKeyStoreFile(File file) throws CertificateException {
        if (file == null) {
            file = new File(getKeyStoreFilePath(KEY_STORE_FILE_VERSION));
        }
        if (file.length() == 0) {
            /*
             * The file may be empty (e.g., if it was created with
             * File.createTempFile). We can't pass an empty file to
             * Keystore.load. Instead, we let it be created anew.
             */
            file.delete();
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
            mKeyStore = store;
            mKeyStoreFile = file;
        } catch (Exception e) {
            Log.e(LOG_TAG, "Failed to initialize local key store", e);
            // Use of the local key store is effectively disabled.
            mKeyStore = null;
            mKeyStoreFile = null;
        } finally {
            IOUtils.closeQuietly(fis);
        }
    }

    public synchronized void addCertificate(String host, int port,
            X509Certificate certificate) throws CertificateException {
        if (mKeyStore == null) {
            throw new CertificateException(
                    "Certificate not added because key store not initialized");
        }
        try {
            mKeyStore.setCertificateEntry(getCertKey(host, port), certificate);
        } catch (KeyStoreException e) {
            throw new CertificateException(
                    "Failed to add certificate to local key store", e);
        }
        writeCertificateFile();
    }

    private void writeCertificateFile() throws CertificateException {
        java.io.OutputStream keyStoreStream = null;
        try {
            keyStoreStream = new java.io.FileOutputStream(mKeyStoreFile);
            mKeyStore.store(keyStoreStream, "".toCharArray());
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
        if (mKeyStore == null) {
            return false;
        }
        Certificate storedCert = null;
        try {
            storedCert = mKeyStore.getCertificate(getCertKey(host, port));
            return (storedCert != null && storedCert.equals(certificate));
        } catch (KeyStoreException e) {
            return false;
        }
    }

    private static String getCertKey(String host, int port) {
        return host + ":" + port;
    }

    public synchronized void deleteCertificate(String oldHost, int oldPort) {
        if (mKeyStore == null) {
            return;
        }
        try {
            mKeyStore.deleteEntry(getCertKey(oldHost, oldPort));
            writeCertificateFile();
        } catch (KeyStoreException e) {
            // Ignore: most likely there was no cert. found
        } catch (CertificateException e) {
            Log.e(LOG_TAG, "Error updating the local key store file", e);
        }
    }

    private void upgradeKeyStoreFile() throws CertificateException {
        if (KEY_STORE_FILE_VERSION > 0) {
            // Blow away version "0" because certificate aliases have changed.
            new File(getKeyStoreFilePath(0)).delete();
        }
    }

    private String getKeyStoreFilePath(int version) throws CertificateException {
        if (sKeyStoreLocation == null) {
            throw new CertificateException("Local key store location has not been initialized");
        }
        if (version < 1) {
            return sKeyStoreLocation + File.separator + "KeyStore.bks";
        } else {
            return sKeyStoreLocation + File.separator + "KeyStore_v" + version + ".bks";
        }
    }
}
