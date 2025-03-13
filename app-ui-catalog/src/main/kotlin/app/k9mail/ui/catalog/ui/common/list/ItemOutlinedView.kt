package app.k9mail.ui.catalog.ui.common.list

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.k9mail.core.ui.compose.theme2.MainTheme

@Composable
fun ItemOutlinedView(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.default),
        modifier = Modifier
            .padding(defaultItemPadding())
            .then(modifier),
    ) {
        Box(
            modifier = Modifier
                .border(1.dp, Color.Gray)
                .fillMaxWidth(),
        ) {
            content()
        }
    }
}
