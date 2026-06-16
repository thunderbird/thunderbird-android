package net.thunderbird.components.ui.bolt.organism.drawer

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.thunderbird.components.ui.bolt.theme.MainTheme

@Composable
fun NavigationDrawerItemLayout(
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit,
) {
    Row(
        modifier = Modifier
            .then(modifier)
            .padding(
                start = MainTheme.spacings.double,
                end = MainTheme.spacings.triple,
            ),
    ) {
        content(defaultInnerPaddingValues())
    }
}

@Composable
private fun defaultInnerPaddingValues(): PaddingValues {
    return PaddingValues(
        horizontal = MainTheme.spacings.oneHalf,
    )
}
