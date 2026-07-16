package net.thunderbird.components.ui.catalog.ui.page.common.list

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import net.thunderbird.components.ui.bolt.theme.BoltTheme

@Composable
fun defaultItemPadding(): PaddingValues = PaddingValues(
    horizontal = BoltTheme.spacings.double,
)
