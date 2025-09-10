package net.thunderbird.feature.account.storage.legacy

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import kotlin.test.Test
import net.thunderbird.account.fake.FakeAccountAvatarData
import net.thunderbird.account.fake.FakeAccountData
import net.thunderbird.account.fake.FakeAccountProfileData
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.storage.legacy.fake.FakeStorage
import net.thunderbird.feature.account.storage.legacy.fake.FakeStorageEditor
import net.thunderbird.feature.account.storage.profile.AvatarDto
import net.thunderbird.feature.account.storage.profile.AvatarTypeDto

class LegacyProfileDtoStorageHandlerTest {
    private val avatarDtoStorageHandler = LegacyAvatarDtoStorageHandler()
    private val testSubject = LegacyProfileDtoStorageHandler(avatarDtoStorageHandler)

    @Test
    fun `load should populate profile data from storage`() {
        // Arrange
        val account = createAccount(accountId)
        val storage = createStorage(accountId)

        // Act
        testSubject.load(account, storage)

        // Assert
        assertThat(account.name).isEqualTo(NAME)
        assertThat(account.chipColor).isEqualTo(COLOR)
        assertThat(account.avatar.avatarType).isEqualTo(AvatarTypeDto.IMAGE)
        assertThat(account.avatar.avatarMonogram).isEqualTo(null)
        assertThat(account.avatar.avatarImageUri).isEqualTo(AVATAR_IMAGE_URI)
        assertThat(account.avatar.avatarIconName).isEqualTo(null)
    }

    @Test
    fun `save should store profile data to storage`() {
        // Arrange
        val account = createAccount(accountId)
        val storage = FakeStorage()
        val editor = FakeStorageEditor()

        // Act
        testSubject.save(account, storage, editor)

        // Assert
        assertThat(editor.values["${accountId.asRaw()}.description"]).isEqualTo(NAME)
        assertThat(editor.values["${accountId.asRaw()}.chipColor"]).isEqualTo(COLOR.toString())
        assertThat(editor.values["${accountId.asRaw()}.avatarType"]).isEqualTo("IMAGE")
        assertThat(editor.values["${accountId.asRaw()}.avatarMonogram"]).isEqualTo(null)
        assertThat(editor.values["${accountId.asRaw()}.avatarImageUri"]).isEqualTo(AVATAR_IMAGE_URI)
        assertThat(editor.values["${accountId.asRaw()}.avatarIconName"]).isEqualTo(null)
    }

    @Test
    fun `delete should remove profile data from storage`() {
        // Arrange
        val account = createAccount(accountId)
        val storage = FakeStorage()
        val editor = FakeStorageEditor()

        // Act
        testSubject.delete(account, storage, editor)

        // Assert
        assertThat(editor.removedKeys).contains("${accountId.asRaw()}.description")
        assertThat(editor.removedKeys).contains("${accountId.asRaw()}.chipColor")
        assertThat(editor.removedKeys).contains("${accountId.asRaw()}.avatarType")
        assertThat(editor.removedKeys).contains("${accountId.asRaw()}.avatarMonogram")
        assertThat(editor.removedKeys).contains("${accountId.asRaw()}.avatarImageUri")
        assertThat(editor.removedKeys).contains("${accountId.asRaw()}.avatarIconName")
    }

    // Arrange methods
    private fun createAccount(accountId: AccountId): LegacyAccountDto {
        return LegacyAccountDto(accountId.asRaw()).apply {
            name = NAME
            chipColor = COLOR
            avatar = AvatarDto(
                avatarType = AvatarTypeDto.IMAGE,
                avatarMonogram = null,
                avatarImageUri = AVATAR_IMAGE_URI,
                avatarIconName = null,
            )
        }
    }

    private fun createStorage(accountId: AccountId): FakeStorage {
        return FakeStorage(
            mapOf(
                "${accountId.asRaw()}.description" to NAME,
                "${accountId.asRaw()}.chipColor" to COLOR.toString(),
                "${accountId.asRaw()}.avatarType" to AVATAR_TYPE.name,
                "${accountId.asRaw()}.avatarImageUri" to AVATAR_IMAGE_URI,
            ),
        )
    }

    private companion object {
        val accountId = FakeAccountData.ACCOUNT_ID

        const val NAME = FakeAccountProfileData.PROFILE_NAME
        const val COLOR = FakeAccountProfileData.PROFILE_COLOR

        val AVATAR_TYPE = AvatarTypeDto.IMAGE
        const val AVATAR_IMAGE_URI = FakeAccountAvatarData.AVATAR_IMAGE_URI
    }
}
