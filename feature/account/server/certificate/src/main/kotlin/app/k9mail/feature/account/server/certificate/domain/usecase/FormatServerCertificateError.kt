package app.k9mail.feature.account.server.certificate.domain.usecase

import app.k9mail.feature.account.server.certificate.domain.ServerCertificateDomainContract.UseCase
import app.k9mail.feature.account.server.certificate.domain.entity.FormattedServerCertificateError
import app.k9mail.feature.account.server.certificate.domain.entity.ServerCertificateError
import app.k9mail.feature.account.server.certificate.domain.entity.ServerCertificateProperties
import java.security.cert.X509Certificate
import java.text.DateFormat
import java.util.Date
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import okio.ByteString
import okio.HashingSink
import okio.blackholeSink
import okio.buffer

@OptIn(ExperimentalTime::class)
class FormatServerCertificateError(
    private val dateFormat: DateFormat = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT),
) : UseCase.FormatServerCertificateError {
    override operator fun invoke(serverCertificateError: ServerCertificateError): FormattedServerCertificateError {
        val certificate = serverCertificateError.certificateChain.firstOrNull()
            ?: error("Certificate chain must not be empty")

        val notValidBeforeInstant = Instant.fromEpochMilliseconds(certificate.notBefore.time)
        val notValidAfterInstant = Instant.fromEpochMilliseconds(certificate.notAfter.time)

        val subjectAlternativeNames = certificate.subjectAlternativeNames.orEmpty().map { it[1].toString() }

        val notValidBefore = dateFormat.format(Date(notValidBeforeInstant.toEpochMilliseconds()))
        val notValidAfter = dateFormat.format(Date(notValidAfterInstant.toEpochMilliseconds()))

        // TODO: Parse the name to be able to display the components in a more structured way.
        val subject = certificate.subjectX500Principal.toString()
        val issuer = certificate.issuerX500Principal.toString()

        val fingerprintSha1 = computeFingerprint(certificate, HashAlgorithm.SHA_1)
        val fingerprintSha256 = computeFingerprint(certificate, HashAlgorithm.SHA_256)
        val fingerprintSha512 = computeFingerprint(certificate, HashAlgorithm.SHA_512)

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

    private fun computeFingerprint(certificate: X509Certificate, algorithm: HashAlgorithm): ByteString {
        val sink = when (algorithm) {
            HashAlgorithm.SHA_1 -> HashingSink.sha1(blackholeSink())
            HashAlgorithm.SHA_256 -> HashingSink.sha256(blackholeSink())
            HashAlgorithm.SHA_512 -> HashingSink.sha512(blackholeSink())
        }

        sink.buffer()
            .write(certificate.encoded)
            .flush()

        return sink.hash
    }
}

private enum class HashAlgorithm {
    SHA_1,
    SHA_256,
    SHA_512,
}
