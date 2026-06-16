package net.thunderbird.components.ui.bolt.atom.icon

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.components.ui.bolt.PreviewWithThemes

@Preview(showBackground = true)
@Composable
internal fun IconPreview() {
    PreviewWithThemes {
        Icon(
            imageVector = Icons.Outlined.Info,
        )
    }
}

@Preview(showBackground = true)
@Composable
internal fun IconTintedPreview() {
    PreviewWithThemes {
        Icon(
            imageVector = Icons.Outlined.Info,
            tint = Color.Magenta,
        )
    }
}
