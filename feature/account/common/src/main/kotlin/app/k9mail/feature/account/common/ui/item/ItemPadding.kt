package app.k9mail.feature.account.common.ui.item

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import net.thunderbird.components.ui.bolt.theme.MainTheme

@Composable
fun defaultHeadlineItemPadding() = PaddingValues(
    start = MainTheme.spacings.quadruple,
    top = MainTheme.spacings.triple,
    end = MainTheme.spacings.quadruple,
    bottom = MainTheme.spacings.default,
)

@Composable
fun defaultItemPadding() = PaddingValues(
    horizontal = MainTheme.spacings.quadruple,
    vertical = MainTheme.spacings.zero,
)
