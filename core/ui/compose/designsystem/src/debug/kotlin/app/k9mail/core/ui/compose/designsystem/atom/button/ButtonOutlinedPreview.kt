package app.k9mail.core.ui.compose.designsystem.atom.button

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icons

@Composable
@Preview(showBackground = true)
internal fun ButtonOutlinedPreview() {
    PreviewWithThemes {
        ButtonOutlined(
            text = "Button Outlined",
            onClick = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun ButtonOutlinedDisabledPreview() {
    PreviewWithThemes {
        ButtonOutlined(
            text = "Button Outlined Disabled",
            onClick = {},
            enabled = false,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun ButtonOutlinedMultiLinePreview() {
    PreviewWithThemes {
        ButtonOutlined(
            text = "First\nSecond line",
            onClick = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun ButtonOutlinedIconPreview() {
    PreviewWithThemes {
        ButtonOutlined(
            text = "Button Outlined with Icon",
            icon = Icons.Outlined.Upload,
            onClick = {},
        )
    }
}
