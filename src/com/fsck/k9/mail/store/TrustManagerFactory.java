
package com.fsck.k9.mail.store;

import android.content.Context;
import android.util.Log;
import com.fsck.k9.K9;
import com.fsck.k9.helper.DomainNameChecker;
import com.fsck.k9.mail.CertificateChainException;

import org.apache.commons.io.IOUtils;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
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
import java.util.HashMap;
import java.util.Map;

public final class TrustManagerFactory {
    private static final String LOG_TAG = "TrustManagerFactory";

    private static X509TrustManager defaultTrustManager;
    private static X509TrustManager unsecureTrustManager;

    private static File keyStoreFile;
    private static KeyStore keyStore;


    private static class SimpleX509TrustManager implements X509TrustManager {
        public void checkClientTrusted(X509Certificate[] chain, String authType)
        throws CertificateException {
        }

        public void checkServerTrusted(X509Certificate[] chain, String authType)
        throws CertificateException {
        }

        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }
    }

    private static class SecureX509TrustManager implements X509TrustManager {
        private static final Map<String, SecureX509TrustManager> mTrustManager =
            new HashMap<String, SecureX509TrustManager>();

        private final String mHost;
        private final int mPort;

        private SecureX509TrustManager(String host, int port) {
            mHost = host;
            mPort = port;
        }

        public synchronized static X509TrustManager getInstance(String host, int port) {
            String key = getCertKey(host, port);
            SecureX509TrustManager trustManager;
            if (mTrustManager.containsKey(key)) {
                trustManager = mTrustManager.get(key);
            } else {
                trustManager = new SecureX509TrustManager(host, port);
                mTrustManager.put(key, trustManager);
            }

            return trustManager;
        }

        public void checkClientTrusted(X509Certificate[] chain, String authType)
        throws CertificateException {
            defaultTrustManager.checkClientTrusted(chain, authType);
        }

        public void checkServerTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
            boolean foundInGlobalKeyStore = false;
            try {
                defaultTrustManager.checkServerTrusted(chain, authType);
                foundInGlobalKeyStore = true;
            } catch (CertificateException e) { /* ignore */ }

            X509Certificate certificate = chain[0];

            // Check the local key store if we couldn't verify the certificate using the global
            // key store or if the host name doesn't match the certificate name
            if (!foundInGlobalKeyStore || !DomainNameChecker.match(certificate, mHost)) {
                try {
                    Certificate storedCert = keyStore.getCertificate(getCertKey(mHost, mPort));
                    if (storedCert != null && storedCert.equals(certificate)) {
                        return;
                    }
                } catch (KeyStoreException e) {
                    throw new CertificateException("Certificate cannot be verified", e);
                }

                String message = (foundInGlobalKeyStore) ?
                        "Certificate domain name does not match " + mHost :
                        "Couldn't find certificate in local key store";

                throw new CertificateChainException(message, chain);
            }
        }

        public X509Certificate[] getAcceptedIssuers() {
            return defaultTrustManager.getAcceptedIssuers();
        }

    }

    static {
        try {
            loadKeyStore();

            javax.net.ssl.TrustManagerFactory tmf = javax.net.ssl.TrustManagerFactory.getInstance("X509");
            tmf.init((KeyStore) null);

            TrustManager[] tms = tmf.getTrustManagers();
            if (tms != null) {
                for (TrustManager tm : tms) {
                    if (tm instanceof X509TrustManager) {
                        defaultTrustManager = (X509TrustManager) tm;
                        break;
                    }
                }
            }
        } catch (NoSuchAlgorithmException e) {
            Log.e(LOG_TAG, "Unable to get X509 Trust Manager ", e);
        } catch (KeyStoreException e) {
            Log.e(LOG_TAG, "Key Store exception while initializing TrustManagerFactory ", e);
        }
        unsecureTrustManager = new SimpleX509TrustManager();
    }

    static void loadKeyStore() throws KeyStoreException, NoSuchAlgorithmException {
        Context context = K9.app;

        keyStoreFile = new File(context.getDir("KeyStore", Context.MODE_PRIVATE) +
                File.separator + "KeyStore.bks");
        keyStore = KeyStore.getInstance(KeyStore.getDefaultType());

        FileInputStream fis;
        try {
            fis = new FileInputStream(keyStoreFile);
        } catch (FileNotFoundException e) {
            // If the file doesn't exist, that's fine, too
            fis = null;
        }

        try {
            keyStore.load(fis, "".toCharArray());
        } catch (IOException e) {
            Log.e(LOG_TAG, "KeyStore IOException while initializing TrustManagerFactory ", e);
            keyStore = null;
        } catch (CertificateException e) {
            Log.e(LOG_TAG, "KeyStore CertificateException while initializing TrustManagerFactory ", e);
            keyStore = null;
        } finally {
            IOUtils.closeQuietly(fis);
        }
    }

    private TrustManagerFactory() {
    }

    public static X509TrustManager get(String host, int port, boolean secure) {
        return secure ? SecureX509TrustManager.getInstance(host, port) :
               unsecureTrustManager;
    }

    public static void addCertificate(String host, int port, X509Certificate certificate) throws CertificateException {
        try {
            keyStore.setCertificateEntry(getCertKey(host, port), certificate);

            java.io.OutputStream keyStoreStream = null;
            try {
                keyStoreStream = new java.io.FileOutputStream(keyStoreFile);
                keyStore.store(keyStoreStream, "".toCharArray());
            } catch (FileNotFoundException e) {
                throw new CertificateException("Unable to write KeyStore: " + e.getMessage());
            } catch (CertificateException e) {
                throw new CertificateException("Unable to write KeyStore: " + e.getMessage());
            } catch (IOException e) {
                throw new CertificateException("Unable to write KeyStore: " + e.getMessage());
            } finally {
                IOUtils.closeQuietly(keyStoreStream);
            }

        } catch (NoSuchAlgorithmException e) {
            Log.e(LOG_TAG, "Unable to get X509 Trust Manager ", e);
        } catch (KeyStoreException e) {
            Log.e(LOG_TAG, "Key Store exception while initializing TrustManagerFactory ", e);
        }
    }

    private static String getCertKey(String host, int port) {
        return host + ":" + port;
    }
}
