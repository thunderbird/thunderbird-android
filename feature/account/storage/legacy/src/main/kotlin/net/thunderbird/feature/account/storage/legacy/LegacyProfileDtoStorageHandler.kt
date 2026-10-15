package net.thunderbird.feature.account.storage.legacy

import net.thunderbird.core.android.account.AccountDefaultsProvider
import net.thunderbird.core.preference.storage.Storage
import net.thunderbird.core.preference.storage.StorageEditor
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.storage.profile.ProfileDto

class LegacyProfileDtoStorageHandler(
    private val avatarDtoStorageHandler: AvatarDtoStorageHandler,
) : ProfileDtoStorageHandler {

    override fun load(
        accountId: AccountId,
        storage: Storage,
    ): ProfileDto {
        val keyGen = AccountKeyGenerator(accountId)

        val avatar = avatarDtoStorageHandler.load(accountId, storage)

        val profileDto = ProfileDto(
            id = accountId,
            name = storage.getStringOrDefault(keyGen.create(KEY_NAME), ""),
            color = storage.getInt(keyGen.create(KEY_COLOR), AccountDefaultsProvider.COLOR),
            avatar = avatar,
        )

        return profileDto
    }

    override fun save(
        data: ProfileDto,
        storage: Storage,
        editor: StorageEditor,
    ) {
        val keyGen = AccountKeyGenerator(data.id)

        with(data) {
            editor.putString(keyGen.create(KEY_NAME), name)
            editor.putInt(keyGen.create(KEY_COLOR), color)
        }

        avatarDtoStorageHandler.save(data.avatar, storage, editor)
    }

    override fun delete(
        accountId: AccountId,
        storage: Storage,
        editor: StorageEditor,
    ) {
        val keyGen = AccountKeyGenerator(accountId)

        editor.remove(keyGen.create(KEY_NAME))
        editor.remove(keyGen.create(KEY_COLOR))

        avatarDtoStorageHandler.delete(accountId, storage, editor)
    }

    private companion object Companion {
        const val KEY_COLOR = "chipColor"
        const val KEY_NAME = "description"
    }
}
