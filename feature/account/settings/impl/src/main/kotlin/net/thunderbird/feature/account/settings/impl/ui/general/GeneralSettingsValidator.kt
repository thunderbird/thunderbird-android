package net.thunderbird.feature.account.settings.impl.ui.general

import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.UseCase.ValidateAccountName
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.UseCase.ValidateAvatarMonogram

internal class GeneralSettingsValidator(
    private val accountNameValidator: ValidateAccountName,
    private val avatarMonogramValidator: ValidateAvatarMonogram,
) : GeneralSettingsContract.Validator {
    override fun validateName(name: String) = accountNameValidator(name)

    override fun validateMonogram(monogram: String) = avatarMonogramValidator(monogram)
}
