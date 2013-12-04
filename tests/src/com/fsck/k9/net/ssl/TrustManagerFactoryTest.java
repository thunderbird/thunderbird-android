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

          private static final String DIGI_CERT =
            "-----BEGIN CERTIFICATE-----\n"
          + "MIIG5jCCBc6gAwIBAgIQAze5KDR8YKauxa2xIX84YDANBgkqhkiG9w0BAQUFADBs\n"
          + "MQswCQYDVQQGEwJVUzEVMBMGA1UEChMMRGlnaUNlcnQgSW5jMRkwFwYDVQQLExB3\n"
          + "d3cuZGlnaWNlcnQuY29tMSswKQYDVQQDEyJEaWdpQ2VydCBIaWdoIEFzc3VyYW5j\n"
          + "ZSBFViBSb290IENBMB4XDTA3MTEwOTEyMDAwMFoXDTIxMTExMDAwMDAwMFowaTEL\n"
          + "MAkGA1UEBhMCVVMxFTATBgNVBAoTDERpZ2lDZXJ0IEluYzEZMBcGA1UECxMQd3d3\n"
          + "LmRpZ2ljZXJ0LmNvbTEoMCYGA1UEAxMfRGlnaUNlcnQgSGlnaCBBc3N1cmFuY2Ug\n"
          + "RVYgQ0EtMTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAPOWYth1bhn/\n"
          + "PzR8SU8xfg0ETpmB4rOFVZEwscCvcLssqOcYqj9495BoUoYBiJfiOwZlkKq9ZXbC\n"
          + "7L4QWzd4g2B1Rca9dKq2n6Q6AVAXxDlpufFP74LByvNK28yeUE9NQKM6kOeGZrzw\n"
          + "PnYoTNF1gJ5qNRQ1A57bDIzCKK1Qss72kaPDpQpYSfZ1RGy6+c7pqzoC4E3zrOJ6\n"
          + "4GAiBTyC01Li85xH+DvYskuTVkq/cKs+6WjIHY9YHSpNXic9rQpZL1oRIEDZaARo\n"
          + "LfTAhAsKG3jf7RpY3PtBWm1r8u0c7lwytlzs16YDMqbo3rcoJ1mIgP97rYlY1R4U\n"
          + "pPKwcNSgPqcCAwEAAaOCA4UwggOBMA4GA1UdDwEB/wQEAwIBhjA7BgNVHSUENDAy\n"
          + "BggrBgEFBQcDAQYIKwYBBQUHAwIGCCsGAQUFBwMDBggrBgEFBQcDBAYIKwYBBQUH\n"
          + "AwgwggHEBgNVHSAEggG7MIIBtzCCAbMGCWCGSAGG/WwCATCCAaQwOgYIKwYBBQUH\n"
          + "AgEWLmh0dHA6Ly93d3cuZGlnaWNlcnQuY29tL3NzbC1jcHMtcmVwb3NpdG9yeS5o\n"
          + "dG0wggFkBggrBgEFBQcCAjCCAVYeggFSAEEAbgB5ACAAdQBzAGUAIABvAGYAIAB0\n"
          + "AGgAaQBzACAAQwBlAHIAdABpAGYAaQBjAGEAdABlACAAYwBvAG4AcwB0AGkAdAB1\n"
          + "AHQAZQBzACAAYQBjAGMAZQBwAHQAYQBuAGMAZQAgAG8AZgAgAHQAaABlACAARABp\n"
          + "AGcAaQBDAGUAcgB0ACAARQBWACAAQwBQAFMAIABhAG4AZAAgAHQAaABlACAAUgBl\n"
          + "AGwAeQBpAG4AZwAgAFAAYQByAHQAeQAgAEEAZwByAGUAZQBtAGUAbgB0ACAAdwBo\n"
          + "AGkAYwBoACAAbABpAG0AaQB0ACAAbABpAGEAYgBpAGwAaQB0AHkAIABhAG4AZAAg\n"
          + "AGEAcgBlACAAaQBuAGMAbwByAHAAbwByAGEAdABlAGQAIABoAGUAcgBlAGkAbgAg\n"
          + "AGIAeQAgAHIAZQBmAGUAcgBlAG4AYwBlAC4wEgYDVR0TAQH/BAgwBgEB/wIBADCB\n"
          + "gwYIKwYBBQUHAQEEdzB1MCQGCCsGAQUFBzABhhhodHRwOi8vb2NzcC5kaWdpY2Vy\n"
          + "dC5jb20wTQYIKwYBBQUHMAKGQWh0dHA6Ly93d3cuZGlnaWNlcnQuY29tL0NBQ2Vy\n"
          + "dHMvRGlnaUNlcnRIaWdoQXNzdXJhbmNlRVZSb290Q0EuY3J0MIGPBgNVHR8EgYcw\n"
          + "gYQwQKA+oDyGOmh0dHA6Ly9jcmwzLmRpZ2ljZXJ0LmNvbS9EaWdpQ2VydEhpZ2hB\n"
          + "c3N1cmFuY2VFVlJvb3RDQS5jcmwwQKA+oDyGOmh0dHA6Ly9jcmw0LmRpZ2ljZXJ0\n"
          + "LmNvbS9EaWdpQ2VydEhpZ2hBc3N1cmFuY2VFVlJvb3RDQS5jcmwwHQYDVR0OBBYE\n"
          + "FExYyyXwQU9S9CjIgUObpqig5pLlMB8GA1UdIwQYMBaAFLE+w2kD+L9HAdSYJhoI\n"
          + "Au9jZCvDMA0GCSqGSIb3DQEBBQUAA4IBAQBMeheHKF0XvLIyc7/NLvVYMR3wsXFU\n"
          + "nNabZ5PbLwM+Fm8eA8lThKNWYB54lBuiqG+jpItSkdfdXJW777UWSemlQk808kf/\n"
          + "roF/E1S3IMRwFcuBCoHLdFfcnN8kpCkMGPAc5K4HM+zxST5Vz25PDVR708noFUjU\n"
          + "xbvcNRx3RQdIRYW9135TuMAW2ZXNi419yWBP0aKb49Aw1rRzNubS+QOy46T15bg+\n"
          + "BEkAui6mSnKDcp33C4ypieez12Qf1uNgywPE3IjpnSUBAHHLA7QpYCWP+UbRe3Gu\n"
          + "zVMSW4SOwg/H7ZMZ2cn6j1g0djIvruFQFGHUqFijyDATI+/GJYw2jxyA\n"
          + "-----END CERTIFICATE-----\n";

          private static final String GITHUB_CERT =
            "-----BEGIN CERTIFICATE-----\n"
          + "MIIHOjCCBiKgAwIBAgIQBH++LkveAITSyvjj7P5wWDANBgkqhkiG9w0BAQUFADBp\n"
          + "MQswCQYDVQQGEwJVUzEVMBMGA1UEChMMRGlnaUNlcnQgSW5jMRkwFwYDVQQLExB3\n"
          + "d3cuZGlnaWNlcnQuY29tMSgwJgYDVQQDEx9EaWdpQ2VydCBIaWdoIEFzc3VyYW5j\n"
          + "ZSBFViBDQS0xMB4XDTEzMDYxMDAwMDAwMFoXDTE1MDkwMjEyMDAwMFowgfAxHTAb\n"
          + "BgNVBA8MFFByaXZhdGUgT3JnYW5pemF0aW9uMRMwEQYLKwYBBAGCNzwCAQMTAlVT\n"
          + "MRkwFwYLKwYBBAGCNzwCAQITCERlbGF3YXJlMRAwDgYDVQQFEwc1MTU3NTUwMRcw\n"
          + "FQYDVQQJEw41NDggNHRoIFN0cmVldDEOMAwGA1UEERMFOTQxMDcxCzAJBgNVBAYT\n"
          + "AlVTMRMwEQYDVQQIEwpDYWxpZm9ybmlhMRYwFAYDVQQHEw1TYW4gRnJhbmNpc2Nv\n"
          + "MRUwEwYDVQQKEwxHaXRIdWIsIEluYy4xEzARBgNVBAMTCmdpdGh1Yi5jb20wggEi\n"
          + "MA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDt04nDXXByCfMzTxpydNm2WpVQ\n"
          + "u2hhn/f7Hxnh2gQxrxV8Gn/5c68d5UMrVgkARWlK6MRb38J3UlEZW9Er2TllNqAy\n"
          + "GRxBc/sysj2fmOyCWws3ZDkstxCDcs3w6iRL+tmULsOFFTmpOvaI2vQniaaVT4Si\n"
          + "N058JXg6yYNtAheVeH1HqFWD7hPIGRqzPPFf/jsC4YX7EWarCV2fTEPwxyReKXIo\n"
          + "ztR1aE8kcimuOSj8341PTYNzdAxvEZun3WLe/+LrF+b/DL/ALTE71lmi8t2HSkh7\n"
          + "bTMRFE00nzI49sgZnfG2PcVG71ELisYz7UhhxB0XG718tmfpOc+lUoAK9OrNAgMB\n"
          + "AAGjggNUMIIDUDAfBgNVHSMEGDAWgBRMWMsl8EFPUvQoyIFDm6aooOaS5TAdBgNV\n"
          + "HQ4EFgQUh9GPGW7kh29TjHeRB1Dfo79VRyAwJQYDVR0RBB4wHIIKZ2l0aHViLmNv\n"
          + "bYIOd3d3LmdpdGh1Yi5jb20wDgYDVR0PAQH/BAQDAgWgMB0GA1UdJQQWMBQGCCsG\n"
          + "AQUFBwMBBggrBgEFBQcDAjBjBgNVHR8EXDBaMCugKaAnhiVodHRwOi8vY3JsMy5k\n"
          + "aWdpY2VydC5jb20vZXZjYTEtZzIuY3JsMCugKaAnhiVodHRwOi8vY3JsNC5kaWdp\n"
          + "Y2VydC5jb20vZXZjYTEtZzIuY3JsMIIBxAYDVR0gBIIBuzCCAbcwggGzBglghkgB\n"
          + "hv1sAgEwggGkMDoGCCsGAQUFBwIBFi5odHRwOi8vd3d3LmRpZ2ljZXJ0LmNvbS9z\n"
          + "c2wtY3BzLXJlcG9zaXRvcnkuaHRtMIIBZAYIKwYBBQUHAgIwggFWHoIBUgBBAG4A\n"
          + "eQAgAHUAcwBlACAAbwBmACAAdABoAGkAcwAgAEMAZQByAHQAaQBmAGkAYwBhAHQA\n"
          + "ZQAgAGMAbwBuAHMAdABpAHQAdQB0AGUAcwAgAGEAYwBjAGUAcAB0AGEAbgBjAGUA\n"
          + "IABvAGYAIAB0AGgAZQAgAEQAaQBnAGkAQwBlAHIAdAAgAEMAUAAvAEMAUABTACAA\n"
          + "YQBuAGQAIAB0AGgAZQAgAFIAZQBsAHkAaQBuAGcAIABQAGEAcgB0AHkAIABBAGcA\n"
          + "cgBlAGUAbQBlAG4AdAAgAHcAaABpAGMAaAAgAGwAaQBtAGkAdAAgAGwAaQBhAGIA\n"
          + "aQBsAGkAdAB5ACAAYQBuAGQAIABhAHIAZQAgAGkAbgBjAG8AcgBwAG8AcgBhAHQA\n"
          + "ZQBkACAAaABlAHIAZQBpAG4AIABiAHkAIAByAGUAZgBlAHIAZQBuAGMAZQAuMH0G\n"
          + "CCsGAQUFBwEBBHEwbzAkBggrBgEFBQcwAYYYaHR0cDovL29jc3AuZGlnaWNlcnQu\n"
          + "Y29tMEcGCCsGAQUFBzAChjtodHRwOi8vY2FjZXJ0cy5kaWdpY2VydC5jb20vRGln\n"
          + "aUNlcnRIaWdoQXNzdXJhbmNlRVZDQS0xLmNydDAMBgNVHRMBAf8EAjAAMA0GCSqG\n"
          + "SIb3DQEBBQUAA4IBAQBfFW1nwzrVo94WnEUzJtU9yRZ0NMqHSBsUkG31q0eGufW4\n"
          + "4wFFZWjuqRJ1n3Ym7xF8fTjP3fdKGQnxIHKSsE0nuuh/XbQX5DpBJknHdGFoLwY8\n"
          + "xZ9JPI57vgvzLo8+fwHyZp3Vm/o5IYLEQViSo+nlOSUQ8YAVqu6KcsP/e612UiqS\n"
          + "+UMBmgdx9KPDDzZy4MJZC2hbfUoXj9A54mJN8cuEOPyw3c3yKOcq/h48KzVguQXi\n"
          + "SdJbwfqNIbQ9oJM+YzDjzS62+TCtNSNWzWbwABZCmuQxK0oEOSbTmbhxUF7rND3/\n"
          + "+mx9u8cY//7uAxLWYS5gIZlCbxcf0lkiKSHJB319\n"
          + "-----END CERTIFICATE-----\n";

    private File mKeyStoreFile;
    private LocalKeyStore mKeyStore;
    private X509Certificate mCert1;
    private X509Certificate mCert2;
    private X509Certificate mCaCert;
    private X509Certificate mCert3;
    private X509Certificate mDigiCert;
    private X509Certificate mGithubCert;


    public TrustManagerFactoryTest() throws CertificateException {
        mCert1 = loadCert(K9_EXAMPLE_COM_CERT1);
        mCert2 = loadCert(K9_EXAMPLE_COM_CERT2);
        mCaCert = loadCert(CA_CERT);
        mCert3 = loadCert(CERT3);
        mDigiCert = loadCert(DIGI_CERT);
        mGithubCert = loadCert(GITHUB_CERT);
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
        assertCertificateRejection(trustManager, new X509Certificate[] { mCert2 });
    }

    public void testCertificateOfOtherHost() throws Exception {
        mKeyStore.addCertificate(MATCHING_HOST, PORT1, mCert1);
        mKeyStore.addCertificate(MATCHING_HOST, PORT2, mCert2);

        X509TrustManager trustManager = TrustManagerFactory.get(MATCHING_HOST, PORT1, true);
        assertCertificateRejection(trustManager, new X509Certificate[] { mCert2 });
    }

    public void testUntrustedCertificateChain() throws Exception {
        X509TrustManager trustManager = TrustManagerFactory.get(MATCHING_HOST, PORT1, true);
        assertCertificateRejection(trustManager, new X509Certificate[] { mCert3, mCaCert });
    }

    public void testLocallyTrustedCertificateChain() throws Exception {
        mKeyStore.addCertificate(MATCHING_HOST, PORT1, mCert3);

        X509TrustManager trustManager = TrustManagerFactory.get(MATCHING_HOST, PORT1, true);
        trustManager.checkServerTrusted(new X509Certificate[] { mCert3, mCaCert }, "authType");
    }

    public void testLocallyTrustedCertificateChainNotMatchingHost() throws Exception {
        mKeyStore.addCertificate(NOT_MATCHING_HOST, PORT1, mCert3);

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
        mKeyStore.addCertificate(MATCHING_HOST, PORT1, mGithubCert);

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
