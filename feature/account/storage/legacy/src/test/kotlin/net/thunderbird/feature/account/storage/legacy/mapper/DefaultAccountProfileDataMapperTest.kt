package net.thunderbird.feature.account.storage.legacy.mapper

import assertk.assertThat
import assertk.assertions.isEqualTo
import net.thunderbird.account.fake.FakeAccountAvatarData.AVATAR_IMAGE_URI
import net.thunderbird.account.fake.FakeAccountData.ACCOUNT_ID
import net.thunderbird.account.fake.FakeAccountProfileData.PROFILE_COLOR
import net.thunderbird.account.fake.FakeAccountProfileData.PROFILE_NAME
import net.thunderbird.account.fake.FakeAccountProfileData.createAccountProfile
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.storage.profile.AvatarDto
import net.thunderbird.feature.account.storage.profile.AvatarTypeDto
import net.thunderbird.feature.account.storage.profile.ProfileDto
import org.junit.Test

class DefaultAccountProfileDataMapperTest {

    @Test
    fun `toDomain should convert ProfileDto to AccountProfile`() {
        // Arrange
        val dto = createProfileDto()
        val expected = createAccountProfile()

        val testSubject = DefaultAccountProfileDataMapper(
            avatarMapper = FakeAccountAvatarDataMapper(
                dto = dto.avatar,
                domain = expected.avatar,
            ),
        )

        // Act
        val result = testSubject.toDomain(dto)

        // Assert
        assertThat(result.id).isEqualTo(expected.id)
        assertThat(result.name).isEqualTo(expected.name)
        assertThat(result.color).isEqualTo(expected.color)
        assertThat(result.avatar).isEqualTo(expected.avatar)
    }

    @Test
    fun `toDto should convert AccountProfile to ProfileDto`() {
        // Arrange
        val domain = createAccountProfile()
        val expected = createProfileDto()

        val testSubject = DefaultAccountProfileDataMapper(
            avatarMapper = FakeAccountAvatarDataMapper(
                dto = expected.avatar,
                domain = domain.avatar,
            ),
        )

        // Act
        val result = testSubject.toDto(domain)

        // Assert
        assertThat(result.id).isEqualTo(expected.id)
        assertThat(result.name).isEqualTo(expected.name)
        assertThat(result.color).isEqualTo(expected.color)
        assertThat(result.avatar).isEqualTo(expected.avatar)
    }

    private companion object {
        fun createProfileDto(
            id: AccountId = ACCOUNT_ID,
            name: String = PROFILE_NAME,
            color: Int = PROFILE_COLOR,
            avatar: AvatarDto = AvatarDto(
                avatarType = AvatarTypeDto.IMAGE,
                avatarMonogram = null,
                avatarImageUri = AVATAR_IMAGE_URI,
                avatarIconName = null,
            ),
        ): ProfileDto {
            return ProfileDto(
                id = id,
                name = name,
                color = color,
                avatar = avatar,
            )
        }
    }
}
