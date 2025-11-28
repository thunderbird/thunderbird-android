package net.thunderbird.feature.account.settings.impl.domain.usecase

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.eygraber.uri.Uri
import com.eygraber.uri.toKmpUri
import java.io.IOException
import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.file.MimeType
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.account.avatar.Avatar
import net.thunderbird.feature.account.avatar.AvatarImageRepository
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.AccountSettingError

class UpdateAvatarImageTest {

    @Test
    fun `should store avatar image and return Avatar_Image on success`() = runTest {
        // Arrange
        val accountId = AccountIdFactory.create()
        val pickedUri = "file:///picked/image.jpg".toKmpUri()
        val repo = SuccessAvatarImageRepository()
        val mimeTypeResolver = FakeMimeTypeResolver(
            mapOf(
                pickedUri to MimeType.JPEG,
            ),
        )
        val useCase = UpdateAvatarImage(repo, mimeTypeResolver)

        // Act
        val result = useCase(accountId, pickedUri)

        // Assert
        when (result) {
            is Outcome.Success -> {
                val avatar = result.data
                assertThat(avatar).isInstanceOf(Avatar.Image::class)
                assertThat(avatar.uri).isEqualTo(repo.lastUpdatedUri?.toString())
            }
            else -> error("Expected Success but was $result")
        }
        // ensure repository received correct inputs
        assertThat(repo.lastAccountId).isEqualTo(accountId)
        assertThat(repo.lastUpdatedUri).isEqualTo(pickedUri)
    }

    @Test
    fun `should map exception to Outcome_Failure with StorageError`() = runTest {
        // Arrange
        val accountId = AccountIdFactory.create()
        val pickedUri = "file:///picked/image.jpg".toKmpUri()
        val failingRepo = FailingAvatarImageRepository()
        val mimeTypeResolver = FakeMimeTypeResolver(
            mapOf(
                pickedUri to MimeType.JPEG,
            ),
        )
        val useCase = UpdateAvatarImage(failingRepo, mimeTypeResolver)

        // Act
        val result = useCase(accountId, pickedUri)

        // Assert
        assertThat(result).isInstanceOf(Outcome.Failure::class)
        val error = (result as Outcome.Failure).error
        assertThat(error).isInstanceOf(AccountSettingError.StorageError::class)
    }

    @Test
    fun `should return UnsupportedFormat when mime type is not JPEG`() = runTest {
        // Arrange
        val accountId = AccountIdFactory.create()
        val pickedUri = "file:///picked/image.png".toKmpUri()
        val repo = SuccessAvatarImageRepository()
        val mimeTypeResolver = FakeMimeTypeResolver(
            mapOf(
                pickedUri to MimeType.PNG,
            ),
        )
        val useCase = UpdateAvatarImage(repo, mimeTypeResolver)

        // Act
        val result = useCase(accountId, pickedUri)

        // Assert
        assertThat(result).isInstanceOf(Outcome.Failure::class)
        val error = (result as Outcome.Failure).error
        assertThat(error).isInstanceOf(AccountSettingError.UnsupportedFormat::class)
    }

    @Test
    fun `should return UnsupportedFormat when mime type is unknown`() = runTest {
        // Arrange
        val accountId = AccountIdFactory.create()
        val pickedUri = "file:///picked/image".toKmpUri()
        val repo = SuccessAvatarImageRepository()
        val mimeTypeResolver = FakeMimeTypeResolver(
            mapOf(
                pickedUri to null,
            ),
        )
        val useCase = UpdateAvatarImage(repo, mimeTypeResolver)

        // Act
        val result = useCase(accountId, pickedUri)

        // Assert
        assertThat(result).isInstanceOf(Outcome.Failure::class)
        val error = (result as Outcome.Failure).error
        assertThat(error).isInstanceOf(AccountSettingError.UnsupportedFormat::class)
    }
}

private class SuccessAvatarImageRepository : AvatarImageRepository {
    var lastAccountId: AccountId? = null
    var lastUpdatedUri: Uri? = null

    override suspend fun update(id: AccountId, imageUri: Uri): Uri {
        lastAccountId = id
        lastUpdatedUri = imageUri
        // In a real repo this could return a different stored location; for the test we echo input
        return imageUri
    }

    override suspend fun delete(id: AccountId) {
        // not needed for this test
    }
}

private class FailingAvatarImageRepository : AvatarImageRepository {
    override suspend fun update(id: AccountId, imageUri: Uri): Uri {
        throw IOException("disk full")
    }

    override suspend fun delete(id: AccountId) {
        // not needed
    }
}
