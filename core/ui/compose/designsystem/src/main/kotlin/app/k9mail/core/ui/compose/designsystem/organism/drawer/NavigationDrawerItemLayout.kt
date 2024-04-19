package app.k9mail.core.ui.compose.designsystem.organism.drawer

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.theme2.MainTheme

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
