package net.thunderbird.feature.account.settings.impl.ui.general

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import net.thunderbird.core.common.resources.StringsResourceManager
import net.thunderbird.core.featureflag.FeatureFlagProvider
import net.thunderbird.core.outcome.fold
import net.thunderbird.core.ui.setting.Setting
import net.thunderbird.core.ui.setting.SettingDecoration
import net.thunderbird.core.ui.setting.SettingValue
import net.thunderbird.core.ui.setting.Settings
import net.thunderbird.feature.account.avatar.Avatar
import net.thunderbird.feature.account.avatar.AvatarIcon
import net.thunderbird.feature.account.avatar.AvatarIconCatalog
import net.thunderbird.feature.account.avatar.AvatarMonogramCreator
import net.thunderbird.feature.account.settings.AccountSettingsFeatureFlags
import net.thunderbird.feature.account.settings.R
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.ValidateAccountNameError
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.ValidateMonogramError
import net.thunderbird.feature.account.settings.impl.ui.general.GeneralSettingsContract.Event
import net.thunderbird.feature.account.settings.impl.ui.general.GeneralSettingsContract.State
import net.thunderbird.feature.account.settings.impl.ui.general.components.AvatarImageSelection
import net.thunderbird.feature.account.settings.impl.ui.general.components.GeneralSettingsProfileView

/**
 * Builds the General Settings from [State].
 */
internal class GeneralSettingsBuilder(
    private val resources: StringsResourceManager,
    private val accountColors: ImmutableList<Int>,
    private val monogramCreator: AvatarMonogramCreator,
    private val validator: GeneralSettingsContract.Validator,
    private val featureFlagProvider: FeatureFlagProvider,
    private val iconCatalog: AvatarIconCatalog<AvatarIcon<ImageVector>>,
) : GeneralSettingsContract.SettingsBuilder {

    override fun build(
        state: State,
        onEvent: (Event) -> Unit,
    ): Settings {
        val settings = mutableListOf<Setting>()

        settings += profile(
            name = state.name.value,
            color = Color(state.color.value ?: 0),
            avatar = state.avatar ?: Avatar.Icon(name = iconCatalog.defaultIcon.id),
        )

        if (featureFlagProvider.provide(AccountSettingsFeatureFlags.EnableAvatarCustomization).isEnabled()) {
            settings += avatar(
                name = state.name.value,
                avatar = state.avatar,
            )

            when (val avatar = state.avatar) {
                is Avatar.Monogram -> settings += avatarMonogram(monogram = avatar.value)
                is Avatar.Image -> settings += avatarImage(
                    onSelectImageClick = { onEvent(Event.OnSelectAvatarImageClick) },
                )

                is Avatar.Icon -> settings += avatarIcon(
                    icon = avatar,
                    color = Color(state.color.value ?: 0),
                )

                null -> Unit
            }
        }

        settings += name(name = state.name.value)
        settings += color(color = state.color.value ?: 0)

        return settings.toImmutableList()
    }

    private fun profile(
        name: String,
        color: Color,
        avatar: Avatar,
    ): Setting = SettingDecoration.Custom(
        id = GeneralSettingId.PROFILE,
    ) { modifier ->
        GeneralSettingsProfileView(
            name = name,
            email = null,
            color = color,
            avatar = avatar,
            modifier = modifier,
        )
    }

    private fun avatar(
        name: String,
        avatar: Avatar?,
    ): Setting {
        val options = avatarOptions(avatar = avatar, name = name)
        val selected = selectAvatarOption(avatar = avatar, options = options)
        return SettingValue.SegmentedButton(
            id = GeneralSettingId.AVATAR_OPTIONS,
            title = { resources.stringResource(R.string.account_settings_general_avatar_title) },
            description = { resources.stringResource(R.string.account_settings_general_avatar_description) },
            value = selected,
            options = options,
        )
    }

    private fun name(
        name: String,
    ): Setting = SettingValue.Text(
        id = GeneralSettingId.NAME,
        title = { resources.stringResource(R.string.account_settings_general_name_title) },
        description = { resources.stringResource(R.string.account_settings_general_name_description) },
        icon = { null },
        value = name,
        validate = {
            validator.validateName(it).fold(
                onSuccess = { null },
                onFailure = { failure ->
                    when (failure) {
                        is ValidateAccountNameError.EmptyName -> resources.stringResource(
                            R.string.account_settings_general_name_error_empty,
                        )

                        is ValidateAccountNameError.TooLongName -> resources.stringResource(
                            R.string.account_settings_general_name_error_too_long,
                        )
                    }
                },
            )
        },
    )

    private fun color(
        color: Int,
    ): Setting = SettingValue.Color(
        id = GeneralSettingId.COLOR,
        title = { resources.stringResource(R.string.account_settings_general_color_title) },
        description = { resources.stringResource(R.string.account_settings_general_color_description) },
        icon = { null },
        value = color,
        colors = accountColors,
    )

    private fun avatarMonogram(
        monogram: String,
    ): Setting = SettingValue.Text(
        id = GeneralSettingId.AVATAR_MONOGRAM,
        title = { resources.stringResource(R.string.account_settings_general_avatar_monogram_title) },
        description = { resources.stringResource(R.string.account_settings_general_avatar_monogram_description) },
        icon = { null },
        value = monogram,
        transform = { it.uppercase() },
        validate = {
            validator.validateMonogram(it).fold(
                onSuccess = { null },
                onFailure = { failure ->
                    when (failure) {
                        is ValidateMonogramError.EmptyMonogram -> resources.stringResource(
                            R.string.account_settings_general_avatar_monogram_error_empty,
                        )

                        is ValidateMonogramError.TooLongMonogram -> resources.stringResource(
                            R.string.account_settings_general_avatar_monogram_error_too_long,
                        )
                    }
                },
            )
        },
    )

    private fun avatarImage(
        onSelectImageClick: () -> Unit,
    ): Setting = SettingDecoration.Custom(
        id = GeneralSettingId.AVATAR_IMAGE,
    ) { modifier ->
        AvatarImageSelection(
            onSelectImageClick = onSelectImageClick,
        )
    }

    private fun avatarIcon(
        icon: Avatar.Icon,
        color: Color,
    ): Setting {
        val selectedIconId = if (iconCatalog.contains(icon.name)) icon.name else iconCatalog.defaultIcon.id
        val icons = iconCatalog.all().map { icon ->
            SettingValue.IconList.IconOption(
                id = icon.id,
                icon = { icon.icon },
            )
        }
        val selected = icons.firstOrNull { it.id == selectedIconId }
            ?: icons.first { it.id == iconCatalog.defaultIcon.id }
        return SettingValue.IconList(
            id = GeneralSettingId.AVATAR_ICON,
            title = { resources.stringResource(R.string.account_settings_general_avatar_icon_title) },
            description = { resources.stringResource(R.string.account_settings_general_avatar_icon_description) },
            color = color,
            value = selected,
            icons = icons.toImmutableList(),
        )
    }

    private fun avatarOptions(
        avatar: Avatar?,
        name: String,
    ): ImmutableList<SettingValue.SegmentedButton.SegmentedButtonOption<Avatar>> =
        persistentListOf(
            SettingValue.SegmentedButton.SegmentedButtonOption(
                id = AVATAR_MONOGRAM_ID,
                title = { resources.stringResource(R.string.account_settings_general_avatar_option_monogram) },
                value = (avatar as? Avatar.Monogram) ?: Avatar.Monogram(
                    value = monogramCreator.create(name, null),
                ),
            ),
            SettingValue.SegmentedButton.SegmentedButtonOption(
                id = AVATAR_IMAGE_ID,
                title = { resources.stringResource(R.string.account_settings_general_avatar_option_image) },
                value = (avatar as? Avatar.Image) ?: Avatar.Image(
                    uri = "avatar_placeholder_uri",
                ),
            ),
            SettingValue.SegmentedButton.SegmentedButtonOption(
                id = AVATAR_ICON_ID,
                title = { resources.stringResource(R.string.account_settings_general_avatar_option_icon) },
                value = (avatar as? Avatar.Icon) ?: Avatar.Icon(
                    name = iconCatalog.defaultIcon.id,
                ),
            ),
        )

    private fun selectAvatarOption(
        avatar: Avatar?,
        options: List<SettingValue.SegmentedButton.SegmentedButtonOption<Avatar>>,
    ): SettingValue.SegmentedButton.SegmentedButtonOption<Avatar> = when (avatar) {
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
