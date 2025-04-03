package net.thunderbird.feature.account.settings.impl.ui.general

import android.content.Context
import androidx.compose.ui.graphics.vector.ImageVector
import net.thunderbird.feature.account.settings.R
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.ResourceProvider
import app.k9mail.core.ui.legacy.theme2.common.R as ThunderbirdCommonR

internal class GeneralResourceProvider(
    private val context: Context,
) : ResourceProvider.GeneralResourceProvider {

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
    override val colors: List<Int> = context.resources.getIntArray(ThunderbirdCommonR.array.account_colors).toList()
}
