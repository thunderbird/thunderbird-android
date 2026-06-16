package net.thunderbird.components.ui.bolt.atom.button

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.components.ui.bolt.PreviewWithThemes
import net.thunderbird.components.ui.bolt.atom.icon.Icons

@Composable
@Preview(showBackground = true)
internal fun ButtonIconPreview() {
    PreviewWithThemes {
        ButtonIcon(
            onClick = { },
            imageVector = Icons.Outlined.Info,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun ButtonIconFilledPreview() {
    PreviewWithThemes {
        ButtonIcon(
            onClick = { },
            imageVector = Icons.Filled.Cancel,
        )
    }
}
