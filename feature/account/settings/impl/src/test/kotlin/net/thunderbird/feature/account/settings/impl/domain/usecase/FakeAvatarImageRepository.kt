package net.thunderbird.feature.account.settings.impl.domain.usecase

import com.eygraber.uri.Uri
import com.eygraber.uri.Url
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.avatar.AvatarImageRepository

class FakeAvatarImageRepository(
    initialAvatarUri: Uri? = null,
) : AvatarImageRepository {

    private var currentAvatarUri = initialAvatarUri

    override suspend fun update(
        id: AccountId,
        imageUri: Uri,
    ): Uri {
        currentAvatarUri = imageUri
        return Url.parse("https://fake.storage/$id.jpg")
    }

    override suspend fun delete(id: AccountId) {
        currentAvatarUri = null
    }
}
