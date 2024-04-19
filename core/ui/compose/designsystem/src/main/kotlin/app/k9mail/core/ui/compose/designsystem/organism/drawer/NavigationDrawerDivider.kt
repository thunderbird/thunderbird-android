package app.k9mail.core.ui.compose.designsystem.organism.drawer

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun NavigationDrawerDivider(
    modifier: Modifier = Modifier,
) {
    NavigationDrawerItemLayout(
        modifier = modifier,
    ) { paddingValues ->
        HorizontalDivider(
            modifier = Modifier
                .padding(paddingValues),
        )
    }
}
