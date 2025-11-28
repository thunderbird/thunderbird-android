package net.thunderbird.feature.account.settings.impl.ui.general.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonOutlined
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyMedium
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleMedium
import app.k9mail.core.ui.compose.theme2.MainTheme
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icons
import net.thunderbird.core.ui.setting.component.list.item.SettingItemLayout
import net.thunderbird.feature.account.settings.R

@Composable
internal fun AvatarImageSelection(
    onSelectImageClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingItemLayout(
        onClick = null,
        icon = null,
        modifier = modifier,
    ) {
        TextTitleMedium(text = stringResource(R.string.account_settings_general_avatar_image_title))
        TextBodyMedium(text = stringResource(R.string.account_settings_general_avatar_image_description))
        Spacer(modifier = Modifier.height(MainTheme.spacings.default))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MainTheme.spacings.default, Alignment.CenterHorizontally),
            modifier = Modifier.fillMaxWidth(),
        ) {
            ButtonOutlined(
                text = stringResource(R.string.account_settings_general_avatar_image_select),
                icon = Icons.Outlined.Upload,
                onClick = onSelectImageClick,
            )
        }
    }
}
