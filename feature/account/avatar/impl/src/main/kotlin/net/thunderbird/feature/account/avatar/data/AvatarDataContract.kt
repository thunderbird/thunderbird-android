package net.thunderbird.feature.account.avatar.data

import com.eygraber.uri.Uri
import net.thunderbird.feature.account.AccountId

internal interface AvatarDataContract {

    interface DataSource {

        interface LocalAvatarImage {
            suspend fun update(id: AccountId, imageUri: Uri): Uri

            suspend fun delete(id: AccountId)

            companion object {
                const val DIRECTORY_NAME = "account_avatars"
            }
        }
    }
}
