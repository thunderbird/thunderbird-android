
package com.fsck.k9.mail.store;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.io.IOUtils;

import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.fsck.k9.K9;
import com.fsck.k9.helper.DomainNameChecker;
import com.fsck.k9.mail.MessagingException;

public final class TrustManagerFactory {
    private static final String LOG_TAG = "TrustManagerFactory";

    private static X509TrustManager defaultTrustManager;
    private static X509TrustManager unsecureTrustManager;
    private static X509TrustManager localTrustManager;

    private static X509Certificate[] lastCertChain = null;

    private static File keyStoreFile;
    private static KeyStore keyStore;

    // this indicates we should "harvest" some connection information from inside
    // the SSL handshake, then abort the handshake with a custom exception
    private static ThreadLocal<Boolean> interactiveClientCertificateAliasSelectionRequired = new ThreadLocal<Boolean>() {
		@Override
		protected Boolean initialValue() {
			return Boolean.FALSE;
		}
    	
    };
    public static void setInteractiveClientCertificateAliasSelectionRequired(boolean die) {
    	interactiveClientCertificateAliasSelectionRequired.set(die);
    }
    
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
                localTrustManager.checkServerTrusted(new X509Certificate[] {chain[0]}, authType);
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
    
    public static boolean isPlatformSupportsClientCertificates() {
    	return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH);
    }
    
    private static SSLContext createSslContext(String host, boolean secure, String clientCertificateAlias) throws NoSuchAlgorithmException, KeyManagementException, MessagingException {
    	if (!isPlatformSupportsClientCertificates() &&
    			(interactiveClientCertificateAliasSelectionRequired.get() || clientCertificateAlias != null)) {
    		throw new MessagingException("Client Certificate support is only availble in Android 4.0 (ICS)", true);
    	}
    	
        KeyManager[] keyManagers = null;
        if (interactiveClientCertificateAliasSelectionRequired.get()) {
        	keyManagers = new KeyManager[] {new KeyChainKeyManager()};
        } else if (clientCertificateAlias != null){
        	keyManagers = new KeyManager[] {new KeyChainKeyManager(clientCertificateAlias)};
        }

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagers, new TrustManager[] {
                TrustManagerFactory.get(host, secure)
            }, new SecureRandom());

        return sslContext;
    }
    
    public static Socket createSslSocket(String host, boolean secure, String clientCertificateAlias) throws NoSuchAlgorithmException, KeyManagementException, IOException, MessagingException {
    	SSLContext sslContext = createSslContext(host, secure, clientCertificateAlias);
        return sslContext.getSocketFactory().createSocket();
    }
    
    public static Socket performStartTls(Socket socket, String host, int port, boolean secure, String clientCertificateAlias) throws NoSuchAlgorithmException, KeyManagementException, IOException, MessagingException {
    	SSLContext sslContext = createSslContext(host, secure, clientCertificateAlias);
        boolean autoClose = true;
        return sslContext.getSocketFactory().createSocket(socket, host, port, autoClose);
    }
}
