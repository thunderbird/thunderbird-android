package net.thunderbird.feature.account.storage.legacy

import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.preference.storage.Storage
import net.thunderbird.core.preference.storage.StorageEditor

class LegacyProfileDtoStorageHandler(
    private val avatarDtoStorageHandler: AvatarDtoStorageHandler,
) : ProfileDtoStorageHandler {

    override fun load(
        data: LegacyAccount,
        storage: Storage,
    ) {
        val keyGen = AccountKeyGenerator(data.id)

        with(data) {
            name = storage.getStringOrNull(keyGen.create(KEY_NAME))
            chipColor = storage.getInt(keyGen.create(KEY_COLOR), FALLBACK_ACCOUNT_COLOR)
        }

        avatarDtoStorageHandler.load(data, storage)
    }

    override fun save(
        data: LegacyAccount,
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
        data: LegacyAccount,
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

        // TODO why?
        const val FALLBACK_ACCOUNT_COLOR = 0x0099CC
    }
}
