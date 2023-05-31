package app.k9mail.feature.account.setup.ui.common

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import app.k9mail.core.ui.compose.theme.MainTheme

@Composable
internal fun defaultHeadlineItemPadding() = PaddingValues(
    start = MainTheme.spacings.quadruple,
    top = MainTheme.spacings.triple,
    end = MainTheme.spacings.quadruple,
    bottom = MainTheme.spacings.default,
)

@Composable
internal fun defaultItemPadding() = PaddingValues(
    horizontal = MainTheme.spacings.quadruple,
    vertical = MainTheme.spacings.zero,
)
