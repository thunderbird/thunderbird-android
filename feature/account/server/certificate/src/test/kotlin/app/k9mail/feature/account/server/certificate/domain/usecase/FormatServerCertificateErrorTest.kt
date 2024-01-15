package app.k9mail.feature.account.server.certificate.domain.usecase

import app.k9mail.feature.account.server.certificate.domain.entity.ServerCertificateError
import app.k9mail.feature.account.server.certificate.domain.entity.ServerCertificateProperties
import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.text.DateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.test.BeforeTest
import kotlin.test.Test
import okio.ByteString.Companion.decodeHex

class FormatServerCertificateErrorTest {
    @BeforeTest
    fun setTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @Test
    fun `format expired certificate`() {
        val formatCertificateError = FormatServerCertificateError(
            dateFormat = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT, Locale.ROOT),
        )
        val serverCertificateError = ServerCertificateError(
            hostname = "expired.badssl.com",
            port = 443,
            certificateChain = listOf(readCertificate(EXPIRED_CERTIFICATE)),
        )

        val result = formatCertificateError(serverCertificateError)

        assertThat(result.hostname).isEqualTo("expired.badssl.com")
        assertThat(result.serverCertificateProperties).isEqualTo(
            ServerCertificateProperties(
                subjectAlternativeNames = listOf("*.badssl.com", "badssl.com"),
                notValidBefore = "2015 Apr 9 00:00",
                notValidAfter = "2015 Apr 12 23:59",
                subject = "CN=*.badssl.com, OU=PositiveSSL Wildcard, OU=Domain Control Validated",
                issuer = "CN=COMODO RSA Domain Validation Secure Server CA, O=COMODO CA Limited, L=Salford, " +
                    "ST=Greater Manchester, C=GB",
                fingerprintSha1 = "404bbd2f1f4cc2fdeef13aabdd523ef61f1c71f3".decodeHex(),
                fingerprintSha256 = "ba105ce02bac76888ecee47cd4eb7941653e9ac993b61b2eb3dcc82014d21b4f".decodeHex(),
                fingerprintSha512 = (
                    "851d7249d64f85d1242090b06224b6da67d442ae38cea5d8a78ae1d7d8c3e2f8" +
                        "f4ad44c7cf239ba5abb05170e0910fd72e6ea5e5c2604888f6c59e5f57c3db27"
                    ).decodeHex(),
            ),
        )
    }

    @Test
    fun `format certificate without subject alternative names`() {
        val formatCertificateError = FormatServerCertificateError(
            dateFormat = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT, Locale.ROOT),
        )
        val serverCertificateError = ServerCertificateError(
            hostname = "10.0.0.1",
            port = 993,
            certificateChain = listOf(readCertificate(CERTIFICATE_WITHOUT_SAN)),
        )

        val result = formatCertificateError(serverCertificateError)

        assertThat(result.serverCertificateProperties.subjectAlternativeNames).isEmpty()
    }

    private fun readCertificate(asciiArmoredCertificate: String): X509Certificate {
        val inputStream = asciiArmoredCertificate.byteInputStream()

        val certificateFactory = CertificateFactory.getInstance("X.509")
        return certificateFactory.generateCertificate(inputStream) as X509Certificate
    }

    companion object {
        val EXPIRED_CERTIFICATE = """
            -----BEGIN CERTIFICATE-----
            MIIFSzCCBDOgAwIBAgIQSueVSfqavj8QDxekeOFpCTANBgkqhkiG9w0BAQsFADCB
            kDELMAkGA1UEBhMCR0IxGzAZBgNVBAgTEkdyZWF0ZXIgTWFuY2hlc3RlcjEQMA4G
            A1UEBxMHU2FsZm9yZDEaMBgGA1UEChMRQ09NT0RPIENBIExpbWl0ZWQxNjA0BgNV
            BAMTLUNPTU9ETyBSU0EgRG9tYWluIFZhbGlkYXRpb24gU2VjdXJlIFNlcnZlciBD
            QTAeFw0xNTA0MDkwMDAwMDBaFw0xNTA0MTIyMzU5NTlaMFkxITAfBgNVBAsTGERv
            bWFpbiBDb250cm9sIFZhbGlkYXRlZDEdMBsGA1UECxMUUG9zaXRpdmVTU0wgV2ls
            ZGNhcmQxFTATBgNVBAMUDCouYmFkc3NsLmNvbTCCASIwDQYJKoZIhvcNAQEBBQAD
            ggEPADCCAQoCggEBAMIE7PiM7gTCs9hQ1XBYzJMY61yoaEmwIrX5lZ6xKyx2PmzA
            S2BMTOqytMAPgLaw+XLJhgL5XEFdEyt/ccRLvOmULlA3pmccYYz2QULFRtMWhyef
            dOsKnRFSJiFzbIRMeVXk0WvoBj1IFVKtsyjbqv9u/2CVSndrOfEk0TG23U3AxPxT
            uW1CrbV8/q71FdIzSOciccfCFHpsKOo3St/qbLVytH5aohbcabFXRNsKEqveww9H
            dFxBIuGa+RuT5q0iBikusbpJHAwnnqP7i/dAcgCskgjZjFeEU4EFy+b+a1SYQCeF
            xxC7c3DvaRhBB0VVfPlkPz0sw6l865MaTIbRyoUCAwEAAaOCAdUwggHRMB8GA1Ud
            IwQYMBaAFJCvajqUWgvYkOoSVnPfQ7Q6KNrnMB0GA1UdDgQWBBSd7sF7gQs6R2lx
            GH0RN5O8pRs/+zAOBgNVHQ8BAf8EBAMCBaAwDAYDVR0TAQH/BAIwADAdBgNVHSUE
            FjAUBggrBgEFBQcDAQYIKwYBBQUHAwIwTwYDVR0gBEgwRjA6BgsrBgEEAbIxAQIC
            BzArMCkGCCsGAQUFBwIBFh1odHRwczovL3NlY3VyZS5jb21vZG8uY29tL0NQUzAI
            BgZngQwBAgEwVAYDVR0fBE0wSzBJoEegRYZDaHR0cDovL2NybC5jb21vZG9jYS5j
            b20vQ09NT0RPUlNBRG9tYWluVmFsaWRhdGlvblNlY3VyZVNlcnZlckNBLmNybDCB
            hQYIKwYBBQUHAQEEeTB3ME8GCCsGAQUFBzAChkNodHRwOi8vY3J0LmNvbW9kb2Nh
            LmNvbS9DT01PRE9SU0FEb21haW5WYWxpZGF0aW9uU2VjdXJlU2VydmVyQ0EuY3J0
            MCQGCCsGAQUFBzABhhhodHRwOi8vb2NzcC5jb21vZG9jYS5jb20wIwYDVR0RBBww
            GoIMKi5iYWRzc2wuY29tggpiYWRzc2wuY29tMA0GCSqGSIb3DQEBCwUAA4IBAQBq
            evHa/wMHcnjFZqFPRkMOXxQhjHUa6zbgH6QQFezaMyV8O7UKxwE4PSf9WNnM6i1p
            OXy+l+8L1gtY54x/v7NMHfO3kICmNnwUW+wHLQI+G1tjWxWrAPofOxkt3+IjEBEH
            fnJ/4r+3ABuYLyw/zoWaJ4wQIghBK4o+gk783SHGVnRwpDTysUCeK1iiWQ8dSO/r
            ET7BSp68ZVVtxqPv1dSWzfGuJ/ekVxQ8lEEFeouhN0fX9X3c+s5vMaKwjOrMEpsi
            8TRwz311SotoKQwe6Zaoz7ASH1wq7mcvf71z81oBIgxw+s1F73hczg36TuHvzmWf
            RwxPuzZEaFZcVlmtqoq8
            -----END CERTIFICATE-----
        """.trimIndent()

        val CERTIFICATE_WITHOUT_SAN = """
            -----BEGIN CERTIFICATE-----
            MIIDfDCCAmSgAwIBAgIJAJB2iRjpM5OgMA0GCSqGSIb3DQEBCwUAME4xMTAvBgNV
            BAsMKE5vIFNOSSBwcm92aWRlZDsgcGxlYXNlIGZpeCB5b3VyIGNsaWVudC4xGTAX
            BgNVBAMTEGludmFsaWQyLmludmFsaWQwHhcNMTUwMTAxMDAwMDAwWhcNMzAwMTAx
            MDAwMDAwWjBOMTEwLwYDVQQLDChObyBTTkkgcHJvdmlkZWQ7IHBsZWFzZSBmaXgg
            eW91ciBjbGllbnQuMRkwFwYDVQQDExBpbnZhbGlkMi5pbnZhbGlkMIIBIjANBgkq
            hkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAzWJP5cMThJgMBeTvRKKl7N6ZcZAbKDVA
            tNBNnRhIgSitXxCzKtt9rp2RHkLn76oZjdNO25EPp+QgMiWU/rkkB00Y18Oahw5f
            i8s+K9dRv6i+gSOiv2jlIeW/S0hOswUUDH0JXFkEPKILzpl5ML7wdp5kt93vHxa7
            HswOtAxEz2WtxMdezm/3CgO3sls20wl3W03iI+kCt7HyvhGy2aRPLhJfeABpQr0U
            ku3q6mtomy2cgFawekN/X/aH8KknX799MPcuWutM2q88mtUEBsuZmy2nsjK9J7/y
            hhCRDzOV/yY8c5+l/u/rWuwwkZ2lgzGp4xBBfhXdr6+m9kmwWCUm9QIDAQABo10w
            WzAOBgNVHQ8BAf8EBAMCAqQwHQYDVR0lBBYwFAYIKwYBBQUHAwEGCCsGAQUFBwMC
            MA8GA1UdEwEB/wQFMAMBAf8wGQYDVR0OBBIEELsPOJZvPr5PK0bQQWrUrLUwDQYJ
            KoZIhvcNAQELBQADggEBALnZ4lRc9WHtafO4Y+0DWp4qgSdaGygzS/wtcRP+S2V+
            HFOCeYDmeZ9qs0WpNlrtyeBKzBH8hOt9y8aUbZBw2M1F2Mi23Q+dhAEUfQCOKbIT
            tunBuVfDTTbAHUuNl/eyr78v8Egi133z7zVgydVG1KA0AOSCB+B65glbpx+xMCpg
            ZLux9THydwg3tPo/LfYbRCof+Mb8I3ZCY9O6FfZGjuxJn+0ux3SDora3NX/FmJ+i
            kTCTsMtIFWhH3hoyYAamOOuITpPZHD7yP0lfbuncGDEqAQu2YWbYxRixfq2VSxgv
            gWbFcmkgBLYpE8iDWT3Kdluo1+6PHaDaLg2SacOY6Go=
            -----END CERTIFICATE-----
        """.trimIndent()
    }
}
