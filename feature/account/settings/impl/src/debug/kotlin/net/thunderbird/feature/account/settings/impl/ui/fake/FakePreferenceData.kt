package net.thunderbird.feature.account.settings.impl.ui.fake

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.designsystem.atom.card.CardElevated
import app.k9mail.core.ui.compose.designsystem.atom.icon.Icons
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyLarge
import app.k9mail.core.ui.compose.theme2.MainTheme
import kotlinx.collections.immutable.persistentListOf
import net.thunderbird.core.ui.compose.preference.api.PreferenceDisplay
import net.thunderbird.core.ui.compose.preference.api.PreferenceSetting

object FakePreferenceData {

    val textPreference = PreferenceSetting.Text(
        id = "text",
        icon = { Icons.Outlined.Info },
        title = { "Title" },
        description = { "Description" },
        value = "Value",
    )

    val colorPreference = PreferenceSetting.Color(
        id = "color",
        icon = { Icons.Outlined.Info },
        title = { "Title" },
        description = { "Description" },
        value = 0xFFFF0000.toInt(),
        colors = listOf(
            0xFFFF0000.toInt(),
            0xFF00FF00.toInt(),
            0xFF0000FF.toInt(),
        ),
    )

    val customPreference = PreferenceDisplay.Custom(
        id = "custom",
        customUi = { modifier ->
            CardElevated(
                modifier = modifier.fillMaxWidth()
                    .padding(MainTheme.spacings.double),
            ) {
                TextBodyLarge(
                    text = "Custom UI",
                    modifier = Modifier.padding(MainTheme.spacings.default)
                        .fillMaxWidth(),
                )
            }
        },
    )

    val preferences = persistentListOf(
        textPreference,
        colorPreference,
        customPreference,
    )
}
