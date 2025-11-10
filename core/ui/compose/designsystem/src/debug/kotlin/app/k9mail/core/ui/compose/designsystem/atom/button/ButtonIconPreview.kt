package app.k9mail.core.ui.compose.designsystem.atom.button

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icons

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
