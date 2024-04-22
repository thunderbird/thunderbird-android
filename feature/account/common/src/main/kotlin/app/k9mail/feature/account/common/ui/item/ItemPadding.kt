package app.k9mail.feature.account.common.ui.item

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import app.k9mail.core.ui.compose.theme2.MainTheme

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
