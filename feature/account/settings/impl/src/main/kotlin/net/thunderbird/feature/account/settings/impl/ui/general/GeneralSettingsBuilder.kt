package net.thunderbird.feature.account.settings.impl.ui.general

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import net.thunderbird.core.featureflag.FeatureFlagProvider
import net.thunderbird.core.ui.setting.Setting
import net.thunderbird.core.ui.setting.SettingDecoration
import net.thunderbird.core.ui.setting.SettingValue
import net.thunderbird.core.ui.setting.Settings
import net.thunderbird.feature.account.avatar.Avatar
import net.thunderbird.feature.account.avatar.AvatarMonogramCreator
import net.thunderbird.feature.account.settings.AccountSettingsFeatureFlags
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract
import net.thunderbird.feature.account.settings.impl.ui.general.GeneralSettingsContract.State

/**
 * Builds the General Settings from [State].
 */
internal class GeneralSettingsBuilder(
    private val resources: AccountSettingsDomainContract.ResourceProvider.GeneralResourceProvider,
    private val provider: FeatureFlagProvider,
    private val monogramCreator: AvatarMonogramCreator,
) : GeneralSettingsContract.SettingsBuilder {

    override fun build(state: State): Settings {
        val settings = mutableListOf<Setting>()

        settings += profile(
            name = state.name.value,
            color = state.color.value ?: 0,
            avatar = state.avatar,
        )

        if (provider.provide(AccountSettingsFeatureFlags.EnableAvatarCustomization).isEnabled()) {
            settings += avatar(
                name = state.name.value,
                avatar = state.avatar,
            )

            val monogramValue = (state.avatar as? Avatar.Monogram)?.value
            if (monogramValue != null) {
                settings += monogram(monogram = monogramValue)
            }
        }

        settings += name(name = state.name.value)
        settings += color(color = state.color.value ?: 0)

        return settings.toImmutableList()
    }

    private fun profile(
        name: String,
        color: Int,
        avatar: Avatar?,
    ): Setting = SettingDecoration.Custom(
        id = GeneralSettingId.PROFILE,
    ) { modifier ->
        val composable = resources.profileUi(name = name, color = color, avatar = avatar)
        composable(modifier)
    }

    private fun avatar(
        name: String,
        avatar: Avatar?,
    ): Setting {
        val options = avatarOptions(avatar = avatar, name = name)
        val selected = selectAvatarOption(avatar = avatar, options = options)
        return SettingValue.CompactSelectSingleOption(
            id = GeneralSettingId.AVATAR,
            title = resources.profileIndicatorTitle,
            value = selected,
            options = options,
        )
    }

    private fun name(
        name: String,
    ): Setting = SettingValue.Text(
        id = GeneralSettingId.NAME,
        title = resources.nameTitle,
        description = resources.nameDescription,
        icon = resources.nameIcon,
        value = name,
    )

    private fun color(
        color: Int,
    ): Setting = SettingValue.Color(
        id = GeneralSettingId.COLOR,
        title = resources.colorTitle,
        description = resources.colorDescription,
        icon = resources.colorIcon,
        value = color,
        colors = resources.colors,
    )

    private fun monogram(
        monogram: String,
    ): Setting = SettingValue.Text(
        id = GeneralSettingId.AVATAR_MONOGRAM,
        title = resources.profileIndicatorMonogram,
        description = { null },
        icon = { null },
        value = monogram,
    )

    private fun avatarOptions(
        avatar: Avatar?,
        name: String,
    ): ImmutableList<SettingValue.CompactSelectSingleOption.CompactOption<Avatar>> =
        persistentListOf(
            SettingValue.CompactSelectSingleOption.CompactOption(
                id = AVATAR_MONOGRAM_ID,
                title = resources.profileIndicatorMonogram,
                value = (avatar as? Avatar.Monogram) ?: Avatar.Monogram(
                    value = monogramCreator.create(name, null),
                ),
            ),
            SettingValue.CompactSelectSingleOption.CompactOption(
                id = AVATAR_IMAGE_ID,
                title = resources.profileIndicatorImage,
                value = (avatar as? Avatar.Image) ?: Avatar.Image(
                    uri = "avatar_placeholder_uri",
                ),
            ),
            SettingValue.CompactSelectSingleOption.CompactOption(
                id = AVATAR_ICON_ID,
                title = resources.profileIndicatorIcon,
                value = (avatar as? Avatar.Icon) ?: Avatar.Icon(
                    name = "user",
                ),
            ),
        )

    private fun selectAvatarOption(
        avatar: Avatar?,
        options: List<SettingValue.CompactSelectSingleOption.CompactOption<Avatar>>,
    ): SettingValue.CompactSelectSingleOption.CompactOption<Avatar> = when (avatar) {
        is Avatar.Monogram, null -> options.first { it.id == AVATAR_MONOGRAM_ID }
        is Avatar.Image -> options.first { it.id == AVATAR_IMAGE_ID }
        is Avatar.Icon -> options.first { it.id == AVATAR_ICON_ID }
    }

    private companion object {
        const val AVATAR_MONOGRAM_ID = "avatar_monogram"
        const val AVATAR_IMAGE_ID = "avatar_image"
        const val AVATAR_ICON_ID = "avatar_icon"
    }
}
