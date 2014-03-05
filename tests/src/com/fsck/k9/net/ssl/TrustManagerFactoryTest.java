package com.fsck.k9.net.ssl;

import javax.net.ssl.X509TrustManager;

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

    private static final String CA_CERT =
            "-----BEGIN CERTIFICATE-----\n"
          + "MIIDbTCCAlWgAwIBAgIJANCdQ+Cwnyg+MA0GCSqGSIb3DQEBBQUAME0xCzAJBgNV\n"
          + "BAYTAkNIMRMwEQYDVQQIDApTb21lLVN0YXRlMQwwCgYDVQQKDANLLTkxGzAZBgNV\n"
          + "BAMMEnRlc3QtY2EuazltYWlsLm9yZzAeFw0xMzEyMDIxMjUwNThaFw0yMzExMzAx\n"
          + "MjUwNThaME0xCzAJBgNVBAYTAkNIMRMwEQYDVQQIDApTb21lLVN0YXRlMQwwCgYD\n"
          + "VQQKDANLLTkxGzAZBgNVBAMMEnRlc3QtY2EuazltYWlsLm9yZzCCASIwDQYJKoZI\n"
          + "hvcNAQEBBQADggEPADCCAQoCggEBAJ+YLg9enfFk5eba6B3LtQzUE7GiR2tIpQSi\n"
          + "zHMtHzn8KUnRDiGwC8VnSuWCOX7hXyQ0P6i2+DVRVBYOAeDCNMZHOq1hRqI66B33\n"
          + "QqLfkBnJAIDeLqfqlgigHs1+//7eagVA6Z38ZFre3PFuKnK9NCwS+gz7PKw/poIG\n"
          + "/FZP+ltMlkwvPww4S8SMlY6RXXH09+S/uM8aG6DUBT298eoAXTbSEIeaNhwBHZPe\n"
          + "rXqqzd8QDAIE9BFXSkh/BQiVEFDPSBMSdmUzUAsT2aM8osntnKWY5/G7B60wutvA\n"
          + "jYCULgtR6lR6jIDbG3ECHVDsTWR+Pgl+h1zeyERhN5iG1ffOtLUCAwEAAaNQME4w\n"
          + "HQYDVR0OBBYEFBlUYiTGlOu9zIPx8Q13xcnDL5QpMB8GA1UdIwQYMBaAFBlUYiTG\n"
          + "lOu9zIPx8Q13xcnDL5QpMAwGA1UdEwQFMAMBAf8wDQYJKoZIhvcNAQEFBQADggEB\n"
          + "AJ6oC6O6I6p0vgA4+7dfyxKX745zl/fK6IVHV/GO75mLjVdyw00USbHGHAmZM5C6\n"
          + "eCKVV83m/Re5lHf8ZBjc+3rWdGCEjwyUwvDeUvzpcKF3wPxYDUOOqSI+np1cxj6q\n"
          + "6+XI5QXwyUObWtWyw1GOpLuFPbxny/TlRWvk8AfOaLANg3UhvITNZMdMHoQ2sJ3u\n"
          + "MrQ+CHe/Tal2MkwiCrYT91f3YWVaswiEAxpqxnwuSXnYyaJpqMCcA1txBDgX84FP\n"
          + "dSIM4ut+QltV2Tlx0lpH43dvttAwkPB+iL7ZF6zUki/Nq5aKyNoHOL88TACe18Lq\n"
          + "zOztD2HZfxhIz3uH2gXmqUo=\n"
          + "-----END CERTIFICATE-----\n";

    private static final String CERT3 =
            "-----BEGIN CERTIFICATE-----\n"
          + "MIIDjDCCAnSgAwIBAgIBATANBgkqhkiG9w0BAQUFADBNMQswCQYDVQQGEwJDSDET\n"
          + "MBEGA1UECAwKU29tZS1TdGF0ZTEMMAoGA1UECgwDSy05MRswGQYDVQQDDBJ0ZXN0\n"
          + "LWNhLms5bWFpbC5vcmcwHhcNMTMxMjAyMTMxNzEyWhcNMjMxMTMwMTMxNzEyWjBJ\n"
          + "MQswCQYDVQQGEwJDSDETMBEGA1UECAwKU29tZS1TdGF0ZTEMMAoGA1UECgwDSy05\n"
          + "MRcwFQYDVQQDDA5rOS5leGFtcGxlLmNvbTCCASIwDQYJKoZIhvcNAQEBBQADggEP\n"
          + "ADCCAQoCggEBAL9OvWtLcp6bd40Hai6A6cCmJRwn3mwcTB8E41iEQgQexqx/f9RR\n"
          + "BuQi2s80k/vXq8QU2GbwGiPkBBXMUHuiT27Lsoj8kMOnH5BXeKLaWDiMpvNqfent\n"
          + "UzBXSIOK6Yu9UtlU0MzAuYxXaunrXoS5Dejrbz743P9yW8hx7pANNU0Qfck+ekR7\n"
          + "Q4PWNgfbFHrnvcobzuFzJeWg8x9iTTsVGIaX9AVMjMUlIKvhhOWTlcTJHKzU67sp\n"
          + "OLzwH9IJ3hqwdmsgZu5D/2AZlYlpFk6AlnoxNhfy9m+T41P8+iWDYCJoxvf3d6gl\n"
          + "TlZ1FL0PzPReXeAgugyJ1qx5gJ9Vhf/rBaUCAwEAAaN7MHkwCQYDVR0TBAIwADAs\n"
          + "BglghkgBhvhCAQ0EHxYdT3BlblNTTCBHZW5lcmF0ZWQgQ2VydGlmaWNhdGUwHQYD\n"
          + "VR0OBBYEFPm9hbTbfmcnjjfOzrec/TrvsS5ZMB8GA1UdIwQYMBaAFBlUYiTGlOu9\n"
          + "zIPx8Q13xcnDL5QpMA0GCSqGSIb3DQEBBQUAA4IBAQAgvYQoCEklJNXBwLuWpSMx\n"
          + "CQrVxLI1XsYRzqMs0kUgM59OhwAPwdSR+UEuyXQ8QGKwSt1d//DkdhzQDATXSBYc\n"
          + "VHr16ocYPGNd/VNo7BoUCvykp3cCH3WxYYpAugXbLU8RBJzQwCM75SLQtFe20qfI\n"
          + "LErbrmKONtMk3Rfg6XtLLcaOVh1A3q13CKqDvwtZT4oo56EJOvkBkzlCvTuxJb6s\n"
          + "FD9pwROFpIN8O54C333tZzj4TDP4g9zb3sofAJ4U0osfQAXekZJdZETFGJsU6TIM\n"
          + "Dcf5/G8bZe2DnavBQfML1wI5d7NUWE8CWb95SsIvFXI0qZE0oIR+axBVl9u97uaO\n"
          + "-----END CERTIFICATE-----\n";

    private static final String STARFIELD_CERT =
            "-----BEGIN CERTIFICATE-----\n" +
            "MIIFBzCCA++gAwIBAgICAgEwDQYJKoZIhvcNAQEFBQAwaDELMAkGA1UEBhMCVVMx\n" +
            "JTAjBgNVBAoTHFN0YXJmaWVsZCBUZWNobm9sb2dpZXMsIEluYy4xMjAwBgNVBAsT\n" +
            "KVN0YXJmaWVsZCBDbGFzcyAyIENlcnRpZmljYXRpb24gQXV0aG9yaXR5MB4XDTA2\n" +
            "MTExNjAxMTU0MFoXDTI2MTExNjAxMTU0MFowgdwxCzAJBgNVBAYTAlVTMRAwDgYD\n" +
            "VQQIEwdBcml6b25hMRMwEQYDVQQHEwpTY290dHNkYWxlMSUwIwYDVQQKExxTdGFy\n" +
            "ZmllbGQgVGVjaG5vbG9naWVzLCBJbmMuMTkwNwYDVQQLEzBodHRwOi8vY2VydGlm\n" +
            "aWNhdGVzLnN0YXJmaWVsZHRlY2guY29tL3JlcG9zaXRvcnkxMTAvBgNVBAMTKFN0\n" +
            "YXJmaWVsZCBTZWN1cmUgQ2VydGlmaWNhdGlvbiBBdXRob3JpdHkxETAPBgNVBAUT\n" +
            "CDEwNjg4NDM1MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA4qddo+1m\n" +
            "72ovKzYf3Y3TBQKgyg9eGa44cs8W2lRKy0gK9KFzEWWFQ8lbFwyaK74PmFF6YCkN\n" +
            "bN7i6OUVTVb/kNGnpgQ/YAdKym+lEOez+FyxvCsq3AF59R019Xoog/KTc4KJrGBt\n" +
            "y8JIwh3UBkQXPKwBR6s+cIQJC7ggCEAgh6FjGso+g9I3s5iNMj83v6G3W1/eXDOS\n" +
            "zz4HzrlIS+LwVVAv+HBCidGTlopj2WYN5lhuuW2QvcrchGbyOY5bplhVc8tibBvX\n" +
            "IBY7LFn1y8hWMkpQJ7pV06gBy3KpdIsMrTrlFbYq32X43or174Q7+edUZQuAvUdF\n" +
            "pfBE2FM7voDxLwIDAQABo4IBRDCCAUAwHQYDVR0OBBYEFElLUifRG7zyoSFqYntR\n" +
            "QnqK19VWMB8GA1UdIwQYMBaAFL9ft9HO3R+G9FtVrNzXEMIOqYjnMBIGA1UdEwEB\n" +
            "/wQIMAYBAf8CAQAwOQYIKwYBBQUHAQEELTArMCkGCCsGAQUFBzABhh1odHRwOi8v\n" +
            "b2NzcC5zdGFyZmllbGR0ZWNoLmNvbTBMBgNVHR8ERTBDMEGgP6A9hjtodHRwOi8v\n" +
            "Y2VydGlmaWNhdGVzLnN0YXJmaWVsZHRlY2guY29tL3JlcG9zaXRvcnkvc2Zyb290\n" +
            "LmNybDBRBgNVHSAESjBIMEYGBFUdIAAwPjA8BggrBgEFBQcCARYwaHR0cDovL2Nl\n" +
            "cnRpZmljYXRlcy5zdGFyZmllbGR0ZWNoLmNvbS9yZXBvc2l0b3J5MA4GA1UdDwEB\n" +
            "/wQEAwIBBjANBgkqhkiG9w0BAQUFAAOCAQEAhlK6sx+mXmuQpmQq/EWyrp8+s2Kv\n" +
            "2x9nxL3KoS/HnA0hV9D4NiHOOiU+eHaz2d283vtshF8Mow0S6xE7cV+AHvEfbQ5f\n" +
            "wezUpfdlux9MlQETsmqcC+sfnbHn7RkNvIV88xe9WWOupxoFzUfjLZZiUTIKCGhL\n" +
            "Indf90XcYd70yysiKUQl0p8Ld3qhJnxK1w/C0Ty6DqeVmlsFChD5VV/Bl4t0zF4o\n" +
            "aRN+0AqNnQ9gVHrEjBs1D3R6cLKCzx214orbKsayUWm/EheSYBeqPVsJ+IdlHaek\n" +
            "KOUiAgOCRJo0Y577KM/ozS4OUiDtSss4fJ2ubnnXlSyokfOGASGRS7VApA==\n" +
            "-----END CERTIFICATE-----\n";

    private static final String LINUX_COM_CERT =
            "-----BEGIN CERTIFICATE-----\n" +
            "MIIFfDCCBGSgAwIBAgIHJ7DOOMo+MDANBgkqhkiG9w0BAQUFADCB3DELMAkGA1UE\n" +
            "BhMCVVMxEDAOBgNVBAgTB0FyaXpvbmExEzARBgNVBAcTClNjb3R0c2RhbGUxJTAj\n" +
            "BgNVBAoTHFN0YXJmaWVsZCBUZWNobm9sb2dpZXMsIEluYy4xOTA3BgNVBAsTMGh0\n" +
            "dHA6Ly9jZXJ0aWZpY2F0ZXMuc3RhcmZpZWxkdGVjaC5jb20vcmVwb3NpdG9yeTEx\n" +
            "MC8GA1UEAxMoU3RhcmZpZWxkIFNlY3VyZSBDZXJ0aWZpY2F0aW9uIEF1dGhvcml0\n" +
            "eTERMA8GA1UEBRMIMTA2ODg0MzUwHhcNMTExMDA1MDI1MTQyWhcNMTQxMDA1MDI1\n" +
            "MTQyWjBPMRQwEgYDVQQKFAsqLmxpbnV4LmNvbTEhMB8GA1UECxMYRG9tYWluIENv\n" +
            "bnRyb2wgVmFsaWRhdGVkMRQwEgYDVQQDFAsqLmxpbnV4LmNvbTCCASIwDQYJKoZI\n" +
            "hvcNAQEBBQADggEPADCCAQoCggEBANoZR/TDp2/8LtA8k9Li55I665ssC7rHX+Wk\n" +
            "oiGa6xBeCKTvNy9mgaUVzHwrOQlwJ2GbxFI+X0e3W2sWXUDTSxESZSEW2VZnjEn2\n" +
            "600Qm8XMhZPvqztLRweHH8IuBNNYZHnW4Z2L4DS/Mi03EmjKZt2g3heGQbrv74m4\n" +
            "v9/g6Jgr5ZOIwES6LUJchSWV2zcL8VYunpxnAtbi2hq1YfA9oYU82ngP40Ds7HEB\n" +
            "9pUlzcWu9gcasWGzTvbVBZ4nA29pz5zWn1LHYfSYVSmXKU/ggfZb2nXd5/NkbWQX\n" +
            "7B2SNH9/OVrHtFZldzD1+ddfCt1DQjXfGv7QqpAVsFTdKspPDLMCAwEAAaOCAc0w\n" +
            "ggHJMA8GA1UdEwEB/wQFMAMBAQAwHQYDVR0lBBYwFAYIKwYBBQUHAwEGCCsGAQUF\n" +
            "BwMCMA4GA1UdDwEB/wQEAwIFoDA5BgNVHR8EMjAwMC6gLKAqhihodHRwOi8vY3Js\n" +
            "LnN0YXJmaWVsZHRlY2guY29tL3NmczEtMjAuY3JsMFkGA1UdIARSMFAwTgYLYIZI\n" +
            "AYb9bgEHFwEwPzA9BggrBgEFBQcCARYxaHR0cDovL2NlcnRpZmljYXRlcy5zdGFy\n" +
            "ZmllbGR0ZWNoLmNvbS9yZXBvc2l0b3J5LzCBjQYIKwYBBQUHAQEEgYAwfjAqBggr\n" +
            "BgEFBQcwAYYeaHR0cDovL29jc3Auc3RhcmZpZWxkdGVjaC5jb20vMFAGCCsGAQUF\n" +
            "BzAChkRodHRwOi8vY2VydGlmaWNhdGVzLnN0YXJmaWVsZHRlY2guY29tL3JlcG9z\n" +
            "aXRvcnkvc2ZfaW50ZXJtZWRpYXRlLmNydDAfBgNVHSMEGDAWgBRJS1In0Ru88qEh\n" +
            "amJ7UUJ6itfVVjAhBgNVHREEGjAYggsqLmxpbnV4LmNvbYIJbGludXguY29tMB0G\n" +
            "A1UdDgQWBBQ44sIiZfPIl4PY51fh2TCZkqtToTANBgkqhkiG9w0BAQUFAAOCAQEA\n" +
            "HFMuDtEZ+hIrIp4hnRJXUiTsc4Vaycxd5X/axDzUx+ooT3y2jBw0rcNnFhgD1T3u\n" +
            "9zKiOLGXidvy2G/ppy/ymE+gcNqcEzfV1pKggNqStCwpEX1K8GBD46mX5qJ1RxI+\n" +
            "QoHo/FZe7Vt+dQjHHdGWh27iVWadpBo/FJnHOsTaHewKL8+Aho0M84nxnUolYxzC\n" +
            "9H3ViEz+mfMISLzvWicxVU71aJ4yI9JmaL1ddRppBovZHOeWshizcMVtFwcza1S0\n" +
            "ZfajonXj48ZkXMXGWuomWxE2dGro6ZW6DdyIjTpZHCJuIvGC10J3mHIR5XaTj6mv\n" +
            "zkVBz5DhpshQe97x6OGLOA==\n" +
            "-----END CERTIFICATE-----\n";

    private File mKeyStoreFile;
    private LocalKeyStore mKeyStore;
    private X509Certificate mCert1;
    private X509Certificate mCert2;
    private X509Certificate mCaCert;
    private X509Certificate mCert3;
    private X509Certificate mStarfieldCert;
    private X509Certificate mLinuxComCert;


    public TrustManagerFactoryTest() throws CertificateException {
        mCert1 = loadCert(K9_EXAMPLE_COM_CERT1);
        mCert2 = loadCert(K9_EXAMPLE_COM_CERT2);
        mCaCert = loadCert(CA_CERT);
        mCert3 = loadCert(CERT3);
        mStarfieldCert = loadCert(STARFIELD_CERT);
        mLinuxComCert = loadCert(LINUX_COM_CERT);
    }

    private X509Certificate loadCert(String encodedCert) throws CertificateException {
        CertificateFactory certFactory = CertificateFactory.getInstance("X509");
        return (X509Certificate) certFactory.generateCertificate(
                new ByteArrayInputStream(encodedCert.getBytes()));
    }

    @Override
    public void setUp() throws Exception {
        mKeyStoreFile = File.createTempFile("localKeyStore", null, getContext().getCacheDir());
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

        X509TrustManager trustManager1 = TrustManagerFactory.get(NOT_MATCHING_HOST, PORT1);
        X509TrustManager trustManager2 = TrustManagerFactory.get(NOT_MATCHING_HOST, PORT2);
        trustManager2.checkServerTrusted(new X509Certificate[] { mCert2 }, "authType");
        trustManager1.checkServerTrusted(new X509Certificate[] { mCert1 }, "authType");
    }

    public void testSelfSignedCertificateMatchingHost() throws Exception {
        mKeyStore.addCertificate(MATCHING_HOST, PORT1, mCert1);
        X509TrustManager trustManager = TrustManagerFactory.get(MATCHING_HOST, PORT1);
        trustManager.checkServerTrusted(new X509Certificate[] { mCert1 }, "authType");
    }

    public void testSelfSignedCertificateNotMatchingHost() throws Exception {
        mKeyStore.addCertificate(NOT_MATCHING_HOST, PORT1, mCert1);
        X509TrustManager trustManager = TrustManagerFactory.get(NOT_MATCHING_HOST, PORT1);
        trustManager.checkServerTrusted(new X509Certificate[] { mCert1 }, "authType");
    }

    public void testWrongCertificate() throws Exception {
        mKeyStore.addCertificate(MATCHING_HOST, PORT1, mCert1);
        X509TrustManager trustManager = TrustManagerFactory.get(MATCHING_HOST, PORT1);
        assertCertificateRejection(trustManager, new X509Certificate[] { mCert2 });
    }

    public void testCertificateOfOtherHost() throws Exception {
        mKeyStore.addCertificate(MATCHING_HOST, PORT1, mCert1);
        mKeyStore.addCertificate(MATCHING_HOST, PORT2, mCert2);

        X509TrustManager trustManager = TrustManagerFactory.get(MATCHING_HOST, PORT1);
        assertCertificateRejection(trustManager, new X509Certificate[] { mCert2 });
    }

    public void testUntrustedCertificateChain() throws Exception {
        X509TrustManager trustManager = TrustManagerFactory.get(MATCHING_HOST, PORT1);
        assertCertificateRejection(trustManager, new X509Certificate[] { mCert3, mCaCert });
    }

    public void testLocallyTrustedCertificateChain() throws Exception {
        mKeyStore.addCertificate(MATCHING_HOST, PORT1, mCert3);

        X509TrustManager trustManager = TrustManagerFactory.get(MATCHING_HOST, PORT1);
        trustManager.checkServerTrusted(new X509Certificate[] { mCert3, mCaCert }, "authType");
    }

    public void testLocallyTrustedCertificateChainNotMatchingHost() throws Exception {
        mKeyStore.addCertificate(NOT_MATCHING_HOST, PORT1, mCert3);

        X509TrustManager trustManager = TrustManagerFactory.get(NOT_MATCHING_HOST, PORT1);
        trustManager.checkServerTrusted(new X509Certificate[] { mCert3, mCaCert }, "authType");
    }

    public void testGloballyTrustedCertificateChain() throws Exception {
        X509TrustManager trustManager = TrustManagerFactory.get("www.linux.com", PORT1);
        X509Certificate[] certificates = new X509Certificate[] { mLinuxComCert, mStarfieldCert };
        trustManager.checkServerTrusted(certificates, "authType");
    }

    public void testGloballyTrustedCertificateNotMatchingHost() throws Exception {
        X509TrustManager trustManager = TrustManagerFactory.get(NOT_MATCHING_HOST, PORT1);
        assertCertificateRejection(trustManager, new X509Certificate[] { mLinuxComCert, mStarfieldCert });
    }

    public void testGloballyTrustedCertificateNotMatchingHostOverride() throws Exception {
        mKeyStore.addCertificate(MATCHING_HOST, PORT1, mLinuxComCert);

        X509TrustManager trustManager = TrustManagerFactory.get(MATCHING_HOST, PORT1);
        X509Certificate[] certificates = new X509Certificate[] { mLinuxComCert, mStarfieldCert };
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
