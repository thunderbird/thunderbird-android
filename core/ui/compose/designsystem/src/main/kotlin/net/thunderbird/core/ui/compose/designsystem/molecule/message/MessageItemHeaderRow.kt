package net.thunderbird.core.ui.compose.designsystem.molecule.message

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal fun HeaderRow(
    modifier: Modifier = Modifier,
    headerRowContent: @Composable ((RowScope) -> Unit),
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier.defaultMinSize(
            minHeight = AccountIndicatorIcon.ACCOUNT_INDICATOR_DEFAULT_HEIGHT,
        ),
    ) {
        headerRowContent(this)
    }
}

@Composable
internal fun HeaderRowCompact(
    modifier: Modifier = Modifier,
    headerRowContent: @Composable ((RowScope) -> Unit),
) {
    FlowRow(
        verticalArrangement = Arrangement.Center,
        horizontalArrangement = Arrangement.Start,
        maxLines = 2,
        modifier = modifier.defaultMinSize(
            minHeight = AccountIndicatorIcon.ACCOUNT_INDICATOR_DEFAULT_HEIGHT,
        ),
    ) {
        headerRowContent(this)
    }
}
