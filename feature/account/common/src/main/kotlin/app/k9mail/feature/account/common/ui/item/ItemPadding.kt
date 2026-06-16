package app.k9mail.feature.account.common.ui.item

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import net.thunderbird.components.ui.bolt.theme.BoltTheme

@Composable
fun defaultHeadlineItemPadding() = PaddingValues(
    start = BoltTheme.spacings.quadruple,
    top = BoltTheme.spacings.triple,
    end = BoltTheme.spacings.quadruple,
    bottom = BoltTheme.spacings.default,
)

@Composable
fun defaultItemPadding() = PaddingValues(
    horizontal = BoltTheme.spacings.quadruple,
    vertical = BoltTheme.spacings.zero,
)
