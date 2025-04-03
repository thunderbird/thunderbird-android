package net.thunderbird.feature.navigation.drawer.siderail.ui.account

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes
import app.k9mail.core.ui.compose.theme2.MainTheme

@Composable
@Preview(showBackground = true)
internal fun SideRailAccountIndicatorPreview() {
    PreviewWithThemes {
        SideRailAccountIndicator(
            accountColor = 0,
            modifier = Modifier.height(MainTheme.spacings.double),
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun SideRailAccountIndicatorPreviewWithYellowAccountColor() {
    PreviewWithThemes {
        SideRailAccountIndicator(
            accountColor = Color.Yellow.toArgb(),
            modifier = Modifier.height(MainTheme.spacings.double),
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun SideRailAccountIndicatorPreviewWithGrayAccountColor() {
    PreviewWithThemes {
        SideRailAccountIndicator(
            accountColor = Color.Gray.toArgb(),
            modifier = Modifier.height(MainTheme.spacings.double),
        )
    }
}
