package app.k9mail.core.ui.compose.designsystem

import androidx.compose.ui.tooling.preview.AndroidUiModes.UI_MODE_NIGHT_YES
import androidx.compose.ui.tooling.preview.AndroidUiModes.UI_MODE_TYPE_NORMAL
import androidx.compose.ui.tooling.preview.Preview

@Preview(name = "Light", device = "spec:width=673dp,height=841dp,orientation=landscape")
@Preview(
    name = "Dark",
    uiMode = UI_MODE_NIGHT_YES or UI_MODE_TYPE_NORMAL,
    device = "spec:width=673dp,height=841dp,orientation=landscape",
)
annotation class PreviewLightDarkLandscape
