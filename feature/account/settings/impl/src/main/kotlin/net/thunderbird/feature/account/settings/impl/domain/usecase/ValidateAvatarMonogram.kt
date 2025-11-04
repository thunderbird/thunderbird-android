package net.thunderbird.feature.account.settings.impl.domain.usecase

import net.thunderbird.core.outcome.Outcome
import net.thunderbird.core.validation.ValidationError
import net.thunderbird.core.validation.ValidationOutcome
import net.thunderbird.core.validation.ValidationSuccess
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.UseCase

internal class ValidateAvatarMonogram : UseCase.ValidateAvatarMonogram {

    override fun invoke(monogram: String): ValidationOutcome = when {
        monogram.isBlank() -> Outcome.Failure(ValidateAvatarMonogramError.EmptyMonogram)
        monogram.length > MAX_MONOGRAM_LENGTH -> Outcome.Failure(ValidateAvatarMonogramError.TooLongMonogram)
        else -> ValidationSuccess
    }

    sealed interface ValidateAvatarMonogramError : ValidationError {
        data object EmptyMonogram : ValidateAvatarMonogramError
        data object TooLongMonogram : ValidateAvatarMonogramError
    }

    private companion object {
        const val MAX_MONOGRAM_LENGTH = 3
    }
}
