package net.thunderbird.feature.account.settings.impl.ui.general

import android.content.Context
import androidx.compose.ui.graphics.vector.ImageVector
import net.thunderbird.feature.account.settings.R
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.ResourceProvider

internal class GeneralResourceProvider(
    private val context: Context,
) : ResourceProvider.GeneralResourceProvider {

    override val nameTitle: () -> String = {
        context.getString(R.string.account_settings_general_name_title)
    }
    override val nameDescription: () -> String? = { null }
    override val nameIcon: () -> ImageVector? = { null }
}
