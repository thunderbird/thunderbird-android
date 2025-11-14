package app.k9mail.feature.account.server.settings.domain.usecase

import app.k9mail.feature.account.server.settings.domain.ServerSettingsDomainContract.UseCase
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.core.validation.ValidationError
import net.thunderbird.core.validation.ValidationOutcome
import net.thunderbird.core.validation.ValidationSuccess

internal class ValidatePort : UseCase.ValidatePort {

    override fun execute(port: Long?): ValidationOutcome {
        return when (port) {
            null -> Outcome.Failure(ValidatePortError.EmptyPort)
            in MIN_PORT_NUMBER..MAX_PORT_NUMBER -> ValidationSuccess
            else -> Outcome.Failure(ValidatePortError.InvalidPort)
        }
    }

    sealed interface ValidatePortError : ValidationError {
        data object EmptyPort : ValidatePortError
        data object InvalidPort : ValidatePortError
    }

    companion object {
        const val MAX_PORT_NUMBER = 65535
        const val MIN_PORT_NUMBER = 1
    }
}
