package com.fsck.k9.mail.store;

import javax.net.ssl.X509TrustManager;
import com.fsck.k9.K9;
import java.io.File;
import java.lang.reflect.Method;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.concurrent.CountDownLatch;

import android.app.Application;
import android.content.Context;
import android.content.res.AssetManager;
import android.test.AndroidTestCase;

/**
 * Test the functionality of {@link TrustManagerFactory}.
 */
public class TrustManagerFactoryTest extends AndroidTestCase {
    public static final String MATCHING_HOST = "k9.example.com";
    public static final String NOT_MATCHING_HOST = "bla.example.com";
    public static final int PORT1 = 993;
    public static final int PORT2 = 465;


    private Context mTestContext;
    private X509Certificate mCert1;
    private X509Certificate mCert2;
    private X509Certificate mCaCert;
    private X509Certificate mCert3;
    private X509Certificate mDigiCert;
    private X509Certificate mGithubCert;


    @Override
    public void setUp() throws Exception {
        waitForAppInitialization();

        // Hack to make sure TrustManagerFactory.loadKeyStore() can create the key store file
        K9.app = new DummyApplication(getContext());

        // Source: https://kmansoft.wordpress.com/2011/04/18/accessing-resources-in-an-androidtestcase/
        Method m = AndroidTestCase.class.getMethod("getTestContext", new Class[] {});
        mTestContext = (Context) m.invoke(this, (Object[]) null);

        // Delete the key store file to make sure we start without any stored certificates
        File keyStoreDir = getContext().getDir("KeyStore", Context.MODE_PRIVATE);
        new File(keyStoreDir + File.separator + "KeyStore.bks").delete();

        // Load the empty key store file
        TrustManagerFactory.loadKeyStore();

        // Load certificates
        AssetManager assets = mTestContext.getAssets();

        CertificateFactory certFactory = CertificateFactory.getInstance("X509");
        mCert1 = (X509Certificate) certFactory.generateCertificate(assets.open("cert1.der"));
        mCert2 = (X509Certificate) certFactory.generateCertificate(assets.open("cert2.der"));

        mCaCert = (X509Certificate) certFactory.generateCertificate(assets.open("cacert.der"));
        mCert3 = (X509Certificate) certFactory.generateCertificate(assets.open("cert3.der"));

        mDigiCert = (X509Certificate) certFactory.generateCertificate(assets.open("digicert.der"));
        mGithubCert = (X509Certificate) certFactory.generateCertificate(assets.open("github.der"));
    }

    private void waitForAppInitialization() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);

        K9.registerApplicationAware(new K9.ApplicationAware() {
            @Override
            public void initializeComponent(Application application) {
                latch.countDown();
            }
        });

        latch.await();
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
        TrustManagerFactory.addCertificate(NOT_MATCHING_HOST, PORT1, mCert1);
        TrustManagerFactory.addCertificate(NOT_MATCHING_HOST, PORT2, mCert2);

        X509TrustManager trustManager1 = TrustManagerFactory.get(NOT_MATCHING_HOST, PORT1, true);
        X509TrustManager trustManager2 = TrustManagerFactory.get(NOT_MATCHING_HOST, PORT2, true);
        trustManager2.checkServerTrusted(new X509Certificate[] { mCert2 }, "authType");
        trustManager1.checkServerTrusted(new X509Certificate[] { mCert1 }, "authType");
    }

    public void testSelfSignedCertificateMatchingHost() throws Exception {
        TrustManagerFactory.addCertificate(MATCHING_HOST, PORT1, mCert1);
        X509TrustManager trustManager = TrustManagerFactory.get(MATCHING_HOST, PORT1, true);
        trustManager.checkServerTrusted(new X509Certificate[] { mCert1 }, "authType");
    }

    public void testSelfSignedCertificateNotMatchingHost() throws Exception {
        TrustManagerFactory.addCertificate(NOT_MATCHING_HOST, PORT1, mCert1);
        X509TrustManager trustManager = TrustManagerFactory.get(NOT_MATCHING_HOST, PORT1, true);
        trustManager.checkServerTrusted(new X509Certificate[] { mCert1 }, "authType");
    }

    public void testWrongCertificate() throws Exception {
        TrustManagerFactory.addCertificate(MATCHING_HOST, PORT1, mCert1);
        X509TrustManager trustManager = TrustManagerFactory.get(MATCHING_HOST, PORT1, true);
        assertCertificateRejection(trustManager, new X509Certificate[] { mCert2 });
    }

    public void testCertificateOfOtherHost() throws Exception {
        TrustManagerFactory.addCertificate(MATCHING_HOST, PORT1, mCert1);
        TrustManagerFactory.addCertificate(MATCHING_HOST, PORT2, mCert2);

        X509TrustManager trustManager = TrustManagerFactory.get(MATCHING_HOST, PORT1, true);
        assertCertificateRejection(trustManager, new X509Certificate[] { mCert2 });
    }

    public void testUntrustedCertificateChain() throws Exception {
        X509TrustManager trustManager = TrustManagerFactory.get(MATCHING_HOST, PORT1, true);
        assertCertificateRejection(trustManager, new X509Certificate[] { mCert3, mCaCert });
    }

    public void testLocallyTrustedCertificateChain() throws Exception {
        TrustManagerFactory.addCertificate(MATCHING_HOST, PORT1, mCert3);

        X509TrustManager trustManager = TrustManagerFactory.get(MATCHING_HOST, PORT1, true);
        trustManager.checkServerTrusted(new X509Certificate[] { mCert3, mCaCert }, "authType");
    }

    public void testLocallyTrustedCertificateChainNotMatchingHost() throws Exception {
        TrustManagerFactory.addCertificate(NOT_MATCHING_HOST, PORT1, mCert3);

        X509TrustManager trustManager = TrustManagerFactory.get(NOT_MATCHING_HOST, PORT1, true);
        trustManager.checkServerTrusted(new X509Certificate[] { mCert3, mCaCert }, "authType");
    }

    public void testGloballyTrustedCertificateChain() throws Exception {
        X509TrustManager trustManager = TrustManagerFactory.get("github.com", PORT1, true);
        X509Certificate[] certificates = new X509Certificate[] { mGithubCert, mDigiCert };
        trustManager.checkServerTrusted(certificates, "authType");
    }

    public void testGloballyTrustedCertificateNotMatchingHost() throws Exception {
        X509TrustManager trustManager = TrustManagerFactory.get(NOT_MATCHING_HOST, PORT1, true);
        assertCertificateRejection(trustManager, new X509Certificate[] { mGithubCert, mDigiCert});
    }

    public void testGloballyTrustedCertificateNotMatchingHostOverride() throws Exception {
        TrustManagerFactory.addCertificate(MATCHING_HOST, PORT1, mGithubCert);

        X509TrustManager trustManager = TrustManagerFactory.get(MATCHING_HOST, PORT1, true);
        X509Certificate[] certificates = new X509Certificate[] { mGithubCert, mDigiCert };
        trustManager.checkServerTrusted(certificates, "authType");
    }

    private void assertCertificateRejection(X509TrustManager trustManager,
            X509Certificate[] certificates) {
        boolean certificateValid;
        try {
            trustManager.checkServerTrusted(certificates, "authType");
            certificateValid = true;
        } catch (CertificateException e) {
            certificateValid = false;
        }
        assertFalse("The certificate should have been rejected but wasn't", certificateValid);
    }

    private static class DummyApplication extends Application {
        private final Context mContext;

        DummyApplication(Context context) {
            mContext = context;
        }

        public File getDir(String name, int mode) {
            return mContext.getDir(name, mode);
        }
    }
}
