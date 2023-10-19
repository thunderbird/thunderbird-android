package app.k9mail.feature.account.server.certificate.ui

import androidx.lifecycle.viewModelScope
import app.k9mail.core.ui.compose.common.mvi.BaseViewModel
import app.k9mail.feature.account.server.certificate.domain.ServerCertificateDomainContract
import app.k9mail.feature.account.server.certificate.domain.ServerCertificateDomainContract.UseCase
import app.k9mail.feature.account.server.certificate.domain.entity.ServerCertificateError
import app.k9mail.feature.account.server.certificate.ui.ServerCertificateErrorContract.Effect
import app.k9mail.feature.account.server.certificate.ui.ServerCertificateErrorContract.Event
import app.k9mail.feature.account.server.certificate.ui.ServerCertificateErrorContract.State
import kotlinx.coroutines.launch

class ServerCertificateErrorViewModel(
    private val certificateErrorRepository: ServerCertificateDomainContract.ServerCertificateErrorRepository,
    private val addServerCertificateException: UseCase.AddServerCertificateException,
    private val formatServerCertificateError: UseCase.FormatServerCertificateError,
    initialState: State = State(),
) : BaseViewModel<State, Event, Effect>(initialState), ServerCertificateErrorContract.ViewModel {
    private val serverCertificateError: ServerCertificateError? = certificateErrorRepository.getCertificateError()

    init {
        serverCertificateError?.let { serverCertificateError ->
            updateState {
                it.copy(
                    hostname = serverCertificateError.hostname,
                    errorText = buildErrorMessage(serverCertificateError),
                )
            }
        }
    }

    override fun event(event: Event) {
        when (event) {
            Event.OnShowAdvancedClicked -> showAdvanced()
            Event.OnCertificateAcceptedClicked -> acceptCertificate()
            Event.OnBackClicked -> navigateBack()
        }
    }

    private fun showAdvanced() {
        updateState {
            it.copy(isShowServerCertificate = true)
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

    private fun buildErrorMessage(serverCertificateError: ServerCertificateError): String {
        val formattedError = formatServerCertificateError(serverCertificateError)
        val certificate = formattedError.serverCertificateProperties

        return buildString {
            if (certificate.subjectAlternativeNames.isNotEmpty()) {
                append("Subject alternative names:\n")
                for (subjectAlternativeName in certificate.subjectAlternativeNames) {
                    append("- ").append(subjectAlternativeName).append("\n")
                }
                append("\n")
            }

            append("Not valid before: ").append(certificate.notValidBefore).append("\n")
            append("Not valid after: ").append(certificate.notValidAfter).append("\n")
            append("\n")

            append("Subject: ").append(certificate.subject).append("\n")
            append("Issuer: ").append(certificate.issuer).append("\n")
            append("\n")

            append("Fingerprint (SHA-1): \n").append(certificate.fingerprintSha1).append("\n")
            append("Fingerprint (SHA-256): \n").append(certificate.fingerprintSha256).append("\n")
            append("Fingerprint (SHA-512): \n").append(certificate.fingerprintSha512).append("\n")
        }
    }
}
