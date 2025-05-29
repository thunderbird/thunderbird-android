package net.thunderbird.core.ui.compose.preference.ui.components.dialog

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithTheme
import net.thunderbird.core.ui.compose.preference.ui.fake.FakePreferenceData

@Composable
@Preview(showBackground = true)
internal fun PreferenceDialogSingleChoiceCompactViewPreview() {
    PreviewWithTheme {
        PreferenceDialogSingleChoiceCompactView(
            preference = FakePreferenceData.singleChoiceCompactPreference,
            onConfirmClick = {},
            onDismissClick = {},
            onDismissRequest = {},
        )
    }
}
