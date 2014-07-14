
package com.fsck.k9.security;

import java.net.Socket;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import javax.net.ssl.X509ExtendedKeyManager;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.security.KeyChain;
import android.security.KeyChainAliasCallback;
import android.security.KeyChainException;
import android.util.Log;

import com.fsck.k9.K9;
import com.fsck.k9.mail.ClientCertificateRequiredException;

/**
 * For client certificate authentication! Provide private keys and certificates
 * during the TLS handshake using the Android 4.0 KeyChain API. If interactive
 * selection is requested, we harvest the parameters during the handshake and
 * abort with a custom (runtime) ClientCertificateRequiredException.
 */
@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class KeyChainKeyManager extends X509ExtendedKeyManager {

    private static PrivateKey sClientCertificateReferenceWorkaround;

    private String mAlias;

    public KeyChainKeyManager() {
        mAlias = null;
        if (K9.DEBUG)
            Log.d(K9.LOG_TAG, "KeyChainKeyManager set to interactive prompting required");
    }

    public KeyChainKeyManager(String alias) {
        if (alias == null || "".equals(alias)) {
            throw new IllegalArgumentException(
                    "KeyChainKeyManager: The provided alias is null or empty!");
        }

        mAlias = alias;
        if (K9.DEBUG)
            Log.d(K9.LOG_TAG, "KeyChainKeyManager set up with for auto-selected alias " + alias);
    }

    @Override
    public String chooseClientAlias(String[] keyTypes, Principal[] issuers, Socket socket) {
        if (mAlias == null) {
            throw new ClientCertificateRequiredException(keyTypes, issuers,
                    socket.getInetAddress().getHostName(), socket.getPort());
        }

        if (K9.DEBUG)
            Log.d(K9.LOG_TAG, "KeyChainKeyManager.chooseClientAlias returning preselected alias "
                    + mAlias);

        return mAlias;
    }

    @Override
    public X509Certificate[] getCertificateChain(String alias) {
        try {
            if (K9.DEBUG)
                Log.d(K9.LOG_TAG, "KeyChainKeyManager.getCertificateChain for " + alias);

            X509Certificate[] chain = KeyChain.getCertificateChain(K9.app, alias);

            if (chain == null || chain.length == 0) {
                throw new IllegalStateException("No certificate chain found for: " + alias);
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
            if (K9.DEBUG)
                Log.d(K9.LOG_TAG, "KeyChainKeyManager.getPrivateKey for " + alias);

            PrivateKey key;

            /*
             * We need to keep reference to the first private key retrieved so
             * it won't get garbage collected. If it will then the whole app
             * will crash on Android < 4.2 with "Fatal signal 11 code=1". See
             * https://code.google.com/p/android/issues/detail?id=62319
             */
            if (sClientCertificateReferenceWorkaround == null
                    && Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
                key = retrieveFirstPrivateKey(alias);
            } else {
                key = KeyChain.getPrivateKey(K9.app, alias);
            }

            if (key == null) {
                throw new IllegalStateException("No private key found for: " + alias);
            }
            return key;
        } catch (KeyChainException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    private synchronized PrivateKey retrieveFirstPrivateKey(String alias)
            throws KeyChainException, InterruptedException {
        PrivateKey key = KeyChain.getPrivateKey(K9.app, alias);
        if (sClientCertificateReferenceWorkaround == null) {
            sClientCertificateReferenceWorkaround = key;
        }
        return key;
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

    public static String interactivelyChooseClientCertificateAlias(Activity activity,
            String[] keyTypes, Principal[] issuers, String hostName, int port,
            String preSelectedAlias) {
        // defined as array to be able to set it inside the callback
        final String[] selectedAlias = new String[1];

        KeyChain.choosePrivateKeyAlias(activity, new KeyChainAliasCallback() {
            @Override
            public void alias(String alias) {
                synchronized (selectedAlias) {
                    if (K9.DEBUG)
                        Log.d(K9.LOG_TAG, "User has selected client certificate alias:" + alias);

                    // see below. not null is condition for breaking out of loop
                    if (alias == null) {
                        alias = "";
                    }
                    selectedAlias[0] = alias;
                    selectedAlias.notifyAll();
                }

            }
        }, keyTypes, issuers, hostName, port, preSelectedAlias);

        synchronized (selectedAlias) {
            while (selectedAlias[0] == null) {
                try {
                    selectedAlias.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            if ("".equals(selectedAlias[0])) {
                selectedAlias[0] = null;
            }
        }

        return selectedAlias[0];
    }
}
