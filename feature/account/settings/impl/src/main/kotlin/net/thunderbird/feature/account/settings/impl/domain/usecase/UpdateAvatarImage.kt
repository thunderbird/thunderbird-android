package net.thunderbird.feature.account.settings.impl.domain.usecase

import com.eygraber.uri.Uri
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.avatar.Avatar
import net.thunderbird.feature.account.avatar.AvatarImageRepository
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.AccountSettingError
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.UseCase

internal class UpdateAvatarImage(
    private val repository: AvatarImageRepository,
) : UseCase.UpdateAvatarImage {

    override suspend fun invoke(
        accountId: AccountId,
        imageUri: Uri,
    ): Outcome<Avatar.Image, AccountSettingError> = try {
        val storedUri = repository.update(accountId, imageUri)
        Outcome.Success(Avatar.Image(uri = storedUri.toString()))
    } catch (e: Exception) {
        Outcome.Failure(AccountSettingError.StorageError(e.message ?: "Failed to store avatar image"))
    }
}
