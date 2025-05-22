package net.thunderbird.core.ui.compose.preference.ui.components.list

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes
import net.thunderbird.core.ui.compose.preference.ui.fake.FakePreferenceData

@Composable
@Preview(showBackground = true)
internal fun PreferenceItemButtonSegmentedSingleChoiceViewPreview() {
    PreviewWithThemes {
        PreferenceItemButtonSegmentedSingleChoiceView(
            preference = FakePreferenceData.singleChoicePreference.copy(description = { null }),
            onPreferenceChange = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun PreferenceItemButtonSegmentedSingleChoiceViewWithDescriptionPreview() {
    PreviewWithThemes {
        PreferenceItemButtonSegmentedSingleChoiceView(
            preference = FakePreferenceData.singleChoicePreference,
            onPreferenceChange = {},
        )
    }
}
