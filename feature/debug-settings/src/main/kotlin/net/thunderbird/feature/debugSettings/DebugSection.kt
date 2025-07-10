package net.thunderbird.feature.debugSettings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.designsystem.atom.divider.HorizontalDivider
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleLarge
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleMedium
import app.k9mail.core.ui.compose.theme2.MainTheme

@Composable
internal fun DebugSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    DebugSection(
        title = { TextTitleLarge(title) },
        modifier = modifier,
        content = content,
    )
}

@Composable
internal fun DebugSubSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    DebugSection(
        title = { TextTitleMedium(title) },
        modifier = modifier,
        content = content,
    )
}

@Composable
internal fun DebugSection(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier) {
        title()
        HorizontalDivider(modifier = Modifier.padding(vertical = MainTheme.spacings.double))
        content()
    }
}
