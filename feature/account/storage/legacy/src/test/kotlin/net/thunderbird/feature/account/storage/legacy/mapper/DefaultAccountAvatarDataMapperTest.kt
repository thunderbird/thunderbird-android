package net.thunderbird.feature.account.storage.legacy.mapper

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlin.test.Test
import net.thunderbird.feature.account.avatar.Avatar
import net.thunderbird.feature.account.storage.profile.AvatarDto
import net.thunderbird.feature.account.storage.profile.AvatarTypeDto

class DefaultAccountAvatarDataMapperTest {

    private val testSubject = DefaultAvatarDataMapper()

    @Test
    fun `toDomain should map valid AvatarDto to correct Avatar type`() {
        require(testCases.isNotEmpty()) { "Test cases should not be empty" }

        testCases.forEach { case ->
            // Arrange
            val dto = case.dto
            val expected = case.domain

            // Act
            val result = testSubject.toDomain(dto)

            // Assert
            when (result) {
                is Avatar.Monogram -> assertDomainMonogram(result, expected)
                is Avatar.Image -> assertDomainImage(result, expected)
                is Avatar.Icon -> assertDomainIcon(result, expected)
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
            assertDomainMonogram(result, Avatar.Monogram("XX"))
        }
    }

    @Test
    fun `toDto should map valid Avatar to correct AvatarDto type`() {
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

    private fun assertDomainMonogram(actual: Avatar, expected: Avatar) {
        require(expected is Avatar.Monogram) { "Expected Avatar to be of type Monogram" }
        assertThat(actual).isEqualTo(expected)
    }

    private fun assertDomainImage(actual: Avatar, expected: Avatar) {
        require(expected is Avatar.Image) { "Expected Avatar to be of type Image" }
        assertThat(actual).isEqualTo(expected)
    }

    private fun assertDomainIcon(actual: Avatar, expected: Avatar) {
        require(expected is Avatar.Icon) { "Expected Avatar to be of type Icon" }
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
            val domain: Avatar,
        )

        val testCases = listOf(
            TestCase(
                AvatarDto(AvatarTypeDto.MONOGRAM, "AB", null, null),
                Avatar.Monogram("AB"),
            ),
            TestCase(
                AvatarDto(AvatarTypeDto.IMAGE, null, "uri://img", null),
                Avatar.Image("uri://img"),
            ),
            TestCase(
                AvatarDto(AvatarTypeDto.ICON, null, null, "icon_name"),
                Avatar.Icon("icon_name"),
            ),
        )
    }
}
