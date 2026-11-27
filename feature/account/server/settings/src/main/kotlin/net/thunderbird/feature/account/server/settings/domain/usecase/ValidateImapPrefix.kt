package net.thunderbird.feature.account.server.settings.domain.usecase

import net.thunderbird.components.core.outcome.Outcome
import net.thunderbird.core.validation.ValidationError
import net.thunderbird.core.validation.ValidationOutcome
import net.thunderbird.feature.account.server.settings.domain.ServerSettingsDomainContract.UseCase

internal class ValidateImapPrefix : UseCase.ValidateImapPrefix {

    override fun execute(imapPrefix: String): ValidationOutcome {
        return when {
            imapPrefix.isEmpty() -> Outcome.Success(Unit)
            imapPrefix.isBlank() -> Outcome.Failure(ValidateImapPrefixError.BlankImapPrefix)
            else -> Outcome.Success(Unit)
        }
    }

    sealed interface ValidateImapPrefixError : ValidationError {
        data object BlankImapPrefix : ValidateImapPrefixError
    }
}
