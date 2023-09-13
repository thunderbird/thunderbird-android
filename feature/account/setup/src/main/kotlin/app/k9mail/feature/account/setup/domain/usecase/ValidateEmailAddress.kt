package app.k9mail.feature.account.setup.domain.usecase

import app.k9mail.core.common.domain.usecase.validation.ValidationError
import app.k9mail.core.common.domain.usecase.validation.ValidationResult
import app.k9mail.feature.account.setup.domain.DomainContract.UseCase

class ValidateEmailAddress : UseCase.ValidateEmailAddress {

    // TODO replace by new email validation
    override fun execute(emailAddress: String): ValidationResult {
        return when {
            emailAddress.isBlank() -> ValidationResult.Failure(ValidateEmailAddressError.EmptyEmailAddress)

            !EMAIL_ADDRESS.matches(emailAddress) -> ValidationResult.Failure(
                ValidateEmailAddressError.InvalidEmailAddress,
            )

            else -> ValidationResult.Success
        }
    }

    sealed interface ValidateEmailAddressError : ValidationError {
        object EmptyEmailAddress : ValidateEmailAddressError
        object InvalidEmailAddress : ValidateEmailAddressError
    }

    private companion object {
        val EMAIL_ADDRESS =
            "[a-zA-Z0-9+._%\\-]{1,256}@[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}(\\.[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25})+".toRegex()
    }
}
