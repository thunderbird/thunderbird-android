package app.k9mail.core.ui.compose.designsystem.atom.image

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithTheme
import app.k9mail.core.ui.compose.theme2.MainTheme

@Composable
@Preview(showBackground = true)
internal fun FixedScaleImageBottomCenterPreview() {
    PreviewWithTheme {
        Box(
            modifier = Modifier
                .width(MainTheme.sizes.huge)
                .height(MainTheme.sizes.huge),
        ) {
            FixedScaleImage(
                id = MainTheme.images.logo,
                alignment = Alignment.BottomCenter,
            )
        }
    }
}

@Composable
@Preview(showBackground = true)
internal fun FixedScaleImageCroppedPreview() {
    PreviewWithTheme {
        Box(
            modifier = Modifier
                .width(MainTheme.sizes.medium)
                .height(MainTheme.sizes.medium),
        ) {
            FixedScaleImage(
                id = MainTheme.images.logo,
            )
        }
    }
}

@Composable
@Preview(showBackground = true)
internal fun FixedScaleImageHorizontallyCroppedPreview() {
    PreviewWithTheme {
        Box(
            modifier = Modifier
                .width(MainTheme.sizes.huge)
                .height(MainTheme.sizes.medium),
        ) {
            FixedScaleImage(
                id = MainTheme.images.logo,
            )
        }
    }
}

@Composable
@Preview(showBackground = true)
internal fun FixedScaleImageVerticallyCroppedPreview() {
    PreviewWithTheme {
        Box(
            modifier = Modifier
                .width(MainTheme.sizes.medium)
                .height(MainTheme.sizes.huge),
        ) {
            FixedScaleImage(
                id = MainTheme.images.logo,
            )
        }
    }
}
