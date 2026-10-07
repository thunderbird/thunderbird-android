package net.thunderbird.feature.account.storage.legacy

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import kotlin.test.Test
import net.thunderbird.account.fake.FakeAccountAvatarData
import net.thunderbird.account.fake.FakeAccountData
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
        val storage = createStorage(accountId)

        // Act
        val result = testSubject.load(accountId, storage)

        // Assert
        assertThat(result.id).isEqualTo(accountId)
        assertThat(result.avatarType).isEqualTo(AVATAR_TYPE)
        assertThat(result.avatarMonogram).isEqualTo(AVATAR_MONOGRAM)
        assertThat(result.avatarImageUri).isEqualTo(AVATAR_IMAGE_URI)
        assertThat(result.avatarIconName).isEqualTo(AVATAR_ICON_NAME)
    }

    @Test
    fun `save should store avatar data to storage`() {
        // Arrange
        val avatarDto = createAvatarDto(accountId)
        val storage = FakeStorage()
        val editor = FakeStorageEditor()

        // Act
        testSubject.save(avatarDto, storage, editor)

        // Assert
        assertThat(editor.values["$accountId.avatarType"]).isEqualTo(AVATAR_TYPE.name)
        assertThat(editor.values["$accountId.avatarMonogram"]).isEqualTo(AVATAR_MONOGRAM)
        assertThat(editor.values["$accountId.avatarImageUri"]).isEqualTo(AVATAR_IMAGE_URI)
        assertThat(editor.values["$accountId.avatarIconName"]).isEqualTo(null)
    }

    @Test
    fun `delete should remove avatar data from storage`() {
        // Arrange
        val storage = FakeStorage()
        val editor = FakeStorageEditor()

        // Act
        testSubject.delete(accountId, storage, editor)

        // Assert
        assertThat(editor.removedKeys).contains("$accountId.avatarType")
        assertThat(editor.removedKeys).contains("$accountId.avatarMonogram")
        assertThat(editor.removedKeys).contains("$accountId.avatarImageUri")
        assertThat(editor.removedKeys).contains("$accountId.avatarIconName")
    }

    private fun createAvatarDto(accountId: AccountId): AvatarDto {
        return AvatarDto(
            id = accountId,
            avatarType = AVATAR_TYPE,
            avatarMonogram = AVATAR_MONOGRAM,
            avatarImageUri = AVATAR_IMAGE_URI,
            avatarIconName = null,
        )
    }

    private fun createStorage(accountId: AccountId): FakeStorage {
        return FakeStorage(
            mapOf(
                "$accountId.avatarType" to AVATAR_TYPE.name,
                "$accountId.avatarMonogram" to AVATAR_MONOGRAM,
                "$accountId.avatarImageUri" to AVATAR_IMAGE_URI,
                "$accountId.avatarIconName" to AVATAR_ICON_NAME,
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
