package app.k9mail.feature.account.setup.domain.usecase

import app.k9mail.core.common.domain.usecase.validation.ValidationResult
import app.k9mail.feature.account.common.domain.entity.SpecialFolderOption
import app.k9mail.feature.account.common.domain.entity.SpecialFolderOptions
import app.k9mail.feature.account.setup.domain.DomainContract.UseCase
import app.k9mail.feature.account.setup.domain.DomainContract.UseCase.ValidateSpecialFolderOptions.Failure

class ValidateSpecialFolderOptions : UseCase.ValidateSpecialFolderOptions {
    override fun invoke(specialFolderOptions: SpecialFolderOptions): ValidationResult {
        return if (specialFolderOptions.hasMissingDefaultOption()) {
            ValidationResult.Failure(error = Failure.MissingDefaultSpecialFolderOption)
        } else {
            ValidationResult.Success
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
