package net.thunderbird.feature.debug.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.thunderbird.components.ui.bolt.atom.DividerHorizontal
import net.thunderbird.components.ui.bolt.atom.text.TextTitleLarge
import net.thunderbird.components.ui.bolt.atom.text.TextTitleMedium
import net.thunderbird.components.ui.bolt.theme.MainTheme

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
        DividerHorizontal(modifier = Modifier.padding(vertical = MainTheme.spacings.double))
        content()
    }
}
