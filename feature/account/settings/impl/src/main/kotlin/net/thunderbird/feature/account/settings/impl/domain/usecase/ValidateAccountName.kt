package net.thunderbird.feature.account.settings.impl.domain.usecase

import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.UseCase
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.ValidateAccountNameError

internal class ValidateAccountName : UseCase.ValidateAccountName {

    override fun invoke(name: String): Outcome<Unit, ValidateAccountNameError> {
        return when {
            name.isBlank() -> Outcome.failure(ValidateAccountNameError.EmptyName)
            name.length > MAX_NAME_LENGTH -> Outcome.failure(ValidateAccountNameError.TooLongName)
            else -> Outcome.success(Unit)
        }
    }

    private companion object {
        /**
         * Maximum length for an account name.
         *
         * See [RFC 5321, 4.5.3.1.3](https://datatracker.ietf.org/doc/html/rfc5321#section-4.5.3.1.3).
         *
         * The account name could be an email address. So the maximum length of 'Path' limits the length of the
         * whole email address and thus the account name.
         */
        const val MAX_NAME_LENGTH = 254
    }
}
