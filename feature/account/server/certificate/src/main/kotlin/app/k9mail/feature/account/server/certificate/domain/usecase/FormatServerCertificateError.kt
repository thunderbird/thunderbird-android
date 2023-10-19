package app.k9mail.feature.account.server.certificate.domain.usecase

import app.k9mail.feature.account.server.certificate.domain.ServerCertificateDomainContract.UseCase
import app.k9mail.feature.account.server.certificate.domain.entity.FormattedServerCertificateError
import app.k9mail.feature.account.server.certificate.domain.entity.ServerCertificateError
import app.k9mail.feature.account.server.certificate.domain.entity.ServerCertificateProperties
import com.fsck.k9.logging.Timber
import com.fsck.k9.mail.filter.Hex
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.cert.CertificateEncodingException
import java.security.cert.X509Certificate
import java.text.DateFormat
import java.util.Date
import kotlinx.datetime.Instant

class FormatServerCertificateError(
    private val dateFormat: DateFormat = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT),
) : UseCase.FormatServerCertificateError {
    override operator fun invoke(serverCertificateError: ServerCertificateError): FormattedServerCertificateError {
        val certificate = serverCertificateError.certificateChain.firstOrNull()
            ?: error("Certificate chain must not be empty")

        val notValidBeforeInstant = Instant.fromEpochMilliseconds(certificate.notBefore.time)
        val notValidAfterInstant = Instant.fromEpochMilliseconds(certificate.notAfter.time)

        val subjectAlternativeNames = certificate.subjectAlternativeNames.map { it[1].toString() }

        val notValidBefore = dateFormat.format(Date(notValidBeforeInstant.toEpochMilliseconds()))
        val notValidAfter = dateFormat.format(Date(notValidAfterInstant.toEpochMilliseconds()))

        // TODO: Parse the name to be able to display the components in a more structured way.
        val subject = certificate.subjectX500Principal.toString()
        val issuer = certificate.issuerX500Principal.toString()

        val fingerprintSha1 = getFingerprint(certificate, algorithm = "SHA-1")
        val fingerprintSha256 = getFingerprint(certificate, algorithm = "SHA-256")
        val fingerprintSha512 = getFingerprint(certificate, algorithm = "SHA-512")

        return FormattedServerCertificateError(
            hostname = serverCertificateError.hostname,
            serverCertificateProperties = ServerCertificateProperties(
                subjectAlternativeNames,
                notValidBefore,
                notValidAfter,
                subject,
                issuer,
                fingerprintSha1,
                fingerprintSha256,
                fingerprintSha512,
            ),
        )
    }

    private fun getFingerprint(certificate: X509Certificate, algorithm: String): String {
        val fingerprint = computeFingerprint(certificate, algorithm)
        return formatFingerprint(fingerprint)
    }

    private fun computeFingerprint(certificate: X509Certificate, algorithm: String): ByteArray {
        val digest = try {
            MessageDigest.getInstance(algorithm)
        } catch (e: NoSuchAlgorithmException) {
            Timber.e(e, "Error while initializing MessageDigest (%s)", algorithm)
            return "??".toByteArray()
        }

        return try {
            digest.digest(certificate.encoded)
        } catch (e: CertificateEncodingException) {
            Timber.e(e, "Error while encoding certificate")
            "??".toByteArray()
        }
    }

    private fun formatFingerprint(fingerprint: ByteArray): String {
        return Hex.encodeHex(fingerprint)
    }
}
