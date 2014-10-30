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

    private static final String STARTCOM_ORG_CERT =
            "-----BEGIN CERTIFICATE-----\n" +
            "MIIGnzCCBIegAwIBAgIBPTANBgkqhkiG9w0BAQsFADBTMQswCQYDVQQGEwJJTDEW\n" +
            "MBQGA1UEChMNU3RhcnRDb20gTHRkLjEsMCoGA1UEAxMjU3RhcnRDb20gQ2VydGlm\n" +
            "aWNhdGlvbiBBdXRob3JpdHkgRzIwHhcNMDYwOTE3MTk0NjM3WhcNMzYwOTE3MTk0\n" +
            "NjM3WjB9MQswCQYDVQQGEwJJTDEWMBQGA1UEChMNU3RhcnRDb20gTHRkLjErMCkG\n" +
            "A1UECxMiU2VjdXJlIERpZ2l0YWwgQ2VydGlmaWNhdGUgU2lnbmluZzEpMCcGA1UE\n" +
            "AxMgU3RhcnRDb20gQ2VydGlmaWNhdGlvbiBBdXRob3JpdHkwggIiMA0GCSqGSIb3\n" +
            "DQEBAQUAA4ICDwAwggIKAoICAQDBiNsJvGxGfHiflXu1M5DycmLWwTYgIiRezul3\n" +
            "8kMKogZkpMyONvg45iPwbm2xPN1yo4UcodM9tDMr0y+v/uqwQVlntsQGfQqedIXW\n" +
            "eUyAN3rfOQVSWff0G0ZDpNKFhdLDcfN1YjS6LIp/Ho/u7TTQEceWzVI9ujPW3U3e\n" +
            "CztKS5/CJi/6tRYccjV3yjxd5srhJosaNnZcAdt0FCX+7bWgiA/deMotHweXMAEt\n" +
            "cnn6RtYTKqi5pquDSR3l8u/d5AGOGAqPY1MWhWKpDhk6zLVmpsJrdAfkK+F2PrRt\n" +
            "2PZE4XNiHzvEvqBTViVsUQn3qqvKv3b9bZvzndu/PWa8DFaqr5hIlTpL36dYUNk4\n" +
            "dalb6kMMAv+Z6+hsTXBbKWWc3apdzK8BMewM69KN6Oqce+Zu9ydmDBpI125C4z/e\n" +
            "IT574Q1w+2OqqGwaVLRcJXrJosmLFqa7LH4XXgVNWG4SHQHuEhANxjJ/GP/89PrN\n" +
            "bpHoNkm+Gkhpi8KWTRoSsmkXwQqQ1vp5Iki/untp+HDH+no32NgN0nZPV/+Qt+OR\n" +
            "0t3vwmC3Zzrd/qqc8NSLf3Iizsafl7b4r4qgEKjZ+xjGtrVcUjyJthkqcwEKDwOz\n" +
            "EmDyei+B26Nu/yYwl/WL3YlXtq09s68rxbd2AvCl1iuahhQqcvbjM4xdCUsT37uM\n" +
            "dBNSSwIDAQABo4IBUjCCAU4wEgYDVR0TAQH/BAgwBgEB/wIBAjAOBgNVHQ8BAf8E\n" +
            "BAMCAQYwHQYDVR0OBBYEFE4L7xqkQFulF2mHMMo0aEPQQa7yMB8GA1UdIwQYMBaA\n" +
            "FEvFtEBrrRyzpRxlbkY2iYcFDA62MG8GCCsGAQUFBwEBBGMwYTAqBggrBgEFBQcw\n" +
            "AYYeaHR0cDovL29jc3Auc3RhcnRzc2wuY29tL2NhLWcyMDMGCCsGAQUFBzAChido\n" +
            "dHRwOi8vYWlhLnN0YXJ0c3NsLmNvbS9jZXJ0cy9jYS1nMi5jZXIwMgYDVR0fBCsw\n" +
            "KTAnoCWgI4YhaHR0cDovL2NybC5zdGFydHNzbC5jb20vY2EtZzIuY3JsMEMGA1Ud\n" +
            "IAQ8MDowOAYEVR0gADAwMC4GCCsGAQUFBwIBFiJodHRwOi8vd3d3LnN0YXJ0c3Ns\n" +
            "LmNvbS9wb2xpY3kucGRmMA0GCSqGSIb3DQEBCwUAA4ICAQAznlPLrlQsAomwlVYG\n" +
            "grRHeXCXSA8K0E03a1VfCE8uhgRZg7cK3N2oEojBMoeL4VqLOeuqcd3z+xSq85ux\n" +
            "nzPc+cABbiRb4guyQvK/dqzWjjmDcEYVk0ozCqWiBk+ec4W4HmgykQHdvhY+8zgJ\n" +
            "SCQdfqGkhAUc1/U/iE5jAIvc3M77nFeJ6KIe3VanEBDstFzlQhJSo8pSzhL8pEYB\n" +
            "WpudHanbFogZDJwG+GC6pE8PSnIEAHmLKXwoReSr6xoHpUDrMdwChth00gRYCz45\n" +
            "AOrkTylIrIl8ElzzXbtjBrCFMKIctmcxXE0sVc2YT9OCCUL+m7/LKPNdXopGbe8D\n" +
            "WL6HmFUwGLwdt8t5WzXVSxS3xAA7F6DMz05wNT68Jfdf8BlJyQ3HRiTv/T20BbNS\n" +
            "qSVsOWspIyuaPhLR1/Sv2m4PerB5lJYA6/U/eFZ1ot8ge53XOIoPfD99LG9Sed4n\n" +
            "F822o/dHSXAZ8X+ootoBX5tiA+9hfGqfV/JusYcv+hc7SqRdzYTbcs6si+EhRs1h\n" +
            "KWLiufNoQMHP+lcopOUlt3Ip/p6WDZNJOJq46LgvDFj7cQ/WSkMuzA5fp6NPvjQn\n" +
            "VmojjUTsadpRN4hbnajrO7rjJhOJBN3kcd41za7ZQ9lh429y8y5TbJWYJ4EblH4r\n" +
            "iQVKEQgyg4mqmCOllyMYS7CGGQ==\n" +
            "-----END CERTIFICATE-----\n";

    private static final String STARTCOM_INTERMEDIATE_CERT =
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

    private static final String LINUX_FOUNDATION_CERT =
            "-----BEGIN CERTIFICATE-----\n" +
            "MIIGrjCCBZagAwIBAgIDAmHEMA0GCSqGSIb3DQEBCwUAMIGMMQswCQYDVQQGEwJJ\n" +
            "TDEWMBQGA1UEChMNU3RhcnRDb20gTHRkLjErMCkGA1UECxMiU2VjdXJlIERpZ2l0\n" +
            "YWwgQ2VydGlmaWNhdGUgU2lnbmluZzE4MDYGA1UEAxMvU3RhcnRDb20gQ2xhc3Mg\n" +
            "MiBQcmltYXJ5IEludGVybWVkaWF0ZSBTZXJ2ZXIgQ0EwHhcNMTQwODEyMDMwNTQx\n" +
            "WhcNMTYwODEyMTExODUzWjCBqDELMAkGA1UEBhMCVVMxEzARBgNVBAgTCkNhbGlm\n" +
            "b3JuaWExFjAUBgNVBAcTDVNhbiBGcmFuY2lzY28xHTAbBgNVBAoTFFRoZSBMaW51\n" +
            "eCBGb3VuZGF0aW9uMR4wHAYDVQQDFBUqLmxpbnV4Zm91bmRhdGlvbi5vcmcxLTAr\n" +
            "BgkqhkiG9w0BCQEWHmhvc3RtYXN0ZXJAbGludXhmb3VuZGF0aW9uLm9yZzCCASIw\n" +
            "DQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBANbsVKDzSgoI186g+CFHjhFSokCm\n" +
            "QgxuvWpQC3mO/F9bIHTxSVifzKuFJX2yPrHLiZhPfrtbfY7sMnrO5GeM1AEw7KMu\n" +
            "ztTqh9L8Egukc+MJ5qSjXi8kHvb76nCCtr8IgHNDZikr6KOUS+hjMKe5WzFakMOF\n" +
            "cmR5/tMEHqIqCcXjuDTofEoUYeLB0dvhEzoEhrokcPouvCRXA5L/euVmF4ZJP2oA\n" +
            "eYgycjDZ9P9wsA4AXBlEKfOJLGSvcYhPTDIwpo8Rdvx2zrxllNnwZ5MrjCoWHeNW\n" +
            "Lvm5Zsg4FawaWHTtBHF9wROe1a0z0JncOvv80T4kC130GEmsXDsa57Dcg+cCAwEA\n" +
            "AaOCAvkwggL1MAkGA1UdEwQCMAAwCwYDVR0PBAQDAgOoMB0GA1UdJQQWMBQGCCsG\n" +
            "AQUFBwMCBggrBgEFBQcDATAdBgNVHQ4EFgQUUvDaeTHw4aEiKVOBSwgVI3FYXWMw\n" +
            "HwYDVR0jBBgwFoAUEdsjRf1UzGpxb4SKA9e+9wEvJoYwNQYDVR0RBC4wLIIVKi5s\n" +
            "aW51eGZvdW5kYXRpb24ub3JnghNsaW51eGZvdW5kYXRpb24ub3JnMIIBVgYDVR0g\n" +
            "BIIBTTCCAUkwCAYGZ4EMAQICMIIBOwYLKwYBBAGBtTcBAgMwggEqMC4GCCsGAQUF\n" +
            "BwIBFiJodHRwOi8vd3d3LnN0YXJ0c3NsLmNvbS9wb2xpY3kucGRmMIH3BggrBgEF\n" +
            "BQcCAjCB6jAnFiBTdGFydENvbSBDZXJ0aWZpY2F0aW9uIEF1dGhvcml0eTADAgEB\n" +
            "GoG+VGhpcyBjZXJ0aWZpY2F0ZSB3YXMgaXNzdWVkIGFjY29yZGluZyB0byB0aGUg\n" +
            "Q2xhc3MgMiBWYWxpZGF0aW9uIHJlcXVpcmVtZW50cyBvZiB0aGUgU3RhcnRDb20g\n" +
            "Q0EgcG9saWN5LCByZWxpYW5jZSBvbmx5IGZvciB0aGUgaW50ZW5kZWQgcHVycG9z\n" +
            "ZSBpbiBjb21wbGlhbmNlIG9mIHRoZSByZWx5aW5nIHBhcnR5IG9ibGlnYXRpb25z\n" +
            "LjA1BgNVHR8ELjAsMCqgKKAmhiRodHRwOi8vY3JsLnN0YXJ0c3NsLmNvbS9jcnQy\n" +
            "LWNybC5jcmwwgY4GCCsGAQUFBwEBBIGBMH8wOQYIKwYBBQUHMAGGLWh0dHA6Ly9v\n" +
            "Y3NwLnN0YXJ0c3NsLmNvbS9zdWIvY2xhc3MyL3NlcnZlci9jYTBCBggrBgEFBQcw\n" +
            "AoY2aHR0cDovL2FpYS5zdGFydHNzbC5jb20vY2VydHMvc3ViLmNsYXNzMi5zZXJ2\n" +
            "ZXIuY2EuY3J0MCMGA1UdEgQcMBqGGGh0dHA6Ly93d3cuc3RhcnRzc2wuY29tLzAN\n" +
            "BgkqhkiG9w0BAQsFAAOCAQEADPn/peSp2N6IVtkias/dpk7Gju6y5Dg+bYUBShYX\n" +
            "hZ5Zy767SDzZhQ9F6DpuL88ViqO6WAANf6usiHGmnBR3GFMeLSHrYSPiMcwuxL9J\n" +
            "Ko3eBT92lnzRuyMc7Z0jzihwEbSromyXZg91N1XmZpQFGgtytM255f6BhzK8sS5k\n" +
            "49Xv0/wLdONen19eFt/nLmBPUkqwq27YNGinv7Fupe4bw4Xmg2O+/P1s8a+TCXcb\n" +
            "pDLQAKkLZFPdqwmG9gOqvQylTRALylLrhAuP4sUXddbQ+x3AoQCbD3pY7iCITKaI\n" +
            "ozuO5qjkS43nZgjXirAMq7H1LkT35MKcS37vv78sn2WUzw==\n" +
            "-----END CERTIFICATE-----\n";


    private File mKeyStoreFile;
    private LocalKeyStore mKeyStore;
    private X509Certificate mCert1;
    private X509Certificate mCert2;
    private X509Certificate mCaCert;
    private X509Certificate mCert3;
    private X509Certificate mStartcomCert;
    private X509Certificate mStartcomIntermediate;
    private X509Certificate mLinuxFoundationCert;


    public TrustManagerFactoryTest() throws CertificateException {
        mCert1 = loadCert(K9_EXAMPLE_COM_CERT1);
        mCert2 = loadCert(K9_EXAMPLE_COM_CERT2);
        mCaCert = loadCert(CA_CERT);
        mCert3 = loadCert(CERT3);
        mStartcomCert = loadCert(STARTCOM_ORG_CERT);
        mStartcomIntermediate = loadCert(STARTCOM_INTERMEDIATE_CERT);
        mLinuxFoundationCert = loadCert(LINUX_FOUNDATION_CERT);
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
        X509TrustManager trustManager = TrustManagerFactory.get("www.linuxfoundation.org", PORT1);
        X509Certificate[] certificates = new X509Certificate[] { mLinuxFoundationCert, mStartcomIntermediate, mStartcomCert };
        trustManager.checkServerTrusted(certificates, "authType");
    }

    public void testGloballyTrustedCertificateNotMatchingHost() throws Exception {
        X509TrustManager trustManager = TrustManagerFactory.get(NOT_MATCHING_HOST, PORT1);
        assertCertificateRejection(trustManager, new X509Certificate[] { mLinuxFoundationCert, mStartcomIntermediate, mStartcomCert });
    }

    public void testGloballyTrustedCertificateNotMatchingHostOverride() throws Exception {
        mKeyStore.addCertificate(MATCHING_HOST, PORT1, mLinuxFoundationCert);

        X509TrustManager trustManager = TrustManagerFactory.get(MATCHING_HOST, PORT1);
        X509Certificate[] certificates = new X509Certificate[] { mLinuxFoundationCert, mStartcomIntermediate, mStartcomCert };
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
