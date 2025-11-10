package net.thunderbird.feature.account.settings.impl.domain.usecase

import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.UseCase
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.ValidateMonogramError

internal class ValidateAvatarMonogram : UseCase.ValidateAvatarMonogram {

    override fun invoke(monogram: String): Outcome<Unit, ValidateMonogramError> = when {
        monogram.isBlank() -> Outcome.failure(ValidateMonogramError.EmptyMonogram)
        monogram.length > MAX_MONOGRAM_LENGTH -> Outcome.failure(ValidateMonogramError.TooLongMonogram)
        else -> Outcome.success(Unit)
    }

    private companion object {
        const val MAX_MONOGRAM_LENGTH = 3
    }
}
