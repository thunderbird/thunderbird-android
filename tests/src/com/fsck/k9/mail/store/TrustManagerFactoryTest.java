package com.fsck.k9.mail.store;

import javax.net.ssl.X509TrustManager;

import com.fsck.k9.net.ssl.TrustManagerFactory;
import com.fsck.k9.security.LocalKeyStore;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import android.test.AndroidTestCase;

/**
 * Test the functionality of {@link TrustManagerFactory}.
 */
public class TrustManagerFactoryTest extends AndroidTestCase {
    public static final String MATCHING_HOST = "k9.example.com";
    public static final String NOT_MATCHING_HOST = "bla.example.com";
    public static final int PORT1 = 993;
    public static final int PORT2 = 465;

    private static final String K9_EXAMPLE_COM_CERT1 =
              "-----BEGIN CERTIFICATE-----\n"
            + "MIICCTCCAXICCQD/R0TV7d0C5TANBgkqhkiG9w0BAQUFADBJMQswCQYDVQQGEwJD\n"
            + "SDETMBEGA1UECBMKU29tZS1TdGF0ZTEMMAoGA1UEChMDSy05MRcwFQYDVQQDEw5r\n"
            + "OS5leGFtcGxlLmNvbTAeFw0xMTA5MDYxOTU3MzVaFw0yMTA5MDMxOTU3MzVaMEkx\n"
            + "CzAJBgNVBAYTAkNIMRMwEQYDVQQIEwpTb21lLVN0YXRlMQwwCgYDVQQKEwNLLTkx\n"
            + "FzAVBgNVBAMTDms5LmV4YW1wbGUuY29tMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCB\n"
            + "iQKBgQCp7FvHRaQaOIu3iyB5GB0PtPCxy/bLlBxBb8p9QsMimX2Yz3SNjWVUzU5N\n"
            + "ggpXmmeGopLAnvZlhWYSx0yIGWwPB44kGK5eaYDRWav+K+XXgdNCJij1UWPSmFwZ\n"
            + "hUoNbrahco5AFw0jC1qi+3Dht6Y64nfNzTOYTcm1Pz4tqXiADQIDAQABMA0GCSqG\n"
            + "SIb3DQEBBQUAA4GBAIPsgd6fuFRojSOAcUyhaoKaY5hXJf8d7R3AYWxcAPYmn6g7\n"
            + "3Zms+f7/CH0y/tM81oBTlq9ZLbrJyLzC7vG1pqWHMNaK7miAho22IRuk+HwvL6OA\n"
            + "uH3x3W1/mH4ci268cIFVmofID0nYLTqOxBTczfYhI7q0VBUXqv/bZ+3bVMSh\n"
            + "-----END CERTIFICATE-----\n";

    private static final String K9_EXAMPLE_COM_CERT2 =
              "-----BEGIN CERTIFICATE-----\n"
            + "MIICCTCCAXICCQDMryqq0gZ80jANBgkqhkiG9w0BAQUFADBJMQswCQYDVQQGEwJD\n"
            + "SDETMBEGA1UECBMKU29tZS1TdGF0ZTEMMAoGA1UEChMDSy05MRcwFQYDVQQDEw5r\n"
            + "OS5leGFtcGxlLmNvbTAeFw0xMTA5MDYyMDAwNTVaFw0yMTA5MDMyMDAwNTVaMEkx\n"
            + "CzAJBgNVBAYTAkNIMRMwEQYDVQQIEwpTb21lLVN0YXRlMQwwCgYDVQQKEwNLLTkx\n"
            + "FzAVBgNVBAMTDms5LmV4YW1wbGUuY29tMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCB\n"
            + "iQKBgQDOLzRucC3tuXL/NthnGkgTnVn03balrvYPkABvvrG83Dpp5ipIC/iPsQvw\n"
            + "pvqypSNHqrloEB7o3obQ8tiRDtbOsNQ7gKJ+YoD1drDNClV0pBvr7mvRgA2AcDpw\n"
            + "CTLKwVIyKmE+rm3vl8CWFd9CqHcYQ3Mc1KXXasN4DEAzZ/sHRwIDAQABMA0GCSqG\n"
            + "SIb3DQEBBQUAA4GBAFDcHFpmZ9SUrc0WayrKNUpSaHLRG94uzIx0VUMLROcXEEWU\n"
            + "soRw1RfoSBkcy2SEjB4CAvex6qAiOT3ubXuL+BYFav/uU8JPWZ9ovSAYqBZ9aUJo\n"
            + "G6A2hvA1lpvP97qQ/NFaGQ38XqSykZamZwSx3PlZUM/i9S9n/3MfuuXWqtLC\n"
            + "-----END CERTIFICATE-----\n";


    private X509Certificate mCert1;
    private X509Certificate mCert2;
    private File mKeyStoreFile;
    private LocalKeyStore mKeyStore;


    public TrustManagerFactoryTest() throws CertificateException {
        CertificateFactory certFactory = CertificateFactory.getInstance("X509");
        mCert1 = (X509Certificate) certFactory.generateCertificate(
                new ByteArrayInputStream(K9_EXAMPLE_COM_CERT1.getBytes()));
        mCert2 = (X509Certificate) certFactory.generateCertificate(
                new ByteArrayInputStream(K9_EXAMPLE_COM_CERT2.getBytes()));
    }

    @Override
    public void setUp() throws Exception {
        mKeyStoreFile = File.createTempFile("localKeyStore", null, getContext()
                .getCacheDir());
        mKeyStore = LocalKeyStore.getInstance();
        mKeyStore.setKeyStoreFile(mKeyStoreFile);
    }

    @Override
    protected void tearDown() {
        mKeyStoreFile.delete();
    }

    /**
     * Checks if TrustManagerFactory supports a host with different certificates for different
     * services (e.g. SMTP and IMAP).
     *
     * <p>
     * This test is to make sure entries in the keystore file aren't overwritten.
     * See <a href="https://code.google.com/p/k9mail/issues/detail?id=1326">Issue 1326</a>.
     * </p>
     *
     * @throws Exception
     *         if anything goes wrong
     */
    public void testDifferentCertificatesOnSameServer() throws Exception {
        mKeyStore.addCertificate(NOT_MATCHING_HOST, PORT1, mCert1);
        mKeyStore.addCertificate(NOT_MATCHING_HOST, PORT2, mCert2);

        X509TrustManager trustManager1 = TrustManagerFactory.get(NOT_MATCHING_HOST, PORT1, true);
        X509TrustManager trustManager2 = TrustManagerFactory.get(NOT_MATCHING_HOST, PORT2, true);
        trustManager2.checkServerTrusted(new X509Certificate[] { mCert2 }, "authType");
        trustManager1.checkServerTrusted(new X509Certificate[] { mCert1 }, "authType");
    }

    public void testSelfSignedCertificateMatchingHost() throws Exception {
        mKeyStore.addCertificate(MATCHING_HOST, PORT1, mCert1);
        X509TrustManager trustManager = TrustManagerFactory.get(MATCHING_HOST, PORT1, true);
        trustManager.checkServerTrusted(new X509Certificate[] { mCert1 }, "authType");
    }

    public void testSelfSignedCertificateNotMatchingHost() throws Exception {
        mKeyStore.addCertificate(NOT_MATCHING_HOST, PORT1, mCert1);
        X509TrustManager trustManager = TrustManagerFactory.get(NOT_MATCHING_HOST, PORT1, true);
        trustManager.checkServerTrusted(new X509Certificate[] { mCert1 }, "authType");
    }

    public void testWrongCertificate() throws Exception {
        mKeyStore.addCertificate(MATCHING_HOST, PORT1, mCert1);
        X509TrustManager trustManager = TrustManagerFactory.get(MATCHING_HOST, PORT1, true);
        boolean certificateValid;
        try {
            trustManager.checkServerTrusted(new X509Certificate[] { mCert2 }, "authType");
            certificateValid = true;
        } catch (CertificateException e) {
            certificateValid = false;
        }
        assertFalse("The certificate should have been rejected but wasn't", certificateValid);
    }

    public void testCertificateOfOtherHost() throws Exception {
        mKeyStore.addCertificate(MATCHING_HOST, PORT1, mCert1);
        mKeyStore.addCertificate(MATCHING_HOST, PORT2, mCert2);

        X509TrustManager trustManager = TrustManagerFactory.get(MATCHING_HOST, PORT1, true);
        boolean certificateValid;
        try {
            trustManager.checkServerTrusted(new X509Certificate[] { mCert2 }, "authType");
            certificateValid = true;
        } catch (CertificateException e) {
            certificateValid = false;
        }
        assertFalse("The certificate should have been rejected but wasn't", certificateValid);
    }

    public void testKeyStoreLoading() throws Exception {
        mKeyStore.addCertificate(MATCHING_HOST, PORT1, mCert1);
        mKeyStore.addCertificate(NOT_MATCHING_HOST, PORT2, mCert2);
        assertTrue(mKeyStore.isValidCertificate(mCert1, MATCHING_HOST, PORT1));
        assertTrue(mKeyStore.isValidCertificate(mCert2, NOT_MATCHING_HOST, PORT2));

        // reload store from same file
        mKeyStore.setKeyStoreFile(mKeyStoreFile);
        assertTrue(mKeyStore.isValidCertificate(mCert1, MATCHING_HOST, PORT1));
        assertTrue(mKeyStore.isValidCertificate(mCert2, NOT_MATCHING_HOST, PORT2));

        // reload store from empty file
        mKeyStoreFile.delete();
        mKeyStore.setKeyStoreFile(mKeyStoreFile);
        assertFalse(mKeyStore.isValidCertificate(mCert1, MATCHING_HOST, PORT1));
        assertFalse(mKeyStore.isValidCertificate(mCert2, NOT_MATCHING_HOST, PORT2));
    }
}
