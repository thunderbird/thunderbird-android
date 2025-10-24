package app.k9mail.feature.account.server.settings.domain.usecase

import app.k9mail.feature.account.server.settings.domain.ServerSettingsDomainContract.UseCase
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.core.validation.ValidationError
import net.thunderbird.core.validation.ValidationOutcome

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
