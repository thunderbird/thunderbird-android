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

    override val profileIndicatorTitle: () -> String = {
        context.getString(R.string.account_settings_general_profile_indicator_title)
    }

    override val profileIndicatorMonogram: () -> String = {
        context.getString(R.string.account_settings_general_profile_indicator_monogram)
    }

    override val profileIndicatorImage: () -> String = {
        context.getString(R.string.account_settings_general_profile_indicator_image)
    }

    override val profileIndicatorIcon: () -> String = {
        context.getString(R.string.account_settings_general_profile_indicator_icon)
    }

    override val nameTitle: () -> String = {
        context.getString(R.string.account_settings_general_name_title)
    }
    override val nameDescription: () -> String? = {
        context.getString(R.string.account_settings_general_name_description)
    }
    override val nameIcon: () -> ImageVector? = { null }

    override val colorTitle: () -> String = {
        context.getString(R.string.account_settings_general_color_title)
    }
    override val colorDescription: () -> String? = {
        context.getString(R.string.account_settings_general_color_description)
    }
    override val colorIcon: () -> ImageVector? = { null }
}
