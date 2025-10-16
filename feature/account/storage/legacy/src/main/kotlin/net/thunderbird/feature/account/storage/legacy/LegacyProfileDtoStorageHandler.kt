package net.thunderbird.feature.account.storage.legacy

import net.thunderbird.core.android.account.AccountDefaultsProvider
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.preference.storage.Storage
import net.thunderbird.core.preference.storage.StorageEditor

class LegacyProfileDtoStorageHandler(
    private val avatarDtoStorageHandler: AvatarDtoStorageHandler,
) : ProfileDtoStorageHandler {

    override fun load(
        data: LegacyAccountDto,
        storage: Storage,
    ) {
        val keyGen = AccountKeyGenerator(data.id)

        with(data) {
            name = storage.getStringOrNull(keyGen.create(KEY_NAME))
            chipColor = storage.getInt(keyGen.create(KEY_COLOR), AccountDefaultsProvider.COLOR)
        }

        avatarDtoStorageHandler.load(data, storage)
    }

    override fun save(
        data: LegacyAccountDto,
        storage: Storage,
        editor: StorageEditor,
    ) {
        val keyGen = AccountKeyGenerator(data.id)

        with(data) {
            editor.putString(keyGen.create(KEY_NAME), name)
            editor.putInt(keyGen.create(KEY_COLOR), chipColor)
        }

        avatarDtoStorageHandler.save(data, storage, editor)
    }

    override fun delete(
        data: LegacyAccountDto,
        storage: Storage,
        editor: StorageEditor,
    ) {
        val keyGen = AccountKeyGenerator(data.id)

        editor.remove(keyGen.create(KEY_NAME))
        editor.remove(keyGen.create(KEY_COLOR))

        avatarDtoStorageHandler.delete(data, storage, editor)
    }

    private companion object Companion {
        const val KEY_COLOR = "chipColor"
        const val KEY_NAME = "description"
    }
}
