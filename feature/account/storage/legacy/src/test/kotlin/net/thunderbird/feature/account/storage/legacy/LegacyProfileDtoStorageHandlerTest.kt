package net.thunderbird.feature.account.storage.legacy

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import kotlin.test.Test
import net.thunderbird.account.fake.FakeAccountAvatarData
import net.thunderbird.account.fake.FakeAccountData
import net.thunderbird.account.fake.FakeAccountProfileData
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.storage.legacy.fake.FakeStorage
import net.thunderbird.feature.account.storage.legacy.fake.FakeStorageEditor
import net.thunderbird.feature.account.storage.profile.AvatarDto
import net.thunderbird.feature.account.storage.profile.AvatarTypeDto
import net.thunderbird.feature.account.storage.profile.ProfileDto

class LegacyProfileDtoStorageHandlerTest {
    private val avatarDtoStorageHandler = LegacyAvatarDtoStorageHandler()
    private val testSubject = LegacyProfileDtoStorageHandler(avatarDtoStorageHandler)

    @Test
    fun `load should populate profile data from storage`() {
        // Arrange
        val storage = createStorage(accountId)

        // Act
        val result = testSubject.load(accountId, storage)

        // Assert
        assertThat(result.id).isEqualTo(accountId)
        assertThat(result.name).isEqualTo(NAME)
        assertThat(result.color).isEqualTo(COLOR)
        assertThat(result.avatar.avatarType).isEqualTo(AvatarTypeDto.IMAGE)
        assertThat(result.avatar.avatarMonogram).isEqualTo(null)
        assertThat(result.avatar.avatarImageUri).isEqualTo(AVATAR_IMAGE_URI)
        assertThat(result.avatar.avatarIconName).isEqualTo(null)
    }

    @Test
    fun `save should store profile data to storage`() {
        // Arrange
        val profileDto = createProfileDto(accountId)
        val storage = FakeStorage()
        val editor = FakeStorageEditor()

        // Act
        testSubject.save(profileDto, storage, editor)

        // Assert
        assertThat(editor.values["$accountId.description"]).isEqualTo(NAME)
        assertThat(editor.values["$accountId.chipColor"]).isEqualTo(COLOR.toString())
        assertThat(editor.values["$accountId.avatarType"]).isEqualTo("IMAGE")
        assertThat(editor.values["$accountId.avatarMonogram"]).isEqualTo(null)
        assertThat(editor.values["$accountId.avatarImageUri"]).isEqualTo(AVATAR_IMAGE_URI)
        assertThat(editor.values["$accountId.avatarIconName"]).isEqualTo(null)
    }

    @Test
    fun `delete should remove profile data from storage`() {
        // Arrange
        val storage = FakeStorage()
        val editor = FakeStorageEditor()

        // Act
        testSubject.delete(accountId, storage, editor)

        // Assert
        assertThat(editor.removedKeys).contains("$accountId.description")
        assertThat(editor.removedKeys).contains("$accountId.chipColor")
        assertThat(editor.removedKeys).contains("$accountId.avatarType")
        assertThat(editor.removedKeys).contains("$accountId.avatarMonogram")
        assertThat(editor.removedKeys).contains("$accountId.avatarImageUri")
        assertThat(editor.removedKeys).contains("$accountId.avatarIconName")
    }

    private fun createProfileDto(accountId: AccountId): ProfileDto {
        return ProfileDto(
            id = accountId,
            name = NAME,
            color = COLOR,
            avatar = AvatarDto(
                id = accountId,
                avatarType = AvatarTypeDto.IMAGE,
                avatarMonogram = null,
                avatarImageUri = AVATAR_IMAGE_URI,
                avatarIconName = null,
            ),
        )
    }

    private fun createStorage(accountId: AccountId): FakeStorage {
        return FakeStorage(
            mapOf(
                "$accountId.description" to NAME,
                "$accountId.chipColor" to COLOR.toString(),
                "$accountId.avatarType" to AVATAR_TYPE.name,
                "$accountId.avatarImageUri" to AVATAR_IMAGE_URI,
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
