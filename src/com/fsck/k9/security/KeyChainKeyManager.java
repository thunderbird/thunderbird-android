
package com.fsck.k9.security;

import java.net.Socket;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import javax.net.ssl.X509ExtendedKeyManager;

import android.os.Build;
import android.security.KeyChain;
import android.security.KeyChainException;
import android.util.Log;

import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.mail.CertificateValidationException;
import com.fsck.k9.mail.MessagingException;

/**
 * For client certificate authentication! Provide private keys and certificates
 * during the TLS handshake using the Android 4.0 KeyChain API.
 */
public class KeyChainKeyManager extends X509ExtendedKeyManager {

    private static PrivateKey sClientCertificateReferenceWorkaround;

    private String mAlias;

    /**
     * @param alias  Must not be null nor empty
     * @throws MessagingException
     *          Indicates an error in retrieving the certificate for the alias
     *          (likely because the alias is invalid or the certificate was deleted)
     */
    public KeyChainKeyManager(String alias) throws MessagingException {
        mAlias = alias;

        // Check for invalid alias (the user may have deleted the certificate)
        try {
            KeyChain.getCertificateChain(K9.app, alias);
        } catch (KeyChainException e) {
            throw new CertificateValidationException(K9.app.getString(
                    R.string.client_certificate_retrieval_failure, alias), e);
        } catch (InterruptedException e) {
            throw new MessagingException(K9.app.getString(
                    R.string.client_certificate_retrieval_failure, alias), e);
        }
    }

    @Override
    public String chooseClientAlias(String[] keyTypes, Principal[] issuers, Socket socket) {
        return mAlias;
    }

    @Override
    public X509Certificate[] getCertificateChain(String alias) {
        try {
            if (K9.DEBUG)
                Log.d(K9.LOG_TAG, "KeyChainKeyManager.getCertificateChain for " + alias);

            X509Certificate[] chain = KeyChain.getCertificateChain(K9.app, alias);

            if (chain == null || chain.length == 0) {
                Log.w(K9.LOG_TAG, "No certificate chain found for: " + alias);
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
                Log.w(K9.LOG_TAG, "No private key found for: " + alias);
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
}
