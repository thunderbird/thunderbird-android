package app.k9mail.feature.account.server.settings.domain.usecase

import app.k9mail.feature.account.server.settings.domain.ServerSettingsDomainContract.UseCase
import net.thunderbird.core.common.domain.usecase.validation.ValidationError
import net.thunderbird.core.common.domain.usecase.validation.ValidationResult

internal class ValidatePort : UseCase.ValidatePort {

    override fun execute(port: Long?): ValidationResult {
        return when (port) {
            null -> ValidationResult.Failure(ValidatePortError.EmptyPort)
            in MIN_PORT_NUMBER..MAX_PORT_NUMBER -> ValidationResult.Success
            else -> ValidationResult.Failure(ValidatePortError.InvalidPort)
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
