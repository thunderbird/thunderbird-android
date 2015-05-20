package com.fsck.k9.mail.ssl;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import javax.net.ssl.X509TrustManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;


/**
 * Test the functionality of {@link TrustManagerFactory}.
 */
@RunWith(AndroidJUnit4.class)
public class TrustManagerFactoryTest {
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

    private static final String LINUX_COM_FIRST_PARENT_CERT =
            "-----BEGIN CERTIFICATE-----\n" +
            "MIIGNDCCBBygAwIBAgIBGzANBgkqhkiG9w0BAQsFADB9MQswCQYDVQQGEwJJTDEW\n" +
            "MBQGA1UEChMNU3RhcnRDb20gTHRkLjErMCkGA1UECxMiU2VjdXJlIERpZ2l0YWwg\n" +
            "Q2VydGlmaWNhdGUgU2lnbmluZzEpMCcGA1UEAxMgU3RhcnRDb20gQ2VydGlmaWNh\n" +
            "dGlvbiBBdXRob3JpdHkwHhcNMDcxMDI0MjA1NzA5WhcNMTcxMDI0MjA1NzA5WjCB\n" +
            "jDELMAkGA1UEBhMCSUwxFjAUBgNVBAoTDVN0YXJ0Q29tIEx0ZC4xKzApBgNVBAsT\n" +
            "IlNlY3VyZSBEaWdpdGFsIENlcnRpZmljYXRlIFNpZ25pbmcxODA2BgNVBAMTL1N0\n" +
            "YXJ0Q29tIENsYXNzIDIgUHJpbWFyeSBJbnRlcm1lZGlhdGUgU2VydmVyIENBMIIB\n" +
            "IjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA4k85L6GMmoWtCA4IPlfyiAEh\n" +
            "G5SpbOK426oZGEY6UqH1D/RujOqWjJaHeRNAUS8i8gyLhw9l33F0NENVsTUJm9m8\n" +
            "H/rrQtCXQHK3Q5Y9upadXVACHJuRjZzArNe7LxfXyz6CnXPrB0KSss1ks3RVG7RL\n" +
            "hiEs93iHMuAW5Nq9TJXqpAp+tgoNLorPVavD5d1Bik7mb2VsskDPF125w2oLJxGE\n" +
            "d2H2wnztwI14FBiZgZl1Y7foU9O6YekO+qIw80aiuckfbIBaQKwn7UhHM7BUxkYa\n" +
            "8zVhwQIpkFR+ZE3EMFICgtffziFuGJHXuKuMJxe18KMBL47SLoc6PbQpZ4rEAwID\n" +
            "AQABo4IBrTCCAakwDwYDVR0TAQH/BAUwAwEB/zAOBgNVHQ8BAf8EBAMCAQYwHQYD\n" +
            "VR0OBBYEFBHbI0X9VMxqcW+EigPXvvcBLyaGMB8GA1UdIwQYMBaAFE4L7xqkQFul\n" +
            "F2mHMMo0aEPQQa7yMGYGCCsGAQUFBwEBBFowWDAnBggrBgEFBQcwAYYbaHR0cDov\n" +
            "L29jc3Auc3RhcnRzc2wuY29tL2NhMC0GCCsGAQUFBzAChiFodHRwOi8vd3d3LnN0\n" +
            "YXJ0c3NsLmNvbS9zZnNjYS5jcnQwWwYDVR0fBFQwUjAnoCWgI4YhaHR0cDovL3d3\n" +
            "dy5zdGFydHNzbC5jb20vc2ZzY2EuY3JsMCegJaAjhiFodHRwOi8vY3JsLnN0YXJ0\n" +
            "c3NsLmNvbS9zZnNjYS5jcmwwgYAGA1UdIAR5MHcwdQYLKwYBBAGBtTcBAgEwZjAu\n" +
            "BggrBgEFBQcCARYiaHR0cDovL3d3dy5zdGFydHNzbC5jb20vcG9saWN5LnBkZjA0\n" +
            "BggrBgEFBQcCARYoaHR0cDovL3d3dy5zdGFydHNzbC5jb20vaW50ZXJtZWRpYXRl\n" +
            "LnBkZjANBgkqhkiG9w0BAQsFAAOCAgEAbQjxXHkqUPtUY+u8NEFcuKMDITfjvGkl\n" +
            "LgrTuBW63grW+2AuDAZRo/066eNHs6QV4i5e4ujwPSR2dgggY7mOIIBmiDm2QRjF\n" +
            "5CROq6zDlIdqlsFZICkuONDNFpFjaPtZRTmuK1n6gywQgCNSIrbzjPcwR/jL/wow\n" +
            "bfwC9yGme1EeZRqvWy/HzFWacs7UMmWlRk6DTmpfPOPMJo5AxyTZCiCYQQeksV7x\n" +
            "UAeY0kWa+y/FV+eerOPUl6yy4jRHTk7tCySxrciZwYbd6YNLmeIQoUAdRC3CH3nT\n" +
            "B2/JYxltcgyGHMiPU3TtafZgLs8fvncv+wIF1YAF/OGqg8qmzoJ3ghM4upGdTMIu\n" +
            "8vADdmuLC/+dnbzknxX6QEGlWA8zojLUxVhGNfIFoizu/V/DyvSvYuxzzIkPECK5\n" +
            "gDoMoBTTMI/wnxXwulNPtfgF7/5AtDhA4GNAfB2SddxiNQAF7XkUHtMZ9ff3W6Xk\n" +
            "FldOG+NlLFqsDBG/KLckyFK36gq+FqNFCbmtmtXBGB5L1fDIeYzcMKG6hFQxhHS0\n" +
            "oqpdHhp2nWBfLlOnTNqIZNJzOH37OJE6Olk45LNFJtSrqIAZyCCfM6bQgoQvZuIa\n" +
            "xs9SIp+63ZMk9TxEaQj/KteaOyfaPXI9778U7JElMTz3Bls62mslV2I1C/A73Zyq\n" +
            "JZWQZ8NU4ds=\n" +
            "-----END CERTIFICATE-----\n";

    private static final String LINUX_COM_CERT =
        "-----BEGIN CERTIFICATE-----\n" +
        "MIIGhjCCBW6gAwIBAgIDAmiWMA0GCSqGSIb3DQEBCwUAMIGMMQswCQYDVQQGEwJJ\n" +
        "TDEWMBQGA1UEChMNU3RhcnRDb20gTHRkLjErMCkGA1UECxMiU2VjdXJlIERpZ2l0\n" +
        "YWwgQ2VydGlmaWNhdGUgU2lnbmluZzE4MDYGA1UEAxMvU3RhcnRDb20gQ2xhc3Mg\n" +
        "MiBQcmltYXJ5IEludGVybWVkaWF0ZSBTZXJ2ZXIgQ0EwHhcNMTQwODIxMjEwMDI4\n" +
        "WhcNMTYwODIxMDY0NDE0WjCBlDELMAkGA1UEBhMCVVMxEzARBgNVBAgTCkNhbGlm\n" +
        "b3JuaWExFjAUBgNVBAcTDVNhbiBGcmFuY2lzY28xHTAbBgNVBAoTFFRoZSBMaW51\n" +
        "eCBGb3VuZGF0aW9uMRQwEgYDVQQDFAsqLmxpbnV4LmNvbTEjMCEGCSqGSIb3DQEJ\n" +
        "ARYUaG9zdG1hc3RlckBsaW51eC5jb20wggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAw\n" +
        "ggEKAoIBAQCjvFjOigXyqkSiVv0vz1CSDg08iilLnj8gRFRoRMA6fFWhQTp4QGLV\n" +
        "1li5VMEQdZ/vyqTWjmB+FFkuTsBjFDg6gG3yw6DQBGyyM06A1dT9YKUa7LqxOxQr\n" +
        "KhNOacPS/pAupAZ5jOO7fcZwIcpKcKEjjhHn7GXEVvb+K996TMA0vDYcp1lgXtil\n" +
        "7Ij+1GUSA29NrnCZXUun2c5nS7OulRYcgtRyZBa13zfyaVJtEIl14ClP9gsLa/5u\n" +
        "eXzZD71Jj48ZNbiKRThiUZ5FkEnljjSQa25Hr5g9DXY2JvI1r8OJOCUR8jPiRyNs\n" +
        "Kgc1ZG0fibm9VoHjokUZ2aQxyQJP/C1TAgMBAAGjggLlMIIC4TAJBgNVHRMEAjAA\n" +
        "MAsGA1UdDwQEAwIDqDAdBgNVHSUEFjAUBggrBgEFBQcDAgYIKwYBBQUHAwEwHQYD\n" +
        "VR0OBBYEFI0nMnIXZOz02MlXPh2g9aHesvPPMB8GA1UdIwQYMBaAFBHbI0X9VMxq\n" +
        "cW+EigPXvvcBLyaGMCEGA1UdEQQaMBiCCyoubGludXguY29tgglsaW51eC5jb20w\n" +
        "ggFWBgNVHSAEggFNMIIBSTAIBgZngQwBAgIwggE7BgsrBgEEAYG1NwECAzCCASow\n" +
        "LgYIKwYBBQUHAgEWImh0dHA6Ly93d3cuc3RhcnRzc2wuY29tL3BvbGljeS5wZGYw\n" +
        "gfcGCCsGAQUFBwICMIHqMCcWIFN0YXJ0Q29tIENlcnRpZmljYXRpb24gQXV0aG9y\n" +
        "aXR5MAMCAQEagb5UaGlzIGNlcnRpZmljYXRlIHdhcyBpc3N1ZWQgYWNjb3JkaW5n\n" +
        "IHRvIHRoZSBDbGFzcyAyIFZhbGlkYXRpb24gcmVxdWlyZW1lbnRzIG9mIHRoZSBT\n" +
        "dGFydENvbSBDQSBwb2xpY3ksIHJlbGlhbmNlIG9ubHkgZm9yIHRoZSBpbnRlbmRl\n" +
        "ZCBwdXJwb3NlIGluIGNvbXBsaWFuY2Ugb2YgdGhlIHJlbHlpbmcgcGFydHkgb2Js\n" +
        "aWdhdGlvbnMuMDUGA1UdHwQuMCwwKqAooCaGJGh0dHA6Ly9jcmwuc3RhcnRzc2wu\n" +
        "Y29tL2NydDItY3JsLmNybDCBjgYIKwYBBQUHAQEEgYEwfzA5BggrBgEFBQcwAYYt\n" +
        "aHR0cDovL29jc3Auc3RhcnRzc2wuY29tL3N1Yi9jbGFzczIvc2VydmVyL2NhMEIG\n" +
        "CCsGAQUFBzAChjZodHRwOi8vYWlhLnN0YXJ0c3NsLmNvbS9jZXJ0cy9zdWIuY2xh\n" +
        "c3MyLnNlcnZlci5jYS5jcnQwIwYDVR0SBBwwGoYYaHR0cDovL3d3dy5zdGFydHNz\n" +
        "bC5jb20vMA0GCSqGSIb3DQEBCwUAA4IBAQBVkvlwVLfnTNZh1c8j+PQ1t2n6x1dh\n" +
        "tQtZiAYWKvZwi+XqLwU8q2zMxKrTDuqyEVyfCtWCiC1Vkpz72pcyXz2dKu7F7ZVL\n" +
        "86uVHcc1jAGmL59UCXz8LFbfAMcoVQW1f2WtNwsa/WGnPUes3jFSec+shB+XCpvE\n" +
        "WU6mfcZD5TyvbC79Kn5e3Iq+B4DaXhU/BXASRbORgYd8C+dqj++w0PAcMrmjn3D6\n" +
        "EmL1ofqpQ8wCJd5C1b5Fr4RbbYpK8v8AASRcp2Qj9WJjyV882FvXOOFj5V2Jjcnh\n" +
        "G0h67ElS/klu9rPaZ+vr3iIB56wvk08O2Wq1IND3sN+Ke3UsvuPqDxAv\n" +
        "-----END CERTIFICATE-----\n";

    private File mKeyStoreFile;
    private LocalKeyStore mKeyStore;
    private X509Certificate mCert1;
    private X509Certificate mCert2;
    private X509Certificate mCaCert;
    private X509Certificate mCert3;
    private X509Certificate mLinuxComFirstParentCert;
    private X509Certificate mLinuxComCert;


    public TrustManagerFactoryTest() throws CertificateException {
        mCert1 = loadCert(K9_EXAMPLE_COM_CERT1);
        mCert2 = loadCert(K9_EXAMPLE_COM_CERT2);
        mCaCert = loadCert(CA_CERT);
        mCert3 = loadCert(CERT3);
        mLinuxComFirstParentCert = loadCert(LINUX_COM_FIRST_PARENT_CERT);
        mLinuxComCert = loadCert(LINUX_COM_CERT);
    }

    private X509Certificate loadCert(String encodedCert) throws CertificateException {
        CertificateFactory certFactory = CertificateFactory.getInstance("X509");
        return (X509Certificate) certFactory.generateCertificate(
                new ByteArrayInputStream(encodedCert.getBytes()));
    }

    @Before
    public void setUp() throws Exception {
        mKeyStoreFile = File.createTempFile("localKeyStore", null,
                InstrumentationRegistry.getTargetContext().getCacheDir());
        mKeyStore = LocalKeyStore.getInstance();
        mKeyStore.setKeyStoreFile(mKeyStoreFile);
    }

    @After
    public void tearDown() {
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
    @Test
    public void testDifferentCertificatesOnSameServer() throws Exception {
        mKeyStore.addCertificate(NOT_MATCHING_HOST, PORT1, mCert1);
        mKeyStore.addCertificate(NOT_MATCHING_HOST, PORT2, mCert2);

        X509TrustManager trustManager1 = TrustManagerFactory.get(NOT_MATCHING_HOST, PORT1);
        X509TrustManager trustManager2 = TrustManagerFactory.get(NOT_MATCHING_HOST, PORT2);
        trustManager2.checkServerTrusted(new X509Certificate[] { mCert2 }, "authType");
        trustManager1.checkServerTrusted(new X509Certificate[] { mCert1 }, "authType");
    }

    @Test
    public void testSelfSignedCertificateMatchingHost() throws Exception {
        mKeyStore.addCertificate(MATCHING_HOST, PORT1, mCert1);
        X509TrustManager trustManager = TrustManagerFactory.get(MATCHING_HOST, PORT1);
        trustManager.checkServerTrusted(new X509Certificate[] { mCert1 }, "authType");
    }

    @Test
    public void testSelfSignedCertificateNotMatchingHost() throws Exception {
        mKeyStore.addCertificate(NOT_MATCHING_HOST, PORT1, mCert1);
        X509TrustManager trustManager = TrustManagerFactory.get(NOT_MATCHING_HOST, PORT1);
        trustManager.checkServerTrusted(new X509Certificate[] { mCert1 }, "authType");
    }

    @Test
    public void testWrongCertificate() throws Exception {
        mKeyStore.addCertificate(MATCHING_HOST, PORT1, mCert1);
        X509TrustManager trustManager = TrustManagerFactory.get(MATCHING_HOST, PORT1);
        assertCertificateRejection(trustManager, new X509Certificate[] { mCert2 });
    }

    @Test
    public void testCertificateOfOtherHost() throws Exception {
        mKeyStore.addCertificate(MATCHING_HOST, PORT1, mCert1);
        mKeyStore.addCertificate(MATCHING_HOST, PORT2, mCert2);

        X509TrustManager trustManager = TrustManagerFactory.get(MATCHING_HOST, PORT1);
        assertCertificateRejection(trustManager, new X509Certificate[] { mCert2 });
    }

    @Test
    public void testUntrustedCertificateChain() throws Exception {
        X509TrustManager trustManager = TrustManagerFactory.get(MATCHING_HOST, PORT1);
        assertCertificateRejection(trustManager, new X509Certificate[] { mCert3, mCaCert });
    }

    @Test
    public void testLocallyTrustedCertificateChain() throws Exception {
        mKeyStore.addCertificate(MATCHING_HOST, PORT1, mCert3);

        X509TrustManager trustManager = TrustManagerFactory.get(MATCHING_HOST, PORT1);
        trustManager.checkServerTrusted(new X509Certificate[] { mCert3, mCaCert }, "authType");
    }

    @Test
    public void testLocallyTrustedCertificateChainNotMatchingHost() throws Exception {
        mKeyStore.addCertificate(NOT_MATCHING_HOST, PORT1, mCert3);

        X509TrustManager trustManager = TrustManagerFactory.get(NOT_MATCHING_HOST, PORT1);
        trustManager.checkServerTrusted(new X509Certificate[] { mCert3, mCaCert }, "authType");
    }

    @Test
    public void testGloballyTrustedCertificateChain() throws Exception {
        X509TrustManager trustManager = TrustManagerFactory.get("www.linux.com", PORT1);
        X509Certificate[] certificates = new X509Certificate[] { mLinuxComCert, mLinuxComFirstParentCert};
        trustManager.checkServerTrusted(certificates, "authType");
    }

    @Test
    public void testGloballyTrustedCertificateNotMatchingHost() throws Exception {
        X509TrustManager trustManager = TrustManagerFactory.get(NOT_MATCHING_HOST, PORT1);
        assertCertificateRejection(trustManager, new X509Certificate[] { mLinuxComCert, mLinuxComFirstParentCert});
    }

    @Test
    public void testGloballyTrustedCertificateNotMatchingHostOverride() throws Exception {
        mKeyStore.addCertificate(MATCHING_HOST, PORT1, mLinuxComCert);

        X509TrustManager trustManager = TrustManagerFactory.get(MATCHING_HOST, PORT1);
        X509Certificate[] certificates = new X509Certificate[] { mLinuxComCert, mLinuxComFirstParentCert};
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

    @Test
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
