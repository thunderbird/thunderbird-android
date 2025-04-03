package net.thunderbird.core.ui.compose.preference.ui.components.dialog

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithTheme
import net.thunderbird.core.ui.compose.preference.ui.fake.FakePreferenceData

@Composable
@Preview(showBackground = true)
internal fun PreferenceDialogColorViewPreview() {
    PreviewWithTheme {
        PreferenceDialogColorView(
            preference = FakePreferenceData.colorPreference,
            onConfirmClick = {},
            onDismissClick = {},
            onDismissRequest = {},
        )
    }
}
