package net.thunderbird.feature.account.settings.impl.domain.usecase

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.featureflag.FeatureFlagResult
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.core.ui.setting.SettingDecoration
import net.thunderbird.core.ui.setting.SettingValue
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.account.profile.AccountAvatar
import net.thunderbird.feature.account.profile.AccountProfile
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.ResourceProvider
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.SettingsError
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.UseCase
import net.thunderbird.feature.account.settings.impl.domain.entity.GeneralPreference
import net.thunderbird.feature.account.settings.impl.domain.entity.generateId

internal class GetGeneralSettingsTest {

    @Test
    fun `should emit settings when account profile present`() = runTest {
        // Arrange
        val accountId = AccountIdFactory.create()
        val accountProfile = AccountProfile(
            id = accountId,
            name = "Test Account",
            color = 0xFF0000,
            avatar = AccountAvatar.Icon(name = "star"),
        )
        val resourceProvider = FakeGeneralResourceProvider()
        val testSubject = createTestSubject(accountProfile)

        // Act & Assert
        testSubject(accountId).test {
            val outcome = awaitItem()
            assertThat(outcome).isInstanceOf(Outcome.Success::class)

            val success = outcome as Outcome.Success
            val settings = success.data

            assertThat(settings[0]).isEqualTo(
                SettingDecoration.Custom(
                    id = "${accountId.asRaw()}-general-profile",
                    customUi = resourceProvider.profileUi(
                        name = accountProfile.name,
                        color = accountProfile.color,
                    ),
                ),
            )

            val profileIndicator = settings[1] as SettingValue.CompactSelectSingleOption<*>
            assertThat(profileIndicator.id).isEqualTo(GeneralPreference.PROFILE_INDICATOR.generateId(accountId))
            assertThat(profileIndicator.options.size).isEqualTo(3)

            assertThat(settings[2]).isEqualTo(
                SettingValue.Text(
                    id = "${accountId.asRaw()}-general-name",
                    title = resourceProvider.nameTitle,
                    description = resourceProvider.nameDescription,
                    icon = resourceProvider.nameIcon,
                    value = accountProfile.name,
                ),
            )

            assertThat(settings[2]).isEqualTo(
                SettingValue.Text(
                    id = "${accountId.asRaw()}-general-name",
                    title = resourceProvider.nameTitle,
                    description = resourceProvider.nameDescription,
                    icon = resourceProvider.nameIcon,
                    value = accountProfile.name,
                ),
            )

            assertThat(settings[3]).isEqualTo(
                SettingValue.Color(
                    id = "${accountId.asRaw()}-general-color",
                    title = resourceProvider.colorTitle,
                    description = resourceProvider.colorDescription,
                    icon = resourceProvider.colorIcon,
                    value = accountProfile.color,
                    colors = resourceProvider.colors,
                ),
            )
        }
    }

    @Test
    fun `should emit NotFound when account profile not found`() = runTest {
        // Arrange
        val accountId = AccountIdFactory.create()
        val testSubject = createTestSubject()

        // Act & Assert
        testSubject(accountId).test {
            assertThat(awaitItem()).isEqualTo(
                Outcome.failure(
                    SettingsError.NotFound(
                        message = "Account profile not found for accountId: ${accountId.asRaw()}",
                    ),
                ),
            )
        }
    }

    @Test
    fun `should omit profile indicator when flag disabled`() = runTest {
        // Arrange
        val accountId = AccountIdFactory.create()
        val accountProfile = AccountProfile(
            id = accountId,
            name = "Test Account",
            color = 0xFF0000,
            avatar = AccountAvatar.Icon(name = "star"),
        )
        val resourceProvider = FakeGeneralResourceProvider()
        val testSubject = createTestSubject(accountProfile, resourceProvider, enableAvatarCustomization = false)

        // Act & Assert
        testSubject(accountId).test {
            val outcome = awaitItem()
            assertThat(outcome).isInstanceOf(Outcome.Success::class)

            val success = outcome as Outcome.Success
            val settings = success.data

            assertThat(settings.size).isEqualTo(3)
            // Ensure second item is Name (no indicator)
            assertThat(settings[1]).isEqualTo(
                SettingValue.Text(
                    id = GeneralPreference.NAME.generateId(accountId),
                    title = resourceProvider.nameTitle,
                    description = resourceProvider.nameDescription,
                    icon = resourceProvider.nameIcon,
                    value = accountProfile.name,
                ),
            )
        }
    }

    private fun createTestSubject(
        accountProfile: AccountProfile? = null,
        resourceProvider: ResourceProvider.GeneralResourceProvider = FakeGeneralResourceProvider(),
        enableAvatarCustomization: Boolean = true,
    ): UseCase.GetGeneralSettings {
        return GetGeneralSettings(
            repository = FakeAccountProfileRepository(accountProfile),
            resourceProvider = resourceProvider,
            monogramCreator = FakeMonogramCreator(),
            featureFlagProvider = {
                if (enableAvatarCustomization) FeatureFlagResult.Enabled else FeatureFlagResult.Disabled
            },
        )
    }
}
