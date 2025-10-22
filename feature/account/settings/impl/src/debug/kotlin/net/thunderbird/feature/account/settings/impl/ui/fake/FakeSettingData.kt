package net.thunderbird.feature.account.settings.impl.ui.fake

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import app.k9mail.core.ui.compose.designsystem.atom.card.CardElevated
import app.k9mail.core.ui.compose.designsystem.atom.icon.Icons
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyLarge
import app.k9mail.core.ui.compose.theme2.MainTheme
import kotlinx.collections.immutable.persistentListOf
import net.thunderbird.core.ui.setting.SettingDecoration
import net.thunderbird.core.ui.setting.SettingValue

object FakeSettingData {

    val text = SettingValue.Text(
        id = "text",
        icon = { Icons.Outlined.Info },
        title = { "Title" },
        description = { "Description" },
        value = "Value",
    )

    val color = SettingValue.Color(
        id = "color",
        icon = { Icons.Outlined.Info },
        title = { "Title" },
        description = { "Description" },
        value = 0xFFFF0000.toInt(),
        colors = persistentListOf(
            0xFFFF0000.toInt(),
            0xFF00FF00.toInt(),
            0xFF0000FF.toInt(),
        ),
    )

    val custom = SettingDecoration.Custom(
        id = "custom",
        customUi = { modifier ->
            CardElevated(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(MainTheme.spacings.double),
            ) {
                TextBodyLarge(
                    text = "Custom UI",
                    modifier = Modifier
                        .padding(MainTheme.spacings.default)
                        .fillMaxWidth(),
                )
            }
        },
    )

    val sectionDivider = SettingDecoration.SectionDivider(
        id = "section_divider",
    )

    val sectionHeader = SettingDecoration.SectionHeader(
        id = "section_header",
        title = { "Section Title" },
        color = { Color.Black },
    )

    val settings = persistentListOf(
        text,
        color,
        custom,
        sectionHeader,
        sectionDivider,
    )
}
