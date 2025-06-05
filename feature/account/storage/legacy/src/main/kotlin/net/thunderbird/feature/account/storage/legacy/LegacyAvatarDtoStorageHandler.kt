package net.thunderbird.feature.account.storage.legacy

import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.preference.storage.Storage
import net.thunderbird.core.preference.storage.StorageEditor
import net.thunderbird.core.preference.storage.getEnumOrDefault
import net.thunderbird.core.preference.storage.putEnum
import net.thunderbird.feature.account.storage.profile.AvatarDto
import net.thunderbird.feature.account.storage.profile.AvatarTypeDto

class LegacyAvatarDtoStorageHandler : AvatarDtoStorageHandler {

    override fun load(
        data: LegacyAccount,
        storage: Storage,
    ) {
        val keyGen = AccountKeyGenerator(data.id)

        with(data) {
            avatar = AvatarDto(
                avatarType = storage.getEnumOrDefault(keyGen.create(KEY_AVATAR_TYPE), AvatarTypeDto.MONOGRAM),
                avatarMonogram = storage.getStringOrNull(keyGen.create(KEY_AVATAR_MONOGRAM)),
                avatarImageUri = storage.getStringOrNull(keyGen.create(KEY_AVATAR_IMAGE_URI)),
                avatarIconName = storage.getStringOrNull(keyGen.create(KEY_AVATAR_ICON_NAME)),
            )
        }
    }

    override fun save(
        data: LegacyAccount,
        storage: Storage,
        editor: StorageEditor,
    ) {
        val keyGen = AccountKeyGenerator(data.id)

        with(data.avatar) {
            editor.putEnum(keyGen.create(KEY_AVATAR_TYPE), avatarType)
            editor.putString(keyGen.create(KEY_AVATAR_MONOGRAM), avatarMonogram)
            editor.putString(keyGen.create(KEY_AVATAR_IMAGE_URI), avatarImageUri)
            editor.putString(keyGen.create(KEY_AVATAR_ICON_NAME), avatarIconName)
        }
    }

    override fun delete(
        data: LegacyAccount,
        storage: Storage,
        editor: StorageEditor,
    ) {
        val keyGen = AccountKeyGenerator(data.id)

        editor.remove(keyGen.create(KEY_AVATAR_TYPE))
        editor.remove(keyGen.create(KEY_AVATAR_MONOGRAM))
        editor.remove(keyGen.create(KEY_AVATAR_IMAGE_URI))
        editor.remove(keyGen.create(KEY_AVATAR_ICON_NAME))
    }

    private companion object Companion {
        const val KEY_AVATAR_TYPE = "avatarType"
        const val KEY_AVATAR_MONOGRAM = "avatarMonogram"
        const val KEY_AVATAR_IMAGE_URI = "avatarImageUri"
        const val KEY_AVATAR_ICON_NAME = "avatarIconName"
    }
}
