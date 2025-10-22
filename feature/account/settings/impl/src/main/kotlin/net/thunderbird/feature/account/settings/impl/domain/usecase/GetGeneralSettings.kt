package net.thunderbird.feature.account.settings.impl.domain.usecase

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.thunderbird.core.featureflag.FeatureFlagProvider
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.core.ui.setting.Setting
import net.thunderbird.core.ui.setting.SettingDecoration
import net.thunderbird.core.ui.setting.SettingValue
import net.thunderbird.core.ui.setting.SettingValue.CompactSelectSingleOption.CompactOption
import net.thunderbird.core.ui.setting.Settings
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.avatar.AvatarMonogramCreator
import net.thunderbird.feature.account.profile.AccountAvatar
import net.thunderbird.feature.account.profile.AccountProfile
import net.thunderbird.feature.account.profile.AccountProfileRepository
import net.thunderbird.feature.account.settings.AccountSettingsFeatureFlags
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.ResourceProvider
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.SettingsError
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.UseCase
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsOutcome
import net.thunderbird.feature.account.settings.impl.domain.entity.GeneralPreference
import net.thunderbird.feature.account.settings.impl.domain.entity.generateId

internal class GetGeneralSettings(
    private val repository: AccountProfileRepository,
    private val resourceProvider: ResourceProvider.GeneralResourceProvider,
    private val monogramCreator: AvatarMonogramCreator,
    private val featureFlagProvider: FeatureFlagProvider,
) : UseCase.GetGeneralSettings {
    override fun invoke(accountId: AccountId): Flow<AccountSettingsOutcome> {
        return repository.getById(accountId).map { profile ->
            if (profile != null) {
                Outcome.success(generateSettings(accountId, profile))
            } else {
                Outcome.failure(
                    SettingsError.NotFound(
                        message = "Account profile not found for accountId: ${accountId.asRaw()}",
                    ),
                )
            }
        }
    }

    private fun generateSettings(accountId: AccountId, profile: AccountProfile): Settings {
        val settings = mutableListOf<Setting>()
        settings += SettingDecoration.Custom(
            id = GeneralPreference.PROFILE.generateId(accountId),
            customUi = resourceProvider.profileUi(
                name = profile.name,
                color = profile.color,
            ),
        )
        if (featureFlagProvider.provide(AccountSettingsFeatureFlags.EnableAvatarCustomization).isEnabled()) {
            val profileIndicatorOptions = generateProfileIndicatorOptions(profile.id, profile.avatar, profile.name)
            settings += SettingValue.CompactSelectSingleOption(
                id = GeneralPreference.PROFILE_INDICATOR.generateId(accountId),
                title = resourceProvider.profileIndicatorTitle,
                value = selectProfileIndicatorOption(profile, profileIndicatorOptions),
                options = profileIndicatorOptions,
            )
        }
        settings += SettingValue.Text(
            id = GeneralPreference.NAME.generateId(accountId),
            title = resourceProvider.nameTitle,
            description = resourceProvider.nameDescription,
            icon = resourceProvider.nameIcon,
            value = profile.name,
        )
        settings += SettingValue.Color(
            id = GeneralPreference.COLOR.generateId(accountId),
            title = resourceProvider.colorTitle,
            description = resourceProvider.colorDescription,
            icon = resourceProvider.colorIcon,
            value = profile.color,
            colors = resourceProvider.colors,
        )
        return settings.toImmutableList()
    }

    private fun selectProfileIndicatorOption(
        profile: AccountProfile,
        options: List<CompactOption<AccountAvatar>>,
    ): CompactOption<AccountAvatar> {
        return when (profile.avatar) {
            is AccountAvatar.Monogram -> options.first {
                it.id == generateMonogramId(profile.id.asRaw())
            }

            is AccountAvatar.Image -> options.first {
                it.id == generateImageId(profile.id.asRaw())
            }

            is AccountAvatar.Icon -> options.first {
                it.id == generateIconId(profile.id.asRaw())
            }
        }
    }

    private fun generateProfileIndicatorOptions(
        accountId: AccountId,
        avatar: AccountAvatar,
        name: String,
    ): ImmutableList<CompactOption<AccountAvatar>> {
        return persistentListOf(
            CompactOption(
                id = generateMonogramId(accountId.asRaw()),
                title = resourceProvider.profileIndicatorMonogram,
                value = avatar as? AccountAvatar.Monogram ?: AccountAvatar.Monogram(
                    value = monogramCreator.create(name, null),
                ),
            ),
            CompactOption(
                id = generateImageId(accountId.asRaw()),
                title = resourceProvider.profileIndicatorImage,
                value = avatar as? AccountAvatar.Image ?: AccountAvatar.Image(
                    uri = "avatar_placeholder_uri",
                ),
            ),
            CompactOption(
                id = generateIconId(accountId.asRaw()),
                title = resourceProvider.profileIndicatorIcon,
                value = avatar as? AccountAvatar.Icon ?: AccountAvatar.Icon(
                    name = "user",
                ),
            ),
        )
    }

    private fun generateMonogramId(accountId: String): String =
        "$accountId-profile-indicator-monogram"

    private fun generateImageId(accountId: String): String =
        "$accountId-profile-indicator-image"

    private fun generateIconId(accountId: String): String =
        "$accountId-profile-indicator-icon"
}
