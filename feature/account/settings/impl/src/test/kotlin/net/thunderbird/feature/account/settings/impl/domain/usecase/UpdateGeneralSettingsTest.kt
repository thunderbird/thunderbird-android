package net.thunderbird.feature.account.settings.impl.domain.usecase

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import kotlin.test.Test
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.core.ui.setting.SettingValue
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.account.profile.AccountAvatar
import net.thunderbird.feature.account.profile.AccountProfile
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.SettingsError
import net.thunderbird.feature.account.settings.impl.domain.entity.GeneralPreference
import net.thunderbird.feature.account.settings.impl.domain.entity.generateId

class UpdateGeneralSettingsTest {

    @Test
    fun `should update account profile`() = runTest {
        // Arrange
        val accountId = AccountIdFactory.create()
        val accountProfile = AccountProfile(
            id = accountId,
            name = "Test Account",
            color = 0xFF0000,
            avatar = AccountAvatar.Icon(name = "star"),
        )
        val newName = "Updated Account Name"
        val setting = SettingValue.Text(
            id = GeneralPreference.NAME.generateId(accountId),
            title = { "Name" },
            description = { "Account name" },
            icon = { null },
            value = newName,
        )
        val repository = FakeAccountProfileRepository(
            initialAccountProfile = accountProfile,
        )
        val testSubject = UpdateGeneralSettings(repository)

        // Act
        val result = testSubject(accountId, setting)

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
            avatar = AccountAvatar.Icon(name = "star"),
        )
        val newName = "Updated Account Name"
        val newColor = 0x00FF00
        val settings = listOf(
            SettingValue.Text(
                id = GeneralPreference.NAME.generateId(accountId),
                title = { "Name" },
                description = { "Account name" },
                icon = { null },
                value = newName,
            ),
            SettingValue.Color(
                id = GeneralPreference.COLOR.generateId(accountId),
                title = { "Color" },
                description = { "Account color" },
                icon = { null },
                value = newColor,
                colors = persistentListOf(0xFF0000, 0x00FF00, 0x0000FF),
            ),
        )
        val repository = FakeAccountProfileRepository(
            initialAccountProfile = accountProfile,
        )
        val testSubject = UpdateGeneralSettings(repository)

        // Act
        settings.forEach { setting ->
            testSubject(accountId, setting)
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
        val setting = SettingValue.Text(
            id = GeneralPreference.NAME.generateId(accountId),
            title = { "Name" },
            description = { "Account name" },
            icon = { null },
            value = "Updated Account Name",
        )
        val repository = FakeAccountProfileRepository()
        val testSubject = UpdateGeneralSettings(repository)

        // Act
        val result = testSubject(accountId, setting)

        // Assert
        assertThat(result).isInstanceOf(Outcome.Failure::class)
        assertThat((result as Outcome.Failure).error).isInstanceOf(SettingsError.NotFound::class)
    }
}
