package com.fsck.k9.mail.store;

import java.net.Socket;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import javax.net.ssl.X509ExtendedKeyManager;

import android.annotation.TargetApi;
import android.app.Activity;
import android.security.KeyChain;
import android.security.KeyChainAliasCallback;
import android.security.KeyChainException;
import android.util.Log;

import com.fsck.k9.K9;

@TargetApi(14) 
class KeyChainKeyManager extends X509ExtendedKeyManager {
    private static final String LOG_TAG = "TrustManagerFactory";

    private final Object mAliasLock = new Object();
    private String mAlias;
    private Activity mCurrentActivity;
    // re-use this class type to provide the selected alias out to caller
    private KeyChainAliasCallback mAliasCallback;
    
    public KeyChainKeyManager(String alias) {
    	this.mAlias = alias;
    	Log.d(LOG_TAG, "KeyChainKeyManager "+this+" set up with for auto-selected alias "+alias);
    }
    
    public KeyChainKeyManager(Activity activity, String alias, KeyChainAliasCallback aliasCallback) {
    	this.mCurrentActivity = activity;
    	this.mAlias = alias;
    	this.mAliasCallback = aliasCallback;
    	Log.d(LOG_TAG, "KeyChainKeyManager "+this+
    			" set up with for prompting using activity "+mCurrentActivity+
    			" and pre-selected alias "+mAlias);
    }
    
    @Override 
	public String chooseClientAlias(String[] keyTypes, Principal[] issuers, Socket socket) {
		if (mCurrentActivity == null) {
    		if (mAlias != null) {
    			Log.d(LOG_TAG, "KeyChainKeyManager.chooseClientAlias "+this+" returning preselected alias "+mAlias);
    			return mAlias;
    		}
    		
			Log.w(LOG_TAG, "unable to initialize the KeyChain.chooseClientAlias intent "+
					"in the KeyChain because no parent Activity has been set");
			
			throw new IllegalStateException("BUG: unable to prompt for client certificate");
		}
		
		Log.d(LOG_TAG, "KeyChainKeyManager.chooseClientAlias using activity "+mCurrentActivity);
		
		String tmpAlias = mAlias;
		mAlias = null;
		
		KeyChain.choosePrivateKeyAlias(mCurrentActivity, new AliasResponse(), 
				keyTypes, issuers, 
				socket.getInetAddress().getHostName(), socket.getPort(), 
				tmpAlias);

		synchronized (mAliasLock) {
			while (mAlias == null) {
				try { 
					mAliasLock.wait(); 
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					break;
				}
			}

			if ("".equals(mAlias)) 
				mAlias = null;
			
			if (mAliasCallback != null) {
				mAliasCallback.alias(mAlias);
			}
		}

		// in this context, don't proceed when no client cert chosen.
		if (mAlias == null) {
			throw new IllegalStateException("unable to proceed with SSL handshake without a client certificate");
		}
		
		Log.d(LOG_TAG, "KeyChainKeyManager.chooseClientAlias alias was chosen by user: "+mAlias);

		return mAlias;
	}

    private class AliasResponse implements KeyChainAliasCallback {
		@Override
		public void alias(String alias) {
			Log.d(LOG_TAG, "KeyChainKeyManager: in alias callback with alias "+alias);
			synchronized (mAliasLock) {
				// see above. not null is condition for breaking out of loop
				if (alias == null) {
					alias = "";
				}
				mAlias = alias;
				mAliasLock.notifyAll();
			}
		}
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
}