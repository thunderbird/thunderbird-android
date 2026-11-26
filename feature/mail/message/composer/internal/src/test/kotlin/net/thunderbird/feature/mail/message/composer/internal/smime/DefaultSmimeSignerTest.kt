package net.thunderbird.feature.mail.message.composer.internal.smime

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import assertk.assertions.isInstanceOf
import assertk.assertions.isLessThanOrEqualTo
import assertk.assertions.isTrue
import java.io.ByteArrayInputStream
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.Signature
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.ECGenParameterSpec
import java.util.Base64
import javax.security.auth.x500.X500Principal
import kotlin.test.Test
import net.thunderbird.components.core.logging.testing.TestLogger
import net.thunderbird.feature.mail.message.composer.smime.SmimeCertificateInfo
import net.thunderbird.feature.mail.message.composer.smime.SmimeKeyChainProvider
import net.thunderbird.feature.mail.message.composer.smime.SmimeSigningCredentials
import net.thunderbird.feature.mail.message.composer.smime.SmimeSigningException

class DefaultSmimeSignerTest {

    private val fixedClockMillis = 1_767_323_045_000L // 2026-01-02T03:04:05Z

    @Test
    fun `sign with RSA key should produce verifiable CMS SignedData and valid MIME Base64`() {
        // Arrange
        val keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        val cert = createSelfSignedCertificate(
            keyPair = keyPair,
            subjectDn = "CN=Alice Example",
            signatureAlgName = "SHA256withRSA",
            signatureAlgOid = "1.2.840.113549.1.1.11",
            includeNullParams = true,
        )
        val fakeProvider = FakeSmimeKeyChainProvider(
            credentialsByAlias = mapOf(
                "alice-rsa" to SmimeSigningCredentials("alice-rsa", keyPair.private, listOf(cert)),
            ),
        )
        val testSubject = DefaultSmimeSigner(
            keyChainProvider = fakeProvider,
            cmsSignedDataBuilder = CmsSignedDataBuilder(clock = { fixedClockMillis }),
            logger = TestLogger(),
        )
        val canonicalMime = "Content-Type: text/plain; charset=utf-8\r\n\r\nHello S/MIME world!\r\n"
            .toByteArray(Charsets.UTF_8)

        // Act
        val result = testSubject.sign(canonicalMime, "alice-rsa")

        // Assert
        assertThat(result.micAlg).isEqualTo("sha-256")
        assertValidMimeBase64(result.base64Signature, result.derSignature)
        assertValidCmsDetachedSignature(
            derBytes = result.derSignature,
            canonicalMime = canonicalMime,
            expectedDigestAlg = "SHA-256",
            expectedSignatureAlg = "SHA256withRSA",
            expectedCert = cert,
        )
    }

    @Test
    fun `sign with ECDSA P-256 key should produce verifiable CMS SignedData`() {
        // Arrange
        val keyPair = KeyPairGenerator.getInstance("EC").apply {
            initialize(ECGenParameterSpec("secp256r1"))
        }.generateKeyPair()
        val cert = createSelfSignedCertificate(
            keyPair = keyPair,
            subjectDn = "CN=Bob ECDSA",
            signatureAlgName = "SHA256withECDSA",
            signatureAlgOid = "1.2.840.10045.4.3.2",
            includeNullParams = false,
        )
        val fakeProvider = FakeSmimeKeyChainProvider(
            credentialsByAlias = mapOf(
                "bob-ec" to SmimeSigningCredentials("bob-ec", keyPair.private, listOf(cert)),
            ),
        )
        val testSubject = DefaultSmimeSigner(
            keyChainProvider = fakeProvider,
            cmsSignedDataBuilder = CmsSignedDataBuilder(clock = { fixedClockMillis }),
            logger = TestLogger(),
        )
        val canonicalMime = "Content-Type: text/plain; charset=utf-8\r\n\r\nSigned with ECDSA\r\n"
            .toByteArray(Charsets.UTF_8)

        // Act
        val result = testSubject.sign(canonicalMime, "bob-ec")

        // Assert
        assertThat(result.micAlg).isEqualTo("sha-256")
        assertValidMimeBase64(result.base64Signature, result.derSignature)
        assertValidCmsDetachedSignature(
            derBytes = result.derSignature,
            canonicalMime = canonicalMime,
            expectedDigestAlg = "SHA-256",
            expectedSignatureAlg = "SHA256withECDSA",
            expectedCert = cert,
        )
    }

    @Test
    fun `sign with ECDSA P-384 key should select sha-384 digest and signature`() {
        // Arrange
        val keyPair = KeyPairGenerator.getInstance("EC").apply {
            initialize(ECGenParameterSpec("secp384r1"))
        }.generateKeyPair()
        val cert = createSelfSignedCertificate(
            keyPair = keyPair,
            subjectDn = "CN=Carol P384",
            signatureAlgName = "SHA384withECDSA",
            signatureAlgOid = "1.2.840.10045.4.3.3",
            includeNullParams = false,
        )
        val fakeProvider = FakeSmimeKeyChainProvider(
            credentialsByAlias = mapOf(
                "carol-ec384" to SmimeSigningCredentials("carol-ec384", keyPair.private, listOf(cert)),
            ),
        )
        val testSubject = DefaultSmimeSigner(
            keyChainProvider = fakeProvider,
            cmsSignedDataBuilder = CmsSignedDataBuilder(clock = { fixedClockMillis }),
            logger = TestLogger(),
        )
        val canonicalMime = "Content-Type: text/plain; charset=utf-8\r\n\r\nSigned with P-384\r\n"
            .toByteArray(Charsets.UTF_8)

        // Act
        val result = testSubject.sign(canonicalMime, "carol-ec384")

        // Assert
        assertThat(result.micAlg).isEqualTo("sha-384")
        assertValidCmsDetachedSignature(
            derBytes = result.derSignature,
            canonicalMime = canonicalMime,
            expectedDigestAlg = "SHA-384",
            expectedSignatureAlg = "SHA384withECDSA",
            expectedCert = cert,
        )
    }

    @Test
    fun `sign should propagate SmimeSigningException when alias is missing`() {
        // Arrange
        val fakeProvider = FakeSmimeKeyChainProvider(emptyMap())
        val testSubject = DefaultSmimeSigner(
            keyChainProvider = fakeProvider,
            cmsSignedDataBuilder = CmsSignedDataBuilder(clock = { fixedClockMillis }),
            logger = TestLogger(),
        )

        // Act & Assert
        assertFailure {
            testSubject.sign("test".toByteArray(), "missing-alias")
        }.isInstanceOf<SmimeSigningException>()
    }

    private fun assertValidMimeBase64(base64Bytes: ByteArray, expectedDer: ByteArray) {
        val text = String(base64Bytes, Charsets.US_ASCII)
        assertThat(text.endsWith("\r\n")).isTrue()
        val lines = text.split("\r\n").dropLast(1)
        assertThat(lines.size).isGreaterThan(0)
        for (line in lines) {
            assertThat(line.length).isLessThanOrEqualTo(76)
        }
        val decoded = Base64.getMimeDecoder().decode(text)
        assertThat(decoded).isEqualTo(expectedDer)
    }

    private fun assertValidCmsDetachedSignature(
        derBytes: ByteArray,
        canonicalMime: ByteArray,
        expectedDigestAlg: String,
        expectedSignatureAlg: String,
        expectedCert: X509Certificate,
    ) {
        val contentInfo = parseDerNode(derBytes, 0).first
        assertThat(contentInfo.tag).isEqualTo(0x30)
        val contentInfoChildren = parseDerChildren(contentInfo.value)
        assertThat(contentInfoChildren[0].raw).isEqualTo(DerEncoder.encodeOid("1.2.840.113549.1.7.2"))
        assertThat(contentInfoChildren[1].tag).isEqualTo(0xA0)

        val signedData = parseDerNode(contentInfoChildren[1].value, 0).first
        assertThat(signedData.tag).isEqualTo(0x30)
        val signedDataChildren = parseDerChildren(signedData.value)
        // version = 1
        assertThat(signedDataChildren[0].raw).isEqualTo(DerEncoder.encodeInteger(1))
        // encapContentInfo = SEQUENCE { id-data } (detached: only 1 child)
        val encapChildren = parseDerChildren(signedDataChildren[2].value)
        assertThat(encapChildren.size).isEqualTo(1)
        assertThat(encapChildren[0].raw).isEqualTo(DerEncoder.encodeOid("1.2.840.113549.1.7.1"))
        // certificates [0] IMPLICIT
        assertThat(signedDataChildren[3].tag).isEqualTo(0xA0)
        assertThat(signedDataChildren[3].value).isEqualTo(expectedCert.encoded)

        // signerInfos SET OF SignerInfo
        val signerInfos = parseDerChildren(signedDataChildren[4].value)
        assertThat(signerInfos.size).isEqualTo(1)
        val signerInfoChildren = parseDerChildren(signerInfos[0].value)
        // version = 1
        assertThat(signerInfoChildren[0].raw).isEqualTo(DerEncoder.encodeInteger(1))
        // sid = IssuerAndSerialNumber
        val sidChildren = parseDerChildren(signerInfoChildren[1].value)
        assertThat(sidChildren[0].raw).isEqualTo(expectedCert.issuerX500Principal.encoded)
        assertThat(sidChildren[1].raw).isEqualTo(DerEncoder.encodeInteger(expectedCert.serialNumber))

        // signedAttrs [0] IMPLICIT
        val signedAttrsImplicit = signerInfoChildren[3]
        assertThat(signedAttrsImplicit.tag).isEqualTo(0xA0)
        val expectedDigest = MessageDigest.getInstance(expectedDigestAlg).digest(canonicalMime)
        val expectedDigestAttrValue = DerEncoder.encodeOctetString(expectedDigest)
        val attrs = parseDerChildren(signedAttrsImplicit.value)
        val digestAttr = attrs.first { attr ->
            parseDerChildren(attr.value)[0].raw.contentEquals(DerEncoder.encodeOid("1.2.840.113549.1.9.4"))
        }
        val digestSet = parseDerChildren(parseDerChildren(digestAttr.value)[1].value)
        assertThat(digestSet[0].raw).isEqualTo(expectedDigestAttrValue)

        // Verify cryptographic signature over EXPLICIT SET (0x31) of signedAttrs
        val signedAttrsAsExplicitSet = DerEncoder.encodeSet(attrs.map { it.raw })
        val signatureOctetString = signerInfoChildren[5]
        assertThat(signatureOctetString.tag).isEqualTo(0x04)

        val verifier = Signature.getInstance(expectedSignatureAlg).apply {
            initVerify(expectedCert.publicKey)
            update(signedAttrsAsExplicitSet)
        }
        assertThat(verifier.verify(signatureOctetString.value)).isTrue()
    }

    private fun createSelfSignedCertificate(
        keyPair: KeyPair,
        subjectDn: String,
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
            DerEncoder.encodeTime(fixedClockMillis - 86_400_000L),
            DerEncoder.encodeTime(fixedClockMillis + 31_536_000_000L),
        )
        val tbsCertificate = DerEncoder.encodeSequence(
            DerEncoder.encodeContextSpecific(0, constructed = true, content = DerEncoder.encodeInteger(2)),
            DerEncoder.encodeInteger(BigInteger.valueOf(123456789L)),
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
        val bitString = encodeBitString(sigBytes)
        val certDer = DerEncoder.encodeSequence(tbsCertificate, algId, bitString)
        val factory = CertificateFactory.getInstance("X.509")
        return factory.generateCertificate(ByteArrayInputStream(certDer)) as X509Certificate
    }

    private fun encodeBitString(bytes: ByteArray): ByteArray {
        val content = ByteArray(bytes.size + 1)
        content[0] = 0x00 // 0 unused bits
        bytes.copyInto(content, destinationOffset = 1)
        val lengthBytes = when {
            content.size < 128 -> byteArrayOf(content.size.toByte())
            content.size < 256 -> byteArrayOf(0x81.toByte(), content.size.toByte())
            else -> byteArrayOf(0x82.toByte(), (content.size ushr 8).toByte(), (content.size and 0xFF).toByte())
        }
        return byteArrayOf(0x03) + lengthBytes + content
    }

    private data class DerNode(
        val tag: Int,
        val value: ByteArray,
        val raw: ByteArray,
    )

    private fun parseDerChildren(bytes: ByteArray): List<DerNode> {
        val nodes = mutableListOf<DerNode>()
        var offset = 0
        while (offset < bytes.size) {
            val (node, nextOffset) = parseDerNode(bytes, offset)
            nodes.add(node)
            offset = nextOffset
        }
        return nodes
    }

    private fun parseDerNode(bytes: ByteArray, startOffset: Int): Pair<DerNode, Int> {
        val tag = bytes[startOffset].toInt() and 0xFF
        val firstLenByte = bytes[startOffset + 1].toInt() and 0xFF
        val length: Int
        val valueStart: Int
        if (firstLenByte < 128) {
            length = firstLenByte
            valueStart = startOffset + 2
        } else {
            val numLenBytes = firstLenByte and 0x7F
            var acc = 0
            for (i in 0 until numLenBytes) {
                acc = (acc shl 8) or (bytes[startOffset + 2 + i].toInt() and 0xFF)
            }
            length = acc
            valueStart = startOffset + 2 + numLenBytes
        }
        val endOffset = valueStart + length
        val value = bytes.copyOfRange(valueStart, endOffset)
        val raw = bytes.copyOfRange(startOffset, endOffset)
        return DerNode(tag, value, raw) to endOffset
    }

    private class FakeSmimeKeyChainProvider(
        private val credentialsByAlias: Map<String, SmimeSigningCredentials>,
    ) : SmimeKeyChainProvider {
        override fun getSigningCredentials(alias: String): SmimeSigningCredentials {
            return credentialsByAlias[alias] ?: throw SmimeSigningException("Certificate not found for alias")
        }

        override fun getCertificateInfo(alias: String): SmimeCertificateInfo? = null
    }
}
