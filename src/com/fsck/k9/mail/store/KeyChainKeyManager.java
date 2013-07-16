package com.fsck.k9.mail.store;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.net.Socket;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

import javax.net.ssl.X509ExtendedKeyManager;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.security.KeyChain;
import android.security.KeyChainAliasCallback;
import android.security.KeyChainException;
import android.util.Base64InputStream;
import android.util.Base64OutputStream;
import android.util.Log;

import com.fsck.k9.K9;
import com.fsck.k9.mail.ClientCertificateAliasRequiredException;
import com.fsck.k9.mail.filter.Base64;

/**
 * provide private keys and certificates during the ssl/tls handshake 
 * using the android ICS  KeyChain API.  if interactive selection
 * is requested, we harvest the parameters during the handshake and
 * abort with a custom (runtime) ClientCertificateAliasRequiredException
 * 
 * @author David Mansfield
 */

@TargetApi(14) 
public class KeyChainKeyManager extends X509ExtendedKeyManager {
    private static final String LOG_TAG = "TrustManagerFactory";

    private String mAlias;

    
    public KeyChainKeyManager() {
    	this.mAlias = null;
    	Log.d(LOG_TAG, "KeyChainKeyManager "+this+" set to interactive prompting required");
    	
    }
    
    public KeyChainKeyManager(String alias) {
    	if (alias == null || "".equals(alias)) {
    		throw new IllegalArgumentException("an invalid alias ("+alias+") was provided for auto-selection");
    	}
    	
    	this.mAlias = alias;
    	Log.d(LOG_TAG, "KeyChainKeyManager "+this+" set up with for auto-selected alias "+alias);
    }
    
    @Override 
	public String chooseClientAlias(String[] keyTypes, Principal[] issuers, Socket socket) {
    	if (mAlias == null) {
    		throw new ClientCertificateAliasRequiredException(keyTypes, issuers, 
    				socket.getInetAddress().getHostName(), socket.getPort());
    	}
    	
    	Log.d(LOG_TAG, "KeyChainKeyManager.chooseClientAlias "+this+" returning preselected alias "+mAlias);

    	return mAlias;
	}

	@Override
	public X509Certificate[] getCertificateChain(String alias) {
		try {
    		Log.d(LOG_TAG, "KeyChainKeyManager.getCertificateChain for "+alias);
			X509Certificate[] chain = KeyChain.getCertificateChain(K9.app, alias);
			
			if (chain == null || chain.length == 0) {
				throw new IllegalStateException("no certificate chain found for: "+alias);
			}
			
			return chain;
		} catch (KeyChainException e) {
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return null;
		}
	}

	@Override
	public PrivateKey getPrivateKey(String alias) {
		try {
    		Log.d(LOG_TAG, "KeyChainKeyManager.getPrivateKey for "+alias);
    		
    		PrivateKey key = KeyChain.getPrivateKey(K9.app, alias);
			
			if (key == null) {
				throw new IllegalStateException("no private key found for: "+alias);
			}
			
			//We need to keep reference to this key so it won't get GC. 
			//If it will then whole app will crash in Android 4.0+  with "Fatal signal 11 code=1" it's some kind of a bug in a system I guess
			K9.certsReferenceKeeper.add(key);
			
			return key;
		} catch (KeyChainException e) {
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return null;
		}
	}


	@Override
	public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
		// not valid for client side
		throw new UnsupportedOperationException();		
	}

	@Override
	public String[] getClientAliases(String keyType, Principal[] issuers) {
		// not valid for client side
		throw new UnsupportedOperationException();		
	}

	@Override
	public String[] getServerAliases(String keyType, Principal[] issuers) {
		// not valid for client side
		throw new UnsupportedOperationException();		
	}
	
	public static boolean areAnyCerts() {
		try 
        {
            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            if (ks != null) 
            {
                ks.load(null, null);
                Enumeration<String> aliases = ks.aliases();
                
                if(aliases.hasMoreElements()) {
                	return true;
                } else {
                	return false;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } catch (KeyStoreException e) {
            e.printStackTrace();
            return false;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return false;
        } catch (java.security.cert.CertificateException e) {
            e.printStackTrace();
            return false;
        }
		
		return false;
	}
	
	public static String interactivelyChooseClientCertificateAlias(Activity activity, String[] keyTypes,
			Principal[] issuers, String hostName, int port, String preSelectedAlias) {
		final String[] selected = new String[1];
		
		KeyChain.choosePrivateKeyAlias(activity, new KeyChainAliasCallback() {
			@Override
			public void alias(String alias) {
				synchronized(selected) {
					Log.d(LOG_TAG, "user has selected client certificate alias:"+alias);
					// see below. not null is condition for breaking out of loop
					if (alias == null) {
						alias = "";
					}
					selected[0] = alias;
					selected.notifyAll();
				}
				
			}
		}, keyTypes, issuers, hostName, port, preSelectedAlias); 

		synchronized (selected) {
			while (selected[0] == null) {
				try { 
					selected.wait(); 
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					break;
				}
			}

			if ("".equals(selected[0])) {
				selected[0] = null;
			}
		}

		return selected[0];
	}
}