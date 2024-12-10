package app.k9mail.core.ui.compose.designsystem.atom.image

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithTheme
import app.k9mail.core.ui.compose.designsystem.atom.icon.Icons
import app.k9mail.core.ui.compose.theme2.MainTheme

@Composable
@Preview(showBackground = true)
fun RemoteImagePreview() {
    PreviewWithTheme {
        val painter = rememberVectorPainter(Icons.Outlined.AccountCircle)
        RemoteImage(
            url = "",
            modifier = Modifier.size(MainTheme.sizes.large),
            previewPlaceholder = painter,
        )
    }
}
