package net.thunderbird.core.ui.compose.preference.ui.components.list

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes
import net.thunderbird.core.ui.compose.preference.ui.fake.FakePreferenceData

@Composable
@Preview(showBackground = true)
internal fun Preview_Switch_On_Enabled() {
    PreviewWithThemes {
        PreferenceItemSwitchView(
            preference = FakePreferenceData.switchPreference,
            onPreferenceChange = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun Preview_Switch_Off_Enabled() {
    PreviewWithThemes {
        PreferenceItemSwitchView(
            preference = FakePreferenceData.switchPreference.copy(value = false),
            onPreferenceChange = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun Preview_Switch_On_Disabled() {
    PreviewWithThemes {
        PreferenceItemSwitchView(
            preference = FakePreferenceData.switchPreference.copy(enabled = false),
            onPreferenceChange = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun Preview_Switch_Off_Disabled() {
    PreviewWithThemes {
        PreferenceItemSwitchView(
            preference = FakePreferenceData.switchPreference.copy(value = false, enabled = false),
            onPreferenceChange = {},
        )
    }
}
