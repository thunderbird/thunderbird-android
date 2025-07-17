package net.thunderbird.feature.account.storage.legacy.mapper

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlin.test.Test
import net.thunderbird.feature.account.profile.AccountAvatar
import net.thunderbird.feature.account.storage.profile.AvatarDto
import net.thunderbird.feature.account.storage.profile.AvatarTypeDto

class DefaultAccountAvatarDataMapperTest {

    private val testSubject = DefaultAccountAvatarDataMapper()

    @Test
    fun `toDomain should map valid AvatarDto to correct AccountAvatar type`() {
        require(testCases.isNotEmpty()) { "Test cases should not be empty" }

        testCases.forEach { case ->
            // Arrange
            val dto = case.dto
            val expected = case.domain

            // Act
            val result = testSubject.toDomain(dto)

            // Assert
            when (result) {
                is AccountAvatar.Monogram -> assertDomainMonogram(result, expected)
                is AccountAvatar.Image -> assertDomainImage(result, expected)
                is AccountAvatar.Icon -> assertDomainIcon(result, expected)
            }
        }
    }

    @Test
    fun `toDomain should return default monogram for invalid AvatarDto`() {
        val avatarTypeDtos = AvatarTypeDto.entries

        avatarTypeDtos.forEach { type ->
            // Arrange
            val dto = AvatarDto(
                avatarType = type,
                avatarMonogram = null,
                avatarImageUri = null,
                avatarIconName = null,
            )

            // Act
            val result = testSubject.toDomain(dto)

            // Assert
            assertDomainMonogram(result, AccountAvatar.Monogram("XX"))
        }
    }

    @Test
    fun `toDto should map valid AccountAvatar to correct AvatarDto type`() {
        require(testCases.isNotEmpty()) { "Test cases should not be empty" }

        testCases.forEach { case ->
            // Arrange
            val domain = case.domain
            val expected = case.dto

            // Act
            val result = testSubject.toDto(domain)

            // Assert
            when (result.avatarType) {
                AvatarTypeDto.MONOGRAM -> assertDtoMonogram(result, expected)
                AvatarTypeDto.IMAGE -> assertDtoImage(result, expected)
                AvatarTypeDto.ICON -> assertDtoIcon(result, expected)
            }
        }
    }

    private fun assertDomainMonogram(actual: AccountAvatar, expected: AccountAvatar) {
        require(expected is AccountAvatar.Monogram) { "Expected AccountAvatar to be of type Monogram" }
        assertThat(actual).isEqualTo(expected)
    }

    private fun assertDomainImage(actual: AccountAvatar, expected: AccountAvatar) {
        require(expected is AccountAvatar.Image) { "Expected AccountAvatar to be of type Image" }
        assertThat(actual).isEqualTo(expected)
    }

    private fun assertDomainIcon(actual: AccountAvatar, expected: AccountAvatar) {
        require(expected is AccountAvatar.Icon) { "Expected AccountAvatar to be of type Icon" }
        assertThat(actual).isEqualTo(expected)
    }

    private fun assertDtoMonogram(actual: AvatarDto, expected: AvatarDto) {
        assertThat(actual.avatarType).isEqualTo(AvatarTypeDto.MONOGRAM)
        assertThat(actual.avatarMonogram).isEqualTo(expected.avatarMonogram)
        assertThat(actual.avatarImageUri).isNull()
        assertThat(actual.avatarIconName).isNull()
    }

    private fun assertDtoImage(actual: AvatarDto, expected: AvatarDto) {
        assertThat(actual.avatarType).isEqualTo(AvatarTypeDto.IMAGE)
        assertThat(actual.avatarMonogram).isNull()
        assertThat(actual.avatarImageUri).isEqualTo(expected.avatarImageUri)
        assertThat(actual.avatarIconName).isNull()
    }

    private fun assertDtoIcon(actual: AvatarDto, expected: AvatarDto) {
        assertThat(actual.avatarType).isEqualTo(AvatarTypeDto.ICON)
        assertThat(actual.avatarMonogram).isNull()
        assertThat(actual.avatarImageUri).isNull()
        assertThat(actual.avatarIconName).isEqualTo(expected.avatarIconName)
    }

    private companion object {
        data class TestCase(
            val dto: AvatarDto,
            val domain: AccountAvatar,
        )

        val testCases = listOf(
            TestCase(
                AvatarDto(AvatarTypeDto.MONOGRAM, "AB", null, null),
                AccountAvatar.Monogram("AB"),
            ),
            TestCase(
                AvatarDto(AvatarTypeDto.IMAGE, null, "uri://img", null),
                AccountAvatar.Image("uri://img"),
            ),
            TestCase(
                AvatarDto(AvatarTypeDto.ICON, null, null, "icon_name"),
                AccountAvatar.Icon("icon_name"),
            ),
        )
    }
}
