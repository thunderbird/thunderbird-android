
package com.fsck.k9.mail.store;

import android.app.Application;
import android.content.Context;
import android.net.http.DomainNameChecker;
import android.util.Log;
import com.fsck.k9.K9;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public final class TrustManagerFactory
{
    private static final String LOG_TAG = "TrustManagerFactory";

    private static X509TrustManager defaultTrustManager;
    private static X509TrustManager unsecureTrustManager;
    private static X509TrustManager localTrustManager;

    private static X509Certificate[] lastCertChain = null;

    private static File keyStoreFile;
    private static KeyStore keyStore;


    private static class SimpleX509TrustManager implements X509TrustManager
    {
        public void checkClientTrusted(X509Certificate[] chain, String authType)
        throws CertificateException
        {
        }

        public void checkServerTrusted(X509Certificate[] chain, String authType)
        throws CertificateException
        {
        }

        public X509Certificate[] getAcceptedIssuers()
        {
            return null;
        }
    }

    private static class SecureX509TrustManager implements X509TrustManager
    {
        private String mHost;
        private static SecureX509TrustManager me;

        private SecureX509TrustManager()
        {
        }

        public static X509TrustManager getInstance(String host)
        {
            if (me == null)
            {
                me = new SecureX509TrustManager();
            }
            me.mHost = host;
            return me;
        }

        public void setHost(String host)
        {
            mHost = host;
        }

        public void checkClientTrusted(X509Certificate[] chain, String authType)
        throws CertificateException
        {
            defaultTrustManager.checkClientTrusted(chain, authType);
        }

        public void checkServerTrusted(X509Certificate[] chain, String authType)
        throws CertificateException
        {
            TrustManagerFactory.setLastCertChain(chain);
            try
            {
                defaultTrustManager.checkServerTrusted(chain, authType);
            }
            catch (CertificateException e)
            {
                localTrustManager.checkServerTrusted(new X509Certificate[] {chain[0]}, authType);
            }
            if (!DomainNameChecker.match(chain[0], mHost))
            {
                try
                {
                    String dn = chain[0].getSubjectDN().toString();
                    if ((dn != null) && (dn.equalsIgnoreCase(keyStore.getCertificateAlias(chain[0]))))
                    {
                        return;
                    }
                }
                catch (KeyStoreException e)
                {
                    throw new CertificateException("Certificate cannot be verified; KeyStore Exception: " + e);
                }
                throw new CertificateException("Certificate domain name does not match "
                                               + mHost);
            }
        }

        public X509Certificate[] getAcceptedIssuers()
        {
            return defaultTrustManager.getAcceptedIssuers();
        }

    }

    static
    {
        try
        {
            javax.net.ssl.TrustManagerFactory tmf = javax.net.ssl.TrustManagerFactory.getInstance("X509");
            Application app = K9.app;
            keyStoreFile = new File(app.getDir("KeyStore", Context.MODE_PRIVATE) + File.separator + "KeyStore.bks");
            keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            java.io.FileInputStream fis;
            try
            {
                fis = new java.io.FileInputStream(keyStoreFile);
            }
            catch (FileNotFoundException e1)
            {
                fis = null;
            }
            try
            {
                keyStore.load(fis, "".toCharArray());
                //if (fis != null) {
                // fis.close();
                //}
            }
            catch (IOException e)
            {
                Log.e(LOG_TAG, "KeyStore IOException while initializing TrustManagerFactory ", e);
                keyStore = null;
            }
            catch (CertificateException e)
            {
                Log.e(LOG_TAG, "KeyStore CertificateException while initializing TrustManagerFactory ", e);
                keyStore = null;
            }
            tmf.init(keyStore);
            TrustManager[] tms = tmf.getTrustManagers();
            if (tms != null)
            {
                for (TrustManager tm : tms)
                {
                    if (tm instanceof X509TrustManager)
                    {
                        localTrustManager = (X509TrustManager)tm;
                        break;
                    }
                }
            }
            tmf = javax.net.ssl.TrustManagerFactory.getInstance("X509");
            tmf.init((KeyStore)null);
            tms = tmf.getTrustManagers();
            if (tms != null)
            {
                for (TrustManager tm : tms)
                {
                    if (tm instanceof X509TrustManager)
                    {
                        defaultTrustManager = (X509TrustManager) tm;
                        break;
                    }
                }
            }

        }
        catch (NoSuchAlgorithmException e)
        {
            Log.e(LOG_TAG, "Unable to get X509 Trust Manager ", e);
        }
        catch (KeyStoreException e)
        {
            Log.e(LOG_TAG, "Key Store exception while initializing TrustManagerFactory ", e);
        }
        unsecureTrustManager = new SimpleX509TrustManager();
    }

    private TrustManagerFactory()
    {
    }

    public static X509TrustManager get(String host, boolean secure)
    {
        return secure ? SecureX509TrustManager.getInstance(host) :
               unsecureTrustManager;
    }

    public static KeyStore getKeyStore()
    {
        return keyStore;
    }

    public static void setLastCertChain(X509Certificate[] chain)
    {
        lastCertChain = chain;
    }
    public static X509Certificate[] getLastCertChain()
    {
        return lastCertChain;
    }

    public static void addCertificateChain(String alias, X509Certificate[] chain) throws CertificateException
    {
        try
        {
            javax.net.ssl.TrustManagerFactory tmf = javax.net.ssl.TrustManagerFactory.getInstance("X509");
            for (int i = 0; i < chain.length; i++)
            {
                keyStore.setCertificateEntry
                (chain[i].getSubjectDN().toString(), chain[i]);
            }

            tmf.init(keyStore);
            TrustManager[] tms = tmf.getTrustManagers();
            if (tms != null)
            {
                for (TrustManager tm : tms)
                {
                    if (tm instanceof X509TrustManager)
                    {
                        localTrustManager = (X509TrustManager) tm;
                        break;
                    }
                }
            }
            java.io.FileOutputStream keyStoreStream;
            try
            {
                keyStoreStream = new java.io.FileOutputStream(keyStoreFile);
                keyStore.store(keyStoreStream, "".toCharArray());
                keyStoreStream.close();
            }
            catch (FileNotFoundException e)
            {
                throw new CertificateException("Unable to write KeyStore: " + e.getMessage());
            }
            catch (CertificateException e)
            {
                throw new CertificateException("Unable to write KeyStore: " + e.getMessage());
            }
            catch (IOException e)
            {
                throw new CertificateException("Unable to write KeyStore: " + e.getMessage());
            }

        }
        catch (NoSuchAlgorithmException e)
        {
            Log.e(LOG_TAG, "Unable to get X509 Trust Manager ", e);
        }
        catch (KeyStoreException e)
        {
            Log.e(LOG_TAG, "Key Store exception while initializing TrustManagerFactory ", e);
        }
    }
}
