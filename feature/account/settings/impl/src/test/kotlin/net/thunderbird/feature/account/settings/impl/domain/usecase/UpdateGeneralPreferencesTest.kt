package net.thunderbird.feature.account.settings.impl.domain.usecase

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import kotlin.test.Test
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.core.ui.compose.preference.api.PreferenceSetting
import net.thunderbird.feature.account.api.AccountId
import net.thunderbird.feature.account.api.profile.AccountProfile
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.SettingsError
import net.thunderbird.feature.account.settings.impl.domain.entity.GeneralPreference
import net.thunderbird.feature.account.settings.impl.domain.entity.generateId

class UpdateGeneralPreferencesTest {

    @Test
    fun `should update account profile`() = runTest {
        // Arrange
        val accountId = AccountId.create()
        val accountProfile = AccountProfile(
            accountId = accountId,
            name = "Test Account",
            color = 0xFF0000,
        )
        val newName = "Updated Account Name"
        val preference = PreferenceSetting.Text(
            id = GeneralPreference.NAME.generateId(accountId),
            title = { "Name" },
            description = { "Account name" },
            icon = { null },
            value = newName,
        )
        val repository = FakeAccountProfileRepository(
            initialAccountProfile = accountProfile,
        )
        val testSubject = UpdateGeneralPreferences(repository)

        // Act
        val result = testSubject(accountId, preference)

        // Assert
        assertThat(result).isInstanceOf(Outcome.Success::class)
        assertThat(repository.getById(accountId).firstOrNull()).isEqualTo(
            accountProfile.copy(name = newName),
        )
    }

    @Test
    fun `should emit NotFound when account profile not found`() = runTest {
        // Arrange
        val accountId = AccountId.create()
        val preference = PreferenceSetting.Text(
            id = GeneralPreference.NAME.generateId(accountId),
            title = { "Name" },
            description = { "Account name" },
            icon = { null },
            value = "Updated Account Name",
        )
        val repository = FakeAccountProfileRepository()
        val testSubject = UpdateGeneralPreferences(repository)

        // Act
        val result = testSubject(accountId, preference)

        // Assert
        assertThat(result).isInstanceOf(Outcome.Failure::class)
        assertThat((result as Outcome.Failure).error).isInstanceOf(SettingsError.NotFound::class)
    }
}
