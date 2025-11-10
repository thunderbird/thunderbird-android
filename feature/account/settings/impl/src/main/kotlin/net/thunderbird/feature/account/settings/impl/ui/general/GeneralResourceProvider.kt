package net.thunderbird.feature.account.settings.impl.ui.general

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.collections.immutable.ImmutableList
import net.thunderbird.feature.account.avatar.Avatar
import net.thunderbird.feature.account.settings.R
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.ResourceProvider
import net.thunderbird.feature.account.settings.impl.ui.general.components.GeneralSettingsProfileView

internal class GeneralResourceProvider(
    private val context: Context,
    override val colors: ImmutableList<Int>,
) : ResourceProvider.GeneralResourceProvider {

    override fun profileUi(
        name: String,
        color: Int,
        avatar: Avatar?,
    ): @Composable ((Modifier) -> Unit) = { modifier ->
        GeneralSettingsProfileView(
            name = name,
            email = null,
            color = Color(color),
            avatar = avatar,
            modifier = modifier,
        )
    }

    override val avatarTitle: () -> String = {
        context.getString(R.string.account_settings_general_avatar_title)
    }

    override val avatarDescription: () -> String? = {
        context.getString(R.string.account_settings_general_avatar_description)
    }

    override val avatarOptionMonogram: () -> String = {
        context.getString(R.string.account_settings_general_avatar_option_monogram)
    }

    override val avatarOptionImage: () -> String = {
        context.getString(R.string.account_settings_general_avatar_option_image)
    }

    override val avatarOptionIcon: () -> String = {
        context.getString(R.string.account_settings_general_avatar_option_icon)
    }

    override val nameTitle: () -> String = {
        context.getString(R.string.account_settings_general_name_title)
    }
    override val nameDescription: () -> String? = {
        context.getString(R.string.account_settings_general_name_description)
    }
    override val nameIcon: () -> ImageVector? = { null }

    override val nameEmptyError: () -> String = {
        context.getString(R.string.account_settings_general_name_error_empty)
    }
    override val nameTooLongError: () -> String = {
        context.getString(R.string.account_settings_general_name_error_too_long)
    }
    override val monogramTitle: () -> String = {
        context.getString(R.string.account_settings_general_avatar_monogram_title)
    }
    override val monogramDescription: () -> String? = {
        context.getString(R.string.account_settings_general_avatar_monogram_description)
    }
    override val monogramEmptyError: () -> String = {
        context.getString(R.string.account_settings_general_avatar_monogram_error_empty)
    }
    override val monogramTooLongError: () -> String = {
        context.getString(R.string.account_settings_general_avatar_monogram_error_too_long)
    }

    override val colorTitle: () -> String = {
        context.getString(R.string.account_settings_general_color_title)
    }
    override val colorDescription: () -> String? = {
        context.getString(R.string.account_settings_general_color_description)
    }
    override val colorIcon: () -> ImageVector? = { null }
}
