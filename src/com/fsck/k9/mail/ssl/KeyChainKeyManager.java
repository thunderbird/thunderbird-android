
package com.fsck.k9.mail.ssl;

import java.net.Socket;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedKeyManager;
import javax.security.auth.x500.X500Principal;

import android.content.Context;
import android.os.Build;
import android.security.KeyChain;
import android.security.KeyChainException;
import android.util.Log;

import com.fsck.k9.mail.CertificateValidationException;
import com.fsck.k9.mail.MessagingException;

import static com.fsck.k9.mail.K9MailLib.LOG_TAG;
import static com.fsck.k9.mail.CertificateValidationException.Reason;
import static com.fsck.k9.mail.CertificateValidationException.Reason.RetrievalFailure;

/**
 * For client certificate authentication! Provide private keys and certificates
 * during the TLS handshake using the Android 4.0 KeyChain API.
 */
class KeyChainKeyManager extends X509ExtendedKeyManager {

    private static PrivateKey sClientCertificateReferenceWorkaround;


    private static synchronized void savePrivateKeyReference(PrivateKey privateKey) {
        if (sClientCertificateReferenceWorkaround == null) {
            sClientCertificateReferenceWorkaround = privateKey;
        }
    }


    private final String mAlias;
    private final X509Certificate[] mChain;
    private final PrivateKey mPrivateKey;


    /**
     * @param alias  Must not be null nor empty
     * @throws MessagingException
     *          Indicates an error in retrieving the certificate for the alias
     *          (likely because the alias is invalid or the certificate was deleted)
     */
    public KeyChainKeyManager(Context context, String alias) throws MessagingException {
        mAlias = alias;

        try {
            mChain = fetchCertificateChain(context, alias);
            mPrivateKey = fetchPrivateKey(context, alias);
        } catch (KeyChainException e) {
            // The certificate was possibly deleted.  Notify user of error.
            throw new CertificateValidationException(e.getMessage(), RetrievalFailure, alias);
        } catch (InterruptedException e) {
            throw new CertificateValidationException(e.getMessage(), RetrievalFailure, alias);
        }
    }

    private X509Certificate[] fetchCertificateChain(Context context, String alias)
            throws KeyChainException, InterruptedException, MessagingException {

        X509Certificate[] chain = KeyChain.getCertificateChain(context, alias);
        if (chain == null || chain.length == 0) {
            throw new MessagingException("No certificate chain found for: " + alias);
        }
        try {
            for (X509Certificate certificate : chain) {
                certificate.checkValidity();
            }
        } catch (CertificateException e) {
            throw new CertificateValidationException(e.getMessage(), Reason.Expired, alias);
        }

        return chain;
    }

    private PrivateKey fetchPrivateKey(Context context, String alias) throws KeyChainException,
            InterruptedException, MessagingException {

        PrivateKey privateKey = KeyChain.getPrivateKey(context, alias);
        if (privateKey == null) {
            throw new MessagingException("No private key found for: " + alias);
        }

        /*
         * We need to keep reference to the first private key retrieved so
         * it won't get garbage collected. If it will then the whole app
         * will crash on Android < 4.2 with "Fatal signal 11 code=1". See
         * https://code.google.com/p/android/issues/detail?id=62319
         */
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            savePrivateKeyReference(privateKey);
        }

        return privateKey;
    }

    @Override
    public String chooseClientAlias(String[] keyTypes, Principal[] issuers, Socket socket) {
        return chooseAlias(keyTypes, issuers);
    }

    @Override
    public X509Certificate[] getCertificateChain(String alias) {
        return (mAlias.equals(alias) ? mChain : null);
    }

    @Override
    public PrivateKey getPrivateKey(String alias) {
        return (mAlias.equals(alias) ? mPrivateKey : null);
    }

    @Override
    public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
        return chooseAlias(new String[] { keyType }, issuers);
    }

    @Override
    public String[] getClientAliases(String keyType, Principal[] issuers) {
        final String al = chooseAlias(new String[] { keyType }, issuers);
        return (al == null ? null : new String[] { al });
    }

    @Override
    public String[] getServerAliases(String keyType, Principal[] issuers) {
        final String al = chooseAlias(new String[] { keyType }, issuers);
        return (al == null ? null : new String[] { al });
    }

    @Override
    public String chooseEngineClientAlias(String[] keyTypes, Principal[] issuers, SSLEngine engine) {
        return chooseAlias(keyTypes, issuers);
    }

    @Override
    public String chooseEngineServerAlias(String keyType, Principal[] issuers, SSLEngine engine) {
        return chooseAlias(new String[] { keyType }, issuers);
    }

    private String chooseAlias(String[] keyTypes, Principal[] issuers) {
        if (keyTypes == null || keyTypes.length == 0) {
            return null;
        }
        final X509Certificate cert = mChain[0];
        final String certKeyAlg = cert.getPublicKey().getAlgorithm();
        final String certSigAlg = cert.getSigAlgName().toUpperCase(Locale.US);
        for (String keyAlgorithm : keyTypes) {
            if (keyAlgorithm == null) {
                continue;
            }
            final String sigAlgorithm;
            // handle cases like EC_EC and EC_RSA
            int index = keyAlgorithm.indexOf('_');
            if (index == -1) {
                sigAlgorithm = null;
            } else {
                sigAlgorithm = keyAlgorithm.substring(index + 1);
                keyAlgorithm = keyAlgorithm.substring(0, index);
            }
            // key algorithm does not match
            if (!certKeyAlg.equals(keyAlgorithm)) {
                continue;
            }
            /*
             * TODO find a more reliable test for signature
             * algorithm. Unfortunately value varies with
             * provider. For example for "EC" it could be
             * "SHA1WithECDSA" or simply "ECDSA".
             */
            // sig algorithm does not match
            if (sigAlgorithm != null && certSigAlg != null
                    && !certSigAlg.contains(sigAlgorithm)) {
                continue;
            }
            // no issuers to match
            if (issuers == null || issuers.length == 0) {
                return mAlias;
            }
            List<Principal> issuersList = Arrays.asList(issuers);
            // check that a certificate in the chain was issued by one of the specified issuers
            for (X509Certificate certFromChain : mChain) {
                /*
                 * Note use of X500Principal from
                 * getIssuerX500Principal as opposed to Principal
                 * from getIssuerDN. Principal.equals test does
                 * not work in the case where
                 * xcertFromChain.getIssuerDN is a bouncycastle
                 * org.bouncycastle.jce.X509Principal.
                 */
                X500Principal issuerFromChain = certFromChain.getIssuerX500Principal();
                if (issuersList.contains(issuerFromChain)) {
                    return mAlias;
                }
            }
            Log.w(LOG_TAG, "Client certificate " + mAlias + " not issued by any of the requested issuers");
            return null;
        }
        Log.w(LOG_TAG, "Client certificate " + mAlias + " does not match any of the requested key types");
        return null;
    }
}
