package net.thunderbird.feature.mail.message.composer.internal.smime

import assertk.assertThat
import assertk.assertions.isEqualTo
import java.io.ByteArrayInputStream
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.cert.CertificateFactory
import java.security.cert.TrustAnchor
import java.security.cert.X509Certificate
import java.security.spec.ECGenParameterSpec
import javax.security.auth.x500.X500Principal
import kotlin.test.Test
import net.thunderbird.components.core.logging.testing.TestLogger
import net.thunderbird.feature.mail.message.composer.smime.SmimeCertificateInfo
import net.thunderbird.feature.mail.message.composer.smime.SmimeKeyChainProvider
import net.thunderbird.feature.mail.message.composer.smime.SmimeSigningCredentials
import net.thunderbird.feature.mail.message.composer.smime.SmimeSigningException
import net.thunderbird.feature.mail.message.composer.smime.SmimeVerificationStatus

class DefaultSmimeVerifierTest {

    private val nowMillis = System.currentTimeMillis()

    @Test
    fun `verify should return VALID_TRUSTED for trusted RSA signature in Base64`() {
        // Arrange
        val keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        val cert = createSelfSignedCertificate(
            keyPair = keyPair,
            subjectDn = "CN=Alice Example, EMAILADDRESS=alice@example.com",
            notBeforeMillis = nowMillis - ONE_DAY_MILLIS,
            notAfterMillis = nowMillis + ONE_YEAR_MILLIS,
            signatureAlgName = "SHA256withRSA",
            signatureAlgOid = "1.2.840.113549.1.1.11",
            includeNullParams = true,
        )
        val signer = createSigner(alias = "alice", keyPair = keyPair, cert = cert)
        val mimeBytes = "Content-Type: text/plain; charset=utf-8\r\n\r\nHello S/MIME!\r\n".toByteArray(Charsets.UTF_8)
        val signed = signer.sign(mimeBytes, "alice")
        val testSubject = DefaultSmimeVerifier(
            cmsSignedDataVerifier = CmsSignedDataVerifier(
                trustAnchorsProvider = { setOf(TrustAnchor(cert, null)) },
            ),
            logger = TestLogger(),
        )

        // Act
        val result = testSubject.verify(mimeBytes, signed.base64Signature, "alice@example.com")

        // Assert
        assertThat(result.status).isEqualTo(SmimeVerificationStatus.VALID_TRUSTED)
        assertThat(result.signerDisplayName).isEqualTo("Alice Example")
        assertThat(result.signerEmail).isEqualTo("alice@example.com")
        assertThat(result.issuerDisplayName).isEqualTo("Alice Example")
    }

    @Test
    fun `verify should return VALID_UNTRUSTED for valid self-signed ECDSA signature not in trust store`() {
        // Arrange
        val keyPair = KeyPairGenerator.getInstance("EC").apply {
            initialize(ECGenParameterSpec("secp256r1"))
        }.generateKeyPair()
        val cert = createSelfSignedCertificate(
            keyPair = keyPair,
            subjectDn = "CN=Bob SelfSigned, EMAILADDRESS=bob@example.com",
            notBeforeMillis = nowMillis - ONE_DAY_MILLIS,
            notAfterMillis = nowMillis + ONE_YEAR_MILLIS,
            signatureAlgName = "SHA256withECDSA",
            signatureAlgOid = "1.2.840.10045.4.3.2",
            includeNullParams = false,
        )
        val signer = createSigner(alias = "bob", keyPair = keyPair, cert = cert)
        val mimeBytes = "Content-Type: text/plain; charset=utf-8\r\n\r\nSigned ECDSA email".toByteArray(Charsets.UTF_8)
        val signed = signer.sign(mimeBytes, "bob")
        val testSubject = DefaultSmimeVerifier(
            cmsSignedDataVerifier = CmsSignedDataVerifier(trustAnchorsProvider = { emptySet() }),
            logger = TestLogger(),
        )

        // Act
        val result = testSubject.verify(mimeBytes, signed.derSignature, "bob@example.com")

        // Assert
        assertThat(result.status).isEqualTo(SmimeVerificationStatus.VALID_UNTRUSTED)
        assertThat(result.signerDisplayName).isEqualTo("Bob SelfSigned")
    }

    @Test
    fun `verify should return INVALID_SIGNATURE when content is tampered`() {
        // Arrange
        val keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        val cert = createSelfSignedCertificate(
            keyPair = keyPair,
            subjectDn = "CN=Alice, EMAILADDRESS=alice@example.com",
            notBeforeMillis = nowMillis - ONE_DAY_MILLIS,
            notAfterMillis = nowMillis + ONE_YEAR_MILLIS,
            signatureAlgName = "SHA256withRSA",
            signatureAlgOid = "1.2.840.113549.1.1.11",
            includeNullParams = true,
        )
        val signer = createSigner(alias = "alice", keyPair = keyPair, cert = cert)
        val original = "Content-Type: text/plain\r\n\r\nOriginal body\r\n".toByteArray(Charsets.UTF_8)
        val tampered = "Content-Type: text/plain\r\n\r\nTampered body\r\n".toByteArray(Charsets.UTF_8)
        val signed = signer.sign(original, "alice")
        val testSubject = DefaultSmimeVerifier(
            cmsSignedDataVerifier = CmsSignedDataVerifier(
                trustAnchorsProvider = { setOf(TrustAnchor(cert, null)) },
            ),
            logger = TestLogger(),
        )

        // Act
        val result = testSubject.verify(tampered, signed.base64Signature, "alice@example.com")

        // Assert
        assertThat(result.status).isEqualTo(SmimeVerificationStatus.INVALID_SIGNATURE)
    }

    @Test
    fun `verify should return VALID_SENDER_MISMATCH when signer email differs from From address`() {
        // Arrange
        val keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        val cert = createSelfSignedCertificate(
            keyPair = keyPair,
            subjectDn = "CN=Alice, EMAILADDRESS=alice@example.com",
            notBeforeMillis = nowMillis - ONE_DAY_MILLIS,
            notAfterMillis = nowMillis + ONE_YEAR_MILLIS,
            signatureAlgName = "SHA256withRSA",
            signatureAlgOid = "1.2.840.113549.1.1.11",
            includeNullParams = true,
        )
        val signer = createSigner(alias = "alice", keyPair = keyPair, cert = cert)
        val mimeBytes = "Content-Type: text/plain\r\n\r\nMessage\r\n".toByteArray(Charsets.UTF_8)
        val signed = signer.sign(mimeBytes, "alice")
        val testSubject = DefaultSmimeVerifier(
            cmsSignedDataVerifier = CmsSignedDataVerifier(
                trustAnchorsProvider = { setOf(TrustAnchor(cert, null)) },
            ),
            logger = TestLogger(),
        )

        // Act
        val result = testSubject.verify(mimeBytes, signed.base64Signature, "other@example.com")

        // Assert
        assertThat(result.status).isEqualTo(SmimeVerificationStatus.VALID_SENDER_MISMATCH)
    }

    @Test
    fun `verify should return VALID_EXPIRED when certificate is expired`() {
        // Arrange
        val keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        val cert = createSelfSignedCertificate(
            keyPair = keyPair,
            subjectDn = "CN=Expired Alice, EMAILADDRESS=alice@example.com",
            notBeforeMillis = nowMillis - 2 * ONE_YEAR_MILLIS,
            notAfterMillis = nowMillis - ONE_DAY_MILLIS,
            signatureAlgName = "SHA256withRSA",
            signatureAlgOid = "1.2.840.113549.1.1.11",
            includeNullParams = true,
        )
        val signer = createSigner(alias = "alice", keyPair = keyPair, cert = cert)
        val mimeBytes = "Content-Type: text/plain\r\n\r\nExpired cert message\r\n".toByteArray(Charsets.UTF_8)
        val signed = signer.sign(mimeBytes, "alice")
        val testSubject = DefaultSmimeVerifier(
            cmsSignedDataVerifier = CmsSignedDataVerifier(
                trustAnchorsProvider = { setOf(TrustAnchor(cert, null)) },
            ),
            logger = TestLogger(),
        )

        // Act
        val result = testSubject.verify(mimeBytes, signed.base64Signature, "alice@example.com")

        // Assert
        assertThat(result.status).isEqualTo(SmimeVerificationStatus.VALID_EXPIRED)
    }

    @Test
    fun `verify should tolerate single trailing CRLF difference`() {
        // Arrange
        val keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        val cert = createSelfSignedCertificate(
            keyPair = keyPair,
            subjectDn = "CN=Alice, EMAILADDRESS=alice@example.com",
            notBeforeMillis = nowMillis - ONE_DAY_MILLIS,
            notAfterMillis = nowMillis + ONE_YEAR_MILLIS,
            signatureAlgName = "SHA256withRSA",
            signatureAlgOid = "1.2.840.113549.1.1.11",
            includeNullParams = true,
        )
        val signer = createSigner(alias = "alice", keyPair = keyPair, cert = cert)
        val signedBytes = "Content-Type: text/plain\r\n\r\nHello".toByteArray(Charsets.UTF_8)
        val storedBytes = "Content-Type: text/plain\r\n\r\nHello\r\n".toByteArray(Charsets.UTF_8)
        val signed = signer.sign(signedBytes, "alice")
        val testSubject = DefaultSmimeVerifier(
            cmsSignedDataVerifier = CmsSignedDataVerifier(
                trustAnchorsProvider = { setOf(TrustAnchor(cert, null)) },
            ),
            logger = TestLogger(),
        )

        // Act
        val result = testSubject.verify(storedBytes, signed.base64Signature, "alice@example.com")

        // Assert
        assertThat(result.status).isEqualTo(SmimeVerificationStatus.VALID_TRUSTED)
    }

    @Test
    fun `verify should return INVALID_SIGNATURE for malformed bytes`() {
        // Arrange
        val testSubject = DefaultSmimeVerifier(
            cmsSignedDataVerifier = CmsSignedDataVerifier(trustAnchorsProvider = { emptySet() }),
            logger = TestLogger(),
        )

        // Act
        val result = testSubject.verify("hello".toByteArray(), "not-valid-base64!!!".toByteArray(), null)

        // Assert
        assertThat(result.status).isEqualTo(SmimeVerificationStatus.INVALID_SIGNATURE)
    }

    private fun createSigner(alias: String, keyPair: KeyPair, cert: X509Certificate): DefaultSmimeSigner {
        val fakeProvider = object : SmimeKeyChainProvider {
            override fun getSigningCredentials(requestedAlias: String): SmimeSigningCredentials {
                if (requestedAlias != alias) throw SmimeSigningException("Missing alias")
                return SmimeSigningCredentials(alias, keyPair.private, listOf(cert))
            }

            override fun getCertificateInfo(alias: String): SmimeCertificateInfo? = null
        }
        return DefaultSmimeSigner(
            keyChainProvider = fakeProvider,
            cmsSignedDataBuilder = CmsSignedDataBuilder(clock = { nowMillis }),
            logger = TestLogger(),
        )
    }

    private fun createSelfSignedCertificate(
        keyPair: KeyPair,
        subjectDn: String,
        notBeforeMillis: Long,
        notAfterMillis: Long,
        signatureAlgName: String,
        signatureAlgOid: String,
        includeNullParams: Boolean,
    ): X509Certificate {
        val principal = X500Principal(subjectDn)
        val algId = if (includeNullParams) {
            DerEncoder.encodeSequence(DerEncoder.encodeOid(signatureAlgOid), DerEncoder.encodeNull())
        } else {
            DerEncoder.encodeSequence(DerEncoder.encodeOid(signatureAlgOid))
        }
        val validity = DerEncoder.encodeSequence(
            DerEncoder.encodeTime(notBeforeMillis),
            DerEncoder.encodeTime(notAfterMillis),
        )
        val tbsCertificate = DerEncoder.encodeSequence(
            DerEncoder.encodeContextSpecific(0, constructed = true, content = DerEncoder.encodeInteger(2)),
            DerEncoder.encodeInteger(BigInteger.valueOf(987654321L)),
            algId,
            principal.encoded,
            validity,
            principal.encoded,
            keyPair.public.encoded,
        )
        val sigBytes = Signature.getInstance(signatureAlgName).run {
            initSign(keyPair.private)
            update(tbsCertificate)
            sign()
        }
        val content = ByteArray(sigBytes.size + 1).apply {
            this[0] = 0x00
            sigBytes.copyInto(this, destinationOffset = 1)
        }
        val lengthBytes = when {
            content.size < 128 -> byteArrayOf(content.size.toByte())
            content.size < 256 -> byteArrayOf(0x81.toByte(), content.size.toByte())
            else -> byteArrayOf(0x82.toByte(), (content.size ushr 8).toByte(), (content.size and 0xFF).toByte())
        }
        val bitString = byteArrayOf(0x03) + lengthBytes + content
        val certDer = DerEncoder.encodeSequence(tbsCertificate, algId, bitString)
        return CertificateFactory.getInstance("X.509")
            .generateCertificate(ByteArrayInputStream(certDer)) as X509Certificate
    }

    private companion object {
        const val ONE_DAY_MILLIS = 86_400_000L
        const val ONE_YEAR_MILLIS = 31_536_000_000L
    }
}
