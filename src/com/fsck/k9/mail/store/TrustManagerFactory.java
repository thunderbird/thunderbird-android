
package com.fsck.k9.mail.store;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.io.IOUtils;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.fsck.k9.K9;
import com.fsck.k9.helper.DomainNameChecker;

public final class TrustManagerFactory {
    private static final String LOG_TAG = "TrustManagerFactory";

    private static X509TrustManager defaultTrustManager;
    private static X509TrustManager unsecureTrustManager;
    private static X509TrustManager localTrustManager;

    private static X509Certificate[] lastCertChain = null;

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
    /** private
     * but we have to access the class in the junit tests
     **/
    static class SecureX509TrustManager implements X509TrustManager {
        private static final Map<String, SecureX509TrustManager> mTrustManager =
            new HashMap<String, SecureX509TrustManager>();

        private final String mHost;

        private SecureX509TrustManager(String host) {
            mHost = host;
        }

        public synchronized static X509TrustManager getInstance(String host) {
            SecureX509TrustManager trustManager;
            if (mTrustManager.containsKey(host)) {
                trustManager = mTrustManager.get(host);
            } else {
                trustManager = new SecureX509TrustManager(host);
                mTrustManager.put(host, trustManager);
            }

            return trustManager;
        }

        public void checkClientTrusted(X509Certificate[] chain, String authType)
        throws CertificateException {
            defaultTrustManager.checkClientTrusted(chain, authType);
        }

        public void checkServerTrusted(X509Certificate[] chain, String authType)
        throws CertificateException {
            // FIXME: Using a static field to store the certificate chain is a bad idea. Instead
            // create a CertificateException subclass and store the chain there.
            TrustManagerFactory.setLastCertChain(chain);
            try {
                defaultTrustManager.checkServerTrusted(chain, authType);
            } catch (CertificateException e) {
                try {
                    localTrustManager.checkServerTrusted(new X509Certificate[] {chain[0]}, authType);
                } catch (Exception e1) {
                    /*
                     * on HTC phones there is an RSA lib bug which causes this
                     * method to fail permanently with an strange error: E/k9
                     * (25824): Caused by: java.lang.RuntimeException:
                     * error:0407006A:rsa
                     * routines:RSA_padding_check_PKCS1_type_1:block type is not
                     * 01 (SHA-1)
                     *
                     * SO WE try a workaround: we use every pubkey from our
                     * keystore and check if the chain validates against it.
                     * http://code.google.com/p/k9mail/issues/detail?id=3976
                     */
                    try {
                        Map<String, Certificate> aliasmap = new HashMap<String, Certificate>();
                        Enumeration<String> eAliases;
                        try {
                            eAliases = keyStore.aliases();
                            while (eAliases.hasMoreElements()) {
                                String alias = eAliases.nextElement();
                                aliasmap.put(alias, keyStore.getCertificate(alias));
                            }
                            checkServerTrustedHTCWorkaround(aliasmap, chain, authType);
                        } catch (KeyStoreException e2) {
                            throw new CertificateException(e2);
                        }
                    } catch (CertificateException e2) {
                        /* workaround did not report trusted as well
                         * -> throw original exception
                         */
                        throw new CertificateException("Certificate cannot be verified; KeyStore Exception: ", e);
                    }
                }


            }
            if (!DomainNameChecker.match(chain[0], mHost)) {
                try {
                    String dn = chain[0].getSubjectDN().toString();
                    if ((dn != null) && (dn.equalsIgnoreCase(keyStore.getCertificateAlias(chain[0])))) {
                        return;
                    }
                } catch (KeyStoreException e) {
                    throw new CertificateException("Certificate cannot be verified; KeyStore Exception: " + e);
                }
                throw new CertificateException("Certificate domain name does not match "
                                               + mHost);
            }
        }

        /**
         * This method is an workaround for an HTC RSA lib bug.
         *
         * This method is only called by checkServerTrusted
         * NEVER CALL THIS METHOD DIRECTLY.
         *
         * on HTC phones there is an RSA lib bug which causes this method to
         * fail permanently with an strange error: E/k9 (25824): Caused by:
         * java.lang.RuntimeException: error:0407006A:rsa
         * routines:RSA_padding_check_PKCS1_type_1:block type is not 01 (SHA-1)
         *
         * SO WE try a workaround: we use every pubkey from our keystore and
         * check if the chain validates against it.
         * http://code.google.com/p/k9mail/issues/detail?id=3976
         *
         * @param aliases Alias -> Certificate mapping (get from keyStore)
         * @param chain
         * @param authType
         * @throws CertificateException
         */
        public void checkServerTrustedHTCWorkaround(Map<String, Certificate> aliases , X509Certificate[] chain, String authType)
        throws CertificateException {
            Log.d(LOG_TAG, "SSL-connection HTC padding Bug workaround executing");
            boolean verified = false;
            try {
                for (Entry<String, Certificate> alias : aliases.entrySet()) {
                    Certificate cert = alias.getValue();
                    try {
                        /*
                         * we only check the leaf of a certificate chain. during
                         * import for selfsigned certs we also only import the
                         * leaf of a chain.
                         */
                        chain[0].verify(cert.getPublicKey());
                        verified = true;
                        break;
                    } catch (Exception e3) {
                        Log.e(LOG_TAG, e3.toString());
                    }
                }
            } catch (Exception e2) {
                Log.e(LOG_TAG, "KeyStore Problem", e2);
            }
            if (!verified) {
                /* no valid public key found so this connection isnot trusted */
                throw new CertificateException();
            }
        }


        public X509Certificate[] getAcceptedIssuers() {
            return defaultTrustManager.getAcceptedIssuers();
        }

    }

    static {
        java.io.InputStream fis = null;
        try {
            javax.net.ssl.TrustManagerFactory tmf = javax.net.ssl.TrustManagerFactory.getInstance("X509");
            Application app = K9.app;
            keyStoreFile = new File(app.getDir("KeyStore", Context.MODE_PRIVATE) + File.separator + "KeyStore.bks");
            keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            try {
                fis = new java.io.FileInputStream(keyStoreFile);
            } catch (FileNotFoundException e1) {
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
            }
            tmf.init(keyStore);
            TrustManager[] tms = tmf.getTrustManagers();
            if (tms != null) {
                for (TrustManager tm : tms) {
                    if (tm instanceof X509TrustManager) {
                        localTrustManager = (X509TrustManager)tm;
                        break;
                    }
                }
            }
            tmf = javax.net.ssl.TrustManagerFactory.getInstance("X509");
            tmf.init((KeyStore)null);
            tms = tmf.getTrustManagers();
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
        } finally {
            IOUtils.closeQuietly(fis);
        }
        unsecureTrustManager = new SimpleX509TrustManager();
    }

    private TrustManagerFactory() {
    }

    public static X509TrustManager get(String host, boolean secure) {
        return secure ? SecureX509TrustManager.getInstance(host) :
               unsecureTrustManager;
    }

    public static KeyStore getKeyStore() {
        return keyStore;
    }

    public static void setLastCertChain(X509Certificate[] chain) {
        lastCertChain = chain;
    }
    public static X509Certificate[] getLastCertChain() {
        return lastCertChain;
    }

    public static void addCertificateChain(String alias, X509Certificate[] chain) throws CertificateException {
        try {
            javax.net.ssl.TrustManagerFactory tmf = javax.net.ssl.TrustManagerFactory.getInstance("X509");
            for (X509Certificate element : chain) {
                keyStore.setCertificateEntry
                (element.getSubjectDN().toString(), element);
            }

            tmf.init(keyStore);
            TrustManager[] tms = tmf.getTrustManagers();
            if (tms != null) {
                for (TrustManager tm : tms) {
                    if (tm instanceof X509TrustManager) {
                        localTrustManager = (X509TrustManager) tm;
                        break;
                    }
                }
            }
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
}
