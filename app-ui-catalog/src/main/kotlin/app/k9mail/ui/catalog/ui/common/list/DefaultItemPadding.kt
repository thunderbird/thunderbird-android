package app.k9mail.ui.catalog.ui.common.list

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import app.k9mail.core.ui.compose.theme2.MainTheme

@Composable
fun defaultItemPadding(): PaddingValues = PaddingValues(
    horizontal = MainTheme.spacings.double,
)
