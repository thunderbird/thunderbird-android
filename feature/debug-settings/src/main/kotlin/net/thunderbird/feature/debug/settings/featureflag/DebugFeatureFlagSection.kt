package net.thunderbird.feature.debug.settings.featureflag

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleLarge

@Composable
fun DebugFeatureFlagSection(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
    ) {
        TextTitleLarge("Feature Flags")
    }
}
