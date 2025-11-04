package net.thunderbird.feature.account.settings.impl.domain.usecase

import net.thunderbird.core.outcome.Outcome
import net.thunderbird.core.validation.ValidationError
import net.thunderbird.core.validation.ValidationOutcome
import net.thunderbird.core.validation.ValidationSuccess
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.UseCase

internal class ValidateAccountName : UseCase.ValidateAccountName {

    override fun invoke(name: String): ValidationOutcome {
        return when {
            name.isBlank() -> Outcome.Failure(ValidateAccountNameError.EmptyName)
            else -> ValidationSuccess
        }
    }

    sealed interface ValidateAccountNameError : ValidationError {
        data object EmptyName : ValidateAccountNameError
    }
}
