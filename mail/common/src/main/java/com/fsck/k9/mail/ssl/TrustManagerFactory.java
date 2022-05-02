
package com.fsck.k9.mail.ssl;


import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import com.fsck.k9.logging.Timber;
import com.fsck.k9.mail.CertificateChainException;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.apache.hc.client5.http.ssl.DefaultHostnameVerifier;

public class TrustManagerFactory {
    public static TrustManagerFactory createInstance(LocalKeyStore localKeyStore) {
        TrustManagerFactory trustManagerFactory = new TrustManagerFactory(localKeyStore);
        try {
            trustManagerFactory.initialize();
        } catch (NoSuchAlgorithmException | KeyStoreException e) {
            Timber.e(e, "Failed to initialize X509 Trust Manager!");
            throw new IllegalStateException(e);
        }
        return trustManagerFactory;
    }


    private X509TrustManager defaultTrustManager;
    private LocalKeyStore keyStore;
    private final Map<String, SecureX509TrustManager> cachedTrustManagers = new HashMap<>();


    private TrustManagerFactory(LocalKeyStore localKeyStore) {
        this.keyStore = localKeyStore;
    }

    private void initialize() throws KeyStoreException, NoSuchAlgorithmException {
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
    }

    public X509TrustManager getTrustManagerForDomain(String host, int port) {
        String key = host + ":" + port;
        SecureX509TrustManager trustManager;
        if (cachedTrustManagers.containsKey(key)) {
            trustManager = cachedTrustManagers.get(key);
        } else {
            trustManager = new SecureX509TrustManager(host, port);
            cachedTrustManagers.put(key, trustManager);
        }

        return trustManager;
    }

    private class SecureX509TrustManager implements X509TrustManager {
        private final DefaultHostnameVerifier hostnameVerifier = new DefaultHostnameVerifier();

        private final String mHost;
        private final int mPort;

        private SecureX509TrustManager(String host, int port) {
            mHost = host;
            mPort = port;
        }

        public void checkClientTrusted(X509Certificate[] chain, String authType)
        throws CertificateException {
            defaultTrustManager.checkClientTrusted(chain, authType);
        }

        public void checkServerTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
            String message;
            X509Certificate certificate = chain[0];

            Throwable cause;

            try {
                defaultTrustManager.checkServerTrusted(chain, authType);
                hostnameVerifier.verify(mHost, certificate);
                return;
            } catch (CertificateException e) {
                // cert. chain can't be validated
                message = e.getMessage();
                cause = e;
            } catch (SSLException e) {
                // host name doesn't match certificate
                message = e.getMessage();
                cause = e;
            }

            // Check the local key store if we couldn't verify the certificate using the global
            // key store or if the host name doesn't match the certificate name
            if (!keyStore.isValidCertificate(certificate, mHost, mPort)) {
                throw new CertificateChainException(message, chain, cause);
            }
        }

        public X509Certificate[] getAcceptedIssuers() {
            return defaultTrustManager.getAcceptedIssuers();
        }

    }
}
