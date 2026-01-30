package net.thunderbird.feature.account.settings.impl.domain.usecase

import com.eygraber.uri.Uri
import java.io.IOException
import net.thunderbird.core.file.MimeType
import net.thunderbird.core.file.MimeTypeResolver
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.avatar.Avatar
import net.thunderbird.feature.account.avatar.AvatarImageRepository
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.AccountSettingError
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.UseCase

internal class UpdateAvatarImage(
    private val repository: AvatarImageRepository,
    private val mimeTypeResolver: MimeTypeResolver,
) : UseCase.UpdateAvatarImage {

    override suspend fun invoke(
        accountId: AccountId,
        imageUri: Uri,
    ): Outcome<Avatar.Image, AccountSettingError> {
        val mimeType = mimeTypeResolver.getMimeType(imageUri)

        if (mimeType == null || mimeType != MimeType.JPEG) {
            return Outcome.Failure(
                AccountSettingError.UnsupportedFormat(
                    message = "Only JPEG images are supported. Found: ${mimeType ?: "unknown"}",
                ),
            )
        }

        return try {
            val storedUri = repository.update(accountId, imageUri)
            Outcome.Success(Avatar.Image(uri = storedUri.toString()))
        } catch (e: IOException) {
            Outcome.Failure(
                error = AccountSettingError.StorageError("Failed to store avatar image"),
                cause = e,
            )
        } catch (e: SecurityException) {
            Outcome.Failure(
                error = AccountSettingError.StorageError("Permission denied while storing avatar image."),
                cause = e,
            )
        }
    }
}
