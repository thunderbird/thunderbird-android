package net.thunderbird.ui.catalog.ui.page.common.list

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import net.thunderbird.core.ui.compose.theme2.MainTheme

@Composable
fun defaultItemPadding(): PaddingValues = PaddingValues(
    horizontal = MainTheme.spacings.double,
)
