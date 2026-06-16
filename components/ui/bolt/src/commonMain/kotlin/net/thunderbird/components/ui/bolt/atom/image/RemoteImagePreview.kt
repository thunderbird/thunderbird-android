package net.thunderbird.components.ui.bolt.atom.image

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.components.ui.bolt.PreviewWithTheme
import net.thunderbird.components.ui.bolt.atom.icon.Icons
import net.thunderbird.components.ui.bolt.theme.MainTheme

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
