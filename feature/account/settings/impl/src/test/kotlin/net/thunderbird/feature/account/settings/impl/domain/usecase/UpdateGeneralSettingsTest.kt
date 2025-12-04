package net.thunderbird.feature.account.settings.impl.domain.usecase

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import kotlin.test.Test
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.account.avatar.Avatar
import net.thunderbird.feature.account.profile.AccountProfile
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.AccountSettingError
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.UpdateGeneralSettingCommand

class UpdateGeneralSettingsTest {

    @Test
    fun `should update account profile`() = runTest {
        // Arrange
        val accountId = AccountIdFactory.create()
        val accountProfile = AccountProfile(
            id = accountId,
            name = "Test Account",
            color = 0xFF0000,
            avatar = Avatar.Icon(name = "star"),
        )
        val newName = "Updated Account Name"
        val command = UpdateGeneralSettingCommand.UpdateName(newName)
        val repository = FakeAccountProfileRepository(
            initialAccountProfile = accountProfile,
        )
        val testSubject = UpdateGeneralSettings(repository)

        // Act
        val result = testSubject(accountId, command)

        // Assert
        assertThat(result).isInstanceOf(Outcome.Success::class)
        assertThat(repository.getById(accountId).firstOrNull()).isEqualTo(
            accountProfile.copy(name = newName),
        )
    }

    @Test
    fun `should update account profile for all general settings`() = runTest {
        // Arrange
        val accountId = AccountIdFactory.create()
        val accountProfile = AccountProfile(
            id = accountId,
            name = "Test Account",
            color = 0xFF0000,
            avatar = Avatar.Icon(name = "star"),
        )
        val newName = "Updated Account Name"
        val newColor = 0x00FF00
        val commands = listOf(
            UpdateGeneralSettingCommand.UpdateName(newName),
            UpdateGeneralSettingCommand.UpdateColor(newColor),
        )
        val repository = FakeAccountProfileRepository(
            initialAccountProfile = accountProfile,
        )
        val testSubject = UpdateGeneralSettings(repository)

        // Act
        commands.forEach { command ->
            testSubject(accountId, command)
        }

        // Assert
        assertThat(repository.getById(accountId).firstOrNull()).isEqualTo(
            accountProfile.copy(
                name = newName,
                color = newColor,
            ),
        )
    }

    @Test
    fun `should emit NotFound when account profile not found`() = runTest {
        // Arrange
        val accountId = AccountIdFactory.create()
        val command = UpdateGeneralSettingCommand.UpdateName("Updated Account Name")
        val repository = FakeAccountProfileRepository()
        val testSubject = UpdateGeneralSettings(repository)

        // Act
        val result = testSubject(accountId, command)

        // Assert
        assertThat(result).isInstanceOf(Outcome.Failure::class)
        assertThat((result as Outcome.Failure).error).isInstanceOf(AccountSettingError.NotFound::class)
    }

    @Test
    fun `should update avatar when UpdateAvatar command is used`() = runTest {
        // Arrange
        val accountId = AccountIdFactory.create()
        val accountProfile = AccountProfile(
            id = accountId,
            name = "Test Account",
            color = 0xFF0000,
            avatar = Avatar.Icon(name = "star"),
        )
        val imageAvatar = Avatar.Image(uri = "avatar://uri")
        val repository = FakeAccountProfileRepository(initialAccountProfile = accountProfile)
        val testSubject = UpdateGeneralSettings(repository)

        // Act
        val result = testSubject(accountId, UpdateGeneralSettingCommand.UpdateAvatar(imageAvatar))

        // Assert
        assertThat(result).isInstanceOf(Outcome.Success::class)
        assertThat(repository.getById(accountId).firstOrNull()).isEqualTo(
            accountProfile.copy(avatar = imageAvatar),
        )
    }

    @Test
    fun `should emit NotFound when updating avatar for non-existing account`() = runTest {
        // Arrange
        val accountId = AccountIdFactory.create()
        val imageAvatar = Avatar.Image(uri = "avatar://uri")
        val repository = FakeAccountProfileRepository()
        val testSubject = UpdateGeneralSettings(repository)

        // Act
        val result = testSubject(accountId, UpdateGeneralSettingCommand.UpdateAvatar(imageAvatar))

        // Assert
        assertThat(result).isInstanceOf(Outcome.Failure::class)
        assertThat((result as Outcome.Failure).error).isInstanceOf(AccountSettingError.NotFound::class)
    }
}
