package net.thunderbird.core.ui.setting.dialog.ui.components.list.decoration

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.thunderbird.components.ui.bolt.atom.text.TextTitleMedium
import net.thunderbird.components.ui.bolt.theme.MainTheme
import net.thunderbird.core.ui.setting.SettingDecoration

@Composable
internal fun SectionHeaderItem(
    setting: SettingDecoration.SectionHeader,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(MainTheme.spacings.double)) {
        TextTitleMedium(
            text = setting.title(),
            color = setting.color(),
        )
    }
}
