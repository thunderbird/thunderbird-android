package app.k9mail.feature.account.setup.domain.usecase

import app.k9mail.feature.account.setup.domain.DomainContract
import app.k9mail.feature.account.setup.domain.usecase.ValidateEmailSignature.ValidateEmailSignatureError.BlankEmailSignature
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.core.validation.ValidationError
import net.thunderbird.core.validation.ValidationOutcome
import net.thunderbird.core.validation.ValidationSuccess

// TODO check signature for input validity
internal class ValidateEmailSignature : DomainContract.UseCase.ValidateEmailSignature {

    override fun execute(emailSignature: String): ValidationOutcome {
        return when {
            emailSignature.isEmpty() -> ValidationSuccess
            emailSignature.isBlank() -> Outcome.Failure(error = BlankEmailSignature)
            else -> ValidationSuccess
        }
    }

    sealed interface ValidateEmailSignatureError : ValidationError {
        data object BlankEmailSignature : ValidateEmailSignatureError
    }
}
