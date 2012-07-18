package com.fsck.k9.mail.store;

import java.io.ByteArrayInputStream;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;

import android.test.AndroidTestCase;
import android.util.Base64;
import android.util.Log;

import com.fsck.k9.mail.store.TrustManagerFactory.SecureX509TrustManager;

/**
 * Test covers the workaround for htc phones for checking the validity of certificates.
 * @author aatdark
 * @see TrustManagerFactory.checkServerTrustedHTCWorkaround
 *
 */
public class TrustManagerFactoryTest extends AndroidTestCase {

    private static final String LOG_TAG = "TrustManagerFactoryTEST";

    private String cacert = "LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSURtekNDQW9PZ0F3SUJBZ0lKQU5YTy8vMmd4"
            + "ZkNnTUEwR0NTcUdTSWIzRFFFQkJRVUFNR1F4Q3pBSkJnTlYKQkFZVEFrRlVNUW93Q0FZRFZRUUlE"
            + "QUZZTVFvd0NBWURWUVFIREFGWU1Rc3dDUVlEVlFRS0RBSnJPVEVMTUFrRwpBMVVFQ3d3Q2F6a3hF"
            + "REFPQmdOVkJBTU1CMnM1ZEdWemRERXhFVEFQQmdrcWhraUc5dzBCQ1FFV0FtczVNQjRYCkRURXlN"
            + "RGN4TnpJek1UUXhNMW9YRFRJeU1EY3hOVEl6TVRReE0xb3daREVMTUFrR0ExVUVCaE1DUVZReENq"
            + "QUkKQmdOVkJBZ01BVmd4Q2pBSUJnTlZCQWNNQVZneEN6QUpCZ05WQkFvTUFtczVNUXN3Q1FZRFZR"
            + "UUxEQUpyT1RFUQpNQTRHQTFVRUF3d0hhemwwWlhOME1URVJNQThHQ1NxR1NJYjNEUUVKQVJZQ2F6"
            + "a3dnZ0VpTUEwR0NTcUdTSWIzCkRRRUJBUVVBQTRJQkR3QXdnZ0VLQW9JQkFRRFQrV0lndGtjdzI0"
            + "S3lqeUprazRXRjdiOHpOZHkxUzVQRmw4ei8KSmpyYlV5TlZuS3prZVk3UUthbDBMekR5T0lzeVVC"
            + "WDhrUmg0bS9SV1puZUgyUVR1S2VDbHZrRVd3aXMzU1E5UApTY28vRmVieE9TWWxKcCtBaEw1a0Vt"
            + "cGFLRUU4TkM4WmRMdkV4NTFQNU1HemJvVkZzb2VSdlUzeEVDazRyMmZPCmExODRHSU5JVHg2b1U1"
            + "VnpHUmN0T1IrdTZpYllyMUZRVEdGRU53NElTNHJ3c2xudks4cGNmQ2N5TG95UTNKOEMKcFhzRnl6"
            + "enRGd3FQalJ3enhDN09PT0UzaHNtVEtaSlk4eWtHbnZBRVB1MzJnemdPMWlnSGg0cVFhSDMwWE5t"
            + "UgpTenpQMk1zZzlsTDhCL3g5N1NjQnROZjdNQzIySDdCU1kyNmc4THFNczdmZFZkMEJBZ01CQUFH"
            + "alVEQk9NQjBHCkExVWREZ1FXQkJSd2dzTVRPQzJRYVJUcHFNaUFlWXAyM2g2NnhUQWZCZ05WSFNN"
            + "RUdEQVdnQlJ3Z3NNVE9DMlEKYVJUcHFNaUFlWXAyM2g2NnhUQU1CZ05WSFJNRUJUQURBUUgvTUEw"
            + "R0NTcUdTSWIzRFFFQkJRVUFBNElCQVFCcgpxdmQ5SmZrd1kvVHk0L0RzdFVMUVg1SEFPT2REdHlr"
            + "TnUyakpKbEZlUEt3WUx4Y2tHRklzcUtJQVd3SkhEWElPCjNraVJlWjBUUXNqdWRTb2pDSG8rWlR5"
            + "R20rWDVvcmxHWFRZS250bVU4K3RmanFueGNWelJ4RGRJWmJtQ0twSTMKUDcrNmNKOTR2dy9CMlFu"
            + "S1FZRUZCKzg3LzNlQTkvakxYV25RTVYzVHBRMkZMK2M4Z0Vxa3h2N0FOS2RaMzZEaQpzRVlQZ2ty"
            + "d2Q1ZzN2ZFJGOFNRcFVwT1lRdGMwVFZWYVRQQVFWUGUxTGcvMWluTmpXbXNNY0Z1a29EeEtFNWp2"
            + "CmdoNEppNzMyU2F4QzNzME5oekJScGxjY0VNZzA5VHlNTE9sT0RZcGplV2kvQzdZVkJrY1dtdWNq"
            + "eHh1aURNdnkKQUt3YVVIODd3RDEvQlRXcEdabnoKLS0tLS1FTkQgQ0VSVElGSUNBVEUtLS0tLQo=";

    private String cert1 = "LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSURQRENDQWlRQ0FRRXdEUVlKS29aSWh2Y05B"
            + "UUVGQlFBd1pERUxNQWtHQTFVRUJoTUNRVlF4Q2pBSUJnTlYKQkFnTUFWZ3hDakFJQmdOVkJBY01B"
            + "Vmd4Q3pBSkJnTlZCQW9NQW1zNU1Rc3dDUVlEVlFRTERBSnJPVEVRTUE0RwpBMVVFQXd3SGF6bDBa"
            + "WE4wTVRFUk1BOEdDU3FHU0liM0RRRUpBUllDYXprd0hoY05NVEl3TnpFM01qTXlOVFUzCldoY05N"
            + "akl3TnpFMU1qTXlOVFUzV2pCa01Rc3dDUVlEVlFRR0V3SkJWREVLTUFnR0ExVUVDQXdCV0RFTE1B"
            + "a0cKQTFVRUJ3d0Nhemt4Q3pBSkJnTlZCQW9NQW1zNU1Rc3dDUVlEVlFRTERBSnJPVEVRTUE0R0Ex"
            + "VUVBd3dIYXpsMApaWE4wTVRFUU1BNEdDU3FHU0liM0RRRUpBUllCV0RDQ0FTSXdEUVlKS29aSWh2"
            + "Y05BUUVCQlFBRGdnRVBBRENDCkFRb0NnZ0VCQUx5KzJSLy9HL3pZQjAyNGdRUFR4NkF5UERjWURj"
            + "QUdDaXpPdUtKaEMxWk5INzhzaUFHbFppcmcKNFBsMUNodDY2UHFqMUQvTGlPZG9nYTdHaGtrejdo"
            + "VkR3dzB6ZEY3cDhOT1QyZ1NvNlBrT1YxUWJ6cGMwMGRFMgpyTGV6WUZxVlNUM011SXo1R0hxT0w5"
            + "S1ZaK0Vkano3K21ScTZIU2JLb0dxM1RzdFp0bUJPMmlNbll6TGJObW5BCjdFbmRpRlZaaGtrT1VY"
            + "c0FLMVQ3MmptZThJNWV5RkZLZ3U3Z3JZL1gyVVJTRDNUYUhXQzJKczZLNmY2SXMzdnoKOSsrUFo2"
            + "MTJYZ3gzMjJGNnBMbWMvNEpPcXczL0hVMWoraTRoTVpKWjI4WmhnNml5N2Y5dlU0RjRRL0hvS2hL"
            + "SwpyQVd5dC9vd3lCV2FCdFpRU0xzcVdiUGJVUVR4NTFrQ0F3RUFBVEFOQmdrcWhraUc5dzBCQVFV"
            + "RkFBT0NBUUVBCkc2VVJqZzlBcHRrRjdYVno4TUgwUVJON1g4d29FM3JqdUwzTmdDQ0RFWXY5QUVB"
            + "dzJpcHVJbTFkTnpXQy9kWkkKSmlLOEp4OVZrVTdlUktYMXQwUTluUTVNSFFpZjBzTHZMNDExandP"
            + "dTJYeHFpSzFKeG9OaWVLcTlNditzV2lnVgpJS0VrbVVONGdzcUJMelZiMVpKNjM3bFl3OXozZTY1"
            + "NmZSNWt0NlFLcXQ5c0kvekFOT0NWamF5eldreHpkelFFCnpxTmxDd29XODU3TlpOc2ZKZCtqNjlo"
            + "QWlJR1BZcHpLMTk5ZENqeWFTbmQ3cmVVZUdHSXRkKzlkK2pkSFNYWWYKUnB5QXFLRWpqbEplb0xY"
            + "R2N5SWU0ZlNYTU1QVDF4RmZoVHpjZDFpdGhyZ0ZtVFhaeTQ2S1dZekxuNHpGcHd2QgpVZ0s3eS92"
            + "ZnErQWcvV3ZYNVVWVGhnPT0KLS0tLS1FTkQgQ0VSVElGSUNBVEUtLS0tLQo=";



    private String certOTHERCA = "LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSURIRENDQWdRQ0FRRXdEUVlKS29aSWh2Y05B"
            + "UUVGQlFBd1N6RUxNQWtHQTFVRUJoTUNRVlF4Q2pBSUJnTlYKQkFnTUFWZ3hDakFJQmdOVkJBY01B"
            + "Vmd4Q2pBSUJnTlZCQW9NQVZneEN6QUpCZ05WQkFzTUFtczVNUXN3Q1FZRApWUVFEREFKck9UQWVG"
            + "dzB4TWpBM01UY3lNelUzTkRKYUZ3MHlNakEzTVRVeU16VTNOREphTUYweEN6QUpCZ05WCkJBWVRB"
            + "a0ZVTVFvd0NBWURWUVFJREFGWU1Rb3dDQVlEVlFRSERBRllNUW93Q0FZRFZRUUtEQUZZTVFzd0NR"
            + "WUQKVlFRTERBSnJPVEVMTUFrR0ExVUVBd3dDYXpreEVEQU9CZ2txaGtpRzl3MEJDUUVXQVhnd2dn"
            + "RWlNQTBHQ1NxRwpTSWIzRFFFQkFRVUFBNElCRHdBd2dnRUtBb0lCQVFDd2htRlhkL0RnZVNUcy94"
            + "UjFvc2F1c2pXMDNSRmM3OTBKCmtxY2I1Vzk5aXFrcFFVZEpXU2huWGN4ZlZFalNLVVBxbVZnRDQ5"
            + "VG1UQUFjeWVPOGJPdFQzaFd4V0p0c1BLNWEKR1NzTFJFeDM5SmttOTR2RWtpRkZCRWd6NkhtZ1RZ"
            + "YnN3d0EySkJoVnpNZldXTFNWVWZvYTRNdkNzb0krM1B2TgpzWDdNRm9VSVV0UFN2Y2FhNzJlN2to"
            + "ekZRdFZ3U0h1TE9BOWw4UjMwaktzTGJJYm5sMXNhcEkrSmwzMlBKZnRFCkVaNGdjWmV2KzFTYnpU"
            + "RWI1LzJSbWJZKzF4RGljTlpxTUN5bkljNTc1VGl1cE9xdU42b054RUhVSVYwL0FSeWIKTWhsRFcx"
            + "aWEvZVpoUDRsdStSQUlPU2c4a2ZjdHkwelRUMUt6bUJ6NVl3eFFPaHhNQUZqRkFnTUJBQUV3RFFZ"
            + "SgpLb1pJaHZjTkFRRUZCUUFEZ2dFQkFOYVJTSENyMDlFeDEwaTU5Z0VKRkRDVUdUUlhMMC9sZVhr"
            + "Vk1tNndZS3J5CmRKRmd1WmxRQmRHQzQ3U1QwVSsxbEJRUlQ2ZFFJM0swSEJUQ1pFbHNNTExyTUJu"
            + "RCt2dFh6ODBkY2k1d3ltaHoKNHdDU2RVajZVMVNiWlY1L3RLNVVGd045TVJ0ZFVhVUhkOWd6WTl3"
            + "NWUzdnhyb3prcXlvVE5YTDdadjdJUUhBbApwWDltQVBWUEF0R1lod2Nrd1BvQjBYWFdDY3VVWVBU"
            + "RXNjRURRZnk5NzNWL3BKMjJ3OGY1aDYwclVJZVdKYk9UCnduVjk2TGY5YVZpTHNna2VmQmcxeWM0"
            + "bGRSUEZnSmxranNTUjlwZStPQURmTVdnSk5EVXI4LzI1LzJnRGJFV0YKcVRLMW05TlB2REJJRHpm"
            + "eWNxdGNVc2RPdmt5MktFM2p2ci9vQWEwYW5tbz0KLS0tLS1FTkQgQ0VSVElGSUNBVEUtLS0tLQo=";


    /**
     * this tests checks if the (jdk) .verify method works as expected
     * @throws Exception
     */
    public void testaone() throws Exception {
        X509Certificate xcert1 = getCertFromString(cert1);
        X509Certificate xcacert = getCertFromString(cacert);
        Log.e("TEST", "" + xcert1);
        Log.e("TEST", "" + xcacert);
        xcert1.verify(xcacert.getPublicKey());

    }

    /**
     * test if correct CA works
     * @throws Exception
     */
    public void testTrustManagerHTCWorkaroundCA() throws Exception {
        SecureX509TrustManager f = (SecureX509TrustManager) TrustManagerFactory.get("unitttest", true);
        /* create the alias -> certificate map */
        Map<String, Certificate> installedCertsMap = new HashMap<String, Certificate>();

        installedCertsMap.put("ca", getCertFromString(cacert));
        X509Certificate[] chain = new X509Certificate[] { getCertFromString(cert1)};
        f.checkServerTrustedHTCWorkaround(installedCertsMap, chain, "");
        /* should return without throwing an exception */
    }

    /**
     * test if wrong server key leads to exception
     * @throws Exception
     */
    public void testTrustManagerHTCWorkaroundNoCAWrongCert() throws  Exception {
        SecureX509TrustManager f = (SecureX509TrustManager) TrustManagerFactory.get("unitttest", true);
        /* create the alias -> certificate map */
        Map<String, Certificate> installedCertsMap = new HashMap<String, Certificate>();

        installedCertsMap.put("ca", getCertFromString(certOTHERCA));
        X509Certificate[] chain = new X509Certificate[] { getCertFromString(cert1) };
        try {
            f.checkServerTrustedHTCWorkaround(installedCertsMap, chain, "");
            /* should be never reached */
            Assert.assertTrue(false);
        } catch (Exception e) {
        }
    }

    /**
     * creates an java.x509 Certifcate from an base64 encoded plaintext cert
     * @param cert
     * @return
     * @throws Exception
     */
    private X509Certificate getCertFromString(String cert) throws Exception {
        byte[] certData1 = Base64.decode(cert, 0);
        ByteArrayInputStream bis = new ByteArrayInputStream(javax.security.cert.X509Certificate.getInstance(certData1).getEncoded());
        java.security.cert.CertificateFactory cf = java.security.cert.CertificateFactory.getInstance("X.509");
        X509Certificate cert1 = (java.security.cert.X509Certificate)cf.generateCertificate(bis);

        return cert1;
    }
}
