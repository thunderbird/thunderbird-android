package net.thunderbird.feature.mail.message.composer.internal.smime

import java.security.GeneralSecurityException
import java.security.MessageDigest
import java.security.Signature
import java.security.cert.X509Certificate
import java.security.interfaces.ECPublicKey
import java.util.Locale
import net.thunderbird.feature.mail.message.composer.smime.SmimeSigningCredentials
import net.thunderbird.feature.mail.message.composer.smime.SmimeSigningException

internal class CmsDetachedSignature(
    val derBytes: ByteArray,
    val micAlg: String,
)

/**
 * Builds RFC 5652 / RFC 8551 detached CMS SignedData structures using standard JCA primitives,
 * compatible with Android KeyChain hardware-backed private keys.
 */
@Suppress("MagicNumber")
internal class CmsSignedDataBuilder(
    private val clock: () -> Long = System::currentTimeMillis,
) {
    @Throws(SmimeSigningException::class)
    fun buildDetachedSignature(
        canonicalMimeContent: ByteArray,
        credentials: SmimeSigningCredentials,
    ): CmsDetachedSignature {
        val certificateChain = credentials.certificateChain
        val signerCert = certificateChain.firstOrNull()
            ?: throw SmimeSigningException("Certificate chain is empty")
        val algorithmSpec = resolveAlgorithmSpec(credentials.privateKey.algorithm, signerCert)

        return try {
            val messageDigestBytes = MessageDigest.getInstance(algorithmSpec.jcaDigestName)
                .digest(canonicalMimeContent)

            val signedAttrElements = buildSortedSignedAttributes(
                messageDigestBytes = messageDigestBytes,
                signingTimeMillis = clock(),
            )

            // RFC 5652 §5.4: SignedAttributes are signed as an EXPLICIT SET OF (tag 0x31),
            // even though they are encoded with IMPLICIT [0] (tag 0xA0) inside SignerInfo.
            val signedAttrsForSigning = DerEncoder.encodeSet(signedAttrElements)
            val signatureBytes = computeSignature(
                jcaSignatureAlgorithm = algorithmSpec.jcaSignatureName,
                credentials = credentials,
                data = signedAttrsForSigning,
            )

            val digestAlgId = encodeAlgorithmIdentifier(
                oid = algorithmSpec.digestOid,
                includeNullParams = algorithmSpec.includeNullDigestParams,
            )
            val signatureAlgId = encodeAlgorithmIdentifier(
                oid = algorithmSpec.signatureOid,
                includeNullParams = algorithmSpec.includeNullSignatureParams,
            )

            val signerInfo = buildSignerInfo(
                signerCert = signerCert,
                digestAlgId = digestAlgId,
                sortedSignedAttrs = signedAttrElements,
                signatureAlgId = signatureAlgId,
                signatureBytes = signatureBytes,
            )

            val signedData = buildSignedData(
                digestAlgId = digestAlgId,
                certificateChain = certificateChain,
                signerInfo = signerInfo,
            )

            val contentInfo = DerEncoder.encodeSequence(
                DerEncoder.encodeOid(OID_SIGNED_DATA),
                DerEncoder.encodeContextSpecific(tagNumber = 0, constructed = true, content = signedData),
            )

            CmsDetachedSignature(
                derBytes = contentInfo,
                micAlg = algorithmSpec.micAlg,
            )
        } catch (e: GeneralSecurityException) {
            throw SmimeSigningException("Failed to create S/MIME cryptographic signature", e)
        }
    }

    private fun buildSortedSignedAttributes(
        messageDigestBytes: ByteArray,
        signingTimeMillis: Long,
    ): List<ByteArray> {
        val contentTypeAttr = encodeAttribute(
            oid = OID_ATTR_CONTENT_TYPE,
            value = DerEncoder.encodeOid(OID_DATA),
        )
        val signingTimeAttr = encodeAttribute(
            oid = OID_ATTR_SIGNING_TIME,
            value = DerEncoder.encodeTime(signingTimeMillis),
        )
        val messageDigestAttr = encodeAttribute(
            oid = OID_ATTR_MESSAGE_DIGEST,
            value = DerEncoder.encodeOctetString(messageDigestBytes),
        )
        val smimeCapabilitiesAttr = encodeAttribute(
            oid = OID_ATTR_SMIME_CAPABILITIES,
            value = DerEncoder.encodeSequence(
                DerEncoder.encodeSequence(DerEncoder.encodeOid(OID_AES256_CBC)),
                DerEncoder.encodeSequence(DerEncoder.encodeOid(OID_AES128_CBC)),
                DerEncoder.encodeSequence(DerEncoder.encodeOid(OID_SHA256)),
            ),
        )
        return DerEncoder.sortDerElements(
            listOf(contentTypeAttr, signingTimeAttr, messageDigestAttr, smimeCapabilitiesAttr),
        )
    }

    private fun buildSignerInfo(
        signerCert: X509Certificate,
        digestAlgId: ByteArray,
        sortedSignedAttrs: List<ByteArray>,
        signatureAlgId: ByteArray,
        signatureBytes: ByteArray,
    ): ByteArray {
        val issuerAndSerialNumber = DerEncoder.encodeSequence(
            signerCert.issuerX500Principal.encoded,
            DerEncoder.encodeInteger(signerCert.serialNumber),
        )
        val signedAttrsImplicit = DerEncoder.encodeContextSpecific(
            tagNumber = 0,
            constructed = true,
            content = DerEncoder.concat(sortedSignedAttrs),
        )
        return DerEncoder.encodeSequence(
            DerEncoder.encodeInteger(CMS_VERSION_1),
            issuerAndSerialNumber,
            digestAlgId,
            signedAttrsImplicit,
            signatureAlgId,
            DerEncoder.encodeOctetString(signatureBytes),
        )
    }

    private fun buildSignedData(
        digestAlgId: ByteArray,
        certificateChain: List<X509Certificate>,
        signerInfo: ByteArray,
    ): ByteArray {
        val digestAlgorithms = DerEncoder.encodeSet(digestAlgId)
        val encapContentInfo = DerEncoder.encodeSequence(DerEncoder.encodeOid(OID_DATA))
        val encodedCerts = certificateChain.map { it.encoded }
        val certificatesImplicit = DerEncoder.encodeContextSpecific(
            tagNumber = 0,
            constructed = true,
            content = DerEncoder.concat(encodedCerts),
        )
        val signerInfos = DerEncoder.encodeSet(signerInfo)

        return DerEncoder.encodeSequence(
            DerEncoder.encodeInteger(CMS_VERSION_1),
            digestAlgorithms,
            encapContentInfo,
            certificatesImplicit,
            signerInfos,
        )
    }

    private fun computeSignature(
        jcaSignatureAlgorithm: String,
        credentials: SmimeSigningCredentials,
        data: ByteArray,
    ): ByteArray {
        val signature = Signature.getInstance(jcaSignatureAlgorithm)
        signature.initSign(credentials.privateKey)
        signature.update(data)
        return signature.sign()
    }

    private fun encodeAttribute(oid: String, value: ByteArray): ByteArray =
        DerEncoder.encodeSequence(
            DerEncoder.encodeOid(oid),
            DerEncoder.encodeSet(value),
        )

    private fun encodeAlgorithmIdentifier(oid: String, includeNullParams: Boolean): ByteArray =
        if (includeNullParams) {
            DerEncoder.encodeSequence(DerEncoder.encodeOid(oid), DerEncoder.encodeNull())
        } else {
            DerEncoder.encodeSequence(DerEncoder.encodeOid(oid))
        }

    private fun resolveAlgorithmSpec(
        privateKeyAlgorithm: String?,
        signerCert: X509Certificate,
    ): SigningAlgorithmSpec {
        val keyAlgorithm = (privateKeyAlgorithm ?: signerCert.publicKey.algorithm)
            .uppercase(Locale.US)

        return when {
            keyAlgorithm == "RSA" -> SigningAlgorithmSpec(
                jcaDigestName = "SHA-256",
                digestOid = OID_SHA256,
                includeNullDigestParams = true,
                jcaSignatureName = "SHA256withRSA",
                signatureOid = OID_RSA_ENCRYPTION,
                includeNullSignatureParams = true,
                micAlg = "sha-256",
            )

            keyAlgorithm == "EC" || keyAlgorithm == "ECDSA" -> resolveEcdsaSpec(signerCert)

            keyAlgorithm == "ED25519" || keyAlgorithm == "EDDSA" -> SigningAlgorithmSpec(
                jcaDigestName = "SHA-512",
                digestOid = OID_SHA512,
                includeNullDigestParams = false,
                jcaSignatureName = "Ed25519",
                signatureOid = OID_ED25519,
                includeNullSignatureParams = false,
                micAlg = "sha-512",
            )

            else -> throw SmimeSigningException("Unsupported signing key algorithm: $keyAlgorithm")
        }
    }

    private fun resolveEcdsaSpec(signerCert: X509Certificate): SigningAlgorithmSpec {
        val keyBitLength = (signerCert.publicKey as? ECPublicKey)?.params?.order?.bitLength() ?: 256
        return when {
            keyBitLength <= 256 -> SigningAlgorithmSpec(
                jcaDigestName = "SHA-256",
                digestOid = OID_SHA256,
                includeNullDigestParams = false,
                jcaSignatureName = "SHA256withECDSA",
                signatureOid = OID_ECDSA_WITH_SHA256,
                includeNullSignatureParams = false,
                micAlg = "sha-256",
            )

            keyBitLength <= 384 -> SigningAlgorithmSpec(
                jcaDigestName = "SHA-384",
                digestOid = OID_SHA384,
                includeNullDigestParams = false,
                jcaSignatureName = "SHA384withECDSA",
                signatureOid = OID_ECDSA_WITH_SHA384,
                includeNullSignatureParams = false,
                micAlg = "sha-384",
            )

            else -> SigningAlgorithmSpec(
                jcaDigestName = "SHA-512",
                digestOid = OID_SHA512,
                includeNullDigestParams = false,
                jcaSignatureName = "SHA512withECDSA",
                signatureOid = OID_ECDSA_WITH_SHA512,
                includeNullSignatureParams = false,
                micAlg = "sha-512",
            )
        }
    }

    private data class SigningAlgorithmSpec(
        val jcaDigestName: String,
        val digestOid: String,
        val includeNullDigestParams: Boolean,
        val jcaSignatureName: String,
        val signatureOid: String,
        val includeNullSignatureParams: Boolean,
        val micAlg: String,
    )

    private companion object {
        const val CMS_VERSION_1 = 1L

        const val OID_DATA = "1.2.840.113549.1.7.1"
        const val OID_SIGNED_DATA = "1.2.840.113549.1.7.2"
        const val OID_ATTR_CONTENT_TYPE = "1.2.840.113549.1.9.3"
        const val OID_ATTR_MESSAGE_DIGEST = "1.2.840.113549.1.9.4"
        const val OID_ATTR_SIGNING_TIME = "1.2.840.113549.1.9.5"
        const val OID_ATTR_SMIME_CAPABILITIES = "1.2.840.113549.1.9.15"

        const val OID_RSA_ENCRYPTION = "1.2.840.113549.1.1.1"
        const val OID_ECDSA_WITH_SHA256 = "1.2.840.10045.4.3.2"
        const val OID_ECDSA_WITH_SHA384 = "1.2.840.10045.4.3.3"
        const val OID_ECDSA_WITH_SHA512 = "1.2.840.10045.4.3.4"
        const val OID_ED25519 = "1.3.101.112"

        const val OID_SHA256 = "2.16.840.1.101.3.4.2.1"
        const val OID_SHA384 = "2.16.840.1.101.3.4.2.2"
        const val OID_SHA512 = "2.16.840.1.101.3.4.2.3"

        const val OID_AES128_CBC = "2.16.840.1.101.3.4.1.2"
        const val OID_AES256_CBC = "2.16.840.1.101.3.4.1.42"
    }
}
