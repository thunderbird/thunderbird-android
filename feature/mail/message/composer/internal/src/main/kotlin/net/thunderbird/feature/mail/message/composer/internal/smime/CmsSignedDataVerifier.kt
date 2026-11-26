package net.thunderbird.feature.mail.message.composer.internal.smime

import java.io.ByteArrayInputStream
import java.math.BigInteger
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.MessageDigest
import java.security.Signature
import java.security.cert.CertPathValidator
import java.security.cert.CertificateExpiredException
import java.security.cert.CertificateFactory
import java.security.cert.CertificateNotYetValidException
import java.security.cert.PKIXParameters
import java.security.cert.TrustAnchor
import java.security.cert.X509Certificate
import java.util.Locale
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager
import javax.security.auth.x500.X500Principal
import net.thunderbird.feature.mail.message.composer.smime.SmimeVerificationResult
import net.thunderbird.feature.mail.message.composer.smime.SmimeVerificationStatus

/**
 * Verifies RFC 5652 / RFC 8551 detached CMS SignedData signatures using standard JCA primitives
 * and the platform CA trust store.
 */
@Suppress("MagicNumber", "TooManyFunctions")
internal class CmsSignedDataVerifier(
    private val trustAnchorsProvider: () -> Set<TrustAnchor> = ::loadDefaultTrustAnchors,
) {
    fun verifyDetachedSignature(
        canonicalMimeContent: ByteArray,
        cmsDerBytes: ByteArray,
        senderEmail: String?,
    ): SmimeVerificationResult {
        return try {
            verifyParsedSignature(
                canonicalMimeContent = canonicalMimeContent,
                cmsDerBytes = cmsDerBytes,
                senderEmail = senderEmail,
            )
        } catch (_: IllegalArgumentException) {
            SmimeVerificationResult(status = SmimeVerificationStatus.INVALID_SIGNATURE)
        } catch (_: GeneralSecurityException) {
            SmimeVerificationResult(status = SmimeVerificationStatus.INVALID_SIGNATURE)
        }
    }

    private fun verifyParsedSignature(
        canonicalMimeContent: ByteArray,
        cmsDerBytes: ByteArray,
        senderEmail: String?,
    ): SmimeVerificationResult {
        val parsed = parseSignedData(cmsDerBytes)
        val signerInfo = parsed?.signerInfos?.firstOrNull()
        val signerCert = if (parsed != null && signerInfo != null) {
            findSignerCertificate(signerInfo, parsed.certificates)
        } else {
            null
        }

        return when {
            parsed == null || signerInfo == null -> {
                SmimeVerificationResult(status = SmimeVerificationStatus.INVALID_SIGNATURE)
            }

            signerCert == null -> {
                SmimeVerificationResult(status = SmimeVerificationStatus.ERROR)
            }

            else -> {
                buildVerificationResult(
                    canonicalMimeContent = canonicalMimeContent,
                    signerInfo = signerInfo,
                    signerCert = signerCert,
                    allCertificates = parsed.certificates,
                    senderEmail = senderEmail,
                )
            }
        }
    }

    private fun buildVerificationResult(
        canonicalMimeContent: ByteArray,
        signerInfo: ParsedSignerInfo,
        signerCert: X509Certificate,
        allCertificates: List<X509Certificate>,
        senderEmail: String?,
    ): SmimeVerificationResult {
        val signerDisplayName = extractDisplayName(signerCert.subjectX500Principal)
        val signerCertEmails = extractCertificateEmails(signerCert)
        val issuerDisplayName = extractDisplayName(signerCert.issuerX500Principal)

        val signatureValid = verifyCrypto(
            canonicalMimeContent = canonicalMimeContent,
            signerInfo = signerInfo,
            signerCert = signerCert,
        )

        val status = if (!signatureValid) {
            SmimeVerificationStatus.INVALID_SIGNATURE
        } else {
            evaluateCertificateStatus(
                signerCert = signerCert,
                allCertificates = allCertificates,
                signerCertEmails = signerCertEmails,
                senderEmail = senderEmail,
            )
        }

        return SmimeVerificationResult(
            status = status,
            signerDisplayName = signerDisplayName,
            signerEmail = signerCertEmails.firstOrNull(),
            issuerDisplayName = issuerDisplayName,
        )
    }

    private fun parseSignedData(cmsDerBytes: ByteArray): ParsedSignedData? {
        val signedDataChildren = extractSignedDataChildren(cmsDerBytes)
        return signedDataChildren?.takeIf { it.size >= 4 }?.let(::buildParsedSignedData)
    }

    private fun extractSignedDataChildren(cmsDerBytes: ByteArray): List<DerNode>? {
        val contentInfo = DerDecoder.decodeSingle(cmsDerBytes)
            .takeIf { it.tag == DerDecoder.TAG_SEQUENCE }
        val contentInfoChildren = contentInfo?.let(DerDecoder::decodeChildren)
            ?.takeIf { it.size >= 2 && DerDecoder.decodeOid(it[0]) == OID_SIGNED_DATA }
        val explicitContent = contentInfoChildren?.get(1)
            ?.takeIf { it.tag == DerDecoder.TAG_CONTEXT_0_CONSTRUCTED }
        val signedData = explicitContent?.let(DerDecoder::decodeChildren)?.firstOrNull()
            ?.takeIf { it.tag == DerDecoder.TAG_SEQUENCE }
        return signedData?.let(DerDecoder::decodeChildren)
    }

    private fun buildParsedSignedData(children: List<DerNode>): ParsedSignedData? {
        var index = 3
        val certificates = mutableListOf<X509Certificate>()
        if (index < children.size && children[index].tag == DerDecoder.TAG_CONTEXT_0_CONSTRUCTED) {
            certificates.addAll(parseCertificates(children[index]))
            index++
        }
        if (index < children.size && children[index].tag == DerDecoder.TAG_CONTEXT_1_CONSTRUCTED) {
            index++
        }
        val signerInfosNode = children.getOrNull(index)
            ?.takeIf { it.tag == DerDecoder.TAG_SET }
        return signerInfosNode?.let { node ->
            val signerInfos = DerDecoder.decodeChildren(node).mapNotNull(::parseSignerInfo)
            ParsedSignedData(certificates = certificates, signerInfos = signerInfos)
        }
    }

    private fun parseCertificates(certsContainer: DerNode): List<X509Certificate> {
        val factory = CertificateFactory.getInstance("X.509")
        return DerDecoder.decodeChildren(certsContainer).mapNotNull { certNode ->
            try {
                factory.generateCertificate(ByteArrayInputStream(certNode.raw)) as? X509Certificate
            } catch (_: GeneralSecurityException) {
                null
            }
        }
    }

    private fun parseSignerInfo(node: DerNode): ParsedSignerInfo? {
        val children = node.takeIf { it.tag == DerDecoder.TAG_SEQUENCE }
            ?.let(DerDecoder::decodeChildren)
            ?.takeIf { it.size >= 5 }
            ?: return null

        val digestAlgOid = extractAlgorithmOid(children[2])
        var cursor = 3
        val signedAttrsNode = if (children[cursor].tag == DerDecoder.TAG_CONTEXT_0_CONSTRUCTED) {
            children[cursor++]
        } else {
            null
        }
        val signatureAlgOid = children.getOrNull(cursor)?.let(::extractAlgorithmOid)
        val signatureValueNode = children.getOrNull(cursor + 1)
            ?.takeIf { it.tag == DerDecoder.TAG_OCTET_STRING }

        return if (digestAlgOid != null && signatureAlgOid != null && signatureValueNode != null) {
            ParsedSignerInfo(
                sidNode = children[1],
                digestAlgOid = digestAlgOid,
                signedAttrsNode = signedAttrsNode,
                signatureAlgOid = signatureAlgOid,
                signatureBytes = signatureValueNode.value,
            )
        } else {
            null
        }
    }

    private fun findSignerCertificate(
        signerInfo: ParsedSignerInfo,
        certificates: List<X509Certificate>,
    ): X509Certificate? {
        val sid = signerInfo.sidNode
        return when (sid.tag) {
            DerDecoder.TAG_SEQUENCE -> {
                val sidChildren = DerDecoder.decodeChildren(sid)
                if (sidChildren.size < 2) return null
                val expectedIssuer = X500Principal(sidChildren[0].raw)
                val expectedSerial: BigInteger = DerDecoder.decodeInteger(sidChildren[1])
                certificates.firstOrNull { cert ->
                    cert.serialNumber == expectedSerial && cert.issuerX500Principal == expectedIssuer
                }
            }

            DerDecoder.TAG_CONTEXT_0_PRIMITIVE -> {
                val expectedSki = sid.value
                certificates.firstOrNull { cert ->
                    extractSubjectKeyIdentifier(cert)?.contentEquals(expectedSki) == true
                }
            }

            else -> null
        }
    }

    private fun extractSubjectKeyIdentifier(cert: X509Certificate): ByteArray? {
        val extValue = cert.getExtensionValue(OID_SUBJECT_KEY_IDENTIFIER) ?: return null
        return try {
            val outerOctet = DerDecoder.decodeSingle(extValue)
            val innerOctet = DerDecoder.decodeSingle(outerOctet.value)
            innerOctet.value
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun verifyCrypto(
        canonicalMimeContent: ByteArray,
        signerInfo: ParsedSignerInfo,
        signerCert: X509Certificate,
    ): Boolean {
        val jcaDigest = mapDigestAlgorithm(signerInfo.digestAlgOid)
        val jcaSignature = jcaDigest?.let { digest ->
            mapSignatureAlgorithm(
                signatureAlgOid = signerInfo.signatureAlgOid,
                jcaDigest = digest,
                publicKeyAlgorithm = signerCert.publicKey.algorithm,
            )
        }
        val dataToVerify = if (jcaDigest != null && jcaSignature != null) {
            resolveDataToVerify(canonicalMimeContent, signerInfo.signedAttrsNode, jcaDigest)
        } else {
            null
        }

        return if (jcaSignature != null && dataToVerify != null) {
            val verifier = Signature.getInstance(jcaSignature)
            verifier.initVerify(signerCert.publicKey)
            verifier.update(dataToVerify)
            verifier.verify(signerInfo.signatureBytes)
        } else {
            false
        }
    }

    private fun resolveDataToVerify(
        canonicalMimeContent: ByteArray,
        signedAttrsNode: DerNode?,
        jcaDigest: String,
    ): ByteArray? {
        return if (signedAttrsNode == null) {
            canonicalMimeContent
        } else {
            val expectedDigest = extractMessageDigestAttribute(signedAttrsNode)
            if (expectedDigest != null && matchesDigest(canonicalMimeContent, jcaDigest, expectedDigest)) {
                val attrNodes = DerDecoder.decodeChildren(signedAttrsNode)
                val concatenated = DerEncoder.concat(attrNodes.map { it.raw })
                encodeExplicitSetPreservingOrder(concatenated)
            } else {
                null
            }
        }
    }

    private fun matchesDigest(content: ByteArray, jcaDigest: String, expectedDigest: ByteArray): Boolean {
        val md = MessageDigest.getInstance(jcaDigest)
        if (md.digest(content).contentEquals(expectedDigest)) {
            return true
        }
        // Tolerate a single trailing CRLF difference introduced by mail storage/transport
        val crlf = byteArrayOf('\r'.code.toByte(), '\n'.code.toByte())
        return if (content.size >= 2 && content[content.size - 2] == crlf[0] && content[content.size - 1] == crlf[1]) {
            md.digest(content.copyOf(content.size - 2)).contentEquals(expectedDigest)
        } else {
            md.digest(content + crlf).contentEquals(expectedDigest)
        }
    }

    private fun encodeExplicitSetPreservingOrder(content: ByteArray): ByteArray {
        val lengthBytes = when {
            content.size < 128 -> byteArrayOf(content.size.toByte())

            content.size < 256 -> byteArrayOf(0x81.toByte(), content.size.toByte())

            content.size < 65536 -> byteArrayOf(
                0x82.toByte(),
                (content.size ushr 8).toByte(),
                (content.size and 0xFF).toByte(),
            )

            else -> byteArrayOf(
                0x83.toByte(),
                (content.size ushr 16).toByte(),
                ((content.size ushr 8) and 0xFF).toByte(),
                (content.size and 0xFF).toByte(),
            )
        }
        return byteArrayOf(DerDecoder.TAG_SET.toByte()) + lengthBytes + content
    }

    private fun extractMessageDigestAttribute(signedAttrsNode: DerNode): ByteArray? {
        return DerDecoder.decodeChildren(signedAttrsNode)
            .asSequence()
            .filter { it.tag == DerDecoder.TAG_SEQUENCE }
            .map(DerDecoder::decodeChildren)
            .filter { it.size >= 2 && DerDecoder.decodeOid(it[0]) == OID_ATTR_MESSAGE_DIGEST }
            .mapNotNull { parts ->
                DerDecoder.decodeChildren(parts[1])
                    .firstOrNull()
                    ?.takeIf { it.tag == DerDecoder.TAG_OCTET_STRING }
                    ?.value
            }
            .firstOrNull()
    }

    private fun evaluateCertificateStatus(
        signerCert: X509Certificate,
        allCertificates: List<X509Certificate>,
        signerCertEmails: List<String>,
        senderEmail: String?,
    ): SmimeVerificationStatus {
        val isValidityOk = try {
            signerCert.checkValidity()
            true
        } catch (_: CertificateExpiredException) {
            false
        } catch (_: CertificateNotYetValidException) {
            false
        }

        val hasSenderMismatch = !senderEmail.isNullOrBlank() &&
            signerCertEmails.isNotEmpty() &&
            signerCertEmails.none { it.equals(senderEmail.trim(), ignoreCase = true) }

        return when {
            !isValidityOk -> SmimeVerificationStatus.VALID_EXPIRED
            hasSenderMismatch -> SmimeVerificationStatus.VALID_SENDER_MISMATCH
            isCertificateChainTrusted(signerCert, allCertificates) -> SmimeVerificationStatus.VALID_TRUSTED
            else -> SmimeVerificationStatus.VALID_UNTRUSTED
        }
    }

    private fun isCertificateChainTrusted(
        signerCert: X509Certificate,
        allCertificates: List<X509Certificate>,
    ): Boolean {
        val trustAnchors = trustAnchorsProvider()
        if (trustAnchors.isEmpty()) return false

        return try {
            val orderedChain = buildOrderedChain(signerCert, allCertificates)
            val certFactory = CertificateFactory.getInstance("X.509")
            val certPath = certFactory.generateCertPath(orderedChain)
            val params = PKIXParameters(trustAnchors).apply {
                isRevocationEnabled = false
            }
            CertPathValidator.getInstance("PKIX").validate(certPath, params)
            true
        } catch (_: GeneralSecurityException) {
            false
        }
    }

    private fun buildOrderedChain(
        leaf: X509Certificate,
        allCertificates: List<X509Certificate>,
    ): List<X509Certificate> {
        val chain = mutableListOf(leaf)
        val remaining = allCertificates.filter { it != leaf }.toMutableList()
        var current = leaf
        while (current.issuerX500Principal != current.subjectX500Principal) {
            val issuer = remaining.firstOrNull { it.subjectX500Principal == current.issuerX500Principal } ?: break
            chain.add(issuer)
            remaining.remove(issuer)
            current = issuer
        }
        return chain
    }

    private fun extractAlgorithmOid(algIdNode: DerNode): String? {
        return algIdNode
            .takeIf { it.tag == DerDecoder.TAG_SEQUENCE }
            ?.let { DerDecoder.decodeChildren(it).firstOrNull() }
            ?.let(DerDecoder::decodeOid)
    }

    private fun mapDigestAlgorithm(oid: String): String? = when (oid) {
        OID_SHA256 -> "SHA-256"
        OID_SHA384 -> "SHA-384"
        OID_SHA512 -> "SHA-512"
        OID_SHA1 -> "SHA-1"
        else -> null
    }

    private fun mapSignatureAlgorithm(
        signatureAlgOid: String,
        jcaDigest: String,
        publicKeyAlgorithm: String,
    ): String? {
        val digestPrefix = jcaDigest.replace("-", "")
        return when (signatureAlgOid) {
            OID_RSA_ENCRYPTION -> "${digestPrefix}withRSA"

            OID_SHA1_WITH_RSA -> "SHA1withRSA"

            OID_SHA256_WITH_RSA -> "SHA256withRSA"

            OID_SHA384_WITH_RSA -> "SHA384withRSA"

            OID_SHA512_WITH_RSA -> "SHA512withRSA"

            OID_ECDSA_WITH_SHA256 -> "SHA256withECDSA"

            OID_ECDSA_WITH_SHA384 -> "SHA384withECDSA"

            OID_ECDSA_WITH_SHA512 -> "SHA512withECDSA"

            OID_ECDSA_WITH_SHA1 -> "SHA1withECDSA"

            OID_ED25519 -> "Ed25519"

            else -> when (publicKeyAlgorithm.uppercase(Locale.US)) {
                "RSA" -> "${digestPrefix}withRSA"
                "EC", "ECDSA" -> "${digestPrefix}withECDSA"
                else -> null
            }
        }
    }

    private fun extractDisplayName(principal: X500Principal): String? {
        val dn = principal.getName(X500Principal.RFC2253)
        return extractRdnValue(dn, "CN") ?: extractRdnValue(dn, "O")
    }

    private fun extractCertificateEmails(cert: X509Certificate): List<String> {
        val emails = mutableListOf<String>()
        try {
            cert.subjectAlternativeNames?.forEach { san ->
                if (san.size >= 2 && san[0] == SAN_RFC822_NAME) {
                    (san[1] as? String)?.trim()?.takeIf { it.isNotEmpty() }?.let(emails::add)
                }
            }
        } catch (_: GeneralSecurityException) {
            // Ignore malformed SAN extension
        }
        val dn = cert.subjectX500Principal.getName(
            X500Principal.RFC2253,
            mapOf(OID_EMAIL_ADDRESS to "EMAILADDRESS"),
        )
        extractRdnValue(dn, "EMAILADDRESS")?.let { email ->
            if (emails.none { it.equals(email, ignoreCase = true) }) {
                emails.add(email)
            }
        }
        return emails
    }

    private fun extractRdnValue(dn: String, attribute: String): String? {
        val prefix = "$attribute="
        return dn.split(',')
            .asSequence()
            .map { it.trim() }
            .firstOrNull { it.startsWith(prefix, ignoreCase = true) }
            ?.substring(prefix.length)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
    }

    private class ParsedSignedData(
        val certificates: List<X509Certificate>,
        val signerInfos: List<ParsedSignerInfo>,
    )

    private class ParsedSignerInfo(
        val sidNode: DerNode,
        val digestAlgOid: String,
        val signedAttrsNode: DerNode?,
        val signatureAlgOid: String,
        val signatureBytes: ByteArray,
    )

    companion object {
        private const val SAN_RFC822_NAME = 1

        private const val OID_SIGNED_DATA = "1.2.840.113549.1.7.2"
        private const val OID_ATTR_MESSAGE_DIGEST = "1.2.840.113549.1.9.4"
        private const val OID_EMAIL_ADDRESS = "1.2.840.113549.1.9.1"
        private const val OID_SUBJECT_KEY_IDENTIFIER = "2.5.29.14"

        private const val OID_RSA_ENCRYPTION = "1.2.840.113549.1.1.1"
        private const val OID_SHA1_WITH_RSA = "1.2.840.113549.1.1.5"
        private const val OID_SHA256_WITH_RSA = "1.2.840.113549.1.1.11"
        private const val OID_SHA384_WITH_RSA = "1.2.840.113549.1.1.12"
        private const val OID_SHA512_WITH_RSA = "1.2.840.113549.1.1.13"

        private const val OID_ECDSA_WITH_SHA1 = "1.2.840.10045.4.1"
        private const val OID_ECDSA_WITH_SHA256 = "1.2.840.10045.4.3.2"
        private const val OID_ECDSA_WITH_SHA384 = "1.2.840.10045.4.3.3"
        private const val OID_ECDSA_WITH_SHA512 = "1.2.840.10045.4.3.4"
        private const val OID_ED25519 = "1.3.101.112"

        private const val OID_SHA1 = "1.3.14.3.2.26"
        private const val OID_SHA256 = "2.16.840.1.101.3.4.2.1"
        private const val OID_SHA384 = "2.16.840.1.101.3.4.2.2"
        private const val OID_SHA512 = "2.16.840.1.101.3.4.2.3"

        private fun loadDefaultTrustAnchors(): Set<TrustAnchor> {
            val anchors = mutableSetOf<TrustAnchor>()
            try {
                val androidStore = KeyStore.getInstance("AndroidCAStore").apply { load(null) }
                val aliases = androidStore.aliases()
                while (aliases.hasMoreElements()) {
                    val cert = androidStore.getCertificate(aliases.nextElement()) as? X509Certificate
                    if (cert != null) {
                        anchors.add(TrustAnchor(cert, null))
                    }
                }
            } catch (_: GeneralSecurityException) {
                // Non-Android JVM fallback below
            } catch (_: java.io.IOException) {
                // Fallback below
            }

            if (anchors.isEmpty()) {
                try {
                    val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
                    tmf.init(null as KeyStore?)
                    tmf.trustManagers.filterIsInstance<X509TrustManager>().forEach { tm ->
                        tm.acceptedIssuers.forEach { cert ->
                            anchors.add(TrustAnchor(cert, null))
                        }
                    }
                } catch (_: GeneralSecurityException) {
                    // Return empty set if trust store unavailable
                }
            }
            return anchors
        }
    }
}
