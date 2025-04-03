package net.thunderbird.feature.account.settings.impl.ui.general.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes

@Composable
@Preview(showBackground = true)
internal fun GeneralSettingsProfileViewPreview() {
    PreviewWithThemes {
        GeneralSettingsProfileView(
            name = "Name",
            email = "demo@example.com",
            color = Color.Green,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun GeneralSettingsProfileViewWithLongTextPreview() {
    PreviewWithThemes {
        GeneralSettingsProfileView(
            name = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut " +
                "labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris " +
                "nisi ut aliquip ex ea commodo consequat.",
            email = "verylongemailaddress@exampledomainwithaverylongname.com",
            color = Color.Green,
        )
    }
}
