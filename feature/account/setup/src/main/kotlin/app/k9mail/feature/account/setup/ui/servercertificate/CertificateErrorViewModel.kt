package app.k9mail.feature.account.setup.ui.servercertificate

import androidx.lifecycle.viewModelScope
import app.k9mail.core.ui.compose.common.mvi.BaseViewModel
import app.k9mail.feature.account.setup.domain.DomainContract.CertificateErrorRepository
import app.k9mail.feature.account.setup.domain.DomainContract.UseCase.AddServerCertificateException
import app.k9mail.feature.account.setup.domain.entity.CertificateError
import app.k9mail.feature.account.setup.ui.servercertificate.CertificateErrorContract.Effect
import app.k9mail.feature.account.setup.ui.servercertificate.CertificateErrorContract.Event
import app.k9mail.feature.account.setup.ui.servercertificate.CertificateErrorContract.State
import com.fsck.k9.logging.Timber
import com.fsck.k9.mail.filter.Hex
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.cert.CertificateEncodingException
import kotlinx.coroutines.launch

class CertificateErrorViewModel(
    private val certificateErrorRepository: CertificateErrorRepository,
    private val addServerCertificateException: AddServerCertificateException,
    initialState: State = State(),
) : BaseViewModel<State, Event, Effect>(initialState), CertificateErrorContract.ViewModel {
    private val certificateError: CertificateError? = certificateErrorRepository.getCertificateError()

    init {
        setErrorMessage(buildErrorMessage(certificateError))
    }

    override fun event(event: Event) {
        when (event) {
            Event.OnCertificateAcceptedClicked -> acceptCertificate()
            Event.OnBackClicked -> navigateBack()
        }
    }

    private fun acceptCertificate() {
        val certificateError = requireNotNull(certificateError)

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

    private fun buildErrorMessage(certificateError: CertificateError?): String {
        val certificate = certificateError?.certificateChain?.firstOrNull() ?: return ""

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
