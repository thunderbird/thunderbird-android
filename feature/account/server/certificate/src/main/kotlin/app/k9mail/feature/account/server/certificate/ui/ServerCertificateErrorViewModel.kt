package app.k9mail.feature.account.server.certificate.ui

import androidx.lifecycle.viewModelScope
import app.k9mail.core.ui.compose.common.mvi.BaseViewModel
import app.k9mail.feature.account.server.certificate.domain.ServerCertificateDomainContract
import app.k9mail.feature.account.server.certificate.domain.ServerCertificateDomainContract.UseCase
import app.k9mail.feature.account.server.certificate.domain.entity.ServerCertificateError
import app.k9mail.feature.account.server.certificate.ui.ServerCertificateErrorContract.Effect
import app.k9mail.feature.account.server.certificate.ui.ServerCertificateErrorContract.Event
import app.k9mail.feature.account.server.certificate.ui.ServerCertificateErrorContract.State
import com.fsck.k9.logging.Timber
import com.fsck.k9.mail.filter.Hex
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.cert.CertificateEncodingException
import kotlinx.coroutines.launch

class ServerCertificateErrorViewModel(
    private val certificateErrorRepository: ServerCertificateDomainContract.ServerCertificateErrorRepository,
    private val addServerCertificateException: UseCase.AddServerCertificateException,
    initialState: State = State(),
) : BaseViewModel<State, Event, Effect>(initialState), ServerCertificateErrorContract.ViewModel {
    private val serverCertificateError: ServerCertificateError? = certificateErrorRepository.getCertificateError()

    init {
        setErrorMessage(buildErrorMessage(serverCertificateError))
    }

    override fun event(event: Event) {
        when (event) {
            Event.OnCertificateAcceptedClicked -> acceptCertificate()
            Event.OnBackClicked -> navigateBack()
        }
    }

    private fun acceptCertificate() {
        val certificateError = requireNotNull(serverCertificateError)

        viewModelScope.launch {
            addServerCertificateException.addCertificate(
                hostname = certificateError.hostname,
                port = certificateError.port,
                certificate = certificateError.certificateChain.first(),
            )

            certificateErrorRepository.clearCertificateError()
            navigateCertificateAccepted()
        }
    }

    private fun navigateBack() {
        emitEffect(Effect.NavigateBack)
    }

    private fun navigateCertificateAccepted() {
        emitEffect(Effect.NavigateCertificateAccepted)
    }

    private fun buildErrorMessage(serverCertificateError: ServerCertificateError?): String {
        val certificate = serverCertificateError?.certificateChain?.firstOrNull() ?: return ""

        return buildString {
            certificate.subjectAlternativeNames?.let { subjectAlternativeNames ->
                append("Subject alternative names:\n")
                for (subjectAlternativeName in subjectAlternativeNames) {
                    append("- ").append(subjectAlternativeName[1]).append("\n")
                }
            }
            append("\n")

            append("Not valid before: ").append(certificate.notBefore).append("\n")
            append("Not valid after: ").append(certificate.notAfter).append("\n")
            append("\n")

            append("Subject: ").append(certificate.subjectDN).append("\n")
            append("Issuer: ").append(certificate.issuerX500Principal).append("\n")
            append("\n")

            for (algorithm in arrayOf("SHA-1", "SHA-256", "SHA-512")) {
                val digest = try {
                    MessageDigest.getInstance(algorithm)
                } catch (e: NoSuchAlgorithmException) {
                    Timber.e(e, "Error while initializing MessageDigest (%s)", algorithm)
                    null
                }

                if (digest != null) {
                    digest.reset()
                    try {
                        val hash = Hex.encodeHex(digest.digest(certificate.encoded))
                        append("Fingerprint (").append(algorithm).append("): \n").append(hash).append("\n")
                    } catch (e: CertificateEncodingException) {
                        Timber.e(e, "Error while encoding certificate")
                    }
                }
            }
        }
    }

    private fun setErrorMessage(errorText: String) {
        updateState {
            it.copy(errorText = errorText)
        }
    }
}
