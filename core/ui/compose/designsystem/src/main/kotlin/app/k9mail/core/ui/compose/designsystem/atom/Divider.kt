package app.k9mail.core.ui.compose.designsystem.atom

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.k9mail.core.ui.compose.theme.MainTheme
import app.k9mail.core.ui.compose.theme.PreviewWithThemes
import androidx.compose.material.Divider as MaterialDivider

@Composable
fun Divider(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colors.onSurface.copy(alpha = DIVIDER_ALPHA),
    thickness: Dp = 1.dp,
    startIndent: Dp = 0.dp,
) {
    MaterialDivider(
        modifier = modifier,
        color = color,
        thickness = thickness,
        startIndent = startIndent,
    )
}

private const val DIVIDER_ALPHA = 0.12f

@Preview(showBackground = true)
@Composable
internal fun DividerPreview() {
    PreviewWithThemes {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MainTheme.spacings.double),
        ) {
            Divider(
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
