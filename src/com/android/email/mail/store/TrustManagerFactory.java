
package com.android.email.mail.store;

import android.util.Log;
import android.net.http.DomainNameChecker;

import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.KeyStoreException;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateException;

import javax.net.ssl.X509TrustManager;
import javax.net.ssl.TrustManager;

public final class TrustManagerFactory {
    private static final String LOG_TAG = "TrustManagerFactory";

    private static X509TrustManager sSecureTrustManager;
    private static X509TrustManager sUnsecureTrustManager;

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
        private X509TrustManager mTrustManager;
        private String mHost;

        SecureX509TrustManager(X509TrustManager trustManager, String host) {
            mTrustManager = trustManager;
            mHost = host;
        }

        public void checkClientTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
            mTrustManager.checkClientTrusted(chain, authType);
        }

        public void checkServerTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {

            mTrustManager.checkServerTrusted(chain, authType);

            if (!DomainNameChecker.match(chain[0], mHost)) {
                throw new CertificateException("Certificate domain name does not match " 
                        + mHost);
            }
        }

        public X509Certificate[] getAcceptedIssuers() {
            return mTrustManager.getAcceptedIssuers();
        }
    }

    static {
        try {
            javax.net.ssl.TrustManagerFactory tmf = javax.net.ssl.TrustManagerFactory.getInstance("X509");
            tmf.init((KeyStore) null);
            TrustManager[] tms = tmf.getTrustManagers();
            if (tms != null) {
                for (TrustManager tm : tms) {
                    if (tm instanceof X509TrustManager) {
                        sSecureTrustManager = (X509TrustManager) tm;
                        break;
                    }
                }
            }
        } catch (NoSuchAlgorithmException e) {
            Log.e(LOG_TAG, "Unable to get X509 Trust Manager ", e);
        } catch (KeyStoreException e) {
            Log.e(LOG_TAG, "Key Store exception while initializing TrustManagerFactory ", e);
        }

        sUnsecureTrustManager = new SimpleX509TrustManager();
    }

    private TrustManagerFactory() {
    }

    public static X509TrustManager get(String host, boolean secure) {
        return secure ? new SecureX509TrustManager(sSecureTrustManager, host) :
                sUnsecureTrustManager;
    }
}
