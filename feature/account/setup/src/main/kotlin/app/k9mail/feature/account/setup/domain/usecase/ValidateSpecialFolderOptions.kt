package app.k9mail.feature.account.setup.domain.usecase

import app.k9mail.feature.account.common.domain.entity.SpecialFolderOption
import app.k9mail.feature.account.common.domain.entity.SpecialFolderOptions
import app.k9mail.feature.account.setup.domain.DomainContract.UseCase
import app.k9mail.feature.account.setup.domain.DomainContract.UseCase.ValidateSpecialFolderOptions.Failure
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.core.validation.ValidationOutcome
import net.thunderbird.core.validation.ValidationSuccess

class ValidateSpecialFolderOptions : UseCase.ValidateSpecialFolderOptions {
    override fun invoke(specialFolderOptions: SpecialFolderOptions): ValidationOutcome {
        return if (specialFolderOptions.hasMissingDefaultOption()) {
            Outcome.Failure(error = Failure.MissingDefaultSpecialFolderOption)
        } else {
            ValidationSuccess
        }
    }

    private fun SpecialFolderOptions.hasMissingDefaultOption(): Boolean {
        return archiveSpecialFolderOptions.hasMissingDefaultFolder() ||
            draftsSpecialFolderOptions.hasMissingDefaultFolder() ||
            sentSpecialFolderOptions.hasMissingDefaultFolder() ||
            spamSpecialFolderOptions.hasMissingDefaultFolder() ||
            trashSpecialFolderOptions.hasMissingDefaultFolder()
    }

    private fun List<SpecialFolderOption>.hasMissingDefaultFolder(): Boolean {
        return first() is SpecialFolderOption.None
    }
}
