
package com.fsck.k9.mail.store;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.security.KeyChain;
import android.security.KeyChainAliasCallback;
import android.security.KeyChainException;
import android.util.Log;
import com.fsck.k9.K9;
import com.fsck.k9.helper.DomainNameChecker;
import org.apache.commons.io.IOUtils;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedKeyManager;
import javax.net.ssl.X509TrustManager;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

public final class TrustManagerFactory {
    private static final String LOG_TAG = "TrustManagerFactory";

    private static X509TrustManager defaultTrustManager;
    private static X509TrustManager unsecureTrustManager;
    private static X509TrustManager localTrustManager;

    private static X509Certificate[] lastCertChain = null;

    private static File keyStoreFile;
    private static KeyStore keyStore;

    // FIXME: how to do this properly?
    private static Activity mCurrentActivity;
    public static void setCurrentActivity(Activity activity) {
    	mCurrentActivity = activity;
    }
    
    private static final Object mAliasLock = new Object();
    private static String mAlias;
    
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
    
    @TargetApi(14)
    private static class KeyChainKeyManager extends X509ExtendedKeyManager {
    	@Override 
    	public String chooseClientAlias(String[] keyTypes, Principal[] issuers, Socket socket) {
    		synchronized (mAliasLock) {
    			if (mAlias != null) {
    	    		Log.d(LOG_TAG, "KeyChainKeyManager.chooseClientAlias returning preselected alias "+mAlias);
    				return mAlias;
    				
    			}
    		}
    		
    		if (mCurrentActivity == null) {
    			Log.w(LOG_TAG, "unable to initialize the chooseClientAlias intent in the KeyChain because current activity is not set");
    			return null;
    		}
    		
    		Log.d(LOG_TAG, "KeyChainKeyManager.chooseClientAlias using activity "+mCurrentActivity);
    		
    		KeyChain.choosePrivateKeyAlias(mCurrentActivity, new AliasResponse(), 
    				keyTypes, issuers, 
    				socket.getInetAddress().getHostName(), socket.getPort(), 
    				null);

    		String alias;
    		
    		synchronized (mAliasLock) {
    			while (mAlias == null) {
    				try {
    					mAliasLock.wait();
    				} catch (InterruptedException ignored) {
    					
    				}
    			}
    			alias = mAlias;
			}
    		Log.d(LOG_TAG, "KeyChainKeyManager.chooseClientAlias alias was chosen: "+alias);
    		return alias;
    	}

		@Override
		public String chooseServerAlias(String keyType, Principal[] issuers,
				Socket socket) {
			// not valid for client side
			throw new UnsupportedOperationException();		
		}

		@Override
		public X509Certificate[] getCertificateChain(String alias) {
			try {
	    		Log.d(LOG_TAG, "KeyChainKeyManager.getCertificateChain for "+alias+" using activity "+mCurrentActivity);

				return KeyChain.getCertificateChain(K9.app, alias);
			} catch (KeyChainException e) {
				throw new RuntimeException(e);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return null;
			}
		}

		@Override
		public String[] getClientAliases(String keyType, Principal[] issuers) {
			// not valid for client side
			throw new UnsupportedOperationException();		
		}

		@Override
		public PrivateKey getPrivateKey(String alias) {
			PrivateKey privateKey;
			try {
	    		Log.d(LOG_TAG, "KeyChainKeyManager.getPrivateKey for "+alias+" using activity "+mCurrentActivity);
				privateKey = KeyChain.getPrivateKey(K9.app, alias);
				return privateKey;
			} catch (KeyChainException e) {
				throw new RuntimeException(e);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return null;
			}
		}

		@Override
		public String[] getServerAliases(String keyType, Principal[] issuers) {
			// not valid for client side
			throw new UnsupportedOperationException();		
		}
    }
    
    private static class AliasResponse implements KeyChainAliasCallback {
		@Override
		public void alias(String alias) {
			synchronized (mAliasLock) {
				mAlias = alias;
				mAliasLock.notifyAll();
			}
		}
    	
    }
    
    private static SSLContext createSslContext(String host, boolean secure) throws NoSuchAlgorithmException, KeyManagementException {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        KeyManager[] keyManagers = null;
        if (mCurrentActivity != null || mAlias != null) {
        	keyManagers = new KeyManager[] { new KeyChainKeyManager() };
        } else {
        	Log.d(LOG_TAG, 
        			"unable to create keyManager due to no current activity");
        }
        sslContext.init(keyManagers, new TrustManager[] {
                TrustManagerFactory.get(host, secure)
            }, new SecureRandom());
    	return sslContext;
    }
    
    public static Socket createSslSocket(String host, boolean secure) throws NoSuchAlgorithmException, KeyManagementException, IOException {
    	SSLContext sslContext = createSslContext(host, secure);
        return sslContext.getSocketFactory().createSocket();
    }
    
    public static Socket performStartTls(Socket socket, String host, int port, boolean secure) throws NoSuchAlgorithmException, KeyManagementException, IOException {
    	SSLContext sslContext = createSslContext(host, secure);
        boolean autoClose = true;
        return sslContext.getSocketFactory().createSocket(socket, host, port, autoClose);
    }
}
