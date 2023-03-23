package app.k9mail.core.ui.compose.designsystem.atom

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.k9mail.core.ui.compose.theme.MainTheme
import app.k9mail.core.ui.compose.theme.PreviewWithThemes
import androidx.compose.material.Card as MaterialCard

@Composable
fun Card(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    MaterialCard(
        modifier = Modifier.then(modifier),
        content = content,
    )
}

@Preview(showBackground = true)
@Composable
internal fun CardPreview() {
    PreviewWithThemes {
        Card(
            modifier = Modifier
                .width(200.dp)
                .padding(MainTheme.spacings.double),
        ) {
            Column(
                modifier = Modifier.padding(MainTheme.spacings.double),
            ) {
                Text(text = "Card")
            }
        }
    }
}
