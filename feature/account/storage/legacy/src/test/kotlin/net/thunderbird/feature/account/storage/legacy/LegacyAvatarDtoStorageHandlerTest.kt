package net.thunderbird.feature.account.storage.legacy

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import kotlin.test.Test
import net.thunderbird.account.fake.FakeAccountAvatarData
import net.thunderbird.account.fake.FakeAccountData
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.storage.legacy.fake.FakeStorage
import net.thunderbird.feature.account.storage.legacy.fake.FakeStorageEditor
import net.thunderbird.feature.account.storage.profile.AvatarDto
import net.thunderbird.feature.account.storage.profile.AvatarTypeDto

class LegacyAvatarDtoStorageHandlerTest {
    private val testSubject = LegacyAvatarDtoStorageHandler()

    @Test
    fun `load should populate avatar data from storage`() {
        // Arrange
        val account = createAccount(accountId)
        val storage = createStorage(accountId)

        // Act
        testSubject.load(account, storage)

        // Assert
        assertThat(account.avatar.avatarType).isEqualTo(AVATAR_TYPE)
        assertThat(account.avatar.avatarMonogram).isEqualTo(AVATAR_MONOGRAM)
        assertThat(account.avatar.avatarImageUri).isEqualTo(AVATAR_IMAGE_URI)
        assertThat(account.avatar.avatarIconName).isEqualTo(AVATAR_ICON_NAME)
    }

    @Test
    fun `save should store avatar data to storage`() {
        // Arrange
        val account = createAccount(accountId)
        val storage = FakeStorage()
        val editor = FakeStorageEditor()

        // Act
        testSubject.save(account, storage, editor)

        // Assert
        assertThat(editor.values["${accountId.asRaw()}.avatarType"]).isEqualTo(AVATAR_TYPE.name)
        assertThat(editor.values["${accountId.asRaw()}.avatarMonogram"]).isEqualTo(AVATAR_MONOGRAM)
        assertThat(editor.values["${accountId.asRaw()}.avatarImageUri"]).isEqualTo(AVATAR_IMAGE_URI)
        assertThat(editor.values["${accountId.asRaw()}.avatarIconName"]).isEqualTo(null)
    }

    @Test
    fun `delete should remove avatar data from storage`() {
        // Arrange
        val account = createAccount(accountId)
        val storage = FakeStorage()
        val editor = FakeStorageEditor()

        // Act
        testSubject.delete(account, storage, editor)

        // Assert
        assertThat(editor.removedKeys).contains("${accountId.asRaw()}.avatarType")
        assertThat(editor.removedKeys).contains("${accountId.asRaw()}.avatarMonogram")
        assertThat(editor.removedKeys).contains("${accountId.asRaw()}.avatarImageUri")
        assertThat(editor.removedKeys).contains("${accountId.asRaw()}.avatarIconName")
    }

    // Arrange methods
    private fun createAccount(accountId: AccountId): LegacyAccount {
        return LegacyAccount(accountId.asRaw()).apply {
            name = "Test Account"
            chipColor = 0x0099CC // Default color
            avatar = AvatarDto(
                avatarType = AVATAR_TYPE,
                avatarMonogram = AVATAR_MONOGRAM,
                avatarImageUri = AVATAR_IMAGE_URI,
                avatarIconName = null,
            )
        }
    }

    private fun createStorage(accountId: AccountId): FakeStorage {
        return FakeStorage(
            mapOf(
                "${accountId.asRaw()}.avatarType" to AVATAR_TYPE.name,
                "${accountId.asRaw()}.avatarMonogram" to AVATAR_MONOGRAM,
                "${accountId.asRaw()}.avatarImageUri" to AVATAR_IMAGE_URI,
                "${accountId.asRaw()}.avatarIconName" to AVATAR_ICON_NAME,
            ),
        )
    }

    private companion object {
        val accountId = FakeAccountData.ACCOUNT_ID

        val AVATAR_TYPE = AvatarTypeDto.MONOGRAM
        const val AVATAR_MONOGRAM = "TB"
        const val AVATAR_IMAGE_URI = FakeAccountAvatarData.AVATAR_IMAGE_URI
        const val AVATAR_ICON_NAME = "icon-name"
    }
}
