
package com.fsck.k9.helper;

import java.net.Socket;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import com.fsck.k9.mail.ClientCertificateError;
import com.fsck.k9.mail.ClientCertificateException;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedKeyManager;
import javax.security.auth.x500.X500Principal;

import android.content.Context;
import android.security.KeyChain;
import android.security.KeyChainException;

import net.thunderbird.core.common.exception.MessagingException;
import net.thunderbird.core.logging.legacy.Log;


/**
 * For client certificate authentication! Provide private keys and certificates
 * during the TLS handshake using the Android 4.0 KeyChain API.
 */
class KeyChainKeyManager extends X509ExtendedKeyManager {
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
        } catch (KeyChainException | InterruptedException e) {
            throw new ClientCertificateException(ClientCertificateError.RetrievalFailure, e);
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
            throw new ClientCertificateException(ClientCertificateError.CertificateExpired, e);
        }

        return chain;
    }

    private PrivateKey fetchPrivateKey(Context context, String alias) throws KeyChainException,
            InterruptedException, MessagingException {

        PrivateKey privateKey = KeyChain.getPrivateKey(context, alias);
        if (privateKey == null) {
            throw new MessagingException("No private key found for: " + alias);
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
            Log.w("Client certificate %s not issued by any of the requested issuers", mAlias);
            return null;
        }
        Log.w("Client certificate %s does not match any of the requested key types", mAlias);
        return null;
    }
}
